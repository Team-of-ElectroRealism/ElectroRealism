package com.teamofpowersim.powersim.simulation;

import com.mojang.logging.LogUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.slf4j.Logger;

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
    private static final Logger LOGGER = LogUtils.getLogger(); // SLF4J Logger
    private static NgSpiceSimulator INSTANCE;
    private final NgSpiceLibrary lib;
    private volatile CountDownLatch doneLatch;
    private final ReentrantLock runLock = new ReentrantLock();

    private final LinkedBlockingQueue<String> consoleLines = new LinkedBlockingQueue<>();

    private static final int SYNC_SIM_TIMEOUT_SECONDS = 5;  // Increased for robustness
    private static final int ASYNC_SIM_TIMEOUT_SECONDS = 15; // Increased slightly

    private volatile Consumer<Map<String, double[]>> resultConsumerAsync; // Renamed for clarity
    private volatile Consumer<Exception> errorConsumerAsync; // Renamed for clarity
    private volatile String currentNetlistForAsyncDebugging;

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

    public Map<String, double[]> simulateSync(String netlist) throws InterruptedException, IllegalStateException {
        LOGGER.debug("simulateSync: Attempting to acquire runLock.");
        runLock.lock();
        LOGGER.debug("simulateSync: runLock acquired.");
        boolean simError = false;
        try {
            this.resultConsumerAsync = null; // Not used in sync
            this.errorConsumerAsync = null;  // Not used in sync
            this.currentNetlistForAsyncDebugging = null; // Not used for sync

            LOGGER.debug("simulateSync: Loading netlist (first 5 lines):\n{}", getFirstNLines(netlist, 5));
            loadNetlistInMemory(netlist);
            consoleLines.clear();
            doneLatch = new CountDownLatch(1);
            LOGGER.debug("simulateSync: Sending 'bg_run' command.");
            lib.ngSpice_Command("bg_run");

            LOGGER.debug("simulateSync: Awaiting doneLatch (timeout: {}s).", SYNC_SIM_TIMEOUT_SECONDS);
            if (!doneLatch.await(SYNC_SIM_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                simError = true;
                LOGGER.warn("simulateSync: doneLatch TIMEOUT after {}s. Netlist (first 5 lines):\n{}", SYNC_SIM_TIMEOUT_SECONDS, getFirstNLines(netlist, 5));
                LOGGER.warn("simulateSync: Console output during timeout: {}", String.join("\n", consoleLines));
                LOGGER.warn("simulateSync: Sending 'stop' command to un-wedge ngspice.");
                lib.ngSpice_Command("stop");
                // Short wait for 'stop' to potentially trigger callbacks that might count down the latch
                if (!doneLatch.await(200, TimeUnit.MILLISECONDS)) {
                    LOGGER.warn("simulateSync: 'stop' command did not lead to latch countdown. Simulation likely hard-stuck.");
                    throw new IllegalStateException("NgSpice sync simulation (bg_run) timed out after " + SYNC_SIM_TIMEOUT_SECONDS + "s and 'stop' command did not resolve quickly.");
                } else {
                    LOGGER.info("simulateSync: doneLatch counted down after 'stop' command. Proceeding, but results might be from incomplete/stopped sim.");
                    // If a callback (e.g. bgThreadCallback with finished=0) was triggered by 'stop',
                    // it wouldn't have had consumers to call. We proceed to readAllVectors.
                }
            }
            LOGGER.debug("simulateSync: doneLatch completed or timed out. Reading vectors.");
            return readAllVectors();
        } catch (Exception e) {
            simError = true;
            LOGGER.error("simulateSync: Exception during simulation. Netlist (first 5 lines):\n{}", getFirstNLines(netlist, 5), e);
            throw e;
        } finally {
            if (simError) {
                try {
                    LOGGER.warn("simulateSync: An error occurred. Sending 'reset' command to ngspice.");
                    lib.ngSpice_Command("reset");
                } catch (Exception resetEx) {
                    LOGGER.error("simulateSync: Exception during 'reset' command: {}", resetEx.getMessage(), resetEx);
                }
            }
            LOGGER.debug("simulateSync: Releasing runLock.");
            runLock.unlock();
        }
    }

    public void simulateAsync(String netlist,
                                                 Consumer<Map<String,double[]>> onResult,
                                                 Consumer<Exception> onError) {
        LOGGER.debug("simulateAsync_TestWithRunCommand: Scheduling for netlist (first 5 lines):\n{}", getFirstNLines(netlist, 5));
        new Thread(() -> {
            LOGGER.debug("simulateAsync_TestWithRunCommand-Thread: Attempting to acquire runLock.");
            if (!runLock.tryLock()) {
                LOGGER.warn("simulateAsync_TestWithRunCommand-Thread: runLock busy.");
                onError.accept(new IllegalStateException("NgSpiceSimulator is busy."));
                return;
            }
            LOGGER.debug("simulateAsync_TestWithRunCommand-Thread: runLock acquired.");

            boolean simErrorOrProblem = false;
            String netlistSnapshotForErrorLogging = netlist;

            try {
                this.currentNetlistForAsyncDebugging = netlist; // For any callbacks that might still fire

                LOGGER.debug("simulateAsync_TestWithRunCommand-Thread: Loading netlist.");
                loadNetlistInMemory(netlist); // This logs 'remcirc' and 'Circ' return codes
                consoleLines.clear();

                LOGGER.debug("simulateAsync_TestWithRunCommand-Thread: Sending 'run' command.");
                int runCommandReturn = lib.ngSpice_Command("run"); // "run" is blocking
                LOGGER.debug("simulateAsync_TestWithRunCommand-Thread: 'run' command returned with code: {}", runCommandReturn);

                if (runCommandReturn == 0) { // Assuming 0 is success for "run"
                    LOGGER.debug("simulateAsync_TestWithRunCommand-Thread: 'run' successful. Reading vectors.");
                    Map<String, double[]> results = readAllVectors();
                    LOGGER.debug("simulateAsync_TestWithRunCommand-Thread: Vectors read. Calling onResult.");
                    onResult.accept(results);
                } else {
                    simErrorOrProblem = true;
                    String errorMsg = "NgSpice 'run' command failed with code " + runCommandReturn + ". Console output during run:\n" + String.join("\n", consoleLines);
                    LOGGER.error(errorMsg + "\nNetlist (first 5 lines):\n{}", getFirstNLines(netlistSnapshotForErrorLogging, 5));
                    onError.accept(new IllegalStateException(errorMsg));
                }

            } catch (Exception ex) {
                simErrorOrProblem = true;
                LOGGER.error("simulateAsync_TestWithRunCommand-Thread: Exception during 'run' test. Netlist (first 5 lines):\n{}", getFirstNLines(netlistSnapshotForErrorLogging, 5), ex);
                onError.accept(ex);
            } finally {
                if (simErrorOrProblem) {
                    try {
                        LOGGER.warn("simulateAsync_TestWithRunCommand-Thread: An error or problem occurred. Sending 'reset' command.");
                        int rc_reset = lib.ngSpice_Command("reset");
                        LOGGER.warn("simulateAsync_TestWithRunCommand-Thread: 'reset' command returned {}.", rc_reset);
                    } catch (Exception resetEx) {
                        LOGGER.error("simulateAsync_TestWithRunCommand-Thread: Exception during 'reset': {}", resetEx.getMessage(), resetEx);
                    }
                }
                this.currentNetlistForAsyncDebugging = null;
                LOGGER.debug("simulateAsync_TestWithRunCommand-Thread: Releasing runLock.");
                runLock.unlock();
            }
        }, "NgSpice-AsyncSim-TestRun-Handler").start();
    }

    private Map<String,double[]> readAllVectors() {
        // ... (no changes, this method seems fine)
        Map<String,double[]> out = new HashMap<>();
        String plot = lib.ngSpice_CurPlot();
        if (plot == null || plot.isEmpty()) {
            LOGGER.warn("readAllVectors: Current plot is null or empty. No vectors to read. Console output:\n{}", String.join("\n", consoleLines));
            return out; // Return empty map if no plot (e.g., after reset or error)
        }
        Pointer vecs = lib.ngSpice_AllVecs(plot);
        if (vecs == null) {
            LOGGER.warn("readAllVectors: ngSpice_AllVecs returned null for plot '{}'. Console output:\n{}", plot, String.join("\n", consoleLines));
            return out;
        }
        long step = Native.POINTER_SIZE, off = 0;

        while (true) {
            Pointer p = vecs.getPointer(off);
            if (Pointer.nativeValue(p) == 0) break;
            String name = p.getString(0);

            safeLockRealloc();
            Pointer infoPtr = lib.ngGet_Vec_Info(name, plot); // Pass plot name
            if (infoPtr == null) {
                LOGGER.warn("readAllVectors: ngGet_Vec_Info for vector '{}' in plot '{}' returned null. Skipping vector.", name, plot);
                safeUnlockRealloc();
                off += step;
                continue;
            }
            NgSpiceLibrary.VectorInfo vi = new NgSpiceLibrary.VectorInfo(infoPtr);
            vi.read(); // Make sure this reads the fields from the pointer
            if (vi.v_realdata == null || vi.v_length <= 0) {
                LOGGER.warn("readAllVectors: Vector '{}' in plot '{}' has no real data or zero length. Length: {}, Data Ptr: {}. Skipping.", name, plot, vi.v_length, vi.v_realdata);
                safeUnlockRealloc();
                off += step;
                continue;
            }
            double[] data = vi.v_realdata.getDoubleArray(0, vi.v_length);
            safeUnlockRealloc();

            out.put(name, data);
            off += step;
        }
        return out;
    }

    private void loadNetlistInMemory(String net) {
        String[] lines = net.split("\\R");
        int n = lines.length;

        Memory tbl = new Memory((long) (n + 1) * Native.POINTER_SIZE);
        for (int i = 0; i < n; i++) {
            byte[] bytes = Native.toByteArray(lines[i] + '\n');
            Memory cstr  = new Memory(bytes.length);
            cstr.write(0, bytes, 0, bytes.length);
            tbl.setPointer((long) i * Native.POINTER_SIZE, cstr);
        }
        tbl.setPointer((long) n * Native.POINTER_SIZE, Pointer.NULL);

        LOGGER.trace("loadNetlistInMemory: Sending 'remcirc' command.");
        lib.ngSpice_Command("remcirc");
        LOGGER.trace("loadNetlistInMemory: Sending 'ngSpice_Circ' command.");
        int rc = lib.ngSpice_Circ(tbl);
        if (rc != 0) {
            String errorMsg = "ngSpice_Circ failed with code " + rc + ". Console:\n" + String.join("\n", consoleLines);
            LOGGER.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        LOGGER.trace("loadNetlistInMemory: ngSpice_Circ successful.");
    }

    /**
     * Returns true while ngspice says a background run is active.
     * Works on DLLs that export either ngSpice_Running or ngSpice_running.
     */
    private boolean isNgSpiceStillRunning() {
        try {
            return lib.ngSpice_Running() != 0;
        } catch (UnsatisfiedLinkError e1) {
            try {
                java.lang.reflect.Method m =
                        lib.getClass().getMethod("ngSpice_running");
                Object ret = m.invoke(lib);
                return ((Integer) ret) != 0;
            } catch (Throwable e2) {
                LOGGER.warn("Could not determine if ngspice is running (both ngSpice_Running and ngSpice_running symbols missing/failed). Assuming not running.", e2);
                return false;
            }
        }
    }

    private void safeLockRealloc() {
        try { lib.ngSpice_LockRealloc(); }
        catch (UnsatisfiedLinkError ignore) { }
    }

    private void safeUnlockRealloc() {
        try { lib.ngSpice_UnlockRealloc(); }
        catch (UnsatisfiedLinkError ignore) { }
    }

    private String getFirstNLines(String text, int n) {
        if (text == null) return " (null netlist) ";
        String[] lines = text.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(n, lines.length); i++) {
            sb.append(lines[i]).append("\n");
        }
        if (lines.length > n) {
            sb.append("... (").append(lines.length - n).append(" more lines)\n");
        }
        return sb.toString();
    }

    // — callbacks —

    private int sendCharCallback(String line, int id, Pointer ud) {
        consoleLines.add(line);
        LOGGER.trace("NGSPICE_CONSOLE_OUT [ID:{}]: {}", id, line); // More detailed logging
        return 0;
    }

    private int sendStatCallback(String stat, int id, Pointer ud) {
        LOGGER.trace("NGSPICE_STAT [ID:{}]: {}", id, stat);
        return 0;
    }

    private int bgThreadCallback(int finished, int id, Pointer ud) {
        LOGGER.debug("bgThreadCallback: Invoked. finished={}, id={}", finished, id);
        if (finished == 1) { // Success
            if (this.resultConsumerAsync != null) {
                try {
                    LOGGER.debug("bgThreadCallback: Simulation successful. Reading vectors.");
                    Map<String, double[]> results = readAllVectors();
                    LOGGER.debug("bgThreadCallback: Vectors read. Calling resultConsumerAsync.");
                    this.resultConsumerAsync.accept(results);
                } catch (Exception e) {
                    LOGGER.error("bgThreadCallback: Exception during result processing or readAllVectors. Netlist (first 5 lines):\n{}", getFirstNLines(this.currentNetlistForAsyncDebugging, 5), e);
                    if (this.errorConsumerAsync != null) {
                        this.errorConsumerAsync.accept(e);
                    }
                }
            } else {
                LOGGER.warn("bgThreadCallback: Simulation successful but resultConsumerAsync is null (id={}). This might be normal if called from sync path that timed out then recovered.", id);
            }
        } else { // Failure or stop
            String errorMsg = "NgSpice background task failed or was stopped. finished_code=" + finished + ", id=" + id + ". Console:\n" + String.join("\n", consoleLines);
            LOGGER.error("bgThreadCallback: Simulation failed/stopped. finished={}, id={}. Netlist (first 5 lines):\n{}", finished, id, getFirstNLines(this.currentNetlistForAsyncDebugging, 5));
            if (this.errorConsumerAsync != null) {
                this.errorConsumerAsync.accept(new IllegalStateException(errorMsg));
            } else {
                LOGGER.warn("bgThreadCallback: Simulation failed but errorConsumerAsync is null (id={}). Might be from sync path.", id);
            }
        }
        if (doneLatch != null) {
            LOGGER.debug("bgThreadCallback: Counting down doneLatch.");
            doneLatch.countDown();
        } else {
            LOGGER.warn("bgThreadCallback: doneLatch was null when trying to count down (id={}).", id);
        }
        return 0;
    }

    private int controlCallback(int status, int imm, int exitOnQuit, int id, Pointer ud) {
        // This callback indicates a change in ngspice's control state (e.g., ngspice is quitting).
        // It's generally an error condition from our perspective if it happens unexpectedly during a simulation.
        String errorMsg = "NgSpice control state changed (e.g., ngspice quit). status=" + status + ", id=" + id + ". Console:\n" + String.join("\n", consoleLines);
        LOGGER.error("controlCallback: Invoked. status={}, id={}. Netlist (first 5 lines):\n{}", status, id, getFirstNLines(this.currentNetlistForAsyncDebugging, 5));
        if (this.errorConsumerAsync != null) {
            this.errorConsumerAsync.accept(new IllegalStateException(errorMsg));
        } else {
            LOGGER.warn("controlCallback: Control state changed but errorConsumerAsync is null (id={}). Might be from sync path.", id);
        }
        if (doneLatch != null) {
            LOGGER.debug("controlCallback: Counting down doneLatch.");
            doneLatch.countDown();
        } else {
            LOGGER.warn("controlCallback: doneLatch was null when trying to count down (id={}).", id);
        }
        return 0;
    }
}
