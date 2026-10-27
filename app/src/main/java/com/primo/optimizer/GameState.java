package com.primo.optimizer;

import java.util.regex.*;

/** Strict AOSP-style parser. Unknown formats fail closed before mutation. */
public final class GameState {
    public final String mode;
    public final boolean customPresent;
    public final int fps;
    public final double scale;
    private GameState(String m,boolean p,int f,double s){mode=m;customPresent=p;fps=f;scale=s;}
    public static GameState parse(String modes,String configs,String pkg) {
        Advisor.validatePackage(pkg);
        Matcher m=Pattern.compile(Pattern.quote(pkg)+" current mode: (standard|performance|battery|custom),").matcher(modes);
        if(!m.find()) throw new IllegalArgumentException("Current game mode cannot be captured; tuning blocked");
        String mode=m.group(1);
        if(!configs.contains(pkg) || (!configs.contains("Modes:") && !configs.contains("No intervention") && !configs.contains("No interventions"))) throw new IllegalArgumentException("Unrecognized config format; tuning blocked");
        Matcher custom=Pattern.compile("\\[Game Mode:4,Scaling:([-0-9.]+),Use Angle:(true|false),Fps:([0-9]+),Loading Boost Duration:([-0-9]+)\\]").matcher(configs);
        if(custom.find()) {
            // Shell can restore FPS/scale, but cannot safely reconstruct ANGLE/loading-boost overrides.
            if(!custom.group(2).equals("false") || !custom.group(4).equals("-1")) throw new IllegalArgumentException("Custom ANGLE/loading boost cannot be restored by this tool; tuning blocked");
            double scale=Double.parseDouble(custom.group(1));int fps=Integer.parseInt(custom.group(3));
            if((scale!=-1 && (scale<0.1||scale>1)) || fps<0 || fps>1000) throw new IllegalArgumentException("Unsupported original values");
            return new GameState(mode,true,fps,scale);
        }
        if(configs.contains("Game Mode:4") || mode.equals("custom")) throw new IllegalArgumentException("Custom mode capture is incomplete; tuning blocked");
        return new GameState(mode,false,0,-1);
    }
    public String restoreConfig(String pkg) {
        Advisor.validatePackage(pkg);
        return customPresent ? "cmd game set --mode 4 --downscale "+(scale==-1?"disable":Double.toString(scale))+" --fps "+fps+" "+pkg : "cmd game reset --mode 4 "+pkg;
    }
    public boolean sameValues(GameState other) {return mode.equals(other.mode) && customPresent==other.customPresent && (!customPresent || (fps==other.fps && Math.abs(scale-other.scale)<0.0001));}
}
