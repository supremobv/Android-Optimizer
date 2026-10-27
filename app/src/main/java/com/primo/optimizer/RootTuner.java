package com.primo.optimizer;

import android.content.SharedPreferences;
import org.json.*;

/** Recovery journal survives process death. Only the selected game's custom FPS/scale are changed. */
@android.annotation.SuppressLint("ApplySharedPref") // Recovery phases intentionally reach disk before the next OS command.
final class RootTuner {
    private static void save(SharedPreferences prefs,String key,JSONObject value) throws Exception {
        if(!prefs.edit().putString(key,value.toString()).commit()) throw new IllegalStateException("Recovery journal could not be saved; no further commands run");
    }
    private static int user() throws Exception {
        String value=RootShell.run("am get-current-user").trim();
        if(!value.matches("[0-9]{1,6}")) throw new IllegalStateException("Cannot determine Android user");return Integer.parseInt(value);
    }
    private static GameState capture(String pkg) throws Exception {return GameState.parse(RootShell.run("cmd game list-modes "+pkg),RootShell.run("cmd game list-configs "+pkg),pkg);}
    static String apply(SharedPreferences prefs,String pkg,int fps,double scale) throws Exception {
        Advisor.validatePackage(pkg);Advisor.setCommand(pkg,fps,scale);
        int user=user();String key=pkg+".recovery";JSONObject journal=Profiles.read(prefs,key);
        GameState current=capture(pkg);
        if(journal.has("modes")) {
            if(journal.getInt("user")!=user) throw new IllegalStateException("Snapshot belongs to another Android user");
            int oldFps=journal.optInt("wantedFps",-1);double oldScale=journal.optDouble("wantedScale",-2);
            if(!current.mode.equals("custom")||current.fps!=oldFps||Math.abs(current.scale-oldScale)>0.0001) throw new IllegalStateException("OS settings changed outside FramePilot or a request is incomplete. Restore or inspect before applying again.");
        } else {
            String modes=RootShell.run("cmd game list-modes "+pkg), configs=RootShell.run("cmd game list-configs "+pkg);
            GameState stable=GameState.parse(modes,configs,pkg);
            if(!stable.sameValues(current)) throw new IllegalStateException("Game settings changed during capture; retry");
            journal=Profiles.object("user",user,"modes",modes,"configs",configs,"capturedAt",System.currentTimeMillis());
        }
        journal.put("wantedFps",fps).put("wantedScale",scale).put("phase","pending");save(prefs,key,journal);
        prefs.edit().putString(pkg+".osState","Pending root request · restore record saved").commit();
        if(user()!=user) throw new IllegalStateException("Android user changed; request stopped");
        RootShell.run(Advisor.setCommand(pkg,fps,scale));
        GameState after=capture(pkg);
        if(!after.mode.equals("custom") || after.fps!=fps || Math.abs(after.scale-scale)>0.0001) throw new IllegalStateException("Read-back did not match requested FPS, scale and mode. Recovery record retained.");
        journal.put("phase","accepted");save(prefs,key,journal);
        String state="OS read-back matched · "+fps+" FPS cap / "+Math.round(scale*100)+"% scale · game effect unverified";
        prefs.edit().putString(pkg+".osState",state).putLong(pkg+".verifiedAt",System.currentTimeMillis()).commit();return state;
    }
    static String restore(SharedPreferences prefs,String pkg) throws Exception {
        Advisor.validatePackage(pkg);JSONObject journal=Profiles.read(prefs,pkg+".recovery");
        if(!journal.has("modes")) throw new IllegalStateException("No captured settings. Legacy changes cannot be restored exactly.");
        int user=journal.getInt("user");if(user()!=user) throw new IllegalStateException("Switch to the snapshot's Android user first");
        GameState original=GameState.parse(journal.getString("modes"),journal.getString("configs"),pkg), now=capture(pkg);
        if(!original.sameValues(now)) {
            boolean expected=now.customPresent && now.fps==journal.getInt("wantedFps") && Math.abs(now.scale-journal.getDouble("wantedScale"))<0.0001;
            if(!expected) throw new IllegalStateException("Current values differ from FramePilot's last request. Restore stopped to preserve external changes. Export diagnostics and inspect the recovery record.");
            RootShell.run(original.restoreConfig(pkg));
            RootShell.run("cmd game mode --user "+user+" "+original.mode+" "+pkg);
            if(!capture(pkg).sameValues(original)) throw new IllegalStateException("Restoration read-back failed. Snapshot retained for retry.");
        }
        if(!prefs.edit().remove(pkg+".recovery").putString(pkg+".osState","Captured mode, FPS and scale restored · verified by OS read-back").commit()) throw new IllegalStateException("Restored, but journal cleanup failed. Recheck diagnostics.");
        return "Captured mode, FPS and scale restored. Other OS settings and original override provenance are outside this snapshot's scope.";
    }
    static String verify(SharedPreferences prefs,String pkg) throws Exception {
        GameState now=capture(Advisor.validatePackage(pkg));JSONObject journal=Profiles.read(prefs,pkg+".recovery");
        String state;
        if(journal.has("wantedFps")) {
            if(user()!=journal.getInt("user")) throw new IllegalStateException("Android user differs from recovery snapshot");
            boolean match=now.mode.equals("custom")&&now.fps==journal.getInt("wantedFps")&&Math.abs(now.scale-journal.getDouble("wantedScale"))<0.0001;
            state=match?"OS read-back matched · actual game effect unverified":"OS values do not match the last request";
        }else state="OS inspected · mode "+now.mode+" · no FramePilot request to match";
        prefs.edit().putString(pkg+".osState",state).putLong(pkg+".verifiedAt",System.currentTimeMillis()).commit();return state;
    }
}
