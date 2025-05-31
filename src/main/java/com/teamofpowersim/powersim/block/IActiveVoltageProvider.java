package com.teamofpowersim.powersim.block;

/**
 * Implemented by any voltage provider whose output can be ON / OFF.
 * You do *not* have to implement this on batteries or other always-on sources.
 */
public interface IActiveVoltageProvider extends IVoltageProvider {

    /** @return  true while the machine is physically producing power. */
    boolean isActive();
}
