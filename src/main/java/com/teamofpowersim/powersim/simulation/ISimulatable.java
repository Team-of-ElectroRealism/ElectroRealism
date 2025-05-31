package com.teamofpowersim.powersim.simulation;

/**
 * Implemented by every BlockEntity that wants ngspice
 * results (voltage + current) pushed into it after a run.
 */
public interface ISimulatable {

    /** Called once per simulation step (or once, if you only run DC). */
    void applySimulation(double voltage, double current);
}
