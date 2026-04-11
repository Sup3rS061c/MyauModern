package myau.module.modules.chatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat Spam Blocker
 * Detects and blocks spam messages
 */
public class ChatSpamBlock {
    
    private static final List<String> recentMessages = new ArrayList<>();
    private static final Map<String, SpamInfo> playerMessages = new HashMap<>();
    private static final int MAX_RECENT_MESSAGES = 50;
    
    // Patterns for detecting spam
    private static final Pattern[] SPAM_PATTERNS = {
        Pattern.compile("(\\d{2,}\\/\\d{2,}\\/\\d{2,}|\\d{2,}-\\d{2,}-\\d{2,})"), // Dates
        Pattern.compile("\\d{1,3}[.,]\\d{3}[.,]\\d{3}"), // Prices with separators
        Pattern.compile("(?i)(visit|join|come|check out).{0,20}(my|our|the).{0,20}(island|shop|store|discord|guild)"),
        Pattern.compile("(?i)\\b(buy|sell|trade).{0,30}\\b(cheap|low|good|best|price)"),
        Pattern.compile("(?i)\\b(hack|cheat|mod|client).{0,20}(download|free|get|use)"),
        Pattern.compile("\\b[A-Z0-9]{10,}\\b"), // Random caps and numbers
        Pattern.compile("(.)\\1{4,}"), // Repeated characters
    };
    
    // Threshold for spam detection (0-100)
    private static float spamThreshold = 95.0f;
    
    /**
     * Analyze a message for spam likelihood
     * @return spam score from 0-100
     */
    public static float analyzeSpam(String message, String sender) {
        float score = 0f;
        
        // Check against spam patterns
        for (Pattern pattern : SPAM_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                score += 15f;
            }
        }
        
        // Check for repeated messages
        int repeatCount = 0;
        for (String recent : recentMessages) {
            if (similarity(message, recent) > 0.8f) {
                repeatCount++;
            }
        }
        score += repeatCount * 10f;
        
        // Check for excessive caps
        int capsCount = 0;
        int totalLetters = 0;
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                totalLetters++;
                if (Character.isUpperCase(c)) {
                    capsCount++;
                }
            }
        }
        if (totalLetters > 0 && (float) capsCount / totalLetters > 0.7f) {
            score += 10f;
        }
        
        // Check for excessive symbols
        int symbolCount = 0;
        for (char c : message.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                symbolCount++;
            }
        }
        if ((float) symbolCount / message.length() > 0.3f) {
            score += 10f;
        }
        
        // Check player message frequency
        if (sender != null) {
            SpamInfo info = playerMessages.get(sender);
            if (info != null) {
                long timeSinceLast = System.currentTimeMillis() - info.lastMessageTime;
                if (timeSinceLast < 1000) { // Less than 1 second
                    score += 20f;
                }
            }
        }
        
        // Cap at 100
        return Math.min(score, 100f);
    }
    
    /**
     * Check if a message should be blocked as spam
     */
    public static boolean shouldBlock(String message, String sender) {
        float score = analyzeSpam(message, sender);
        return score >= spamThreshold;
    }
    
    /**
     * Record a message for future spam detection
     */
    public static void recordMessage(String message, String sender) {
        // Add to recent messages
        recentMessages.add(message);
        if (recentMessages.size() > MAX_RECENT_MESSAGES) {
            recentMessages.remove(0);
        }
        
        // Update player message info
        if (sender != null) {
            playerMessages.put(sender, new SpamInfo(message, System.currentTimeMillis()));
        }
    }
    
    /**
     * Calculate similarity between two strings
     */
    private static float similarity(String s1, String s2) {
        if (s1.equalsIgnoreCase(s2)) return 1.0f;
        
        // Simple Levenshtein distance approximation
        int maxLength = Math.max(s1.length(), s2.length());
        if (maxLength == 0) return 1.0f;
        
        int distance = levenshteinDistance(s1.toLowerCase(), s2.toLowerCase());
        return 1.0f - ((float) distance / maxLength);
    }
    
    /**
     * Calculate Levenshtein distance
     */
    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1),     // insertion
                    dp[i - 1][j - 1] + cost // substitution
                );
            }
        }
        
        return dp[s1.length()][s2.length()];
    }
    
    public static float getSpamThreshold() {
        return spamThreshold;
    }
    
    public static void setSpamThreshold(float threshold) {
        spamThreshold = threshold;
    }
    
    /**
     * Clear recorded messages
     */
    public static void clear() {
        recentMessages.clear();
        playerMessages.clear();
    }
    
    private static class SpamInfo {
        String lastMessage;
        long lastMessageTime;
        
        SpamInfo(String message, long time) {
            this.lastMessage = message;
            this.lastMessageTime = time;
        }
    }
}
