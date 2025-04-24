package com.teamofelectrorealism.electrorealism.simulation;

import com.sun.jna.Pointer;
import com.teamofelectrorealism.electrorealism.simulation.NgSpiceLibrary.VectorInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Runs ngspice in-process, then pulls back all vectors via the KiCad-style API.
 */
public class NgSpiceSimulator {
    private static NgSpiceSimulator INSTANCE;
    private final NgSpiceLibrary lib;
    private volatile CountDownLatch doneLatch;

    // optional: capture console output
    private final LinkedBlockingQueue<String> consoleLines = new LinkedBlockingQueue<>();

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
    public Map<String, double[]> simulate(String netlistText)
            throws IOException, InterruptedException
    {
        // 1) dump netlist
        File tmp = File.createTempFile("er_netlist", ".cir");
        Files.writeString(tmp.toPath(), netlistText);

        // 2) clear and start
        consoleLines.clear();
        doneLatch = new CountDownLatch(1);

        // 3) source & run in background
        lib.ngSpice_Command("source " + tmp.getAbsolutePath());

        // 4) wait for controlCallback (quit)
        doneLatch.await();

        // 5) KiCad-style vector retrieval
        // … after doneLatch.await() …
        Map<String,double[]> results = new LinkedHashMap<>();
        String plot = lib.ngSpice_CurPlot();
        Pointer vecsPtr = lib.ngSpice_AllVecs(plot);
        if (vecsPtr != null) {
            // JNA will walk until it hits a NULL for us
            Pointer[] vecPointers = vecsPtr.getPointerArray(0);
            for (Pointer p : vecPointers) {
                // if you really want to be defensive:
                if (p == null) break;

                String name = p.getString(0);
                // lock around info queries just like KiCad
                lib.ngSpice_LockRealloc();
                NgSpiceLibrary.VectorInfo vi = lib.ngGet_Vec_Info(name);
                lib.ngSpice_UnlockRealloc();

                if (vi != null && vi.v_length > 0 && vi.v_realdata != null) {
                    double[] data = vi.v_realdata.getDoubleArray(0, vi.v_length);
                    results.put(name, data);
                }
            }
        }
        return results;
    }

    // — callbacks —

    private int sendCharCallback(String line, int id, Pointer ud) {
        consoleLines.add(line);
        return 0;
    }

    private int sendStatCallback(String stat, int id, Pointer ud) {
        return 0;
    }

    private int controlCallback(int status, int imm, int exitOnQuit, int id, Pointer ud) {
        doneLatch.countDown();
        return 0;
    }

    private int bgThreadCallback(int finished, int id, Pointer ud) {
        lib.ngSpice_CircSim();
        return 0;
    }
}
