package com.teamofelectrorealism.electrorealism.network;

import net.minecraft.world.level.Level;

public class PowerNetwork {
    private int id;
    private boolean isValid;

    public PowerNetwork(Level level) {
        this.id = 0;
        this.isValid = true;
    }

    public int getId() {
        return this.id;
    }

    public boolean isValid() {
        return this.isValid;
    }

    public void removed() {
    }

    public void tick(int index) {
        this.id = index;
    }
}
