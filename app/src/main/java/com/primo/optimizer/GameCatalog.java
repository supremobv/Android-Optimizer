package com.primo.optimizer;

/** Editorial starting points. Tested status is stored separately per device/game version. */
final class GameCatalog {
    static String advice(String pkg) {
        if(pkg.equals("com.mobile.legends")) return "Mobile Legends starter: prioritize frame-rate consistency; try lower shadows and effects before increasing quality. Match the game's offered frame-rate options. Menu names and caps vary by release.";
        if(pkg.equals("com.tencent.ig") || pkg.equals("com.pubg.imobile")) return "PUBG/BGMI starter: begin with the game's lowest graphics preset and an offered stable frame-rate option. Device allowlists may restrict choices; OS controls do not bypass them.";
        if(pkg.equals("com.miHoYo.GenshinImpact") || pkg.equals("com.hoyoverse.genshinimpact")) return "Genshin starter: begin with low shadows, effects and render resolution at 30 FPS. Dense scenes and heat can require further reductions.";
        if(pkg.equals("com.activision.callofduty.shooter")) return "Call of Duty starter: prioritize the game's frame-rate option over graphics detail; reduce shadows and effects. Availability varies by device and game version.";
        if(pkg.equals("com.ea.game.pvzfree_row")) return "Plants vs. Zombies starter: use the game's available defaults first; a platform FPS cap cannot bypass an engine cap.";
        return "No game-specific template yet. Select workload manually, change one setting at a time, and use a repeatable scene to establish a local profile.";
    }
}
