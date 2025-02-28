package com.teamofelectrorealism.electrorealism.power;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum WireConnectResult {
    REMOVED(Component.translatable("statusbar.electrorealism.wire.removed")),
    LINKED(Component.translatable("statusbar.electrorealism.wire.linked")),
    CONNECT(Component.translatable("statusbar.electrorealism.wire.connect"));

    private final Component message;

    WireConnectResult(Component message) {
        this.message = message;
    }

    public Component getMessage() {
        return message;
    }

    public boolean isLinked() {
        return this == LINKED;
    }

    public boolean isConnected() {
        return this == CONNECT;
    }

    public static WireConnectResult getLink() {
        return LINKED;
    }

    public static WireConnectResult getConnect() {
        return CONNECT;
    }
}
