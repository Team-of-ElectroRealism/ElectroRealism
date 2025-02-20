package com.teamofelectrorealism.electrorealism.block;

public interface IVoltageReceiver {
    int getResistance();

    void receiveVoltage(int voltage);

    int getBufferCharge();

    void setBufferCharge(int charge);
}
