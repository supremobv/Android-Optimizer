package com.primo.optimizer;

import java.util.*;

/** Imports measured frame durations, never synthetic or optimizer-window frame rates. */
public final class FrameStats {
    public final int count;
    public final double fps, p95ms, missedPercent;
    private FrameStats(int n,double f,double p,double j) {count=n;fps=f;p95ms=p;missedPercent=j;}
    public static FrameStats parse(String csv,int target) {
        if(csv==null || csv.length()>2000000 || target<1 || target>120) throw new IllegalArgumentException("Invalid frame data or target");
        String[] lines=csv.trim().split("\\r?\\n");
        if(lines.length<3 || !lines[0].trim().replace("\ufeff", "").equals("frame_time_ms")) throw new IllegalArgumentException("Expected one-column CSV headed frame_time_ms and at least two frames");
        ArrayList<Double> values=new ArrayList<>(); double total=0;int misses=0;
        for(int i=1;i<lines.length;i++) {
            if(lines[i].trim().isEmpty()) continue;
            double ms;
            try{ms=Double.parseDouble(lines[i].trim());}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid frame on row "+(i+1));}
            if(Double.isNaN(ms)||Double.isInfinite(ms)||ms<=0||ms>60000) throw new IllegalArgumentException("Invalid duration on row "+(i+1));
            values.add(ms);total+=ms;if(ms>1000.0/target*1.05) misses++;
        }
        if(values.size()<2) throw new IllegalArgumentException("Need at least two valid frames");
        Collections.sort(values);int n=values.size();
        return new FrameStats(n,1000*n/total,values.get((int)Math.ceil(n*0.95)-1),100.0*misses/n);
    }
    public static boolean comparable(String beforeScene,String afterScene,long beforeMs,long afterMs) {
        if(beforeScene==null || beforeScene.trim().isEmpty() || !beforeScene.trim().equalsIgnoreCase(afterScene==null?"":afterScene.trim())) return false;
        return beforeMs>=60000 && afterMs>=60000 && afterMs/(double)beforeMs>=0.8 && afterMs/(double)beforeMs<=1.25;
    }
}
