package com.teamofpowersim.powersim.simulation;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Runs ngspice in-process, then pulls back all vectors via the KiCad-style API.
 */
public class NgSpiceSimulator {
    private static NgSpiceSimulator INSTANCE;
    private final NgSpiceLibrary lib;
    private volatile CountDownLatch doneLatch;
    private final ReentrantLock runLock = new ReentrantLock();

    // optional: capture console output
    private final LinkedBlockingQueue<String> consoleLines = new LinkedBlockingQueue<>();

    private static final int ASYNC_TIMEOUT_SEC = 2;

    private volatile Consumer<Map<String, double[]>> resultConsumer;
    private volatile Consumer<Exception> errorConsumer;
    private volatile String currentNetlistForAsync;

    private NgSpiceSimulator(NgSpiceLibrary lib) {
        this.lib = lib;
        lib.ngSpice_Init(
                this::sendCharCallback,
                this::sendStatCallback,
                this::controlCallback,
                null,    // no streaming data callback
                null,    // no init-data callback
                this::bgThreadCallback,
                null
        );
    }

    public static synchronized NgSpiceSimulator instance() throws IOException {
        if (INSTANCE == null) {
            INSTANCE = new NgSpiceSimulator(NgSpiceLoader.get());
        }
        return INSTANCE;
    }

    /**
     * Write netlist, run bg_run, wait for quit, then query all vectors.
     */
    public Map<String, double[]> simulateSync(String netlist) throws InterruptedException, IllegalStateException {
        runLock.lock(); // Block until lock is available
        try {
            // Set a flag indicating this is a sync call, so callbacks don't try to manage async consumers
            this.resultConsumer = null; // Ensure async consumers are null
            this.errorConsumer = null;

            loadNetlistInMemory(netlist);
            consoleLines.clear();
            doneLatch = new CountDownLatch(1);
            lib.ngSpice_Command("bg_run");

            if (!doneLatch.await(2, TimeUnit.SECONDS)) {
                // ... timeout logic (try to stop, throw) ...
                // If quit was called, controlCallback would have run.
            }
            // doneLatch was counted down by bgThreadCallback or controlCallback.
            // At this point, bg_run is finished OR ngspice has quit.
            return readAllVectors();
        } finally {
            runLock.unlock(); // simulateSync ALWAYS releases its own lock.
        }
    }

