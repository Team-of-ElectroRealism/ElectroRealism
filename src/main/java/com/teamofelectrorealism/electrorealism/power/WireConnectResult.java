package com.teamofelectrorealism.electrorealism.power;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.client.model.CompositeModel;

public enum WireConnectResult {

    LINKED(Component.translatable("statusbar.electrorealism.wire.linked")),
    LINKED_IN(Component.translatable("statusbar.electrorealism.wire.linked_in")),
    LINKED_OUT(Component.translatable("statusbar.electrorealism.wire.linked_out")),

    REMOVED(Component.translatable("statusbar.electrorealism.wire.removed")),
    CONNECT(Component.translatable("statusbar.electrorealism.wire.connect")),
    INVALID(Component.translatable("statusbar.electrorealism.wire.invalid")),
    NO_CONNECTION(Component.translatable("statusbar.electrorealism.wire.no_connection")),
    COUNT(Component.translatable("statusbar.electrorealism.wire.count")),
    LONG(Component.translatable("statusbar.electrorealism.wire.long")),
    EXISTS(Component.translatable("statusbar.electrorealism.wire.exists")),
    REQUIRES_HIGH_CURRENT(Component.translatable("statusbar.electrorealism.wire.requires_high_current"));

    private final Component message;

    WireConnectResult(Component message) {
        this.message = message;
    }

    public Component getMessage() {
        return message;
    }

    public boolean isLinked() {
        return this == LINKED || this == LINKED_IN || this == LINKED_OUT;
    }

    public boolean isConnected() {
        return this == CONNECT;
    }

    public static WireConnectResult getLink(boolean in, boolean out) {
        if (in && !out) return LINKED_IN;
        if (!in && out) return LINKED_OUT;
        return LINKED;
    }

    public static WireConnectResult getConnect() {
        return CONNECT;
    }
}
