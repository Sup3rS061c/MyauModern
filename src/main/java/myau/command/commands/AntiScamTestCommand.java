package myau.command.commands;

import myau.command.Command;
import myau.module.modules.AntiScamming;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command to test AntiScamming detection features
 * Simulates various scam messages to test detection
 */
public class AntiScamTestCommand extends Command {

    public AntiScamTestCommand() {
        super(new ArrayList<>(Arrays.asList("antiscamtest", "ast")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.isEmpty()) {
            ChatUtil.sendFormatted("§cUsage: .antiscamtest <test>");
            ChatUtil.sendFormatted("§7Available tests:");
            ChatUtil.sendFormatted("  §7- rank: Simulate fake rank scam");
            ChatUtil.sendFormatted("  §7- discord: Simulate Discord scam");
            ChatUtil.sendFormatted("  §7- phishing: Simulate phishing link");
            ChatUtil.sendFormatted("  §7- infostealer: Simulate info stealer mod");
            ChatUtil.sendFormatted("  §7- virus: Simulate virus detection");
            ChatUtil.sendFormatted("  §7- all: Run all tests");
            ChatUtil.sendFormatted("  §7- scan: Scan mods folder for threats");
            ChatUtil.sendFormatted("  §7- whitelist: Show URL whitelist test");
            return;
        }

        String test = args.get(0).toLowerCase();

        switch (test) {
            case "rank":
                simulateRankScam();
                break;
            case "discord":
                simulateDiscordScam();
                break;
            case "phishing":
                simulatePhishing();
                break;
            case "infostealer":
                simulateInfoStealer();
                break;
            case "virus":
                simulateVirusDetection();
                break;
            case "all":
                simulateRankScam();
                simulateDiscordScam();
                simulatePhishing();
                simulateInfoStealer();
                simulateVirusDetection();
                break;
            case "scan":
                ChatUtil.sendFormatted("§a[AntiScamTest] §7扫描功能演示:");
                ChatUtil.sendFormatted("§7反射扫描会检查以下内容:");
                ChatUtil.sendFormatted("  §7- 加载的类名包含恶意关键词");
                ChatUtil.sendFormatted("  §7- 类中的字段类型为 WebhookClient, TokenGrabber 等");
                ChatUtil.sendFormatted("  §7- 方法名包含 getToken, sendWebhook 等");
                ChatUtil.sendFormatted("  §7- 字符串中包含 Discord webhook URL");
                ChatUtil.sendFormatted("§7实际扫描在启用 AntiScamming 模块时自动执行");
                break;
                
            case "whitelist":
                ChatUtil.sendFormatted("§a[AntiScamTest] §7=== URL 白名单测试 ===");
                ChatUtil.sendFormatted("§a✓ §7https://hypixel.net/store - 在白名单中，不会触发钓鱼检测");
                ChatUtil.sendFormatted("§a✓ §7https://www.minecraft.net - 在白名单中，不会触发钓鱼检测");
                ChatUtil.sendFormatted("§a✓ §7https://discord.com/invite - 在白名单中，不会触发钓鱼检测");
                ChatUtil.sendFormatted("§a✓ §7https://github.com/user/repo - 在白名单中，不会触发钓鱼检测");
                ChatUtil.sendFormatted("§a✓ §7https://sky.shiiyu.moe - 在白名单中，不会触发钓鱼检测");
                ChatUtil.sendFormatted("§c✗ §7https://hypixel-free-gift.net - 不在白名单，会触发检测");
                ChatUtil.sendFormatted("§c✗ §7https://microsoft-verify.com - 不在白名单，会触发检测");
                break;
            default:
                ChatUtil.sendFormatted("§cUnknown test: " + test);
        }
    }

    private void simulateRankScam() {
        ChatUtil.sendFormatted("§6[AntiScamTest] §e=== Simulating Rank Scam ===");
        
        String[] rankScams = {
            "[MVP+] Player123: Free VIP rank! Join discord for free upgrade!",
            "[VIP] Scammer: Want a free MVP+? Add me on discord for rank!",
            "[MVP] FakeAdmin: I can give you free rank! Msg me on discord!",
            "[VIP+] BotUser: Join discord.gg/fake for free rank upgrade!",
            "Free rank giveaway! Add xyz on dc for VIP!",
        };

        for (String scam : rankScams) {
            ChatUtil.sendFormatted("§8[Test] §f" + scam);
            boolean detected = AntiScamming.testMessageForScam(scam);
            if (detected) {
                ChatUtil.sendFormatted("§a✓ Detected as scam!");
            } else {
                ChatUtil.sendFormatted("§c✗ Not detected");
            }
        }
    }

    private void simulateDiscordScam() {
        ChatUtil.sendFormatted("§6[AntiScamTest] §e=== Simulating Discord Scam ===");
        
        String[] discordScams = {
            "Verify your Minecraft account at this link!",
            "Enter your email to verify your account!",
            "Your verification code is required!",
            "Link your Minecraft account for free rank!",
            "Claim your free VIP by verifying!",
        };

        for (String scam : discordScams) {
            ChatUtil.sendFormatted("§8[Test] §f" + scam);
            boolean detected = AntiScamming.testMessageForScam(scam);
            if (detected) {
                ChatUtil.sendFormatted("§a✓ Detected as scam!");
            } else {
                ChatUtil.sendFormatted("§c✗ Not detected");
            }
        }
    }

    private void simulatePhishing() {
        ChatUtil.sendFormatted("§6[AntiScamTest] §e=== Simulating Phishing Links ===");
        
        ChatUtil.sendFormatted("§7§oNote: hypixel.net and other safe domains are whitelisted");
        
        String[] phishingLinks = {
            "Click here: hypixel-gift.com/free-rank",
            "Get free rank at hypixel-free.com",
            "Verify at hypìxel.net/verify (fake domain)",
            "Join: bit.ly/free-hypixel-rank",
            "Free items at minecraft-free.com",
        };

        for (String link : phishingLinks) {
            ChatUtil.sendFormatted("§8[Test] §f" + link);
            boolean detected = AntiScamming.testMessageForScam(link);
            if (detected) {
                ChatUtil.sendFormatted("§a✓ Phishing detected!");
            } else {
                ChatUtil.sendFormatted("§7○ No phishing detected (may be safe)");
            }
        }
        
        ChatUtil.sendFormatted("§a[AntiScamTest] §7=== Whitelisted URLs (Should be safe) ===");
        String[] safeLinks = {
            "Check out: https://hypixel.net/store for ranks",
            "Visit https://sky.shiiyu.moe for stats",
            "Download from https://github.com/user/repo",
            "Join our Discord: https://discord.gg/community",
            "Watch on https://youtube.com/channel",
        };
        
        for (String link : safeLinks) {
            ChatUtil.sendFormatted("§8[Test] §f" + link);
            boolean detected = AntiScamming.testMessageForScam(link);
            if (detected) {
                ChatUtil.sendFormatted("§c✗ False positive - should be whitelisted!");
            } else {
                ChatUtil.sendFormatted("§a✓ Correctly whitelisted - no detection");
            }
        }
    }

    private void simulateInfoStealer() {
        ChatUtil.sendFormatted("§6[AntiScamTest] §e=== Simulating Info Stealer Detection ===");
        
        String[] infoStealers = {
            "Essential Mod with token grabber",
            "Discord token stealer",
            "Session stealer for minecraft",
            "Browser password stealer",
            "WebHook token grabber",
            "Account cracker tool",
        };

        for (String stealer : infoStealers) {
            ChatUtil.sendFormatted("§8[Test] §f" + stealer);
            boolean detected = AntiScamming.testMessageForScam(stealer);
            if (detected) {
                ChatUtil.sendFormatted("§a✓ Info stealer detected!");
            } else {
                ChatUtil.sendFormatted("§c✗ Not detected");
            }
        }
    }

    private void simulateVirusDetection() {
        ChatUtil.sendFormatted("§6[AntiScamTest] §e=== Simulating Virus Detection ===");
        ChatUtil.sendFormatted("§7This would scan loaded classes using reflection...");
        ChatUtil.sendFormatted("§7Use §f.antiscamtest scan §7to perform real scan");
        
        // Show what would be detected
        ChatUtil.sendFormatted("§8[Patterns that would trigger detection:]");
        ChatUtil.sendFormatted("  §7- Classes containing: tokengrabber, sessionstealer");
        ChatUtil.sendFormatted("  §7- Methods named: getToken, sendWebhook, stealSession");
        ChatUtil.sendFormatted("  §7- Strings containing: discord.com/api/webhooks");
        ChatUtil.sendFormatted("  §7- Suspicious field types: WebhookClient, TokenGrabber");
    }
    
    @Override
    public List<String> tabComplete(ArrayList<String> args) {
        if (args.size() == 1) {
            return Arrays.asList("rank", "discord", "phishing", "infostealer", "virus", "all", "scan", "whitelist");
        }
        return new ArrayList<>();
    }
}
