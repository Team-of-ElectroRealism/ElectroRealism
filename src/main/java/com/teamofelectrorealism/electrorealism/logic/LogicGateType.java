package com.teamofelectrorealism.electrorealism.logic;

public enum LogicGateType {
    AND, OR, NOT, XOR, NAND, NOR;

    public boolean compute(boolean[] inputs) {
        return switch (this) {
            case AND -> inputs.length > 0 && allTrue(inputs);
            case OR -> anyTrue(inputs);
            case NOT -> inputs.length == 1 && !inputs[0];
            case XOR -> xor(inputs);
            case NAND -> !allTrue(inputs);
            case NOR -> !anyTrue(inputs);
        };
    }

    private boolean allTrue(boolean[] inputs) {
        for (boolean b : inputs) if (!b) return false;
        return true;
    }

    private boolean anyTrue(boolean[] inputs) {
        for (boolean b : inputs) if (b) return true;
        return false;
    }

    private boolean xor(boolean[] inputs) {
        boolean result = false;
        for (boolean b : inputs) result ^= b;
        return result;
    }
}
