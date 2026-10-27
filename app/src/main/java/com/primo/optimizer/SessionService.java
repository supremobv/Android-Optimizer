package com.primo.optimizer;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;

/** User-started, bounded sampling. The assigned game is not proof of foreground attribution. */
public final class SessionService extends Service {
    static volatile boolean running=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private android.content.SharedPreferences prefs;
    private JSONObject session;
    private long start, lastAlert=-60000;
    private int previousThermal=-1;
    private final Runnable tick=new Runnable(){public void run(){sample();if(session!=null && SystemClock.elapsedRealtime()-start>=1800000) finish("30-minute limit");else if(session!=null) handler.postDelayed(this,5000);}};
    @Override public void onCreate(){super.onCreate();prefs=getSharedPreferences("profiles",MODE_PRIVATE);}
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        startForeground(71,notification("FramePilot session","Recording device conditions · tap to return",false));
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if("stop".equals(intent.getAction())) {
            if(session==null) {session=Profiles.read(prefs,"activeSession");start=session.optLong("startElapsed",SystemClock.elapsedRealtime());}
            if(running) sample();finish(running?"User stopped":"Interrupted · last available samples only");return START_NOT_STICKY;
        }
        if(running) return START_NOT_STICKY;
        String pkg=intent.getStringExtra("pkg"), kind=intent.getStringExtra("kind"), scene=intent.getStringExtra("scene");
        try{Advisor.validatePackage(pkg);if(!"before".equals(kind)&&!"after".equals(kind))throw new IllegalArgumentException();if(scene==null||scene.trim().isEmpty())throw new IllegalArgumentException();}
        catch(Exception e){stopSelf();return START_NOT_STICKY;}
        start=SystemClock.elapsedRealtime();
        session=Profiles.object("package",pkg,"kind",kind,"scene",scene.trim(),"startElapsed",start,"startedAt",System.currentTimeMillis(),"identity",Profiles.identity(this,pkg),"osState",Profiles.state(prefs,pkg),"profile",Profiles.read(prefs,pkg+".custom"),"target",intent.getIntExtra("target",30),"samples",new JSONArray(),"attribution","User assigned; foreground game not verified");
        try{session.put("profile",Profiles.object("fps",intent.getIntExtra("target",30),"scale",intent.getDoubleExtra("scale",0.7),"quality",intent.getStringExtra("quality"),"priority",Profiles.read(prefs,pkg+".custom").optString("priority","Consistency")));}catch(JSONException ignored){}
        running=true;tick.run();return START_NOT_STICKY;
    }
    private void sample() {
        if(session==null)return;
        try {
            DeviceSnapshot d=DeviceSnapshot.read(this);long now=SystemClock.elapsedRealtime();
            JSONArray samples=session.getJSONArray("samples");
            if(samples.length()<400) samples.put(Profiles.object("elapsedMs",Math.max(0,now-start),"availableGb",d.available,"batteryC",d.batteryC,"batteryPercent",d.batteryPercent,"thermal",d.thermal,"saver",d.powerSave));
            session.put("lastElapsed",now).put("durationMs",Math.max(0,now-start));
            prefs.edit().putString("activeSession",session.toString()).apply();
            if(ThermalPolicy.shouldAlert(previousThermal,d.thermal,lastAlert,now)) {
                ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(72,notification("Thermal pressure increased",d.thermalLabel()+" · lower settings or pause to cool down",true));lastAlert=now;
            }
            previousThermal=d.thermal;
        }catch(Exception e){finish("Sampling failed: "+e.getClass().getSimpleName());}
    }
    private void finish(String reason) {
        handler.removeCallbacksAndMessages(null);
        if(session!=null && session.has("package")) {
            try {session.put("endedAt",System.currentTimeMillis()).put("stopReason",reason);
                prefs.edit().putString(session.getString("package")+".session."+session.getString("kind"),session.toString()).remove("activeSession").commit();
            }catch(JSONException ignored){}
        }
        session=null;running=false;stopForeground(true);stopSelf();
    }
    private Notification notification(String title,String text,boolean alert) {
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);String channel=alert?"thermal":"sessions";
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(channel,alert?"Thermal alerts":"Session recording",alert?NotificationManager.IMPORTANCE_HIGH:NotificationManager.IMPORTANCE_LOW));
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,channel):new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setContentIntent(open).setOngoing(!alert).setAutoCancel(alert).build();
    }
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(running)finish("Recorder destroyed · last samples retained");super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
