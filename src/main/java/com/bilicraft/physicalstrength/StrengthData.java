package com.bilicraft.physicalstrength;

import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class StrengthData {
    private long strength;
    private long lastConsume;

    public StrengthData(long strength, long lastConsume){
        this.strength = strength;
        this.lastConsume = lastConsume;
    }

    public void setLastConsume(long lastConsume) {
        this.lastConsume = lastConsume;
    }

    public void setStrength(long strength) {
        this.strength = strength;
    }

    public long getLastConsume() {
        return lastConsume;
    }

    public long getStrength() {
        return strength;
    }

    public boolean tick(int size) {
        long newStrength = strength - size;
        if (newStrength < 0) {
            strength = 0; //设置新值
            return false;
        }
        strength = newStrength;
        lastConsume = System.currentTimeMillis();
        return true;
    }
}
