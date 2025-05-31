package com.teamofpowersim.powersim.simulation;

import com.sun.jna.*;

import java.util.List;

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

    int         ngSpice_Command(String cmd);
    int         ngSpice_CircSim();
    String      ngSpice_CurPlot();
    Pointer     ngSpice_AllVecs(String plotName);
    Pointer     ngGet_Vec_Info(String vecName, String plotName);
    void        ngSpice_LockRealloc();
    void        ngSpice_UnlockRealloc();
    int         ngSpice_Circ(Pointer /*char**/ circArray);
    Pointer     ngSpice_AllPlots();                  // NULL-terminated array of plot names
    int         ngSpice_Running();                   // 0 = idle, 1 = running


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
    @Structure.FieldOrder({"v_name","v_type","v_flags","v_realdata","v_compdata","v_length"})
    class VectorInfo extends Structure {

        public VectorInfo() {}                     // no-arg still allowed
        public VectorInfo(Pointer p) {             // <-- add this
            super(p);
        }

        public String  v_name;     // char*
        public int     v_type;
        public int     v_flags;
        public Pointer v_realdata; // double*
        public Pointer v_compdata; // complex*
        public int     v_length;
    }
}
