package myau.management;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.util.ChatUtil;
import myau.util.FileUtil;
import net.minecraft.client.Minecraft;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Spotify Integration Manager
 * Handles OAuth2 authentication and playback control
 */
public class SpotifyIntegration {
    
    public static final SpotifyIntegration INSTANCE = new SpotifyIntegration();
    
    private static final String SPOTIFY_AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SPOTIFY_API_BASE = "https://api.spotify.com/v1";
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    
    private String clientId = "";
    private String clientSecret = "";
    private String accessToken = "";
    private String refreshToken = "";
    private long tokenExpiryTime = 0;
    
    private boolean authenticated = false;
    private boolean playing = false;
    private String currentTrackName = "";
    private String currentArtistName = "";
    private String currentAlbumName = "";
    private String currentTrackId = "";
    private String albumArtUrl = "";
    private int progressMs = 0;
    private int durationMs = 0;
    private boolean isLiked = false;
    
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 1000; // Update every second
    
    private File configFile;
    
    public SpotifyIntegration() {
        configFile = new File(Minecraft.getMinecraft().mcDataDir, "myau/spotify_config.json");
        loadConfig();
    }
    
    public void setCredentials(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        saveConfig();
        ChatUtil.printMessage("§a[Spotify] Credentials saved! Use .spotify auth to authenticate.");
    }
    
    public void authenticate() {
        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            ChatUtil.printMessage("§c[Spotify] Please set Client ID and Secret first using .spotify setid <id> and .spotify setsecret <secret>");
            return;
        }
        
        try {
            String scope = "user-read-playback-state user-modify-playback-state user-read-currently-playing user-library-read user-library-modify";
            String authUrl = SPOTIFY_AUTH_URL + 
                "?client_id=" + clientId +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);
            
