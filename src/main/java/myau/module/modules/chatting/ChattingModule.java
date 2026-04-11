package myau.module.modules.chatting;

import myau.module.Module;
import myau.property.properties.*;
import myau.util.ChatUtil;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static myau.config.Config.mc;

/**
 * Chatting Module - Chat Enhancement Features
 * Integrated from Polyfrost/Chatting
 * Features: Chat Tabs, Search, Screenshot, Copy, Smooth Animations, Chat Heads, Spam Block
 */
public class ChattingModule extends Module {
    
    private static ChattingModule INSTANCE;
    
    // Chat Window Properties
    public final BooleanProperty chatWindowEnabled = new BooleanProperty("Chat Window", true);
    public final BooleanProperty extendBackground = new BooleanProperty("Extend Background", true);
    public final IntProperty chatWidth = new IntProperty("Chat Width", 280, 100, 500);
    public final BooleanProperty customChatWidth = new BooleanProperty("Custom Chat Width", false);
    public final IntProperty chatScale = new IntProperty("Chat Scale", 10, 5, 20); // 实际值 / 10
    public final IntProperty chatPosX = new IntProperty("Chat X", 2, 0, 1000);
    public final IntProperty chatPosY = new IntProperty("Chat Y", -38, -500, 500);
    public final IntProperty chatPaddingX = new IntProperty("Padding X", 2, 0, 20);
    public final IntProperty chatPaddingY = new IntProperty("Padding Y", 0, 0, 20);
    
    // Animation Properties
    public final BooleanProperty smoothChat = new BooleanProperty("Smooth Chat", true);
    public final IntProperty messageSpeed = new IntProperty("Message Speed", 5, 0, 10); // 实际值 / 10
    public final BooleanProperty smoothScrolling = new BooleanProperty("Smooth Scrolling", true);
    public final IntProperty scrollingSpeed = new IntProperty("Scrolling Speed", 3, 1, 10);
    public final BooleanProperty smoothBackground = new BooleanProperty("Smooth Background", true);
    public final IntProperty backgroundDuration = new IntProperty("BG Duration", 400, 100, 2000);
    public final BooleanProperty fadeChat = new BooleanProperty("Fade Chat", true);
    public final IntProperty fadeTime = new IntProperty("Fade Time", 100, 0, 600); // 实际值 / 10
    
    // Button Properties
    public final BooleanProperty chatCopy = new BooleanProperty("Chat Copy", true);
    public final BooleanProperty chatDelete = new BooleanProperty("Chat Delete", true);
    public final BooleanProperty chatDeleteHistory = new BooleanProperty("Clear History", true);
    public final BooleanProperty chatScreenshot = new BooleanProperty("Chat Screenshot", true);
    public final BooleanProperty chatSearch = new BooleanProperty("Chat Search", true);
    public final BooleanProperty rightClickCopy = new BooleanProperty("Right Click Copy", false);
    public final BooleanProperty rightClickCopyCtrl = new BooleanProperty("Right Click Ctrl", true);
    
    // Chat Tabs Properties
    public final BooleanProperty chatTabs = new BooleanProperty("Chat Tabs", true);
    public final BooleanProperty hypixelOnlyChatTabs = new BooleanProperty("Hypixel Only Tabs", true);
    
    // Chat Heads Properties
    public final BooleanProperty chatHeads = new BooleanProperty("Chat Heads", true);
    public final BooleanProperty hideChatHeadOnConsecutive = new BooleanProperty("Hide Consecutive Heads", true);
    public final BooleanProperty offsetNonPlayerMessages = new BooleanProperty("Offset Non-Player", false);
    
    // Chat Shortcuts Properties
    public final BooleanProperty chatShortcuts = new BooleanProperty("Chat Shortcuts", false);
    public final BooleanProperty hypixelOnlyChatShortcuts = new BooleanProperty("Hypixel Only Shortcuts", true);
    
    // Spam Blocker Properties
    public final BooleanProperty spamBlocker = new BooleanProperty("Spam Blocker", true);
    public final IntProperty spamThreshold = new IntProperty("Spam Threshold", 100, 10, 500);
    public final BooleanProperty customChatFormatting = new BooleanProperty("Custom Formatting", false);
    public final BooleanProperty hideSpam = new BooleanProperty("Hide Spam", false);
    
    // Chat Peek Properties
    public final BooleanProperty chatPeek = new BooleanProperty("Chat Peek", false);
    public final BooleanProperty peekScrolling = new BooleanProperty("Peek Scrolling", true);
    public final IntProperty chatPeekKey = new IntProperty("Peek Key", 90, 0, 255);
    public final BooleanProperty peekModeToggle = new BooleanProperty("Peek Toggle", false);
    
