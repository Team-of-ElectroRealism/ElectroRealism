package com.teamofelectrorealism.electrorealism.logic;

import java.util.*;

public class LogicNetwork {
    private final UUID id = UUID.randomUUID();
    private final Set<ILogicGate> gates = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public void addGate(ILogicGate gate) {
        if (gate != null) {
            gates.add(gate);
        }
    }

    public void removeGate(ILogicGate gate) {
        if (gate != null) {
            gates.remove(gate);
        }
    }

    /**
     * Called every tick to simulate the logic gate state changes.
     */
    public void tick() {
        for (ILogicGate gate : gates) {
            boolean[] inputs = gatherInputs(gate);
            gate.updateInputState(inputs);
        }
    }

    /**
     * This should be replaced with a system that scans connected nodes.
     * For now, it's just a stub.
     */
    private boolean[] gatherInputs(ILogicGate gate) {
        // TODO: Pull signal state from actual connected wires
        return new boolean[] { false, false }; // placeholder
    }

    public Set<ILogicGate> getMembers() {
        return Collections.unmodifiableSet(gates);
    }
}
