package com.primo.optimizer;

/** Conservative resource heuristic, deliberately not an FPS predictor or GPU benchmark. */
public final class Advisor {
    public static final class Result {
        public final String quality, verdict, reason;
        public final int fps;
        public final double scale;
        Result(String q, int f, double s, String v, String r) { quality=q; fps=f; scale=s; verdict=v; reason=r; }
    }
    public static Result assess(double ramGb, double freeGb, int cores, double refresh, int thermal,
                                boolean powerSave, boolean emulator, int workload) {
        if (Double.isNaN(ramGb) || ramGb <= 0 || cores < 1 || workload < 0 || workload > 2)
            throw new IllegalArgumentException("Invalid device or workload");
        int tier = ramGb >= 7 && cores >= 8 ? 2 : ramGb >= 3.5 && cores >= 4 ? 1 : 0;
        boolean constrained = freeGb < 1 || thermal >= 2 || powerSave;
        int headroom = tier - workload - (constrained ? 1 : 0);
        String quality = headroom >= 1 ? "High" : headroom == 0 ? "Medium" : "Low";
        int fps = headroom >= 0 && !constrained && refresh >= 59 ? 60 : 30;
        if (refresh > 0 && refresh < fps) fps = Math.max(1, (int) refresh);
        double scale = headroom >= 1 ? 1.0 : headroom == 0 ? 0.85 : 0.7;
        String verdict = thermal >= 3 ? "Cool down before playing" : headroom < -1 ? "Demand may exceed device capacity" : "Try " + quality.toLowerCase(java.util.Locale.ROOT) + " graphics at " + fps + " FPS";
        String reason = "Starting point from RAM and CPU core count. GPU speed, game version, scene complexity and drivers are unmeasured. "
                + (constrained ? "Memory pressure, heat or battery saver reduces this recommendation. " : "")
                + (emulator ? "Virtual hardware cannot reveal host GPU performance; confidence is very low." : "Confidence is low until tested in your game.");
        return new Result(quality, fps, scale, verdict, reason);
    }
    public static String validatePackage(String value) {
        if (value == null || !value.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))
            throw new IllegalArgumentException("Invalid package name");
        return value;
    }
    public static String setCommand(String pkg, int fps, double scale) {
        validatePackage(pkg);
        if (fps < 1 || fps > 120 || (scale != 0.7 && scale != 0.85 && scale != 1.0))
            throw new IllegalArgumentException("Invalid tuning values");
        return "cmd game set --downscale " + String.valueOf(scale) + " --fps " + fps + " " + pkg;
    }
}
