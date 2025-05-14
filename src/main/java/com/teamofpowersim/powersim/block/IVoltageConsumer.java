package com.teamofpowersim.powersim.block;

public interface IVoltageConsumer {
    int getResistance();

    void receiveVoltage(int voltage);

    int getBufferCharge();

    void setBufferCharge(int charge);
}
