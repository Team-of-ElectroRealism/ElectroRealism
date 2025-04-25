package com.teamofelectrorealism.electrorealism.logic;

import com.teamofelectrorealism.electrorealism.network.INetworkMember;

public interface ILogicGate extends INetworkMember {
    /**
     * Get the current output state of this logic gate.
     */
    boolean getOutputState();

    /**
     * Update the input state(s) to this logic gate.
     *
     * @param inputs An array representing each input's ON/OFF state.
     */
    void updateInputState(boolean[] inputs);

    /**
     * The type of logic gate (AND, OR, NOT, etc.).
     */
    LogicGateType getLogicType();
}
