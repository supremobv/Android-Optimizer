package com.primo.optimizer;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    static final int BG=0xff101410, CARD=0xff1b221b, TEXT=0xfff1f5ec, MUTED=0xffa6b1a2, LIME=0xffb6f36b;
    private LinearLayout root, content, navigation;
    android.content.SharedPreferences prefs;
    DeviceSnapshot device;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayList<Game> games=new ArrayList<>();
    String page="Overview", selected="", selectedName="Choose a game", rootStatus="Not checked", labSection="Sessions";
    private int workload=2;
    private boolean rootReady=false, busy=false, scanning=false;
    private static final class Game {
        String pkg, name;
        Game(String p, String n) { pkg=p; name=n; }
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("profiles", MODE_PRIVATE);
        if(state!=null){page=state.getString("page","Overview");labSection=state.getString("labSection","Sessions");}
        selected=prefs.getString("selected", "");
        selectedName=prefs.getString("selectedName", "Choose a game");
        workload=prefs.getInt(selected+".workload", 2);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(root); root.requestApplyInsets();
        show(); scan();
        worker.execute(() -> {
            String renderer=GpuProbe.renderer();
            handler.post(() -> {prefs.edit().putString("gpu",renderer).apply();if(page.equals("Overview")) show();});
        });
    }
    @Override public void onResume() { super.onResume(); if (prefs != null) show(); }
    @Override protected void onSaveInstanceState(Bundle state){state.putString("page",page);state.putString("labSection",labSection);super.onSaveInstanceState(state);}
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); worker.shutdownNow(); super.onDestroy(); }
    private int dp(float n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    TextView text(String s, int size, int color) {
        TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color); v.setPadding(0,dp(4),0,dp(4)); return v;
    }
    void title(LinearLayout parent, String s, int size) {
        TextView t=text(s,size,TEXT); t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); parent.addView(t);
    }
    LinearLayout card() {
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(20),dp(18),dp(20),dp(18));
        c.setBackground(shape(CARD,20)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(14); content.addView(c,lp); return c;
    }
    private void gap(LinearLayout p, int height) { p.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height))); }
    Button button(LinearLayout parent, String label, boolean primary, Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(primary ? BG : TEXT);
        b.setBackground(shape(primary ? LIME : 0xff303b2e,12)); b.setPadding(dp(12),dp(8),dp(12),dp(8)); b.setMinHeight(dp(48));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(10); parent.addView(b,lp);
        b.setOnClickListener(v -> action.run()); return b;
    }
    void show() {
        if (isFinishing() || isDestroyed()) return;
        device=DeviceSnapshot.read(this);
        device.emulator=device.emulator || prefs.getBoolean("emulatorOverride",false);
        root.removeAllViews();
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(22),dp(14),dp(22),dp(10));
        TextView brand=text("▰  FramePilot",23,TEXT); brand.setTypeface(Typeface.DEFAULT,Typeface.BOLD); header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView tag=text("DEVICE AWARE",10,LIME); header.addView(tag); root.addView(header);
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20),dp(14),dp(20),dp(16));
        scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if (page.equals("Overview")) overview(); else if (page.equals("Games")) library(); else if(page.equals("Lab")) new FeaturePanels(this,content).render(); else controls();
        navigation=new LinearLayout(this); navigation.setPadding(dp(12),dp(8),dp(12),dp(8));
        for (String name:new String[]{"Overview","Games","Controls","Lab"}) {
            TextView tab=text(name,14,page.equals(name)?LIME:MUTED); tab.setGravity(Gravity.CENTER); tab.setMinHeight(dp(48));
            navigation.addView(tab,new LinearLayout.LayoutParams(0,-2,1)); tab.setOnClickListener(v -> {page=name; show();});
        }
        root.addView(navigation);
    }
    Advisor.Result recommendation() {
        Advisor.Result r=Advisor.assess(device.ram,device.available,device.cores,device.refresh,device.thermal,device.powerSave,device.emulator,workload);
        String feedback=prefs.getString(selected+".feedback","");
        if(!prefs.getString(selected+".feedbackIdentity","").equals(Profiles.identity(this,selected))) feedback="";
        String priority=Profiles.read(prefs,selected+".custom").optString("priority","Consistency");
        if(feedback.equals("Stuttered")||feedback.equals("Too hot")||priority.equals("Battery life"))
            return new Advisor.Result("Low",Math.min(30,r.fps),0.7,"Conservative target from your preferences",r.reason+" Personal feedback or battery priority lowers the suggestion. Custom targets remain user choices.");
        return r;
    }
    private String decimal(double v) { return String.format(Locale.US,"%.1f",v); }
    private void overview() {
        content.addView(text("PLAY WITH A PLAN",11,LIME));
        title(content,"Find your smooth spot.",30);
        content.addView(text("A practical starting point for your hardware.",14,MUTED)); gap(content,18);
        LinearLayout hero=card();
        hero.addView(text(device.emulator ? "EMULATOR PROFILE" : "YOUR DEVICE",11,LIME));
        title(hero,device.model,21);
        hero.addView(text("Android "+Build.VERSION.RELEASE+"  ·  "+device.abi,13,MUTED)); gap(hero,10);
        LinearLayout stats=new LinearLayout(this);
        stat(stats,decimal(device.ram)+" GB","total RAM"); stat(stats,""+device.cores,"CPU cores"); stat(stats,""+Math.round(device.refresh)+" Hz","display"); hero.addView(stats);
        hero.addView(text(decimal(device.available)+" GB available RAM  ·  OpenGL ES "+(device.gles >> 16)+"."+(device.gles & 65535),12,MUTED));
        hero.addView(text("GPU: "+prefs.getString("gpu","Reading renderer…"),12,MUTED));
        String platform=Build.HARDWARE;
        if(Build.VERSION.SDK_INT>=31 && !Build.SOC_MODEL.equals(Build.UNKNOWN)) platform=Build.SOC_MANUFACTURER+" "+Build.SOC_MODEL;
        hero.addView(text("Platform: "+platform,12,MUTED));
        if(device.emulator) hero.addView(text("Reported virtual hardware; host GPU performance is not measured.",12,MUTED));
        LinearLayout health=card(); title(health,"Session conditions",18);
        health.addView(text("Thermal status: "+device.thermalLabel(),14,device.thermal>=2 ? 0xffffca7a : TEXT));
        health.addView(text("Battery sensor: "+(Double.isNaN(device.batteryC)?"Unavailable":decimal(device.batteryC)+" °C")+"  ·  Saver "+(device.powerSave?"on":"off"),13,MUTED));
        health.addView(text("Battery temperature is not CPU/GPU temperature. Readings refresh when you return or tap Refresh.",12,MUTED));
        button(health,"Refresh device readings",false,this::show);
        LinearLayout plan=card(); plan.addView(text("GAME PROFILE",11,LIME));
        title(plan,selectedName,20);
        if (selected.isEmpty()) {
            plan.addView(text("Select an installed game, then choose how demanding it is.",14,MUTED));
            button(plan,"Choose a game",true,() -> {page="Games";show();});
        } else {
            plan.addView(text("How demanding is this game?",13,MUTED));
            Spinner demand=new Spinner(this);
            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Light · 2D / casual","Moderate · competitive / 3D","Heavy · large worlds / intensive 3D"});
            demand.setAdapter(adapter); demand.setSelection(workload); plan.addView(demand);
            demand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onNothingSelected(AdapterView<?> p) {}
                public void onItemSelected(AdapterView<?> p,View v,int pos,long id) {
                    if (workload != pos) { workload=pos; prefs.edit().putInt(selected+".workload",workload).apply(); show(); }
                }
            });
            Advisor.Result r=recommendation(); gap(plan,8); title(plan,r.verdict,22);
            plan.addView(text(r.quality+" graphics  /  "+r.fps+" FPS target  /  "+Math.round(r.scale*100)+"% render scale",14,LIME));
            plan.addView(text("Suggested target, not measured FPS. "+r.reason,12,MUTED));
            button(plan,"Save recommendation",true,() -> {
                prefs.edit().putString(selected+".saved",r.quality+" · "+r.fps+" FPS · "+Math.round(r.scale*100)+"%").apply();
                toast("Profile saved. Apply graphics inside your game, or use Controls."); show();
            });
            String saved=prefs.getString(selected+".saved",""); if (!saved.isEmpty()) plan.addView(text("Saved: "+saved,12,LIME));
            plan.addView(text("OS status: "+Profiles.state(prefs,selected),12,LIME));
            org.json.JSONObject custom=Profiles.read(prefs,selected+".custom");
            if(custom.has("fps")) plan.addView(text("Custom request: "+custom.optInt("fps")+" FPS / "+Math.round(custom.optDouble("scale")*100)+"% · "+custom.optString("priority")+" · saving does not apply",12,MUTED));
            plan.addView(text(GameCatalog.advice(selected)+" Starter template · untested until locally validated.",12,MUTED));
            button(plan,"Profiles, sessions & proof",false,() -> {page="Lab";show();});
            button(plan,"Launch "+selectedName,false,this::launch);
            button(plan,"Graphics & access controls",false,() -> {page="Controls";show();});
        }
        LinearLayout note=card(); title(note,"Consistency over peak FPS",18);
        note.addView(text("Lower shadows and effects first. Try a stable 30 FPS before raising the target. Test the same scene for 10 minutes; reduce settings if heat or stutter grows.",14,MUTED));
    }
    private void stat(LinearLayout row,String value,String label) {
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);title(box,value,22);box.addView(text(label,11,MUTED));row.addView(box,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void scan() {
        if (scanning) return; scanning=true;
        worker.execute(() -> {
            ArrayList<Game> found=new ArrayList<>();
            try {
                PackageManager pm=getPackageManager(); Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                HashSet<String> seen=new HashSet<>();
                for (ResolveInfo info:pm.queryIntentActivities(i,0)) {
                    String pkg=info.activityInfo.packageName;
                    if (!pkg.equals(getPackageName()) && seen.add(pkg)) found.add(new Game(pkg,info.loadLabel(pm).toString()));
                }
                Collections.sort(found,(a,b) -> a.name.compareToIgnoreCase(b.name));
                handler.post(() -> { games.clear();games.addAll(found);scanning=false;if (page.equals("Games")) show(); });
            } catch (Exception e) {handler.post(() -> {scanning=false;message("App scan unavailable",e.toString());});}
        });
    }
    private void library() {
        content.addView(text("YOUR LIBRARY",11,LIME)); title(content,"Pick your next session.",28);
        content.addView(text("Launcher apps are listed because some games do not identify themselves as games. Select only the game you want to tune.",14,MUTED));gap(content,16);
        LinearLayout entry=card(); title(entry,"Add by package name",17);
        EditText pkg=new EditText(this);pkg.setTextColor(TEXT);pkg.setHintTextColor(MUTED);pkg.setHint("com.example.game");pkg.setSingleLine(true);pkg.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);entry.addView(pkg);
        button(entry,"Use package",false,() -> {
            try { String id=Advisor.validatePackage(pkg.getText().toString().trim());
                if (getPackageManager().getLaunchIntentForPackage(id)==null) {message("Cannot launch package","The package is unavailable, hidden by Android, or has no launcher. Choose a visible app below.");return;}
                choose(id,getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(id,0)).toString());
            } catch (Exception e) {message("Check package name",e.getMessage());}
        });
        button(entry,scanning?"Scanning…":"Refresh installed apps",false,this::scan);
        if (games.isEmpty()) content.addView(text(scanning?"Reading installed apps…":"No visible launcher apps. Install a game and refresh.",14,MUTED));
        for (Game game:games) {
            LinearLayout c=card(); title(c,game.name,18); c.addView(text(game.pkg,12,MUTED));
            String saved=prefs.getString(game.pkg+".saved",""); if (!saved.isEmpty()) c.addView(text(saved,12,LIME));
            button(c,game.pkg.equals(selected)?"Open selected profile":"Create / open profile",game.pkg.equals(selected),() -> choose(game.pkg,game.name));
        }
    }
    private void choose(String pkg,String name) {
        selected=Advisor.validatePackage(pkg);selectedName=name;workload=prefs.getInt(selected+".workload",2);
        prefs.edit().putString("selected",selected).putString("selectedName",name).apply();page="Overview";show();
    }
    private void launch() {
        try { Intent i=getPackageManager().getLaunchIntentForPackage(selected); if(i==null) throw new IllegalStateException("Game is no longer installed or launchable."); startActivity(i); }
        catch (Exception e) {message("Unable to launch",e.getMessage());}
    }
    private void controls() {
        content.addView(text("CAPABILITY, NOT MAGIC",11,LIME));title(content,"Tune what’s supported.",28);
        content.addView(text("Selected: "+selectedName,14,MUTED));gap(content,16);
        LinearLayout basic=card();title(basic,"01  ·  Standard access",20);
        basic.addView(text("Works without root. Device analysis, saved profiles and game launching are available. Set quality, shadows, effects and FPS in the game's own settings; Android does not grant this app access to another game's private settings.",14,MUTED));
        button(basic,"Open battery saver settings",false,() -> {try {startActivity(new Intent(Build.VERSION.SDK_INT>=22?Settings.ACTION_BATTERY_SAVER_SETTINGS:Settings.ACTION_SETTINGS));} catch(Exception e) {startActivity(new Intent(Settings.ACTION_SETTINGS));}});
        LinearLayout adb=card();title(adb,"02  ·  ADB from your desktop",20);
        adb.addView(text("For Android 14+ builds exposing cmd game set. Enable debugging and authorize your desktop, then inspect adb shell cmd game help. Commands are copied for you to run; this app does not have ADB privileges.",14,MUTED));
        if (!selected.isEmpty() && Build.VERSION.SDK_INT>=34) {
            adb.addView(text(commandPreview(),12,LIME));
            button(adb,"Copy inspect + apply commands",false,() -> copy(commandPreview()));
        } else adb.addView(text(selected.isEmpty()?"Choose a game to generate commands.":"Advanced commands are unavailable below Android 14 in this version. Use in-game settings.",13,MUTED));
        LinearLayout elevated=card();title(elevated,"03  ·  Root access",20);
        elevated.addView(text("Status: "+rootStatus,14,LIME));
        elevated.addView(text("Restore captures effective mode and supported custom FPS/scale only. It does not snapshot unrelated OS settings or whether original values came from a vendor default versus an override. Unsupported formats and ANGLE/loading-boost settings block tuning.",12,MUTED));
        elevated.addView(text("Root is optional and checked only when requested. On supported Android 14+ systems, apply a per-game FPS cap and render scale. A cap cannot raise a game's native FPS. Games may ignore interventions. Restart the game after changing scale.",14,MUTED));
        Button check=button(elevated,busy?"Working…":"Check root & game service",false,this::checkRoot); check.setEnabled(!busy);
        if (rootReady && !selected.isEmpty()) {
            Button apply=button(elevated,"Apply custom profile / recommendation",true,() -> confirm(false));apply.setEnabled(!busy);
            Button reset=button(elevated,"Restore captured mode, FPS & scale",false,() -> confirm(true));reset.setEnabled(!busy);
            Button verify=button(elevated,"Verify OS settings now",false,this::verifyOs);verify.setEnabled(!busy);
        }
        String modified=prefs.getString("modifiedPackages","");
        org.json.JSONObject recovery=Profiles.read(prefs,selected+".recovery");
        if(recovery.has("modes")) elevated.addView(text("Recovery snapshot retained for this game · "+recovery.optString("phase")+". Keep it until captured-value restore succeeds.",12,LIME));
        if (!modified.isEmpty()) elevated.addView(text("Profiles with requested root overrides: "+modified+". Restore before uninstalling; uninstall does not undo OS changes. Legacy 0.1 overrides have no exact snapshot.",12,0xffffca7a));
        LinearLayout emu=card();title(emu,device.emulator?"Emulator profile enabled":"Using a desktop emulator?",20);
        CheckBox virtual=new CheckBox(this);virtual.setText("Treat this device as an emulator");virtual.setTextColor(TEXT);virtual.setChecked(prefs.getBoolean("emulatorOverride",false));emu.addView(virtual);
        virtual.setOnCheckedChangeListener((v,checked) -> {prefs.edit().putBoolean("emulatorOverride",checked).apply();show();});
        button(emu,"Open emulator setup assistant",false,() -> {labSection="Emulator";page="Lab";show();});
        emu.addView(text("BlueStacks, LDPlayer and other emulators expose virtual CPU, RAM and display data. Detection is best effort. Configure host CPU/RAM allocation, hardware virtualization, renderer and resolution inside the emulator's desktop settings. This app cannot measure or change the host GPU or Windows settings. Try 1280 × 720 and a 60 FPS cap as a baseline, then test your game.",14,MUTED));
        LinearLayout limits=card();title(limits,"What this version does",18);
        limits.addView(text("Android 5.0+ · ARM and x86 compatible Java code · local profiles · no network permission. No RAM-clearing tricks, thermal-limit changes, game-file edits or FPS-unlock claims. Thermal readings require Android 10+ and device support. Emulator and vendor behavior needs testing on each target.",13,MUTED));
    }
    private String commandPreview() {
        Advisor.Result r=recommendation();String p=Advisor.validatePackage(selected);
        return "# Inspect and save current state first\nadb shell cmd game help\nadb shell cmd game list-modes "+p+" > framepilot-before-modes.txt\nadb shell cmd game list-configs "+p+" > framepilot-before-configs.txt\n# Manual ADB has no automatic restore journal. Inspect saved values before proceeding.\nadb shell "+Advisor.setCommand(p,Profiles.fps(prefs,p,r),Profiles.scale(prefs,p,r))+"\n# Inspect acceptance (does not prove game performance)\nadb shell cmd game list-modes "+p+"\nadb shell cmd game list-configs "+p;
    }
    private void checkRoot() {
        if (busy) return;
        if (Build.VERSION.SDK_INT<34) {rootStatus="Advanced tuning requires Android 14+";show();return;}
        busy=true;show();
        worker.execute(() -> {
            try {
                String id=RootShell.run("id");if (!id.contains("uid=0")) throw new IllegalStateException("Root was not granted.");
                String help=RootShell.run("cmd game help");
                if (!help.contains("--downscale") || !help.contains("--fps") || !help.contains("reset") || !help.contains("custom") || !help.contains("list-configs")) throw new IllegalStateException("Required game commands are not exposed by this system.");
                handler.post(() -> {busy=false;rootReady=true;rootStatus="Granted · tuning commands available";show();});
            } catch(Exception e) {handler.post(() -> {busy=false;rootReady=false;rootStatus="Unavailable or denied";show();message("Root check",e.getMessage());});}
        });
    }
    private void confirm(boolean reset) {
        Advisor.Result r=recommendation();
        String explanation=reset?"Restore the captured mode and supported custom FPS/scale values for "+selectedName+". Other settings and original override provenance are outside this snapshot. External changes or unknown formats stop restore.":
            "Request "+Profiles.fps(prefs,selected,r)+" FPS cap and "+Math.round(Profiles.scale(prefs,selected,r)*100)+"% render scale. Capture original mode and supported FPS/scale values first. Unsupported settings block tuning. Actual game improvement is unverified.";
        new AlertDialog.Builder(this).setTitle(reset?"Restore captured values?":"Capture and apply?").setMessage(explanation).setNegativeButton("Cancel",null).setPositiveButton(reset?"Restore":"Apply",(d,w) -> mutate(reset)).show();
    }
    private void mutate(boolean reset) {
        if(busy || !rootReady || selected.isEmpty()) return;
        final String pkg=Advisor.validatePackage(selected);final Advisor.Result r=recommendation();
        final int fps=Profiles.fps(prefs,pkg,r);final double scale=Profiles.scale(prefs,pkg,r);busy=true;show();
        worker.execute(() -> {
            try {
                String result=reset?RootTuner.restore(prefs,pkg):RootTuner.apply(prefs,pkg,fps,scale);
                handler.post(() -> {busy=false;if(reset)updateModified(pkg,false);else updateModified(pkg,true);show();message("OS verification",result+"\nRestart the game and compare repeatable sessions in Lab. Read-back is configuration evidence, not FPS evidence.");});
            } catch(Exception e) {handler.post(() -> {busy=false;show();message("Tuning stopped",e.getMessage()+"\nAny recovery snapshot is retained. Inspect diagnostics before retrying.");});}
        });
    }
    private void verifyOs() {
        if(busy || !rootReady || selected.isEmpty())return;
        final String pkg=selected;busy=true;show();worker.execute(() -> {
            try {String result=RootTuner.verify(prefs,pkg);handler.post(() -> {busy=false;show();message("OS verification",result);});}
            catch(Exception e){handler.post(() -> {busy=false;show();message("Verification unavailable",e.getMessage());});}
        });
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);FeaturePanels.fileResult(this,request,result,data);
    }
    @android.annotation.SuppressLint("ApplySharedPref") // Recovery record must reach disk before a root command starts.
    private boolean updateModified(String pkg,boolean add) {
        TreeSet<String> names=new TreeSet<>();String old=prefs.getString("modifiedPackages","");
        if (!old.isEmpty()) names.addAll(Arrays.asList(old.split(", ")));
        if(add) names.add(pkg);else names.remove(pkg);
        StringBuilder joined=new StringBuilder();for(String name:names) {if(joined.length()>0) joined.append(", ");joined.append(name);}
        return prefs.edit().putString("modifiedPackages",joined.toString()).commit();
    }
    private void copy(String value) { ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("FramePilot commands",value));toast("Commands copied"); }
    private void toast(String s) {Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void message(String title,String body) {if(!isFinishing() && !isDestroyed()) new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK",null).show();}
}
