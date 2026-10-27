package com.primo.optimizer;

public final class ThermalPolicy {
    public static boolean shouldAlert(int previous,int current,long lastAlert,long now){return current>=2 && current>previous && now-lastAlert>=60000;}
}
