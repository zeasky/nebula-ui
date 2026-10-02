package com.nebulaui;

/** All toggle states for the menu. */
public final class Settings {
    private Settings() {}

    // HUD
    public static boolean fps = false;
    public static boolean hud = false;
    public static boolean coords = false;
    public static boolean clock = false;
    // Visual
    public static boolean rainbow = false;
    public static boolean particles = true;
    public static boolean strongGlow = false;
    // Combat / Base finding / Donut (switches only)
    public static boolean cps = false, combo = false, armor = false;
    public static boolean waypoints = false, chunkGrid = false, deathMark = false;
    public static boolean sprinkles = false, glaze = false, spin = false;
    /** Names of switches that are currently turned on (combat list). */
    public static final java.util.Set<String> on = new java.util.HashSet<>();
    // Interface
    public static boolean dim = true;
    public static boolean scanlines = true;
    public static boolean hints = true;
}
