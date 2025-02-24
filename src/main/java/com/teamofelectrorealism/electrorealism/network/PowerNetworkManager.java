package com.teamofelectrorealism.electrorealism.network;

import java.util.ArrayList;
import java.util.List;

public class PowerNetworkManager {
    private List<PowerNetwork> networks;

    public PowerNetworkManager() {
        networks = new ArrayList<PowerNetwork>();
    }

    public void addNetwork(PowerNetwork network) {
        networks.add(network);
    }

    public void tick() {
        List<PowerNetwork> validNetworks = new ArrayList<PowerNetwork>();

        for (int i = 0; i < networks.size(); i++) {

            PowerNetwork powerNetwork = networks.get(i);
            if (powerNetwork.isValid()) {
                powerNetwork.tick(i);
                validNetworks.add(powerNetwork);
            }
            powerNetwork.removed();
        }
        networks = validNetworks;
    }
}
