package com.primo.optimizer;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** UI for saved targets, reproducible measurements, and transparent capability reporting. */
final class FeaturePanels {
    private final MainActivity m;
    private final LinearLayout content;
    private static final int IMPORT=301, EXPORT=302;
    FeaturePanels(MainActivity activity,LinearLayout parent){m=activity;content=parent;}
    private void note(LinearLayout p,String s){p.addView(m.text(s,13,MainActivity.MUTED));}
    private void heading(LinearLayout p,String s){m.title(p,s,20);}
    private void say(String title,String body){new AlertDialog.Builder(m).setTitle(title).setMessage(body).setPositiveButton("OK",null).show();}
    private Spinner spinner(LinearLayout parent,String[] choices,String current) {
        Spinner s=new Spinner(m);s.setAdapter(new ArrayAdapter<>(m,android.R.layout.simple_spinner_dropdown_item,choices));
        int index=Arrays.asList(choices).indexOf(current);s.setSelection(Math.max(0,index));parent.addView(s);return s;
    }
    private EditText entry(LinearLayout p,String hint,String value,boolean numeric){
        EditText e=new EditText(m);e.setTextColor(MainActivity.TEXT);e.setHintTextColor(MainActivity.MUTED);e.setHint(hint);e.setSingleLine(true);e.setText(value);
        if(numeric)e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);p.addView(e);return e;
    }
    void render() {
        heading(content,"Your gaming lab");note(content,"Selected: "+m.selectedName);
        Spinner tabs=spinner(content,new String[]{"Sessions","Profiles","Emulator","Report"},m.labSection);
        tabs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,android.view.View v,int pos,long id){String next=(String)p.getItemAtPosition(pos);if(!next.equals(m.labSection)){m.labSection=next;m.show();}}});
        if(m.selected.isEmpty()&&(m.labSection.equals("Sessions")||m.labSection.equals("Profiles"))){LinearLayout c=m.card();note(c,"Choose a game first so profiles and measurements stay associated with the correct package.");m.button(c,"Choose game",true,()->{m.page="Games";m.show();});return;}
        switch(m.labSection){case "Profiles":profiles();break;case "Emulator":emulator();break;case "Report":report();break;default:sessions();}
    }
    private void profiles() {
        JSONObject saved=Profiles.read(m.prefs,m.selected+".custom");Advisor.Result r=m.recommendation();
        LinearLayout c=m.card();heading(c,"Custom targets");
        note(c,"Saved targets are requests, not applied settings. Quality is changed inside the game; root can request supported OS scale/FPS controls.");
        note(c,"Graphics quality (manual in-game choice)");Spinner quality=spinner(c,new String[]{"Low","Medium","High"},saved.optString("quality",r.quality));
        note(c,"FPS cap target · reported display "+Math.round(m.device.refresh)+" Hz");Spinner fps=spinner(c,new String[]{"15","24","30","40","45","60","90","120"},String.valueOf(saved.optInt("fps",r.fps)));
        note(c,"Render scale request");Spinner scale=spinner(c,new String[]{"70%","85%","100%"},Math.round(saved.optDouble("scale",r.scale)*100)+"%");
        note(c,"Priority · choosing a new priority sets starting targets you can edit");Spinner priority=spinner(c,new String[]{"Consistency","Image quality","Battery life"},saved.optString("priority","Consistency"));
        final String[] prior={saved.optString("priority","Consistency")};
        priority.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> p){}public void onItemSelected(AdapterView<?> p,android.view.View v,int pos,long id){String value=(String)p.getItemAtPosition(pos);if(!value.equals(prior[0])){prior[0]=value;if(value.equals("Image quality")){scale.setSelection(2);fps.setSelection(2);}else if(value.equals("Battery life")){scale.setSelection(0);fps.setSelection(2);}else{scale.setSelection(r.scale==1?2:r.scale==0.85?1:0);fps.setSelection(r.fps>=60?5:2);}}}});
        m.button(c,"Save custom profile",true,()->{
            int target=Integer.parseInt(fps.getSelectedItem().toString());double factor=Integer.parseInt(scale.getSelectedItem().toString().replace("%",""))/100.0;
            if(target>m.device.refresh+1){say("Target exceeds reported display","Choose a target at or below the reported display refresh rate. A higher request cannot guarantee more visible frames.");return;}
            JSONObject profile=Profiles.object("fps",target,"scale",factor,"quality",quality.getSelectedItem(),"priority",priority.getSelectedItem(),"savedAt",System.currentTimeMillis(),"identity",Profiles.identity(m,m.selected));
            m.prefs.edit().putString(m.selected+".custom",profile.toString()).apply();m.show();say("Profile saved","Recommendation only until applied. Use Controls for supported root requests; change quality in the game.");
        });
        m.button(c,"Use automatic recommendations",false,()->{m.prefs.edit().remove(m.selected+".custom").apply();m.show();});
        note(c,"Last OS observation: "+Profiles.state(m.prefs,m.selected));
        LinearLayout catalog=m.card();heading(catalog,"Game-specific guidance");note(catalog,GameCatalog.advice(m.selected));note(catalog,"Starter template · not lab tested by FramePilot. Local validation below is tied to this device and installed game version.");
        JSONObject tested=Profiles.read(m.prefs,m.selected+".tested");
        note(catalog,tested.has("identity")?(tested.optString("identity").equals(Profiles.identity(m,m.selected))?"Locally tested profile saved · external measurements supplied by you":"Saved test is stale: device or game version changed"):"No locally tested profile yet. Record comparable before/after sessions and import measured frames in Sessions.");
        JSONObject testedTargets=tested.optJSONObject("profile");
        if(testedTargets!=null)note(catalog,"Tested targets: "+testedTargets.optString("quality","Unspecified")+" quality · "+testedTargets.optInt("fps")+" FPS · "+Math.round(testedTargets.optDouble("scale",1)*100)+"% scale");
        LinearLayout feedback=m.card();heading(feedback,"Personal calibration");
        note(feedback,"Your feedback adjusts automatic recommendations conservatively. Explicit custom targets stay as you saved them. Feedback is self-reported, not measured proof.");
        note(feedback,"Last feedback: "+m.prefs.getString(m.selected+".feedback","None")+" · "+m.prefs.getInt(m.selected+".feedbackCount",0)+" responses");
        for(String label:new String[]{"Smooth","Stuttered","Too hot"})m.button(feedback,label,false,()->{
            m.prefs.edit().putString(m.selected+".feedback",label).putString(m.selected+".feedbackIdentity",Profiles.identity(m,m.selected)).putInt(m.selected+".feedbackCount",m.prefs.getInt(m.selected+".feedbackCount",0)+1).putLong(m.selected+".feedbackAt",System.currentTimeMillis()).apply();m.show();
        });
        m.button(feedback,"Clear calibration",false,()->{m.prefs.edit().remove(m.selected+".feedback").remove(m.selected+".feedbackCount").remove(m.selected+".feedbackIdentity").apply();m.show();});
    }
    private void sessions() {
        LinearLayout c=m.card();heading(c,"Before / after recording");
        note(c,"1. Use the same game scene, route, graphics, brightness and charging conditions. Record BEFORE before tuning.\n2. Stop, apply one change, restart the game, then record AFTER.\n3. Use at least 60 seconds and similar durations (within 25%). Repeat runs to reduce scene and temperature bias.");
        note(c,"Samples are device-wide every 5 seconds while this foreground service is allowed to run, up to 30 minutes. Selected game is user-assigned; foreground execution and game FPS are not inferred.");
        JSONObject active=Profiles.read(m.prefs,"activeSession");
        if(active.has("package")) {
            note(c,"Active / interrupted record: "+active.optString("package")+" · "+active.optString("kind")+" · "+active.optString("scene")+" · "+active.optLong("durationMs")/1000+" sec sampled");
            m.button(c,"Stop and save last samples",true,()->{m.startService(new Intent(m,SessionService.class).setAction("stop"));m.getWindow().getDecorView().postDelayed(m::show,300);say("Stopping recorder","Return to Sessions or tap Refresh to inspect the completed record.");});
        }else {
            EditText scene=entry(c,"Repeatable scene / route name",m.prefs.getString(m.selected+".scene",""),false);
            for(String kind:new String[]{"before","after"})m.button(c,"Record "+kind.toUpperCase(Locale.ROOT),true,()->{
                String name=scene.getText().toString().trim();if(name.isEmpty()){say("Name the scene","Use the same scene name for both runs.");return;}
                JSONObject previous=Profiles.read(m.prefs,m.selected+".session."+kind);
                Runnable start=()->{
                    m.prefs.edit().putString(m.selected+".scene",name).apply();
                    Advisor.Result recommendation=m.recommendation();JSONObject chosen=Profiles.read(m.prefs,m.selected+".custom");
                    Intent intent=new Intent(m,SessionService.class).putExtra("pkg",m.selected).putExtra("kind",kind).putExtra("scene",name).putExtra("target",Profiles.fps(m.prefs,m.selected,recommendation)).putExtra("scale",Profiles.scale(m.prefs,m.selected,recommendation)).putExtra("quality",chosen.optString("quality",recommendation.quality));
                    try{if(Build.VERSION.SDK_INT>=26)m.startForegroundService(intent);else m.startService(intent);m.getWindow().getDecorView().postDelayed(m::show,300);say("Recording started","Open your game and play the named scene. Return here to stop. Device conditions are recorded; game FPS needs an external measured trace.");}catch(Exception e){say("Cannot start recorder",e.toString());}
                };
                if(previous.has("startedAt"))new AlertDialog.Builder(m).setTitle("Replace previous "+kind+" record?").setMessage("The previous record and its imported frame statistics will be replaced.").setNegativeButton("Cancel",null).setPositiveButton("Replace",(d,w)->start.run()).show();else start.run();
            });
        }
        m.button(c,"Refresh session status",false,m::show);
        m.button(c,"Enable / check thermal alerts",false,()->{
            if(Build.VERSION.SDK_INT>=33 && m.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)m.requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},303);
            else say("Thermal alerts",notifications(m)?"Notifications are enabled at app level. Check the Thermal alerts channel in Android settings too. Alerts require reported severity ≥ Moderate and run only during recording.":"Notifications are disabled. Enable them in Android app settings to see alerts.");
        });
        note(c,"Alerts follow rising reported thermal severity; no thermal protections are disabled. Android may suspend or stop recording. Sample gaps and interrupted records are disclosed.");
        JSONObject before=Profiles.read(m.prefs,m.selected+".session.before"),after=Profiles.read(m.prefs,m.selected+".session.after");
        for(String kind:new String[]{"before","after"}) {
            JSONObject s=kind.equals("before")?before:after;LinearLayout record=m.card();heading(record,kind.toUpperCase(Locale.ROOT));
            if(!s.has("startedAt")){note(record,"No completed record");continue;}
            note(record,summary(s));
            note(record,"OS observation at start: "+s.optString("osState")+"\n"+s.optString("attribution"));
            JSONObject frames=s.optJSONObject("frames");
            note(record,frames==null?"FPS/frame times: unavailable until measured frame durations are imported.":"External measured frames: "+frames.optInt("count")+" · average "+fmt(frames.optDouble("fps"))+" FPS · p95 "+fmt(frames.optDouble("p95ms"))+" ms · "+fmt(frames.optDouble("missedPercent"))+"% above target budget (+5% tolerance). Target: "+s.optInt("target")+" FPS. User-supplied attribution, not independently verified.");
            m.button(record,"Import measured frame-time CSV · "+kind,false,()->{
                m.prefs.edit().putString("pendingImportKey",m.selected+".session."+kind).putLong("pendingImportStarted",s.optLong("startedAt")).apply();
                sayImport();
            });
            m.button(record,"Delete "+kind+" record",false,()->new AlertDialog.Builder(m).setTitle("Delete completed record?").setMessage("Remove this session and its imported frame statistics. Your OS settings and custom profile are unaffected.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{m.prefs.edit().remove(m.selected+".session."+kind).apply();m.show();}).show());
        }
        LinearLayout comparison=m.card();heading(comparison,"Did tuning help?");
        String issue=comparisonIssue(before,after);
        if(issue==null&&!after.optString("identity").equals(Profiles.identity(m,m.selected)))issue="saved runs belong to a different installed game/device version";
        if(issue!=null)note(comparison,"Comparison not established: "+issue);
        else {
            note(comparison,"Comparable scene, duration and reported device/game version. This is observational evidence, not proof that FramePilot caused a change.");
            note(comparison,"Average battery-sensor temperature: "+difference(mean(before,"batteryC"),mean(after,"batteryC"),"°C")+"\nAvailable system RAM: "+difference(mean(before,"availableGb"),mean(after,"availableGb"),"GB")+". More free RAM alone is not proof of smoother play.");
            JSONObject b=before.optJSONObject("frames"),a=after.optJSONObject("frames");
            if(b!=null&&a!=null){note(comparison,"Average FPS: "+difference(b.optDouble("fps"),a.optDouble("fps"),"FPS")+"\np95 frame time: "+difference(b.optDouble("p95ms"),a.optDouble("p95ms"),"ms")+". Lower p95 usually indicates fewer long frames. Different FPS caps change expected results; inspect targets above.");}
            else note(comparison,"Smoothness remains unmeasured. Import measured frame times for both runs to compare FPS and p95 frame time.");
            if(b!=null&&a!=null&&b.optInt("count")>=120&&a.optInt("count")>=120)m.button(comparison,"Save locally tested profile",true,()->{
                JSONObject tested=Profiles.object("identity",Profiles.identity(m,m.selected),"profile",after.optJSONObject("profile"),"target",after.optInt("target"),"beforeAt",before.optLong("startedAt"),"afterAt",after.optLong("startedAt"),"evidence","Repeatable sessions with user-supplied measured traces; not independent certification");
                m.prefs.edit().putString(m.selected+".tested",tested.toString()).apply();say("Local test saved","Evidence is tied to the current game/device version. Retest after changes; the template is not a universal benchmark.");
            });
        }
        note(comparison,"Evidence levels: saved recommendation → request sent → OS values read back → game behavior measured. Root read-back checks configuration. Only repeatable game measurements show observed smoothness changes.");
    }
    private void sayImport(){new AlertDialog.Builder(m).setTitle("Measured frames only").setMessage("Select a one-column UTF-8 CSV headed frame_time_ms, followed by positive frame durations in milliseconds. Use a real game profiler/export for exactly this recorded scene. The optimizer cannot independently verify its source. At least 120 frames are required to save a locally tested profile. Example format is documented in README.").setNegativeButton("Cancel",null).setPositiveButton("Choose file",(d,w)->{try{m.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),IMPORT);}catch(Exception e){say("No document picker",e.getMessage());}}).show();}
    static double mean(JSONObject s,String key) {
        JSONArray samples=s.optJSONArray("samples");double sum=0;int n=0;
        if(samples!=null)for(int i=0;i<samples.length();i++){JSONObject v=samples.optJSONObject(i);if(v!=null&&!v.isNull(key)){double value=v.optDouble(key,Double.NaN);if(!Double.isNaN(value)&&value>=0){sum+=value;n++;}}}
        return n==0?Double.NaN:sum/n;
    }
    private static long gap(JSONObject s){JSONArray samples=s.optJSONArray("samples");long gap=0,previous=0;if(samples!=null)for(int i=0;i<samples.length();i++){long t=samples.optJSONObject(i).optLong("elapsedMs");gap=Math.max(gap,t-previous);previous=t;}return gap;}
    static String comparisonIssue(JSONObject before,JSONObject after) {
        if(!before.has("startedAt")||!after.has("startedAt"))return "record both runs first";
        if(!before.optString("identity").equals(after.optString("identity")))return "device or game versions differ";
        if(after.optLong("startedAt")<before.optLong("endedAt"))return "AFTER must follow the completed BEFORE run";
        for(JSONObject s:new JSONObject[]{before,after})if(!(s.optString("stopReason").equals("User stopped")||s.optString("stopReason").equals("30-minute limit")))return "a recorder run was interrupted";
        if(!FrameStats.comparable(before.optString("scene"),after.optString("scene"),before.optLong("durationMs"),after.optLong("durationMs")))return "use the same scene for ≥60 seconds and durations within 25%";
        if(gap(before)>15000 || gap(after)>15000)return "sampling gaps exceed 15 seconds; repeat with recording active";
        return null;
    }
    private static String fmt(double n){return Double.isNaN(n)?"Unavailable":String.format(Locale.US,"%.2f",n);}
    private static String difference(double before,double after,String unit){return fmt(before)+" → "+fmt(after)+" "+unit;}
    private static String summary(JSONObject s){
        JSONArray samples=s.optJSONArray("samples");String drain="Unavailable";
        if(samples!=null&&samples.length()>1){int first=samples.optJSONObject(0).optInt("batteryPercent",-1),last=samples.optJSONObject(samples.length()-1).optInt("batteryPercent",-1);if(first>=0&&last>=0)drain=first+"% → "+last+"% (coarse device reading; charging affects it)";}
        return s.optString("scene")+" · "+s.optLong("durationMs")/1000+" sec · "+(samples==null?0:samples.length())+" samples\nAverage available RAM: "+fmt(mean(s,"availableGb"))+" GB\nAverage battery sensor: "+fmt(mean(s,"batteryC"))+" °C\nBattery: "+drain+"\nLargest sample gap: "+gap(s)/1000+" sec\n"+s.optString("stopReason");
    }
    private void emulator() {
        LinearLayout c=m.card();heading(c,"Desktop setup assistant");note(c,"These steps change settings on your desktop, not from Android. Names vary by emulator version. Host values are entered by you, not detected.");
        Spinner brand=spinner(c,new String[]{"BlueStacks 5","LDPlayer","Other emulator"},m.prefs.getString("emulatorBrand","BlueStacks 5"));
        EditText ram=entry(c,"Host RAM in GB",m.prefs.getString("hostRam","16"),true),cores=entry(c,"Host logical CPU count",m.prefs.getString("hostCores","8"),true);
        m.button(c,"Generate setup steps",true,()->{
            try{
                int hostRam=Integer.parseInt(ram.getText().toString()),hostCores=Integer.parseInt(cores.getText().toString());if(hostRam<2||hostRam>2048||hostCores<1||hostCores>1024)throw new IllegalArgumentException();
                String name=brand.getSelectedItem().toString();m.prefs.edit().putString("emulatorBrand",name).putString("hostRam",String.valueOf(hostRam)).putString("hostCores",String.valueOf(hostCores)).putBoolean("emulatorOverride",true).apply();
                String path=name.equals("BlueStacks 5")?"BlueStacks: gear → Performance for CPU/RAM and FPS; Display for resolution; Graphics for renderer.":name.equals("LDPlayer")?"LDPlayer: open Settings and find CPU/RAM allocation and resolution (often under Advanced); use the graphics options exposed by your installed version.":"Open your emulator's desktop settings for CPU/RAM, resolution and graphics backend.";
                String steps=path+"\n\n1. Check hardware virtualization is enabled using the emulator's desktop diagnostics.\n2. Starting allocation: about "+Math.max(1,Math.min(4,hostCores/2))+" CPU threads and "+Math.max(1,Math.min(4,hostRam/2))+" GB RAM. This is a conservative trial, not an optimum; leave capacity for Windows.\n3. Try 1280 × 720 and a 60 FPS cap first, or 30 FPS if unstable.\n4. Keep the default renderer first. If needed, compare one available renderer at a time, restarting after each change. There is no universal best backend.\n5. Match the Android game version, scene and in-game settings, then record BEFORE and AFTER.\n\nHost GPU performance is not measured by this app.";
                m.prefs.edit().putString("emulatorSteps",steps).apply();m.show();
            }catch(Exception e){say("Check host values","Enter host RAM (2–2048 GB) and logical CPU count (1–1024).");}
        });
        String steps=m.prefs.getString("emulatorSteps","");if(!steps.isEmpty()){LinearLayout guide=m.card();heading(guide,"Your setup steps");note(guide,steps);}
        LinearLayout sources=m.card();heading(sources,"Official instructions");
        m.button(sources,"BlueStacks settings guide",false,()->open("https://support.bluestacks.com/hc/en-us/articles/360053245871-How-to-use-the-BlueStacks-5-Settings-menu"));
        m.button(sources,"LDPlayer CPU / RAM guide",false,()->open("https://www.ldplayer.net/support/ways-to-configure-ram-and-cpu-assignment.html"));
    }
    private void open(String url){try{m.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(Exception e){say("No browser",url);}}
    static boolean notifications(android.content.Context c){return Build.VERSION.SDK_INT>=24?((NotificationManager)c.getSystemService(android.content.Context.NOTIFICATION_SERVICE)).areNotificationsEnabled():true;}
    private void report() {
        LinearLayout c=m.card();heading(c,"Compatibility & evidence");
        note(c,"Device readings: detected\nThermal severity: "+(m.device.thermal<0?"unsupported / unavailable":"reported by OS; sensor fidelity untested")+"\nRoot commands: "+m.rootStatus+"\nRoot graphics: "+(Build.VERSION.SDK_INT>=34?"requires granted root and live command check":"unsupported by this app below Android 14")+"\nGame effect: unverified unless measured traces are supplied\nNotifications: "+(notifications(m)?"app enabled; check channel too":"disabled / denied")+"\nEmulator: "+(m.device.emulator?"profile active; host unmeasured":"not detected / not selected")+"\nBlueStacks / LDPlayer direct integration: untested\nLive in-game FPS capture: unavailable; measured CSV import supported");
        note(c,"Last OS observation: "+Profiles.state(m.prefs,m.selected)+"\nRead-back time: "+(m.prefs.getLong(m.selected+".verifiedAt",0)==0?"Never":new Date(m.prefs.getLong(m.selected+".verifiedAt",0)).toString()));
        m.button(c,"Export diagnostic JSON",true,()->{
            m.prefs.edit().putString("pendingExport",diagnostic().toString()).apply();
            try{m.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"framepilot-diagnostics.json"),EXPORT);}catch(Exception e){say("No document picker",e.getMessage());}
        });
        note(c,"Export contains reported device specs, selected game/version, settings, recovery record, scene labels and session summaries. Review it before sharing. Other installed apps and device serial identifiers are excluded.");
        LinearLayout ready=m.card();heading(ready,"One-tap readiness check");
        m.button(ready,"Check memory, heat & battery saver",false,()->{
            DeviceSnapshot d=DeviceSnapshot.read(m);say("Readiness check",fmt(d.available)+" GB available of "+fmt(d.ram)+" GB\nThermal: "+d.thermalLabel()+"\nBattery saver: "+(d.powerSave?"on":"off")+"\n\n"+(d.available<1?"Memory pressure may affect play. Close unnecessary apps through Android if needed.":"No low-memory warning from this simple check.")+"\nFree RAM is not a speed score. No background apps were killed.");
        });
        note(ready,"RAM boosting: no guaranteed speed gain; this tool does not kill other apps. Thermal bypass: not implemented; safety protections stay active. Universal FPS unlock: unsupported; use the game's offered FPS options and supported OS caps. An FPS cap does not unlock a higher engine limit.");
    }
    private JSONObject diagnostic(){return Profiles.object("appVersion","0.2.0","exportedAt",System.currentTimeMillis(),"device",Profiles.object("model",m.device.model,"androidApi",Build.VERSION.SDK_INT,"abi",m.device.abi,"ramGb",m.device.ram,"availableGb",m.device.available,"cores",m.device.cores,"refreshHz",m.device.refresh,"gpuReported",m.prefs.getString("gpu","Unavailable"),"thermal",m.device.thermal,"emulatorProfile",m.device.emulator),"package",m.selected,"gameVersion",Profiles.version(m,m.selected),"rootProbe",m.rootStatus,"notificationsAppEnabled",notifications(m),"osStateLastObserved",Profiles.state(m.prefs,m.selected),"verifiedAt",m.prefs.getLong(m.selected+".verifiedAt",0),"customProfile",Profiles.read(m.prefs,m.selected+".custom"),"recovery",Profiles.read(m.prefs,m.selected+".recovery"),"before",Profiles.read(m.prefs,m.selected+".session.before"),"after",Profiles.read(m.prefs,m.selected+".session.after"),"activeSession",Profiles.read(m.prefs,"activeSession"),"localTest",Profiles.read(m.prefs,m.selected+".tested"),"calibration",m.prefs.getString(m.selected+".feedback","None"),"limitations","OS read-back is not game performance proof. External traces are user assigned. Host hardware unmeasured.");}
    static void fileResult(MainActivity m,int request,int result,Intent data) {
        if(request!=IMPORT&&request!=EXPORT)return;
        if(result!=Activity.RESULT_OK||data==null||data.getData()==null){m.prefs.edit().remove("pendingImportKey").remove("pendingExport").apply();return;}
        final android.net.Uri uri=data.getData();final String key=m.prefs.getString("pendingImportKey","");final long started=m.prefs.getLong("pendingImportStarted",-1);final String export=m.prefs.getString("pendingExport","");
        m.prefs.edit().remove("pendingImportKey").remove("pendingImportStarted").remove("pendingExport").apply();
        new Thread(()->{
            String message;
            try{
                if(request==EXPORT){if(export.isEmpty())throw new IOException("Export payload unavailable");try(OutputStream out=m.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("Cannot open destination");out.write(export.getBytes(StandardCharsets.UTF_8));}message="Diagnostic JSON exported. Review before sharing.";}
                else {
                    ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=m.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Cannot open source");byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>2000000)throw new IOException("CSV exceeds 2 MB limit");bytes.write(buffer,0,n);}}
                    JSONObject session=Profiles.read(m.prefs,key);if(key.isEmpty()||session.optLong("startedAt")!=started)throw new IOException("The selected session changed; import cancelled");
                    FrameStats stats=FrameStats.parse(new String(bytes.toByteArray(),StandardCharsets.UTF_8),session.optInt("target",30));
                    session.put("frames",Profiles.object("count",stats.count,"fps",stats.fps,"p95ms",stats.p95ms,"missedPercent",stats.missedPercent,"importedAt",System.currentTimeMillis(),"source","External measured CSV · user assigned, not independently verified"));
                    if(!m.prefs.edit().putString(key,session.toString()).commit())throw new IOException("Unable to save imported statistics");message="Imported "+stats.count+" measured frame durations. Source attribution is user supplied.";
                }
            }catch(Exception e){message="File operation failed: "+e.getMessage();}
            final String done=message;m.runOnUiThread(()->{if(!m.isDestroyed()&&!m.isFinishing()){m.show();new AlertDialog.Builder(m).setTitle("File operation").setMessage(done).setPositiveButton("OK",null).show();}});
        },"framepilot-file").start();
    }
}
