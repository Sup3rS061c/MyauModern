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

import java.io.File;
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
    public final BooleanProperty detectInfoStealer = new BooleanProperty("Info Stealer Detection", true);
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
    
    // Info Stealer indicators (discord token stealers, session stealers)
    private final List<String> infoStealerIndicators = new ArrayList<>();
    private final List<Pattern> infoStealerPatterns = new ArrayList<>();
    private final List<String> infoStealerFilePatterns = new ArrayList<>();
    
    // Virus/Malware detection patterns (static for reflection scanning)
    private static final List<String> maliciousClassPatterns = new ArrayList<>();
    private static final List<String> maliciousMethodPatterns = new ArrayList<>();
    private static final List<String> suspiciousUrls = new ArrayList<>();
    
    // URL Whitelist - Safe domains that should be allowed
    private final List<String> urlWhitelist = new ArrayList<>();
    private final List<Pattern> whitelistPatterns = new ArrayList<>();
    
    // Official Discord servers - Only these invite codes are trusted
    private final List<String> officialDiscordServers = new ArrayList<>();
    private final List<String> verifiedDiscordServers = new ArrayList<>();
    
    // Discord invite pattern
    private final Pattern discordInvitePattern = Pattern.compile("discord\\.gg/([a-zA-Z0-9-]+)", Pattern.CASE_INSENSITIVE);
    
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
        
        // === INFO STEALER INDICATORS ===
        // Discord token stealers
        infoStealerIndicators.add("Essential Mod");  // Fake Essential mod injection
        infoStealerIndicators.add("Boze");  // Boze mod injection
        infoStealerIndicators.add("Token Grabber");
        infoStealerIndicators.add("Session Stealer");
        infoStealerIndicators.add("Cookie Stealer");
        infoStealerIndicators.add("Discord Token");
        infoStealerIndicators.add("Webhook Token");
        infoStealerIndicators.add("Account Stealer");
        infoStealerIndicators.add("Password Stealer");
        infoStealerIndicators.add("Credential Stealer");
        infoStealerIndicators.add("Minecraft Stealer");
        infoStealerIndicators.add("Browser Stealer");
        
        // Info stealer file patterns (commonly used in malicious mods)
        infoStealerFilePatterns.add("essential-rat");
        infoStealerFilePatterns.add("boze-rat");
        infoStealerFilePatterns.add("token-grabber");
        infoStealerFilePatterns.add("webhook-sender");
        infoStealerFilePatterns.add("session-logger");
        infoStealerFilePatterns.add("credential-harvest");
        infoStealerFilePatterns.add("discord-webhook");
        infoStealerFilePatterns.add("log-sender");
        
        // Info stealer message patterns
        infoStealerPatterns.add(Pattern.compile("(?i)(?:essential|boze)\\s*(?:mod|injection|rat|stealer)", Pattern.CASE_INSENSITIVE));
        infoStealerPatterns.add(Pattern.compile("(?i)token\\s*(?:grabber|stealer|logger)", Pattern.CASE_INSENSITIVE));
        infoStealerPatterns.add(Pattern.compile("(?i)session\\s*(?:stealer|logger|grabber)", Pattern.CASE_INSENSITIVE));
        infoStealerPatterns.add(Pattern.compile("(?i)(?:discord|minecraft)\\s*(?:token|session)\\s*stealer", Pattern.CASE_INSENSITIVE));
        infoStealerPatterns.add(Pattern.compile("(?i)account\\s*(?:cracker|checker|stealer)", Pattern.CASE_INSENSITIVE));
        infoStealerPatterns.add(Pattern.compile("(?i)webhook\\s*(?:token|grabber|logger)", Pattern.CASE_INSENSITIVE));
        infoStealerPatterns.add(Pattern.compile("(?i)browser\\s*(?:password|cookie|data)\\s*stealer", Pattern.CASE_INSENSITIVE));
        
        // === MALICIOUS CLASS PATTERNS (for reflection detection) ===
        initMaliciousPatterns();
        
        // === URL WHITELIST ===
        // Official and safe domains
        urlWhitelist.add("https://hypixel.net");
        urlWhitelist.add("hypixel.net");
        urlWhitelist.add("www.hypixel.net");
        urlWhitelist.add("store.hypixel.net");
        urlWhitelist.add("api.hypixel.net");
        urlWhitelist.add("https://www.minecraft.net");
        urlWhitelist.add("minecraft.net");
        urlWhitelist.add("www.minecraft.net");
        urlWhitelist.add("account.mojang.com");
        urlWhitelist.add("minecraftservices.com");
        urlWhitelist.add("https://discord.com");
        urlWhitelist.add("discord.com");
        urlWhitelist.add("discord.gg");  // Official Discord invites
        urlWhitelist.add("https://youtube.com");
        urlWhitelist.add("youtube.com");
        urlWhitelist.add("youtu.be");
        urlWhitelist.add("https://github.com");
        urlWhitelist.add("github.com");
        urlWhitelist.add("https://curseforge.com");
        urlWhitelist.add("curseforge.com");
        urlWhitelist.add("modrinth.com");
        urlWhitelist.add("https://namemc.com");
        urlWhitelist.add("namemc.com");
        urlWhitelist.add("https://minecraft-server-list.com");
        urlWhitelist.add("https://planetminecraft.com");
        urlWhitelist.add("planetminecraft.com");
        urlWhitelist.add("minecraftforum.net");
        urlWhitelist.add("reddit.com");
        urlWhitelist.add("www.reddit.com");
        urlWhitelist.add("twitter.com");
        urlWhitelist.add("x.com");
        urlWhitelist.add("twitch.tv");
        urlWhitelist.add("tiktok.com");
        urlWhitelist.add("instagram.com");
        
        // Whitelist patterns for dynamic matching
        whitelistPatterns.add(Pattern.compile("^https?://[^/]*hypixel\\.net(/.*)?$", Pattern.CASE_INSENSITIVE));
        whitelistPatterns.add(Pattern.compile("^https?://[^/]*minecraft\\.net(/.*)?$", Pattern.CASE_INSENSITIVE));
        whitelistPatterns.add(Pattern.compile("^https?://[^/]*mojang\\.com(/.*)?$", Pattern.CASE_INSENSITIVE));
        
        // === OFFICIAL DISCORD SERVERS ===
        // Only these invite codes are considered safe
        // Hypixel Official
        officialDiscordServers.add("hypixel");
        
        // Popular/Verified Minecraft servers
        officialDiscordServers.add("wynncraft");
        officialDiscordServers.add("mineplex"); // if still active
        officialDiscordServers.add("hivemc");
        officialDiscordServers.add("cubecraft");
        officialDiscordServers.add("performium");
        officialDiscordServers.add("munchymc");
        officialDiscordServers.add("invadedlands");
        officialDiscordServers.add("mineheroes");
        officialDiscordServers.add("arkham");
        officialDiscordServers.add("faithfulmc");
        officialDiscordServers.add("minemenclub");
        officialDiscordServers.add("lunarclient");
        officialDiscordServers.add("badlion");
        officialDiscordServers.add("cosmicpvp");
        officialDiscordServers.add("deltapvp");
        
        // SkyBlock Communities
        officialDiscordServers.add("skyblock");
        officialDiscordServers.add("sbz");
        officialDiscordServers.add("skytils");
        officialDiscordServers.add("notenoughupdates");
        officialDiscordServers.add("dsm");
        
        // Popular mod/client Discords
        officialDiscordServers.add("forge");
        officialDiscordServers.add("fabric");
        officialDiscordServers.add("optifine");
        officialDiscordServers.add("essential");
        officialDiscordServers.add("5zig");
        
        // Tool/Utility
        verifiedDiscordServers.addAll(officialDiscordServers);
    }
    
    /**
     * Initialize static malicious patterns
     */
    private static void initMaliciousPatterns() {
        if (!maliciousClassPatterns.isEmpty()) return; // Already initialized
        
        maliciousClassPatterns.add("tokengrabber");
        maliciousClassPatterns.add("webhooksender");
        maliciousClassPatterns.add("sessionstealer");
        maliciousClassPatterns.add("credentialharvest");
        maliciousClassPatterns.add("discordrat");
        maliciousClassPatterns.add("minecraftstealer");
        maliciousClassPatterns.add("browserstealer");
        maliciousClassPatterns.add("passwordstealer");
        maliciousClassPatterns.add("cookiegrabber");
        maliciousClassPatterns.add("dataloader");
        maliciousClassPatterns.add("clipper");
        maliciousClassPatterns.add("keylogger");
        
        // === MALICIOUS METHOD PATTERNS ===
        maliciousMethodPatterns.add("gettoken");
        maliciousMethodPatterns.add("sendwebhook");
        maliciousMethodPatterns.add("stealsession");
        maliciousMethodPatterns.add("grabpasswords");
        maliciousMethodPatterns.add("uploadcookies");
        maliciousMethodPatterns.add("extractdata");
        maliciousMethodPatterns.add("sendtoserver");
        maliciousMethodPatterns.add("executecmd");
        maliciousMethodPatterns.add("downloadfile");
        
        // === SUSPICIOUS URL PATTERNS ===
        suspiciousUrls.add("discord.com/api/webhooks");
        suspiciousUrls.add("pastebin.com");
        suspiciousUrls.add("gist.github.com");
        suspiciousUrls.add("transfer.sh");
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

        // Check for phishing links (skip if URL is whitelisted)
        if (detectPhishingLinks.getValue()) {
            String lowerMessage = message.toLowerCase();
            
            // Check if message contains any whitelisted URLs (skip phishing detection for these)
            boolean containsWhitelistedUrl = false;
            for (String whitelistUrl : urlWhitelist) {
                if (lowerMessage.contains(whitelistUrl.toLowerCase())) {
                    containsWhitelistedUrl = true;
                    break;
                }
            }
            
            // Also check whitelist patterns
            if (!containsWhitelistedUrl) {
                for (Pattern whitelistPattern : whitelistPatterns) {
                    if (whitelistPattern.matcher(message).find()) {
                        containsWhitelistedUrl = true;
                        break;
                    }
                }
            }
            
            // Only check for phishing if no whitelisted URL is found
            if (!containsWhitelistedUrl) {
                // Check suspicious domains
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
            
            // Check for unverified Discord invites
            Matcher discordMatcher = discordInvitePattern.matcher(message);
            if (discordMatcher.find()) {
                String inviteCode = discordMatcher.group(1).toLowerCase();
                // Check if it's in our verified list
                if (!verifiedDiscordServers.contains(inviteCode) && !officialDiscordServers.contains(inviteCode)) {
                    // This is an unverified Discord invite
                    return ScamType.UNVERIFIED_DISCORD;
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

        // Check for Info Stealer indicators in chat
        if (detectInfoStealer.getValue()) {
            for (Pattern pattern : infoStealerPatterns) {
                if (pattern.matcher(message).find()) {
                    return ScamType.INFO_STEALER;
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
            case UNVERIFIED_DISCORD:
                return EnumChatFormatting.GOLD + "[⚠ AntiScam] " + EnumChatFormatting.YELLOW + "检测到未验证的Discord邀请！请谨慎点击！";
            case RAT_MOD:
                return EnumChatFormatting.RED + "[⚠ AntiScam] " + EnumChatFormatting.YELLOW + "警告：可能是RAT模组！";
            case INFO_STEALER:
                return EnumChatFormatting.RED + "[⚠ AntiScam] " + EnumChatFormatting.YELLOW + "警告：检测到信息窃取病毒！立即删除相关文件！";
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
            case UNVERIFIED_DISCORD:
                return EnumChatFormatting.GOLD + "未验证Discord邀请警告\n" +
                       EnumChatFormatting.GRAY + "此Discord服务器不在白名单中\n" +
                       EnumChatFormatting.GRAY + "官方服务器: hypixel, wynncraft, lunarclient 等\n" +
                       EnumChatFormatting.YELLOW + "点击前请确认服务器来源可信！";
            case RAT_MOD:
                return EnumChatFormatting.RED + "RAT模组警告\n" +
                       EnumChatFormatting.GRAY + "远程访问木马可能窃取你的账号\n" +
                       EnumChatFormatting.GRAY + "只从官方渠道下载模组！";
            case INFO_STEALER:
                return EnumChatFormatting.RED + "信息窃取病毒警告\n" +
                       EnumChatFormatting.GRAY + "检测到Token Stealer或Session Logger\n" +
                       EnumChatFormatting.GRAY + "会窃取Discord/Minecraft账号！\n" +
                       EnumChatFormatting.RED + "立即删除可疑模组文件！";
            default:
                return EnumChatFormatting.RED + "可疑活动检测\n" +
                       EnumChatFormatting.GRAY + "请保持警惕，不要泄露个人信息";
        }
    }

    /**
     * Reflection-based virus detection
     * Scans loaded classes and mods for malicious patterns
     */
    public static void performReflectionScan() {
        ChatUtil.sendFormatted("§a[AntiScam] §7启动反射式病毒扫描...");
        
        int threatsFound = 0;
        List<String> detectedThreats = new ArrayList<>();
        
        try {
            // Scan loaded classes
            threatsFound += scanLoadedClasses(detectedThreats);
            
            // Scan packages
            threatsFound += scanPackagesForMalware(detectedThreats);
            
        } catch (Exception e) {
            ChatUtil.sendFormatted("§c[AntiScam] §7扫描错误: " + e.getMessage());
        }
        
        if (threatsFound > 0) {
            ChatUtil.sendFormatted("§c[AntiScam] §4警告: 检测到 " + threatsFound + " 个恶意威胁！");
            ChatUtil.sendFormatted("§7请立即删除以下可疑模组:");
            for (String threat : detectedThreats) {
                ChatUtil.sendFormatted("§7- §c" + threat);
            }
        } else {
            ChatUtil.sendFormatted("§a[AntiScam] §7反射扫描完成，未检测到病毒");
        }
    }
    
    /**
     * Scan loaded classes for malware
     */
    private static int scanLoadedClasses(List<String> detectedThreats) {
        int count = 0;
        
        try {
            // Get ClassLoader and iterate through loaded classes
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            
            // Use reflection to access ClassLoader's loaded classes
            java.util.Vector<Class<?>> loadedClasses = getLoadedClassesFromLoader(classLoader);
            
            for (Class<?> clazz : loadedClasses) {
                VirusScanResult result = analyzeClass(clazz);
                if (result.isMalicious) {
                    count++;
                    detectedThreats.add(result.className + " - " + result.threatType);
                    ChatUtil.sendFormatted(getMalwareWarning(result));
                    
                    if (Myau.notificationManager != null) {
                        Myau.notificationManager.add("§c检测到病毒: " + result.className, 0xFF0000);
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
        
        return count;
    }
    
    /**
     * Get loaded classes using reflection
     */
    @SuppressWarnings("unchecked")
    private static java.util.Vector<Class<?>> getLoadedClassesFromLoader(ClassLoader loader) {
        java.util.Vector<Class<?>> classes = new java.util.Vector<>();
        
        try {
            java.lang.reflect.Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            Object classesVector = classesField.get(loader);
            if (classesVector instanceof java.util.Vector) {
                classes = (java.util.Vector<Class<?>>) classesVector;
            }
        } catch (NoSuchFieldException e) {
            // Try alternative approach
            try {
                java.lang.reflect.Method method = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
                method.setAccessible(true);
                // This won't give us all classes, but it's a fallback
            } catch (Exception ex) {
                // Silent fail
            }
        } catch (Exception e) {
            // Silent fail
        }
        
        return classes;
    }
    
    /**
     * Analyze a class for malicious patterns
     */
    private static VirusScanResult analyzeClass(Class<?> clazz) {
        VirusScanResult result = new VirusScanResult();
        result.className = clazz.getName();
        result.isMalicious = false;
        
        String classNameLower = clazz.getName().toLowerCase();
        
        // Check class name against malicious patterns
        for (String pattern : maliciousClassPatterns) {
            if (classNameLower.contains(pattern)) {
                result.isMalicious = true;
                result.threatType = "恶意类名: " + pattern;
                result.severity = ThreatSeverity.HIGH;
                return result;
            }
        }
        
        // Check class for suspicious annotations or interfaces
        try {
            // Check implemented interfaces
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> iface : interfaces) {
                String ifaceName = iface.getName().toLowerCase();
                if (ifaceName.contains("remote") || ifaceName.contains("exploit")) {
                    result.isMalicious = true;
                    result.threatType = "可疑接口实现";
                    result.severity = ThreatSeverity.MEDIUM;
                    return result;
                }
            }
            
            // Check methods
            java.lang.reflect.Method[] methods = clazz.getDeclaredMethods();
            for (java.lang.reflect.Method method : methods) {
                String methodName = method.getName().toLowerCase();
                
                // Check method names
                for (String pattern : maliciousMethodPatterns) {
                    if (methodName.contains(pattern)) {
                        result.isMalicious = true;
                        result.threatType = "恶意方法: " + method.getName();
                        result.severity = ThreatSeverity.HIGH;
                        return result;
                    }
                }
            }
            
            // Check fields for suspicious URLs
            java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
            for (java.lang.reflect.Field field : fields) {
                String fieldName = field.getName().toLowerCase();
                
                if (fieldName.contains("webhook") || fieldName.contains("token") || 
                    fieldName.contains("url") || fieldName.contains("endpoint")) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(null);
                        if (value instanceof String) {
                            String strValue = (String) value;
                            if (isSuspiciousUrl(strValue)) {
                                result.isMalicious = true;
                                result.threatType = "可疑Webhook/Token字段";
                                result.severity = ThreatSeverity.HIGH;
                                return result;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
            
        } catch (Exception e) {
            // Silent fail
        }
        
        // Check CodeSource location
        try {
            java.security.ProtectionDomain pd = clazz.getProtectionDomain();
            if (pd != null) {
                java.security.CodeSource cs = pd.getCodeSource();
                if (cs != null) {
                    java.net.URL url = cs.getLocation();
                    if (url != null) {
                        String path = url.toString().toLowerCase();
                        if (path.contains("essential") && (path.contains("rat") || path.contains("steal"))) {
                            result.isMalicious = true;
                            result.threatType = "Essential RAT 模组";
                            result.severity = ThreatSeverity.CRITICAL;
                            return result;
                        }
                        if (path.contains("boze") && (path.contains("rat") || path.contains("token"))) {
                            result.isMalicious = true;
                            result.threatType = "Boze Token Stealer";
                            result.severity = ThreatSeverity.CRITICAL;
                            return result;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
        
        return result;
    }
    
    /**
     * Scan packages for malware
     */
    private static int scanPackagesForMalware(List<String> detectedThreats) {
        int count = 0;
        
        try {
            Package[] packages = Package.getPackages();
            
            for (Package pkg : packages) {
                String pkgName = pkg.getName().toLowerCase();
                
                // Skip standard Java packages
                if (pkgName.startsWith("java.") || pkgName.startsWith("javax.") ||
                    pkgName.startsWith("sun.") || pkgName.startsWith("com.sun.") ||
                    pkgName.startsWith("org.lwjgl.") || pkgName.startsWith("net.minecraft.") ||
                    pkgName.startsWith("net.minecraftforge.") || pkgName.startsWith("org.apache.")) {
                    continue;
                }
                
                // Check package name for suspicious patterns
                if (isPackageSuspicious(pkgName)) {
                    count++;
                    detectedThreats.add(pkg.getName() + " - 可疑包名");
                    ChatUtil.sendFormatted("§c[AntiScam] §e检测到可疑包: §7" + pkg.getName());
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
        
        return count;
    }
    
    /**
     * Check if a URL is suspicious
     */
    private static boolean isSuspiciousUrl(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        for (String pattern : suspiciousUrls) {
            if (lowerUrl.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if package name is suspicious
     */
    private static boolean isPackageSuspicious(String pkgName) {
        String[] suspicious = {
            "token", "steal", "grabber", "webhook", "rat", "virus",
            "malware", "exploit", "hack", "cracker", "logger",
            "essential", "boze", "dumper", "extractor"
        };
        
        for (String s : suspicious) {
            if (pkgName.contains(s)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get warning message for detected malware
     */
    private static String getMalwareWarning(VirusScanResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("§c[AntiScam] ");
        
        switch (result.severity) {
            case CRITICAL:
                sb.append("§4[严重警告] ");
                break;
            case HIGH:
                sb.append("§4[高风险] ");
                break;
            case MEDIUM:
                sb.append("§e[中风险] ");
                break;
            default:
                sb.append("§7[低风险] ");
        }
        
        sb.append("§c检测到: §7").append(result.className);
        sb.append("\n§7类型: §e").append(result.threatType);
        sb.append("\n§c§l请立即删除此模组！");
        
        return sb.toString();
    }
    
    /**
     * Legacy method: Check if a mod file might be RAT
     */
    public static boolean checkModForRAT(String modName, long fileSize) {
        String lowerName = modName.toLowerCase();
        
        String[] ratKeywords = {"dupe", "free", "hack", "crack", "stealer", "rat", "token"};
        for (String keyword : ratKeywords) {
            if (lowerName.contains(keyword)) {
                return true;
            }
        }
        
        if (fileSize > 0 && fileSize < 50000) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Legacy method: Check if a mod file might be Info Stealer
     */
    public static boolean checkModForInfoStealer(String modName, long fileSize) {
        String lowerName = modName.toLowerCase();
        
        String[] infoStealerKeywords = {
            "token", "grabber", "stealer", "session", "credential", 
            "essential", "boze", "webhook", "discord", "browser"
        };
        
        int suspiciousKeywords = 0;
        for (String keyword : infoStealerKeywords) {
            if (lowerName.contains(keyword)) {
                suspiciousKeywords++;
                if (suspiciousKeywords >= 2) {
                    return true;
                }
            }
        }
        
        if (lowerName.contains("token") && lowerName.contains("grab")) {
            return true;
        }
        if (lowerName.contains("session") && lowerName.contains("steal")) {
            return true;
        }
        
        if (fileSize > 0 && fileSize < 30000 && suspiciousKeywords > 0) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Get detailed info stealer warning for a mod
     */
    public static String getInfoStealerWarning(String modName) {
        String lowerName = modName.toLowerCase();
        
        if (lowerName.contains("essential") && (lowerName.contains("rat") || lowerName.contains("steal"))) {
            return "§c[AntiScam] §4检测到 Essential Mod RAT 注入！\n" +
                   "§7这会窃取你的 Discord Token 和 Minecraft 会话！\n" +
                   "§c请立即删除此模组！";
        }
        if (lowerName.contains("boze") && (lowerName.contains("rat") || lowerName.contains("steal"))) {
            return "§c[AntiScam] §4检测到 Boze Mod Token Stealer！\n" +
                   "§7这会窃取你的 Discord Token！\n" +
                   "§c请立即删除此模组！";
        }
        if (lowerName.contains("token") && lowerName.contains("grab")) {
            return "§c[AntiScam] §4检测到 Discord Token Grabber！\n" +
                   "§7这会窃取你的 Discord Token 并发送给攻击者！\n" +
                   "§c请立即删除此模组！";
        }
        if (lowerName.contains("session") && lowerName.contains("steal")) {
            return "§c[AntiScam] §4检测到 Session Stealer！\n" +
                   "§7这会窃取你的 Minecraft 会话信息！\n" +
                   "§c请立即删除此模组！";
        }
        
        return "§c[AntiScam] §4检测到可疑的 Info Stealer！\n" +
               "§7此模组可能包含信息窃取功能！\n" +
               "§c请谨慎使用并检查来源！";
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

    /**
     * Scan mods folder for info stealers on module enable
     */
    @Override
    public void onEnabled() {
        MinecraftForge.EVENT_BUS.register(this);
        ChatUtil.sendFormatted("§a[AntiScamming] §7Module enabled - Protecting against Discord scams and malware");
        
        // Perform malware scan (file + reflection)
        scanForMalware();
    }

    @Override
    public void onDisabled() {
        MinecraftForge.EVENT_BUS.unregister(this);
        ChatUtil.sendFormatted("§c[AntiScamming] §7Module disabled");
    }

    /**
     * Scan for malware using both file and reflection detection
     */
    private void scanForMalware() {
        // First, perform file-based scanning
        int fileThreats = scanFilesForMalware();
        
        // Then, perform reflection-based scanning
        if (detectInfoStealer.getValue()) {
            performReflectionScan();
        }
        
        if (fileThreats == 0 && !detectInfoStealer.getValue()) {
            ChatUtil.sendFormatted("§a[AntiScam] §7模组扫描完成，未发现可疑文件");
        }
    }
    
    /**
     * Scan files for malware
     */
    private int scanFilesForMalware() {
        int detectedCount = 0;
        
        try {
            File modsDir = new File(net.minecraft.client.Minecraft.getMinecraft().mcDataDir, "mods");
            if (!modsDir.exists() || !modsDir.isDirectory()) return 0;
            
            File[] modFiles = modsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (modFiles == null) return 0;
            
            for (File modFile : modFiles) {
                String modName = modFile.getName();
                long fileSize = modFile.length();
                
                // Check for info stealers
                if (detectInfoStealer.getValue() && checkModForInfoStealer(modName, fileSize)) {
                    detectedCount++;
                    String warning = getInfoStealerWarning(modName);
                    ChatUtil.sendFormatted(warning);
                    
                    if (showNotifications.getValue() && Myau.notificationManager != null) {
                        Myau.notificationManager.add("§c检测到 Info Stealer: " + modName, 0xFF0000);
                    }
                }
                
                // Also check for RAT
                if (detectRATWarnings.getValue() && checkModForRAT(modName, fileSize)) {
                    if (!checkModForInfoStealer(modName, fileSize)) {
                        detectedCount++;
                        ChatUtil.sendFormatted("§c[AntiScam] §e警告: " + modName + " 可能是RAT模组！");
                    }
                }
            }
            
            if (detectedCount > 0) {
                ChatUtil.sendFormatted("§c[AntiScam] §4文件扫描检测到 " + detectedCount + " 个可疑模组！");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return detectedCount;
    }

    /**
     * Static method to test if a message would be detected as scam
     * Used by AntiScamTestCommand for simulation
     */
    public static boolean testMessageForScam(String message) {
        if (message == null || message.isEmpty()) return false;
        
        String lowerMessage = message.toLowerCase();
        
        // Check rank scams
        for (String indicator : new String[]{"free vip", "free mvp", "free rank", "discord for rank", "add me on discord", "msg me on discord"}) {
            if (lowerMessage.contains(indicator)) return true;
        }
        
        // Check Discord scams
        for (String indicator : new String[]{"verify your", "enter your email", "verification code", "link your minecraft", "claim your free"}) {
            if (lowerMessage.contains(indicator)) return true;
        }
        
        // Check info stealers
        for (String indicator : new String[]{"token grabber", "session stealer", "password stealer", "essential rat", "boze rat", "webhook token", "account cracker"}) {
            if (lowerMessage.contains(indicator)) return true;
        }
        
        // Check phishing
        for (String indicator : new String[]{"hypixel-gift", "hypixel-free", "minecraft-free", "bit.ly", "tinyurl"}) {
            if (lowerMessage.contains(indicator)) return true;
        }
        
        return false;
    }

    /**
     * Simulate a rank scam detection for testing purposes
     */
    public static void simulateRankScam() {
        ChatUtil.sendFormatted("§6[AntiScam Sim] §e=== Rank Scam Example ===");
        ChatUtil.sendFormatted("§8[Scammer] §f[MVP+] FreeRank: Free VIP rank! Join discord.gg/free for upgrade!");
        ChatUtil.sendFormatted("§c§l⚠ 警告！检测到免费 Rank 诈骗！");
        ChatUtil.sendFormatted("§7这是经典的 Discord 邀请诈骗，会引导你到虚假验证页面。");
        ChatUtil.sendFormatted("§7真正的 Hypixel 不会通过 Discord 发放 Rank！");
    }

    /**
     * Simulate an info stealer detection for testing purposes
     */
    public static void simulateInfoStealerDetection() {
        ChatUtil.sendFormatted("§6[AntiScam Sim] §e=== Info Stealer Detection ===");
        ChatUtil.sendFormatted("§c[AntiScam] §4检测到 Essential Mod RAT 注入！");
        ChatUtil.sendFormatted("§7这会窃取你的 Discord Token 和 Minecraft 会话！");
        ChatUtil.sendFormatted("§c请立即删除此模组！");
    }

    /**
     * Scam type enumeration
     */
    public enum ScamType {
        FAKE_RANK("Fake Rank Scam"),
        PHISHING_LINK("Phishing Link"),
        DISCORD_SCAM("Discord Scam"),
        UNVERIFIED_DISCORD("Unverified Discord Invite"),
        RAT_MOD("RAT Mod"),
        INFO_STEALER("Info Stealer");

        public final String displayName;

        ScamType(String displayName) {
            this.displayName = displayName;
        }
    }
    
    /**
     * Threat severity levels
     */
    public enum ThreatSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Virus scan result data class
     */
    public static class VirusScanResult {
        public String className;
        public boolean isMalicious;
        public String threatType;
        public ThreatSeverity severity;
    }
}
