package myau.command.commands;

import myau.command.Command;
import myau.management.SpotifyIntegration;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Spotify Command - Configure and control Spotify playback
 */
public class SpotifyCommand extends Command {
    
    public SpotifyCommand() {
        super("spotify", "s");
    }
    
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        // Handle case where args[0] contains the full command with spaces (when user types without .)
        if (args.length == 1 && args[0].contains(" ")) {
            args = args[0].split(" ");
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "setid":
            case "id":
                ChatUtil.sendFormatted("§cPlease use .spotify setup <client_id> <client_secret> to set both credentials");
                break;
                
            case "setsecret":
            case "secret":
                if (args.length < 2) {
                    ChatUtil.sendFormatted("§cUsage: .spotify setsecret <client_secret>");
                    return;
                }
                StringBuilder secretBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) secretBuilder.append(" ");
                    secretBuilder.append(args[i]);
                }
                ChatUtil.sendFormatted("§c[Spotify] Cannot set secret alone. Please provide both ID and Secret in one command.");
                ChatUtil.sendFormatted("§eUse: .spotify setup <client_id> <client_secret>");
                break;
                
            case "setup":
                if (args.length < 3) {
                    ChatUtil.sendFormatted("§cUsage: .spotify setup <client_id> <client_secret>");
                    ChatUtil.sendFormatted("§7Create an app at https://developer.spotify.com/dashboard");
                    return;
                }
                SpotifyIntegration.INSTANCE.setCredentials(args[1], args[2]);
                break;
                
            case "auth":
            case "authorize":
                SpotifyIntegration.INSTANCE.authenticate();
                break;
                
            case "token":
            case "code":
                if (args.length < 2) {
                    ChatUtil.printMessage("§cUsage: .spotify token <authorization_code>");
                    return;
                }
                SpotifyIntegration.INSTANCE.exchangeCodeForToken(args[1]);
                break;
                
            case "play":
            case "pause":
                SpotifyIntegration.INSTANCE.playPause();
                break;
                
            case "next":
                SpotifyIntegration.INSTANCE.nextTrack();
                break;
                
            case "prev":
            case "previous":
                SpotifyIntegration.INSTANCE.previousTrack();
                break;
                
            case "disconnect":
            case "logout":
                SpotifyIntegration.INSTANCE.disconnect();
                break;
                
            case "status":
                printStatus();
                break;
                
            case "help":
            default:
                printUsage();
                break;
        }
    }
    
    private void printUsage() {
        ChatUtil.sendFormatted("§6[Spotify Command Usage]");
        ChatUtil.sendFormatted("§7.spotify setup <client_id> <client_secret> §f- Set credentials");
        ChatUtil.sendFormatted("§7.spotify auth §f- Authenticate with Spotify");
        ChatUtil.sendFormatted("§7.spotify token <code> §f- Complete authentication with code");
        ChatUtil.sendFormatted("§7.spotify play/pause §f- Toggle playback");
        ChatUtil.sendFormatted("§7.spotify next §f- Next track");
        ChatUtil.sendFormatted("§7.spotify prev §f- Previous track");
        ChatUtil.sendFormatted("§7.spotify disconnect §f- Log out");
        ChatUtil.sendFormatted("§7.spotify status §f- Show current track info");
        ChatUtil.sendFormatted("§e§oNote: Create an app at https://developer.spotify.com/dashboard");
        ChatUtil.sendFormatted("§e§oAdd redirect URI: http://localhost:8888/callback");
    }
    
    private void printStatus() {
        if (!SpotifyIntegration.INSTANCE.isAuthenticated()) {
            ChatUtil.sendFormatted("§c[Spotify] Not authenticated. Use .spotify auth");
            return;
        }
        
        if (SpotifyIntegration.INSTANCE.getCurrentTrackName().isEmpty()) {
            ChatUtil.sendFormatted("§e[Spotify] No track currently playing");
            return;
        }
        
        ChatUtil.sendFormatted("§6[Spotify Now Playing]");
        ChatUtil.sendFormatted("§7Track: §f" + SpotifyIntegration.INSTANCE.getCurrentTrackName());
        ChatUtil.sendFormatted("§7Artist: §f" + SpotifyIntegration.INSTANCE.getCurrentArtistName());
        ChatUtil.sendFormatted("§7Album: §f" + SpotifyIntegration.INSTANCE.getCurrentAlbumName());
        ChatUtil.sendFormatted("§7Progress: §f" + SpotifyIntegration.INSTANCE.getFormattedProgress());
        ChatUtil.sendFormatted("§7Status: §f" + (SpotifyIntegration.INSTANCE.isPlaying() ? "§aPlaying" : "§7Paused"));
    }
    
    @Override
    public List<String> tabComplete(String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            String[] subCommands = {"setup", "auth", "token", "play", "pause", "next", "prev", "disconnect", "status", "help"};
            for (String cmd : subCommands) {
                if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        }
        
        return completions;
    }
}