package com.teamofelectrorealism.electrorealism.block.connector;

public enum ConnectorType {
    Small("small"),
    Large("large"),
    Duo("duo");

    public final String name;

    ConnectorType(String name) {
        this.name = name;
    }
}