    // Visual Properties
    public final ModeProperty textRenderType = new ModeProperty("Text Render", 1, new String[]{"No Shadow", "Shadow", "Full Shadow"});
    public final BooleanProperty underlinedLinks = new BooleanProperty("Underlined Links", false);
    public final BooleanProperty removeScrollBar = new BooleanProperty("Remove Scrollbar", true);
    public final BooleanProperty buttonShadow = new BooleanProperty("Button Shadow", true);
    public final BooleanProperty inputFieldDraft = new BooleanProperty("Input Draft", true);
    public final BooleanProperty compactInputBox = new BooleanProperty("Compact Input", true);
    
    // Screenshot Properties
    public final ModeProperty screenshotMode = new ModeProperty("Screenshot Mode", 0, new String[]{"Save", "Clipboard", "Both"});
    
    // Colors (RGB integer values)
    public final ColorProperty hoveredChatBackgroundColor = new ColorProperty("Hovered BG", 0x505050);
    public final ColorProperty chatButtonColor = new ColorProperty("Button Color", 0xFFFFFF);
    public final ColorProperty chatButtonHoveredColor = new ColorProperty("Button Hover", 0xFFA0FF);
    public final ColorProperty chatButtonBackgroundColor = new ColorProperty("Button BG", 0x000000);
    public final ColorProperty chatButtonHoveredBackgroundColor = new ColorProperty("Button Hover BG", 0xFFFFFF);
    public final ColorProperty chatInputBackgroundColor = new ColorProperty("Input BG", 0x000000);
    
    // Tooltip Properties
    public final BooleanProperty removeTooltipBackground = new BooleanProperty("Remove Tooltip BG", false);
    public final ModeProperty tooltipTextRenderType = new ModeProperty("Tooltip Text", 1, new String[]{"No Shadow", "Shadow", "Full Shadow"});
    
    // Internal state
    private boolean isPeeking = false;
    private boolean shouldSmoothScroll = false;
    private ChatTab currentTab = ChatTab.ALL;
    public String searchQuery = "";
    
    // Chat shortcuts
    private final Map<String, String> shortcuts = new HashMap<>();
    
    public enum ChatTab {
        ALL("All"),
        PARTY("Party"),
        GUILD("Guild"),
        PM("PM");
        
        private final String name;
        
        ChatTab(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    public ChattingModule() {
        super("Chatting", false);
        INSTANCE = this;
        
        // Initialize shortcuts
        shortcuts.put("cc", "cc");
        shortcuts.put("gc", "gc");
        shortcuts.put("p", "p");
        shortcuts.put("r", "r");
        shortcuts.put("shout", "shout");
        shortcuts.put("coords", "My coordinates are %x%, %y%, %z%");
        shortcuts.put("fps", "I have %fps% FPS");
        shortcuts.put("ping", "My ping is %ping%ms");
    }
    
    public static ChattingModule getInstance() {
        return INSTANCE;
    }
    
    @Override
    public void onEnabled() {
        if (chatTabs.getValue()) {
            ChatTabs.initialize();
        }
        if (chatShortcuts.getValue()) {
            ChatShortcuts.initialize();
        }
    }
    
    @Override
    public void onDisabled() {
        // Cleanup
    }
    
    // Tab switching
    public void switchTab() {
        ChatTab[] tabs = ChatTab.values();
        int currentIndex = currentTab.ordinal();
        int nextIndex = (currentIndex + 1) % tabs.length;
        currentTab = tabs[nextIndex];
        ChatUtil.sendMessage(EnumChatFormatting.GREEN + "[Chatting] " + EnumChatFormatting.RESET + "Switched to " + currentTab.getName() + " tab");
    }
    
    public ChatTab getCurrentTab() {
        return currentTab;
    }
    
    // Smooth scroll state
    public void setShouldSmoothScroll(boolean smooth) {
        this.shouldSmoothScroll = smooth;
    }
    
    public boolean getShouldSmoothScroll() {
        return shouldSmoothScroll;
    }
    
    // Shortcut processing
    public String processShortcut(String command) {
        if (!chatShortcuts.getValue()) return command;
        
        for (Map.Entry<String, String> entry : shortcuts.entrySet()) {
            if (command.equalsIgnoreCase(entry.getKey())) {
                return processVariables(entry.getValue());
            }
        }
        return command;
    }
    
    private String processVariables(String text) {
        // Replace variables
        if (mc != null && mc.thePlayer != null) {
            text = text.replace("%x%", String.valueOf((int) mc.thePlayer.posX));
            text = text.replace("%y%", String.valueOf((int) mc.thePlayer.posY));
            text = text.replace("%z%", String.valueOf((int) mc.thePlayer.posZ));
        }
        if (mc != null) {
            text = text.replace("%fps%", String.valueOf(mc.getDebugFPS()));
        }
        return text;
    }
}