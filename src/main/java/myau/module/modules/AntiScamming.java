package myau.module.modules;

import myau.Myau;
import myau.event.EventManager;
import myau.event.EventTarget;
import myau.event.events.Event;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.util.ChatUtil;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AntiScamming Module - Discord Scam Detection and Prevention
 * Based on: https://github.com/SuperShadiao/hypixelhelper/wiki/Discord防骗警示
 *
 * Features:
 * 1. Detect fake rank upgrade scams in lobby chat
 * 2. Detect phishing links in chat messages
 * 3. Detect suspicious Discord friend requests
 * 4. RAT mod detection warnings
 */
public class AntiScamming extends Module {

    // Detection toggles
    public final BooleanProperty detectRankScams = new BooleanProperty("Detect Rank Scams", true);
    public final BooleanProperty detectPhishingLinks = new BooleanProperty("Detect Phishing Links", true);
    public final BooleanProperty detectDiscordScams = new BooleanProperty("Detect Discord Scams", true);
    public final BooleanProperty detectRATWarnings = new BooleanProperty("RAT Warnings", true);
    public final BooleanProperty blockScamMessages = new BooleanProperty("Block Scam Messages", false);
    public final BooleanProperty showNotifications = new BooleanProperty("Show Notifications", true);

    // Action mode
    public final ModeProperty actionMode = new ModeProperty("Action", 0,
            new String[]{"Warn Only", "Block Message", "Both"});

    // Scam patterns - Fake rank upgrade messages
    private final List<Pattern> rankScamPatterns = new ArrayList<>();
    
    // Phishing domain patterns
    private final List<String> suspiciousDomains = new ArrayList<>();
    private final List<Pattern> phishingPatterns = new ArrayList<>();
    
    // Discord scam patterns
    private final List<Pattern> discordScamPatterns = new ArrayList<>();
    
    // RAT mod indicators
    private final List<String> ratIndicators = new ArrayList<>();
    
    // Statistics
    private int blockedMessages = 0;
    private int warningsShown = 0;
    private final List<String> recentScams = new ArrayList<>();
    private static final int MAX_RECENT_SCAMS = 10;

    public AntiScamming() {
        super("AntiScamming", false);
        initPatterns();
    }

    private void initPatterns() {
        // === FAKE RANK SCAM PATTERNS ===
        // Common phrases used in rank upgrade scams
        rankScamPatterns.add(Pattern.compile("(?i)free\\s*(vip|mvp|rank|upgrade)", Pattern.CASE_INSENSITIVE));
        rankScamPatterns.add(Pattern.compile("(?i)want\\s*(a\\s*)?(vip|mvp|rank)", Pattern.CASE_INSENSITIVE));
        rankScamPatterns.add(Pattern.compile("(?i)gave\\s*me\\s*(vip|mvp|rank)", Pattern.CASE_INSENSITIVE));
        rankScamPatterns.add(Pattern.compile("(?i)add\\s*\\w+\\s*on\\s*discord", Pattern.CASE_INSENSITIVE));
        rankScamPatterns.add(Pattern.compile("(?i)join\\s*discord\\s*for\\s*(vip|mvp|rank|free)", Pattern.CASE_INSENSITIVE));
        rankScamPatterns.add(Pattern.compile("(?i)discord\\s*for\\s*(rank|vip|mvp|upgrade)", Pattern.CASE_INSENSITIVE));
        rankScamPatterns.add(Pattern.compile("(?i)msg\\s*me\\s*on\\s*discord", Pattern.CASE_INSENSITIVE));
        rankScamPatterns.add(Pattern.compile("(?i)dm\\s*me\\s*on\\s*discord", Pattern.CASE_INSENSITIVE));
        rankScamPatterns.add(Pattern.compile("(?i)add\\s*me\\s*on\\s*dc", Pattern.CASE_INSENSITIVE));

        // === SUSPICIOUS DOMAINS ===
        // Known phishing domain patterns
        suspiciousDomains.add("hypixel-gift");
        suspiciousDomains.add("hypixel-free");
        suspiciousDomains.add("hypixel-reward");
        suspiciousDomains.add("hypixel-store");
        suspiciousDomains.add("hypixel-shop");
        suspiciousDomains.add("minecraft-free");
        suspiciousDomains.add("minecraft-gift");
        suspiciousDomains.add("microsoft-verify");
        suspiciousDomains.add("minecraft-verify");
        suspiciousDomains.add("xbox-verify");
        
        // === PHISHING PATTERNS ===
        // Lookalike domains with character substitution
        phishingPatterns.add(Pattern.compile("hyp[\u00ec\u00ed\u00ee\u00ef]xel", Pattern.CASE_INSENSITIVE)); // hypìxel
        phishingPatterns.add(Pattern.compile("h[y\u00fd\u00ff]pixel", Pattern.CASE_INSENSITIVE)); // hÿpixel
        phishingPatterns.add(Pattern.compile("hyp[0o]ixel", Pattern.CASE_INSENSITIVE)); // hyp0ixel
        phishingPatterns.add(Pattern.compile("hypixe[l1][.,]", Pattern.CASE_INSENSITIVE)); // hypixel.net vs hypixel-net
        phishingPatterns.add(Pattern.compile("rewards?[.-]hypixel", Pattern.CASE_INSENSITIVE)); // rewards-hypixel
        phishingPatterns.add(Pattern.compile("hypixel[.-]store", Pattern.CASE_INSENSITIVE)); // fake store
        phishingPatterns.add(Pattern.compile("m[1l]crosoft", Pattern.CASE_INSENSITIVE)); // microsoft typos
        phishingPatterns.add(Pattern.compile("xbxlive", Pattern.CASE_INSENSITIVE)); // xbox typos
        
        // URL shorteners commonly used in scams
        phishingPatterns.add(Pattern.compile("(bit\\.ly|tinyurl|t\\.co|goo\\.gl|short\\.link)/", Pattern.CASE_INSENSITIVE));

        // === DISCORD SCAM PATTERNS ===
        discordScamPatterns.add(Pattern.compile("(?i)verify\\s*your\\s*(account|mc|minecraft)", Pattern.CASE_INSENSITIVE));
        discordScamPatterns.add(Pattern.compile("(?i)enter\\s*your\\s*email", Pattern.CASE_INSENSITIVE));
        discordScamPatterns.add(Pattern.compile("(?i)verification\\s*code", Pattern.CASE_INSENSITIVE));
        discordScamPatterns.add(Pattern.compile("(?i)link\\s*your\\s*(mc|minecraft)\\s*account", Pattern.CASE_INSENSITIVE));
        discordScamPatterns.add(Pattern.compile("(?i)claim\\s*your\\s*(vip|mvp|rank)", Pattern.CASE_INSENSITIVE));
        discordScamPatterns.add(Pattern.compile("(?i)join\\s*my\\s*discord", Pattern.CASE_INSENSITIVE));
        discordScamPatterns.add(Pattern.compile("(?i)discord\\s*server\\s*for\\s*free", Pattern.CASE_INSENSITIVE));

        // === RAT INDICATORS ===
        ratIndicators.add("Skyblock Dupe");
        ratIndicators.add("Free Rank");
        ratIndicators.add("Auto Miner");
        ratIndicators.add("Dupe Mod");
        ratIndicators.add("Free Items");
        ratIndicators.add("Item Generator");
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isEnabled()) return;

