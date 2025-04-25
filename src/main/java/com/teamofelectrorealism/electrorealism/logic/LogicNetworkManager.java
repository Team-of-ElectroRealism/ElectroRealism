package com.teamofelectrorealism.electrorealism.logic;

import java.util.*;

public class LogicNetworkManager {
    private final Map<UUID, LogicNetwork> logicNetworks = new HashMap<>();

    public void tick() {
        for (LogicNetwork network : logicNetworks.values()) {
            network.tick();
        }
    }

    public void addGateToNetwork(ILogicGate gate) {
        UUID id = gate.getNetworkId();
        LogicNetwork network = logicNetworks.computeIfAbsent(id, k -> new LogicNetwork());
        network.addGate(gate);
    }

    public void removeGateFromNetwork(ILogicGate gate) {
        UUID id = gate.getNetworkId();
        LogicNetwork network = logicNetworks.get(id);
        if (network != null) {
            network.removeGate(gate);
            if (network.getMembers().isEmpty()) {
                logicNetworks.remove(id);
            }
        }
    }

    public void reset() {
        logicNetworks.clear();
    }
}
