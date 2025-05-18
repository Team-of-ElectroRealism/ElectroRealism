package com.teamofelectrorealism.electrorealism.simulation;

import com.sun.jna.*;

/**
 * JNA mapping of the ngspice API, using the same entry points KiCad does.
 */
public interface NgSpiceLibrary extends Library {

    /** ngSpice_Init( SendChar*, SendStat*, ControlledExit*, SendData*, SendInitData*, BgThreadRunning*, void* ) */
    int ngSpice_Init(
            NgSpiceSendCharCallback  sendChar,
            NgSpiceSendStatCallback  sendStat,
            NgSpiceCtrlCallback      ctrlExit,
            Pointer                  sendData,
            Pointer                  sendInitData,
            NgSpiceBgThreadCallback  bgThread,
            Pointer                  userData
    );

    /** Submit a SPICE command, e.g. “source foo.cir” or “bg_run” */
    int ngSpice_Command(String cmd);

    /** Driver call for background thread */
    int ngSpice_CircSim();

    /** Returns the name of the current plot (e.g. “tran” or “plot1”) */
    String ngSpice_CurPlot();

    /** Returns a NULL-terminated array of char* listing all vectors in the given plot */
    Pointer ngSpice_AllVecs(String plotName);

    /** Returns a pointer to a vector_info struct for the named vector */
    VectorInfo ngGet_Vec_Info(String vecName);

    /** (Optional) lock/unlock if you plan to call ngGet_Vec_Info from multiple threads */
    void ngSpice_LockRealloc();
    void ngSpice_UnlockRealloc();

    interface NgSpiceSendCharCallback extends Callback {
        int apply(String what, int id, Pointer userData);
    }
    interface NgSpiceSendStatCallback extends Callback {
        int apply(String stat, int id, Pointer userData);
    }
    interface NgSpiceCtrlCallback extends Callback {
        int apply(int status, int immediate, int exitOnQuit, int id, Pointer userData);
    }
    interface NgSpiceBgThreadCallback extends Callback {
        int apply(int finished, int id, Pointer userData);
    }

    /** Minimal mapping of ngspice’s vector_info struct (see ngspice.h) */
    class VectorInfo extends Structure {
        public int    v_length;      // number of points
        public Pointer v_realdata;   // double*
        public Pointer v_compdata;   // complex* (struct { double cx_real, cx_imag; })

        @Override
        protected java.util.List<String> getFieldOrder() {
            return java.util.List.of("v_length", "v_realdata", "v_compdata");
        }
    }
}
