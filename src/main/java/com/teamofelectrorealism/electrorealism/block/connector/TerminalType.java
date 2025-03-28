package com.teamofelectrorealism.electrorealism.block.connector;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public enum TerminalType implements StringRepresentable {
    Positive("positive"),
    Negative("negative"),
    None("none");

    private final String name;

    TerminalType(String name) {
        this.name = name;
    }

    /**
     * Gets the lowercase name used for serialization (e.g., in NBT).
     * @return The serialized name ("positive", "negative", or "none").
     */
    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }

    /**
     * Gets the TerminalType enum constant from its serialized name.
     * Used for loading from NBT or other string representations.
     * @param name The serialized name ("positive", "negative", or "none").
     * @return The corresponding TerminalType, or NONE if no specific match is found.
     */
    public static TerminalType fromName(String name) {
        if (name == null) {
            return None; // Default to NONE if name is null
        }
        for (TerminalType type : values()) {
            if (type.getSerializedName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return None; // Default to NONE if name doesn't match known types
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Cycles to the next terminal type in the order: NONE -> POSITIVE -> NEGATIVE -> NONE.
     * @return The next TerminalType in the cycle.
     */
    public TerminalType getNext() {
        return switch (this) {
            case None -> Positive;
            case Positive -> Negative;
            case Negative -> None;
        };
    }
}
