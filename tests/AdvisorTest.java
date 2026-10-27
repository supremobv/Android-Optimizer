package com.primo.optimizer;

public final class AdvisorTest {
    private static int checks;
    private static void check(boolean value,String message) {checks++;if(!value) throw new AssertionError(message);}
    public static void main(String[] args) {
        Advisor.Result low=Advisor.assess(2,0.6,4,60,-1,false,false,2);
        check(low.fps==30 && low.quality.equals("Low"),"Constrained heavy game should reduce settings");
        Advisor.Result high=Advisor.assess(12,6,8,120,0,false,false,0);
        check(high.fps==60 && high.scale==1,"Good resources should allow conservative 60 FPS target");
        Advisor.Result hot=Advisor.assess(12,6,8,120,3,false,false,0);
        check(hot.fps==30 && hot.verdict.contains("Cool"),"Severe heat should advise cooling");
        check(Advisor.assess(12,6,8,120,0,true,false,0).fps==30,"Saver should lower target");
        check(Advisor.assess(12,6,8,24,0,false,false,0).fps<=24,"Target must respect display");
        check(Advisor.assess(12,6,8,60,-1,false,true,0).reason.contains("very low"),"Emulator uncertainty must be disclosed");
        check(Advisor.setCommand("com.example.game",30,0.7).equals("cmd game set --downscale 0.7 --fps 30 com.example.game"),"Command must be deterministic");
        for(String pkg:new String[]{"com.game;reboot","com.game\nreboot","$(id)","","a.b && id","a.b'"}) {
            boolean rejected=false;try {Advisor.setCommand(pkg,30,0.7);} catch(IllegalArgumentException e) {rejected=true;}
            check(rejected,"Reject shell injection: "+pkg);
        }
        boolean rejected=false;try{Advisor.setCommand("com.game.app",300,0.7);}catch(IllegalArgumentException e){rejected=true;}
        check(rejected,"Reject invalid FPS");
        for(int load=0;load<=2;load++) for(int ram=1;ram<=16;ram++) {
            Advisor.Result r=Advisor.assess(ram,ram/2.0,8,60,-1,false,false,load);
            check(r.fps==30 || r.fps==60,"Targets are bounded for all resource classes");
            if(load>0) check(r.fps<=Advisor.assess(ram,ram/2.0,8,60,-1,false,false,load-1).fps,"Increasing workload cannot improve target");
        }
        System.out.println("Passed "+checks+" advisor and command safety checks");
    }
}
