package com.primo.optimizer;

public final class FeatureLogicTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void rejects(Runnable r,String message){boolean rejected=false;try{r.run();}catch(IllegalArgumentException e){rejected=true;}check(rejected,message);}
    public static void main(String[] args){
        FrameStats weighted=FrameStats.parse("frame_time_ms\n10\n30\n",60);
        check(weighted.count==2 && Math.abs(weighted.fps-50)<0.0001,"FPS must use total frames / total duration, not average instantaneous FPS");
        check(weighted.p95ms==30,"Nearest rank p95");check(weighted.missedPercent==50,"Frame budget misses with tolerance");
        check(FrameStats.parse("\ufeffframe_time_ms\r\n16.67\r\n16.67",60).missedPercent==0,"BOM and CRLF accepted");
        for(String csv:new String[]{"fps\n60\n60","frame_time_ms\n16","frame_time_ms\nNaN\n16","frame_time_ms\nInfinity\n16","frame_time_ms\n0\n16","frame_time_ms\n-5\n16","frame_time_ms\n60001\n16","frame_time_ms\n16,17\n16"})rejects(()->FrameStats.parse(csv,60),"Reject malformed/non-measured input");
        rejects(()->FrameStats.parse("frame_time_ms\n16\n16",0),"Reject invalid target");
        check(FrameStats.comparable("Route A","route a",100000,80000),"Same scene lower duration boundary");
        check(FrameStats.comparable(" Route A ","route a",80000,100000),"Trim and upper duration boundary");
        check(!FrameStats.comparable("a","b",100000,100000),"Different scenes cannot compare");
        check(!FrameStats.comparable("a","a",59000,59000),"Short runs cannot compare");
        check(!FrameStats.comparable("a","a",100000,125001),"Disparate durations cannot compare");
        check(!FrameStats.comparable("","",100000,100000),"Unnamed scenes cannot compare");
        String pkg="com.example.game",standard=pkg+" current mode: standard, available game modes: [standard,custom]";
        String absent=pkg+" interventions: [Name:"+pkg+" Modes: {}]";
        GameState original=GameState.parse(standard,absent,pkg);
        check(!original.customPresent && original.mode.equals("standard"),"Capture absent custom config");
        check(original.restoreConfig(pkg).equals("cmd game reset --mode 4 "+pkg),"Reset only custom mode, preserve other mode overrides");
        String config=pkg+" interventions: [Name:"+pkg+" Modes: {4=[Game Mode:4,Scaling:0.85,Use Angle:false,Fps:45,Loading Boost Duration:-1]}]";
        GameState custom=GameState.parse(standard.replace("mode: standard,","mode: custom,"),config,pkg);
        check(custom.fps==45&&custom.scale==0.85,"Capture prior custom FPS and scale");
        check(custom.restoreConfig(pkg).contains("--mode 4 --downscale 0.85 --fps 45"),"Restore actual captured values, not defaults");
        check(custom.sameValues(GameState.parse(standard.replace("mode: standard,","mode: custom,"),config,pkg)),"Round-trip read-back");
        check(!custom.sameValues(original),"Detect external mode/config changes");
        GameState disabled=GameState.parse(standard,config.replace("0.85","-1.0").replace("Fps:45","Fps:0"),pkg);
        check(disabled.restoreConfig(pkg).contains("--downscale disable --fps 0"),"Preserve disabled scaling and uncapped FPS");
        rejects(()->GameState.parse(standard,config.replace("Use Angle:false","Use Angle:true"),pkg),"Do not overwrite unsupported ANGLE settings");
        rejects(()->GameState.parse(standard,config.replace("Duration:-1","Duration:100"),pkg),"Do not overwrite unsupported loading boost");
        rejects(()->GameState.parse(standard,"unknown OEM output",pkg),"Unknown OEM format fails closed");
        rejects(()->GameState.parse("permission denied",absent,pkg),"Cannot tune without capturing mode");
        rejects(()->GameState.parse(standard,config.replace("Scaling:0.85","Scaling:9.0"),pkg),"Reject unsupported old values");
        rejects(()->GameState.parse(standard,config,pkg+";id"),"Package injection rejected in snapshot parser");
        check(ThermalPolicy.shouldAlert(0,2,0,60000),"Alert on rising moderate pressure");
        check(!ThermalPolicy.shouldAlert(0,1,0,60000),"Do not alert on light pressure");
        check(!ThermalPolicy.shouldAlert(2,2,0,60000),"Do not repeat unchanged status");
        check(!ThermalPolicy.shouldAlert(3,2,0,60000),"Do not alert while cooling");
        check(!ThermalPolicy.shouldAlert(2,3,50000,60000),"Rate-limit notifications");
        check(ThermalPolicy.shouldAlert(-1,3,-60000,0),"Severe first reading must alert");
        System.out.println("Passed "+checks+" session comparison and restore parsing checks");
    }
}
