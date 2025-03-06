package com.teamofelectrorealism.electrorealism.network;

import com.teamofelectrorealism.electrorealism.power.LocalNode;
import net.minecraft.world.level.Level;

public class Connection {
    private int id;
    private boolean isValid;

    private LocalNode sourceNode;
    private LocalNode targetNode;
    private Network network;

    public Connection(Level level, LocalNode sourceNode, LocalNode targetNode) {
        this.id = 0;
        this.isValid = true;
        this.sourceNode = sourceNode;
        this.targetNode = targetNode;

        this.network = NetworkManager.instances.get(level).findOrCreateNetwork(sourceNode);
        network.addConnection(this);
    }

    public LocalNode getSourceNode() {
        return sourceNode;
    }

    public LocalNode getTargetNode() {
        return targetNode;
    }

    public int getId() {
        return id;
    }

    public boolean isValid() {
        return isValid;
    }

    public void removed() {
    }

    public void invalidate() {
        this.isValid = false;
    }

    public void tick(int index) {
        this.id = index;
    }
}