    public void simulateAsync(String netlist,
                              Consumer<Map<String, double[]>> onResult,
                              Consumer<Exception> onError) {
        if (!runLock.tryLock()) {                       // probe only
            onError.accept(new IllegalStateException("NgSpiceSimulator is busy."));
            return;
        }
        runLock.unlock();
        // Lock acquired for async. MUST be released by a callback or the async thread's finally block.
        new Thread(() -> {
            try {
                runLock.lock();                         // <- we own the lock now
                this.resultConsumer = onResult;
                this.errorConsumer  = onError;
                this.currentNetlistForAsync = netlist;

                loadNetlistInMemory(netlist);
                consoleLines.clear();
                doneLatch = new CountDownLatch(1);

                lib.ngSpice_Command("bg_run");

                if (!doneLatch.await(ASYNC_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    onError.accept(
                            new IllegalStateException(
                                    "Async simulation (bg_run) timed out after "
                                            + ASYNC_TIMEOUT_SEC + " s."));
                    return;                             // we unlock in finally
                }
            /* normal success path:
               bgThreadCallback will already have delivered onResult */
            } catch (Exception ex) {
                onError.accept(ex);
            } finally {
                /* clear references even on error */
                resultConsumer = null;
                errorConsumer  = null;
                runLock.unlock();                      // always the same owner
            }
        }, "NgSpice-AsyncSim-Handler").start();
    }

    private Map<String,double[]> readAllVectors() {
        Map<String,double[]> out = new HashMap<>();
        String plot = lib.ngSpice_CurPlot();
        Pointer vecs = lib.ngSpice_AllVecs(plot);   // NULL-terminated char**
        long step = Native.POINTER_SIZE, off = 0;

        while (true) {
            Pointer p = vecs.getPointer(off);
            if (Pointer.nativeValue(p) == 0) break;
            String name = p.getString(0);

            // Lock the memory while we read the vector
            safeLockRealloc();

            Pointer infoPtr = lib.ngGet_Vec_Info(name, plot);
            NgSpiceLibrary.VectorInfo vi = new NgSpiceLibrary.VectorInfo(infoPtr);
            vi.read();
            double[] data = vi.v_realdata.getDoubleArray(0, vi.v_length);

            // Unlock the memory after we read the vector
            safeUnlockRealloc();

            out.put(name, data);
            off += step;
        }
        return out;
    }

    /**
     * Builds the char** expected by ngSpice_Circ from a Java String.
     * Each line is a C-string; the table is NULL-terminated.
     */
    private void loadNetlistInMemory(String net) {
        String[] lines = net.split("\\R");
        int n = lines.length;

        Memory tbl = new Memory((long) (n + 1) * Native.POINTER_SIZE);
        for (int i = 0; i < n; i++) {
            byte[] bytes = Native.toByteArray(lines[i] + '\n');   // keep EOL
            Memory cstr  = new Memory(bytes.length);
            cstr.write(0, bytes, 0, bytes.length);
            tbl.setPointer((long) i * Native.POINTER_SIZE, cstr);
        }
        tbl.setPointer((long) n * Native.POINTER_SIZE, Pointer.NULL);

        lib.ngSpice_Command("remcirc");        // wipe previous circuit
        int rc = lib.ngSpice_Circ(tbl);        // push the new one
        if (rc != 0) throw new IllegalStateException("ngSpice_Circ failed (" + rc + ')');
    }

    /**
     * Returns true while ngspice says a background run is active.
     * Works on DLLs that export either ngSpice_Running or ngSpice_running.
     */
    private boolean isNgSpiceStillRunning() {
        try {                                         // most recent builds
            return lib.ngSpice_Running() != 0;
        } catch (UnsatisfiedLinkError e1) {
            try {                                     // older builds
                // invoke via reflection to avoid adding a duplicate method
                java.lang.reflect.Method m =
                        lib.getClass().getMethod("ngSpice_running");
                Object ret = m.invoke(lib);
                return ((Integer) ret) != 0;          // both versions return int
            } catch (Throwable e2) {
                return false;                         // symbol absent → assume idle
            }
        }
    }

    private void safeLockRealloc() {
        try { lib.ngSpice_LockRealloc(); }           // newer DLLs
        catch (UnsatisfiedLinkError ignore) { }      // old DLLs
    }

    private void safeUnlockRealloc() {
        try { lib.ngSpice_UnlockRealloc(); }
        catch (UnsatisfiedLinkError ignore) { }
    }

    private void safeUnlockRunLock() {
        if (runLock.isHeldByCurrentThread()) {
            runLock.unlock();
        }
    }

    // — callbacks —

    private int sendCharCallback(String line, int id, Pointer ud) {
        consoleLines.add(line);
        // System.out.println("NGSPICE_LOG: " + line); // For live debugging
        return 0;
    }

    private int sendStatCallback(String stat, int id, Pointer ud) {
        return 0;
    }

    // controlCallback is called when ngspice quits (e.g. after "quit" command or fatal error)
    private int controlCallback(int status, int imm, int exitOnQuit, int id, Pointer ud) {
        NgSpiceLoader.invalidate(); // If ngspice exits, our current instance is no longer valid
        if (doneLatch != null) {
            doneLatch.countDown(); // Signal completion or failure
        }

        // If this was an async simulation, and bgThreadCallback hasn't handled it:
        if (resultConsumer != null || errorConsumer != null) {
            // Ngspice quit, implying the simulation ended (possibly prematurely or with error)
            // Try to read vectors if not already done by bgThreadCallback.
            // However, if ngspice truly quit, readAllVectors might fail.
            // It's safer to assume an error if bgThreadCallback didn't fire with success.
            if (errorConsumer != null) {
                // Check if consoleLines has error messages
                StringBuilder errors = new StringBuilder();
                consoleLines.forEach(line -> { if (line.toLowerCase().contains("error")) errors.append(line).append("\n"); });
                if (!errors.isEmpty()) {
                    errorConsumer.accept(new IllegalStateException("NgSpice quit during async simulation. Errors:\n" + errors));
                } else {
                    errorConsumer.accept(new IllegalStateException("NgSpice quit unexpectedly during async simulation. Status: " + status));
                }
            }
            resultConsumer = null; // Clear consumers
            errorConsumer = null;
        }
        runLock.unlock(); // Ensure lock is released
        return 0;
    }

    // bgThreadCallback is called when a background task (like bg_run) finishes
    private int bgThreadCallback(int finished, int id, Pointer ud) {
        if (finished == 1) { // 1 means success for bg_run
            if (resultConsumer != null) { // Async simulation
                try {
                    Map<String, double[]> results = readAllVectors();
                    resultConsumer.accept(results);
                } catch (Exception e) {
                    if (errorConsumer != null) {
                        errorConsumer.accept(e);
                    }
                } finally {
                    resultConsumer = null; // Clear consumers
                    errorConsumer = null;
                    safeUnlockRunLock();
                }
            }
        } else if (finished == 0 || finished < 0) { // 0 means error or interruption for bg_run
            if (errorConsumer != null) { // Async simulation error
                StringBuilder errors = new StringBuilder();
                consoleLines.forEach(line -> { if (line.toLowerCase().contains("error")) errors.append(line).append("\n"); });
                if (!errors.isEmpty()){
                    errorConsumer.accept(new IllegalStateException("NgSpice background task failed. Errors:\n" + errors));
                } else {
                    errorConsumer.accept(new IllegalStateException("NgSpice background task (bg_run) failed or was interrupted. Finished code: " + finished));
                }
                resultConsumer = null; // Clear consumers
                errorConsumer = null;
                safeUnlockRunLock();
            }
        }

        if (doneLatch != null) {
            doneLatch.countDown(); // Signal completion for both sync and async internal waiting
        }
        return 0;
    }
}