            Desktop.getDesktop().browse(new URI(authUrl));
            ChatUtil.printMessage("§a[Spotify] Opening browser for authentication...");
            ChatUtil.printMessage("§e[Spotify] After authenticating, use .spotify token <code> with the code from the redirect URL");
        } catch (Exception e) {
            ChatUtil.printMessage("§c[Spotify] Failed to open browser: " + e.getMessage());
        }
    }
    
    public void exchangeCodeForToken(String code) {
        try {
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(SPOTIFY_TOKEN_URL);
            
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            post.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
            
            String body = "grant_type=authorization_code" +
                "&code=" + code +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);
            post.setEntity(new StringEntity(body));
            
            HttpResponse response = client.execute(post);
            String json = EntityUtils.toString(response.getEntity());
            
            JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
            
            if (obj.has("access_token")) {
                accessToken = obj.get("access_token").getAsString();
                refreshToken = obj.get("refresh_token").getAsString();
                int expiresIn = obj.get("expires_in").getAsInt();
                tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000);
                authenticated = true;
                saveConfig();
                ChatUtil.printMessage("§a[Spotify] Successfully authenticated!");
            } else {
                ChatUtil.printMessage("§c[Spotify] Authentication failed: " + json);
            }
        } catch (Exception e) {
            ChatUtil.printMessage("§c[Spotify] Error: " + e.getMessage());
        }
    }
    
    private void refreshAccessToken() {
        if (refreshToken.isEmpty()) return;
        
        try {
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(SPOTIFY_TOKEN_URL);
            
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            post.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
            post.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");
            
            String body = "grant_type=refresh_token&refresh_token=" + refreshToken;
            post.setEntity(new StringEntity(body));
            
            HttpResponse response = client.execute(post);
            String json = EntityUtils.toString(response.getEntity());
            
            JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
            
            if (obj.has("access_token")) {
                accessToken = obj.get("access_token").getAsString();
                int expiresIn = obj.get("expires_in").getAsInt();
                tokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000);
                if (obj.has("refresh_token")) {
                    refreshToken = obj.get("refresh_token").getAsString();
                }
                saveConfig();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void updatePlaybackState() {
        if (!authenticated) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) return;
        lastUpdateTime = currentTime;
        
        if (currentTime >= tokenExpiryTime - 60000) {
            refreshAccessToken();
        }
        
        try {
            HttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(SPOTIFY_API_BASE + "/me/player/currently-playing");
            get.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            
            HttpResponse response = client.execute(get);
            
            if (response.getStatusLine().getStatusCode() == 204) {
                playing = false;
                currentTrackName = "";
                return;
            }
            
            String json = EntityUtils.toString(response.getEntity());
            JsonObject obj = new JsonParser().parse(json).getAsJsonObject();
            
            playing = obj.has("is_playing") && obj.get("is_playing").getAsBoolean();
            
            if (obj.has("item") && !obj.get("item").isJsonNull()) {
                JsonObject item = obj.get("item").getAsJsonObject();
                currentTrackId = item.get("id").getAsString();
                currentTrackName = item.get("name").getAsString();
                durationMs = item.get("duration_ms").getAsInt();
                
                if (item.has("artists") && item.get("artists").getAsJsonArray().size() > 0) {
                    currentArtistName = item.get("artists").getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString();
                }
                
                if (item.has("album")) {
                    JsonObject album = item.get("album").getAsJsonObject();
                    currentAlbumName = album.get("name").getAsString();
                    
                    if (album.has("images") && album.get("images").getAsJsonArray().size() > 0) {
                        albumArtUrl = album.get("images").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();
                    }
                }
            }
            
            if (obj.has("progress_ms")) {
                progressMs = obj.get("progress_ms").getAsInt();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void playPause() {
        if (!authenticated) return;
        
        try {
            HttpClient client = HttpClients.createDefault();
            String endpoint = playing ? "/pause" : "/play";
            HttpPost post = new HttpPost(SPOTIFY_API_BASE + "/me/player" + endpoint);
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            client.execute(post);
            playing = !playing;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void nextTrack() {
        if (!authenticated) return;
        
        try {
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(SPOTIFY_API_BASE + "/me/player/next");
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            client.execute(post);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void previousTrack() {
        if (!authenticated) return;
        
        try {
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(SPOTIFY_API_BASE + "/me/player/previous");
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            client.execute(post);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void seek(int positionMs) {
        if (!authenticated) return;
        
        try {
            HttpClient client = HttpClients.createDefault();
            HttpPost post = new HttpPost(SPOTIFY_API_BASE + "/me/player/seek?position_ms=" + positionMs);
            post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            client.execute(post);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveConfig() {
        try {
            configFile.getParentFile().mkdirs();
            JsonObject obj = new JsonObject();
            obj.addProperty("clientId", clientId);
            obj.addProperty("clientSecret", clientSecret);
            obj.addProperty("accessToken", accessToken);
            obj.addProperty("refreshToken", refreshToken);
            obj.addProperty("tokenExpiryTime", tokenExpiryTime);
            
            java.io.FileWriter writer = new java.io.FileWriter(configFile);
            writer.write(obj.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadConfig() {
        if (!configFile.exists()) return;
        
        try {
            StringBuilder sb = new StringBuilder();
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(configFile));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            JsonObject obj = new JsonParser().parse(sb.toString()).getAsJsonObject();
            
            clientId = obj.has("clientId") ? obj.get("clientId").getAsString() : "";
            clientSecret = obj.has("clientSecret") ? obj.get("clientSecret").getAsString() : "";
            accessToken = obj.has("accessToken") ? obj.get("accessToken").getAsString() : "";
            refreshToken = obj.has("refreshToken") ? obj.get("refreshToken").getAsString() : "";
            tokenExpiryTime = obj.has("tokenExpiryTime") ? obj.get("tokenExpiryTime").getAsLong() : 0;
            
            authenticated = !accessToken.isEmpty() && System.currentTimeMillis() < tokenExpiryTime - 60000;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void disconnect() {
        authenticated = false;
        accessToken = "";
        refreshToken = "";
        saveConfig();
        ChatUtil.printMessage("§a[Spotify] Disconnected!");
    }
    
    // Getters
    public boolean isAuthenticated() { return authenticated; }
    public boolean isPlaying() { return playing; }
    public String getCurrentTrackName() { return currentTrackName; }
    public String getCurrentArtistName() { return currentArtistName; }
    public String getCurrentAlbumName() { return currentAlbumName; }
    public String getCurrentTrackId() { return currentTrackId; }
    public String getAlbumArtUrl() { return albumArtUrl; }
    public int getProgressMs() { return progressMs; }
    public int getDurationMs() { return durationMs; }
    public boolean hasCredentials() { return !clientId.isEmpty() && !clientSecret.isEmpty(); }
    
    public float getProgressPercent() {
        if (durationMs == 0) return 0;
        return (float) progressMs / durationMs;
    }
    
    public String getFormattedProgress() {
        return formatTime(progressMs) + " / " + formatTime(durationMs);
    }
    
    private String formatTime(int ms) {
        int seconds = ms / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}