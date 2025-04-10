package com.teamofelectrorealism.electrorealism.network;

import java.util.List;
import java.util.Map;

public class NetlistBuilder {
    void buildNetlist(Map<INetworkMember, List<ConnectionInfo>> adjacencyList) {
        // go through each networkmember
        // if powersource get intRes, CurrentOutput
        // if poweruser get intRes
        // if connector, figure out resWire and length
        // go through each ConnectionInfo object in list translate to netlist
    }
}
