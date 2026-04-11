package myau.module.modules.chatting;

import myau.Myau;
import net.minecraft.util.EnumChatFormatting;

import java.util.HashMap;
import java.util.Map;

/**
 * Chat Shortcuts System - Quick command aliases
 */
public class ChatShortcuts {
    
    private static final Map<String, String> shortcuts = new HashMap<>();
    private static boolean initialized = false;
    
    // Default shortcuts
    private static void registerDefaults() {
        // Hypixel shortcuts
        register("cc", "/chat g");
        register("gc", "/chat g");
        register("p", "/p ");
        register("r", "/r ");
        register("shout", "/shout ");
        register("o", "/o ");
        register("accept", "/accept ");
        register("t", "/t ");
        register("w", "/w ");
        register("msg", "/msg ");
        register("tell", "/tell ");
        register("reply", "/r ");
        
        // Utility shortcuts with variables
        register("coords", getCoordinates());
        register("fps", getFps());
        register("ping", getPing());
        register("ip", getServerIP());
        
        // Quick commands
        register("inv", "/inv");
        register("pjs", "/pjs");
        register("lobby", "/lobby");
        register("hub", "/hub");
        register("leave", "/leave");
        register("requeue", "/requeue");
        register("rq", "/rq");
        register("spectate", "/spectate");
        register("spec", "/spec");
        register("friend", "/f ");
        register("f", "/f ");
        register("party", "/p ");
        register("ignore", "/ignore ");
        register("unignore", "/unignore ");
        register("report", "/report ");
    }
    
    public static void initialize() {
        if (initialized) return;
        
        shortcuts.clear();
        registerDefaults();
        
        initialized = true;
    }
    
    public static void register(String shortcut, String replacement) {
        shortcuts.put(shortcut.toLowerCase(), replacement);
    }
    
    public static void unregister(String shortcut) {
        shortcuts.remove(shortcut.toLowerCase());
    }
    
    public static String getReplacement(String shortcut) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.chatShortcuts.getValue()) {
            return shortcut;
        }
        
        String replacement = shortcuts.get(shortcut.toLowerCase());
        if (replacement != null) {
            return processVariables(replacement);
        }
        return shortcut;
    }
    
    public static String processShortcut(String input) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.chatShortcuts.getValue()) {
            return input;
        }
        
        String[] parts = input.split(" ", 2);
        String shortcut = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        String replacement = shortcuts.get(shortcut);
        if (replacement != null) {
            replacement = processVariables(replacement);
            if (!args.isEmpty()) {
                replacement += args;
            }
            return replacement;
        }
        
        return input;
    }
    
    public static String handleSentCommand(String command) {
        return processShortcut(command);
    }
    
    private static String processVariables(String text) {
        if (Myau.mc != null && Myau.mc.thePlayer != null) {
            text = text.replace("{x}", String.valueOf((int) Myau.mc.thePlayer.posX));
            text = text.replace("{y}", String.valueOf((int) Myau.mc.thePlayer.posY));
            text = text.replace("{z}", String.valueOf((int) Myau.mc.thePlayer.posZ));
            text = text.replace("%x%", String.valueOf((int) Myau.mc.thePlayer.posX));
            text = text.replace("%y%", String.valueOf((int) Myau.mc.thePlayer.posY));
            text = text.replace("%z%", String.valueOf((int) Myau.mc.thePlayer.posZ));
        }
        
        if (Myau.mc != null) {
            text = text.replace("{fps}", String.valueOf(Myau.mc.getDebugFPS()));
            text = text.replace("%fps%", String.valueOf(Myau.mc.getDebugFPS()));
        }
        
        if (Myau.mc != null && Myau.mc.getCurrentServerData() != null) {
            text = text.replace("{ip}", Myau.mc.getCurrentServerData().serverIP);
            text = text.replace("%ip%", Myau.mc.getCurrentServerData().serverIP);
        }
        
        if (Myau.mc != null && Myau.mc.thePlayer != null && Myau.mc.thePlayer.sendQueue != null) {
            int ping = Myau.mc.thePlayer.sendQueue.getPlayerInfo(Myau.mc.thePlayer.getUniqueID()).getResponseTime();
            text = text.replace("{ping}", String.valueOf(ping));
            text = text.replace("%ping%", String.valueOf(ping));
        }
        
        return text;
    }
    
    private static String getCoordinates() {
        if (Myau.mc != null && Myau.mc.thePlayer != null) {
            return String.format("My coordinates are X: %d Y: %d Z: %d", 
                (int) Myau.mc.thePlayer.posX,
                (int) Myau.mc.thePlayer.posY,
                (int) Myau.mc.thePlayer.posZ);
        }
        return "My coordinates are unknown";
    }
    
    private static String getFps() {
        if (Myau.mc != null) {
            return String.format("I have %d FPS", Myau.mc.getDebugFPS());
        }
        return "FPS unknown";
    }
    
    private static String getPing() {
        if (Myau.mc != null && Myau.mc.thePlayer != null && Myau.mc.thePlayer.sendQueue != null) {
            int ping = Myau.mc.thePlayer.sendQueue.getPlayerInfo(Myau.mc.thePlayer.getUniqueID()).getResponseTime();
            return String.format("My ping is %dms", ping);
        }
        return "Ping unknown";
    }
    
    private static String getServerIP() {
        if (Myau.mc != null && Myau.mc.getCurrentServerData() != null) {
            return "Currently playing on: " + Myau.mc.getCurrentServerData().serverIP;
        }
        return "Not connected to a server";
    }
    
    public static Map<String, String> getAllShortcuts() {
        return new HashMap<>(shortcuts);
    }
    
    public static void clear() {
        shortcuts.clear();
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
    
    public static void cleanup() {
        shortcuts.clear();
        initialized = false;
    }
}