        String message = event.message.getUnformattedText();
        String formattedMessage = event.message.getFormattedText();
        
        if (message == null || message.isEmpty()) return;

        ScamType detectedScam = detectScam(message, formattedMessage);
        
        if (detectedScam != null) {
            handleScamDetection(event, detectedScam, message);
        }
    }

    private ScamType detectScam(String message, String formattedMessage) {
        // Check for rank scams
        if (detectRankScams.getValue()) {
            for (Pattern pattern : rankScamPatterns) {
                if (pattern.matcher(message).find()) {
                    return ScamType.FAKE_RANK;
                }
            }
        }

        // Check for phishing links
        if (detectPhishingLinks.getValue()) {
            // Check suspicious domains
            String lowerMessage = message.toLowerCase();
            for (String domain : suspiciousDomains) {
                if (lowerMessage.contains(domain)) {
                    return ScamType.PHISHING_LINK;
                }
            }
            
            // Check phishing patterns
            for (Pattern pattern : phishingPatterns) {
                if (pattern.matcher(message).find()) {
                    return ScamType.PHISHING_LINK;
                }
            }
        }

        // Check for Discord scams
        if (detectDiscordScams.getValue()) {
            for (Pattern pattern : discordScamPatterns) {
                if (pattern.matcher(message).find()) {
                    return ScamType.DISCORD_SCAM;
                }
            }
        }

        return null;
    }

    private void handleScamDetection(ClientChatReceivedEvent event, ScamType scamType, String originalMessage) {
        // Add to recent scams list
        String scamInfo = "[" + scamType.displayName + "] " + originalMessage.substring(0, Math.min(50, originalMessage.length()));
        recentScams.add(0, scamInfo);
        if (recentScams.size() > MAX_RECENT_SCAMS) {
            recentScams.remove(recentScams.size() - 1);
        }

        // Show notification
        if (showNotifications.getValue() && Myau.notificationManager != null) {
            String warning = "§c[AntiScam] §eDetected " + scamType.displayName + "!";
            Myau.notificationManager.add(warning, 0xFFAA00);
        }

        int action = actionMode.getValue();
        
        // Block message
        if ((action == 1 || action == 2) && blockScamMessages.getValue()) {
            event.setCanceled(true);
            blockedMessages++;
            ChatUtil.sendFormatted("§c[AntiScam] §7Blocked " + scamType.displayName + " message");
            return;
        }

        // Show warning in chat (if not blocked)
        if (action == 0 || action == 2) {
            warningsShown++;
            
            // Send warning message after the scam message
            String warning = getWarningMessage(scamType);
            ChatComponentText warningComponent = new ChatComponentText(warning);
            
            // Add hover text with more info
            ChatStyle style = new ChatStyle();
            style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ChatComponentText(getDetailedWarning(scamType))));
            warningComponent.setChatStyle(style);
            
            // Schedule warning to be sent after this event
            final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getMinecraft();
            new Thread(() -> {
                try {
                    Thread.sleep(50); // Small delay to appear after the scam message
                    if (minecraft.thePlayer != null) {
                        minecraft.thePlayer.addChatMessage(warningComponent);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private String getWarningMessage(ScamType scamType) {
        switch (scamType) {
            case FAKE_RANK:
                return EnumChatFormatting.RED + "[⚠ AntiScam] " + EnumChatFormatting.YELLOW + "检测到免费Rank诈骗！Hypixel不会通过Discord赠送Rank！";
            case PHISHING_LINK:
                return EnumChatFormatting.RED + "[⚠ AntiScam] " + EnumChatFormatting.YELLOW + "检测到钓鱼链接！请勿点击！";
            case DISCORD_SCAM:
                return EnumChatFormatting.RED + "[⚠ AntiScam] " + EnumChatFormatting.YELLOW + "检测到Discord诈骗！不要输入邮箱或验证码！";
            case RAT_MOD:
                return EnumChatFormatting.RED + "[⚠ AntiScam] " + EnumChatFormatting.YELLOW + "警告：可能是RAT模组！";
            default:
                return EnumChatFormatting.RED + "[⚠ AntiScam] " + EnumChatFormatting.YELLOW + "检测到可疑消息！";
        }
    }

    private String getDetailedWarning(ScamType scamType) {
        switch (scamType) {
            case FAKE_RANK:
                return EnumChatFormatting.RED + "免费Rank诈骗\n" +
                       EnumChatFormatting.GRAY + "骗子声称免费送Rank要求加Discord\n" +
                       EnumChatFormatting.GRAY + "真实的Rank只能通过store.hypixel.net购买";
            case PHISHING_LINK:
                return EnumChatFormatting.RED + "钓鱼链接警告\n" +
                       EnumChatFormatting.GRAY + "链接看起来像官方网站但域名有细微差别\n" +
                       EnumChatFormatting.GRAY + "输入邮箱会导致账号被盗！";
            case DISCORD_SCAM:
                return EnumChatFormatting.RED + "Discord诈骗\n" +
                       EnumChatFormatting.GRAY + "假验证页面要求输入邮箱\n" +
                       EnumChatFormatting.GRAY + "Hypixel绝不会要求输入邮箱！";
            case RAT_MOD:
                return EnumChatFormatting.RED + "RAT模组警告\n" +
                       EnumChatFormatting.GRAY + "远程访问木马可能窃取你的账号\n" +
                       EnumChatFormatting.GRAY + "只从官方渠道下载模组！";
            default:
                return EnumChatFormatting.RED + "可疑活动检测\n" +
                       EnumChatFormatting.GRAY + "请保持警惕，不要泄露个人信息";
        }
    }

    /**
     * Check if a mod file might be RAT
     * This can be called from other parts of the client
     */
    public static boolean checkModForRAT(String modName, long fileSize) {
        String lowerName = modName.toLowerCase();
        
        // Check suspicious names
        String[] ratKeywords = {"dupe", "free", "hack", "crack", "stealer", "rat", "token"};
        for (String keyword : ratKeywords) {
            if (lowerName.contains(keyword)) {
                return true;
            }
        }
        
        // Check file size (suspiciously small for claimed features)
        if (fileSize > 0 && fileSize < 50000) { // Less than 50KB
            return true;
        }
        
        return false;
    }

    /**
     * Get statistics for display
     */
    public String getStats() {
        return "Blocked: " + blockedMessages + " | Warnings: " + warningsShown;
    }

    public List<String> getRecentScams() {
        return new ArrayList<>(recentScams);
    }

    @Override
    public void onEnabled() {
        MinecraftForge.EVENT_BUS.register(this);
        ChatUtil.sendFormatted("§a[AntiScamming] §7Module enabled - Protecting against Discord scams");
    }

    @Override
    public void onDisabled() {
        MinecraftForge.EVENT_BUS.unregister(this);
        ChatUtil.sendFormatted("§c[AntiScamming] §7Module disabled");
    }

    /**
     * Scam type enumeration
     */
    public enum ScamType {
        FAKE_RANK("Fake Rank Scam"),
        PHISHING_LINK("Phishing Link"),
        DISCORD_SCAM("Discord Scam"),
        RAT_MOD("RAT Mod");

        public final String displayName;

        ScamType(String displayName) {
            this.displayName = displayName;
        }
    }
}
