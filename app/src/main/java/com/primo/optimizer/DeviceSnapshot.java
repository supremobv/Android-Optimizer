package com.primo.optimizer;

import android.app.ActivityManager;
import android.content.*;
import android.os.*;
import java.util.Locale;

final class DeviceSnapshot {
    double ram, available, batteryC = Double.NaN;
    int cores, thermal = -1, gles, batteryPercent = -1;
    float refresh;
    boolean powerSave, emulator;
    String model, abi;
    static DeviceSnapshot read(Context context) {
        DeviceSnapshot d = new DeviceSnapshot();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        d.ram=mi.totalMem / 1073741824.0; d.available=mi.availMem / 1073741824.0;
        d.cores=Runtime.getRuntime().availableProcessors();
        d.gles=am.getDeviceConfigurationInfo().reqGlEsVersion;
        d.refresh=((android.view.WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRefreshRate();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        d.powerSave=pm.isPowerSaveMode();
        if (Build.VERSION.SDK_INT >= 29) {try{d.thermal=pm.getCurrentThermalStatus();}catch(RuntimeException unavailable){d.thermal=-1;}}
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(battery!=null) {
            int level=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1), scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
            if(level>=0 && scale>0) d.batteryPercent=Math.round(level*100f/scale);
        }
        if (battery != null && battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)) {
            int t=battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            if (t != 0) d.batteryC=t / 10.0;
        }
        d.model=Build.MANUFACTURER + " " + Build.MODEL;
        d.abi=Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "Unknown";
        String hints=(Build.FINGERPRINT+" "+Build.MODEL+" "+Build.MANUFACTURER+" "+Build.HARDWARE+" "+Build.PRODUCT).toLowerCase(Locale.ROOT);
        d.emulator=hints.contains("generic") || hints.contains("emulator") || hints.contains("goldfish") || hints.contains("ranchu") || hints.contains("bluestacks") || hints.contains("ldplayer") || hints.contains("vbox") || hints.contains("nox");
        return d;
    }
    String thermalLabel() {
        String[] names={"Normal", "Light", "Moderate", "Severe", "Critical", "Emergency", "Shutdown"};
        return thermal >= 0 && thermal < names.length ? names[thermal] : "Unavailable on this device";
    }
}
