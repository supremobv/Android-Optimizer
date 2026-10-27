package com.primo.optimizer;

import android.content.SharedPreferences;
import org.json.*;

final class Profiles {
    static JSONObject read(SharedPreferences p,String key) {
        try{return new JSONObject(p.getString(key,"{}"));}catch(JSONException e){return new JSONObject();}
    }
    static JSONObject object(Object... pairs) {
        JSONObject o=new JSONObject();try {for(int i=0;i<pairs.length;i+=2) {
            Object value=pairs[i+1];if(value instanceof Double && (Double.isNaN((Double)value)||Double.isInfinite((Double)value))) value=JSONObject.NULL;
            o.put((String)pairs[i],value);
        }}catch(JSONException e){throw new IllegalArgumentException(e);}return o;
    }
    static int fps(SharedPreferences p,String pkg,Advisor.Result r) {return read(p,pkg+".custom").optInt("fps",r.fps);}
    static double scale(SharedPreferences p,String pkg,Advisor.Result r) {return read(p,pkg+".custom").optDouble("scale",r.scale);}
    static String state(SharedPreferences p,String pkg) {return p.getString(pkg+".osState","Recommendation only · OS not checked");}
    static String version(android.content.Context c,String pkg) {
        try {android.content.pm.PackageInfo info=c.getPackageManager().getPackageInfo(pkg,0);return info.versionName+" ("+(android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode)+")";}
        catch(Exception e){return "Unavailable";}
    }
    static String identity(android.content.Context c,String pkg) {return android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+" / "+android.os.Build.VERSION.SDK_INT+" / "+version(c,pkg);}
}
