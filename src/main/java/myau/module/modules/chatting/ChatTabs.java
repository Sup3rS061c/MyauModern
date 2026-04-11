package myau.module.modules.chatting;

import myau.Myau;
import myau.module.Module;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat Tabs System - Filter chat messages by category
 * ALL, PARTY, GUILD, PM
 */
public class ChatTabs {
    
    private static List<ChatTab> tabs = new ArrayList<>();
    private static boolean initialized = false;
    private static boolean hasCancelledAnimation = false;
    
    public enum ChatTabType {
        ALL("All", 0xFFFFFFFF),
        PARTY("Party", 0xFF55FFFF),
        GUILD("Guild", 0xFF55FF55),
        PM("PM", 0xFFFFAA00),
        SYSTEM("System", 0xFFAAAAAA);
        
        private final String name;
        private final int color;
        
        ChatTabType(String name, int color) {
            this.name = name;
            this.color = color;
        }
        
        public String getName() { return name; }
        public int getColor() { return color; }
    }
    
    public static class ChatTab {
        private ChatTabType type;
        private GuiButton button;
        private boolean active;
        
        public ChatTab(ChatTabType type) {
            this.type = type;
            this.active = false;
        }
        
        public ChatTabType getType() { return type; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public GuiButton getButton() { return button; }
        public void setButton(GuiButton button) { this.button = button; }
    }
    
    public static void initialize() {
        if (initialized) return;
        
        tabs.clear();
        for (ChatTabType type : ChatTabType.values()) {
            tabs.add(new ChatTab(type));
        }
        
        // Set ALL as default active
        tabs.get(0).setActive(true);
        
        initialized = true;
    }
    
    public static List<ChatTab> getTabs() {
        if (!initialized) initialize();
        return tabs;
    }
    
    public static boolean shouldRender(String message) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.chatTabs.getValue()) return true;
        
        // Get current active tab
        ChatTabType activeTab = ChatTabType.ALL;
        for (ChatTab tab : tabs) {
            if (tab.isActive()) {
                activeTab = tab.getType();
                break;
            }
        }
        
        if (activeTab == ChatTabType.ALL) return true;
        
        String lowerMsg = message.toLowerCase();
        
        switch (activeTab) {
            case PARTY:
                return lowerMsg.contains("party") || lowerMsg.contains("p >");
            case GUILD:
                return lowerMsg.contains("guild") || lowerMsg.contains("g >") || lowerMsg.contains("guild >");
            case PM:
                return lowerMsg.contains("->") || lowerMsg.contains("<-") || lowerMsg.contains("from") || lowerMsg.contains("to");
            case SYSTEM:
                return lowerMsg.contains("[system]") || lowerMsg.contains("joined") || lowerMsg.contains("left");
            default:
                return true;
        }
    }
    
    public static boolean shouldRender(net.minecraft.util.IChatComponent component) {
        return shouldRender(component.getUnformattedText());
    }
    
    public static void setActiveTab(ChatTabType type) {
        for (ChatTab tab : tabs) {
            tab.setActive(tab.getType() == type);
        }
    }
    
    public static ChatTabType getActiveTab() {
        for (ChatTab tab : tabs) {
            if (tab.isActive()) return tab.getType();
        }
        return ChatTabType.ALL;
    }
    
    public static boolean getHasCancelledAnimation() {
        return hasCancelledAnimation;
    }
    
    public static void setHasCancelledAnimation(boolean cancelled) {
        hasCancelledAnimation = cancelled;
    }
    
    public static void switchToNextTab() {
        ChatTabType[] types = ChatTabType.values();
        ChatTabType current = getActiveTab();
        int nextIndex = (current.ordinal() + 1) % types.length;
        setActiveTab(types[nextIndex]);
    }
    
    public static void switchToPrevTab() {
        ChatTabType[] types = ChatTabType.values();
        ChatTabType current = getActiveTab();
        int prevIndex = (current.ordinal() - 1 + types.length) % types.length;
        setActiveTab(types[prevIndex]);
    }
    
    public static void cleanup() {
        tabs.clear();
        initialized = false;
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
