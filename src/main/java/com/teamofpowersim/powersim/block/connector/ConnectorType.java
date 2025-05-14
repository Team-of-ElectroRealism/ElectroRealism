package com.teamofpowersim.powersim.block.connector;

public enum ConnectorType {
    Small("small"),
    Large("large"),
    Duo("duo");

    public final String name;

    ConnectorType(String name) {
        this.name = name;
    }
}
