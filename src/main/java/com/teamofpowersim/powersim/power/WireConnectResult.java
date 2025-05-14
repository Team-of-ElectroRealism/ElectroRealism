package com.teamofpowersim.powersim.power;

import net.minecraft.network.chat.Component;

public enum WireConnectResult {

    LINKED(Component.translatable("statusbar.powersim.wire.linked")),
    LINKED_IN(Component.translatable("statusbar.powersim.wire.linked_in")),
    LINKED_OUT(Component.translatable("statusbar.powersim.wire.linked_out")),

    CONNECT(Component.translatable("statusbar.powersim.wire.connect")),
    CONNECT_IN(Component.translatable("statusbar.powersim.wire.connect_in")),
    CONNECT_OUT(Component.translatable("statusbar.powersim.wire.connect_out")),

    REMOVED(Component.translatable("statusbar.powersim.wire.removed")),
    INVALID(Component.translatable("statusbar.powersim.wire.invalid")),
    NO_CONNECTION(Component.translatable("statusbar.powersim.wire.no_connection")),
    COUNT(Component.translatable("statusbar.powersim.wire.count")),
    LONG(Component.translatable("statusbar.powersim.wire.long")),
    EXISTS(Component.translatable("statusbar.powersim.wire.exists")),
    ERROR(Component.translatable("statusbar.powersim.wire.error")),
    REQUIRES_HIGH_CURRENT(Component.translatable("statusbar.powersim.wire.requires_high_current"));

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
        return this == CONNECT || this == CONNECT_IN || this == CONNECT_OUT;
    }

    public static WireConnectResult getLink(boolean in, boolean out) {
        if (in && !out) return LINKED_IN;
        if (!in && out) return LINKED_OUT;
        return LINKED;
    }

    public static WireConnectResult getConnect(boolean in, boolean out) {
        if(in && !out) return CONNECT_IN;
        if(!in && out) return CONNECT_OUT;
        return CONNECT;
    }
}
