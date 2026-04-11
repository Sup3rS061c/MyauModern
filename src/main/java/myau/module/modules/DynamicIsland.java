package myau.module.modules;

import java.awt.Color;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.font.CFontRenderer;
import myau.management.SpotifyIntegration;
import myau.module.Module;
import myau.module.modules.Scaffold;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.ModeProperty;
import myau.util.ChatUtil;
import myau.util.GlowUtils;
import myau.util.RenderUtil;
import myau.util.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.lwjgl.input.Mouse;

public class DynamicIsland extends Module { // nah bro i took 2 hour just to did this shit
    private boolean showScaffold = false;
    private long scaffoldTime = 0;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ColorProperty textColor = new ColorProperty("AccentColor", new Color(255, 30, 0).getRGB());
    public final BooleanProperty textShadow = new BooleanProperty("TextShadow", true);
    public final BooleanProperty enableGlow = new BooleanProperty("Glow", true);
    public final BooleanProperty showSpotify = new BooleanProperty("ShowSpotify", true);
    public final BooleanProperty spotifyCompact = new BooleanProperty("SpotifyCompact", false);

    private final int bgAlpha = 130;
    private final float radius = 8f;
    
    // Spotify display state
    private boolean showSpotifyInfo = false;
    private long spotifyLastActive = 0;
    private float spotifyExpandProgress = 0f;
    private static final long SPOTIFY_HIDE_DELAY = 5000; // Hide after 5 seconds of inactivity
    
    // Album art texture
    private int albumArtTexture = -1;
    private String lastAlbumArtUrl = "";

    public DynamicIsland() { // we always love ai right(no)? credit: ChatGPT and Horaizion
        super("DynamicIsland", true, false);
    }
    
    @Override
    public void onEnabled() {
        albumArtTexture = -1;
        lastAlbumArtUrl = "";
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        // Update Spotify state
        updateSpotifyState();

        ScaledResolution sr = new ScaledResolution(mc);

        String username = mc.thePlayer.getName();
        int ping = getPing();
        int fps = Minecraft.getDebugFPS();
        String server = getServerIP();

        String text = "Myau+  ·  " + username + "  ·  " + ping + "ms to " + server + "  ·  " + fps + "fps";

        float width = mc.fontRendererObj.getStringWidth(text) + 24f;
        float height = 26f;

        float x = sr.getScaledWidth() / 2f - width / 2f;
        float y = 8f;

        // If showing Spotify, draw expanded island
        if (showSpotify.getValue() && spotifyExpandProgress > 0.01f) {
            drawSpotifyIsland(sr, x, y, width, spotifyExpandProgress);
        } else {
            drawBackground(x, y, width, height);

            float textY = y + (height - mc.fontRendererObj.FONT_HEIGHT) / 2f;
            float startX = x + 12f;

            int accentRGB = new Color(this.textColor.getValue()).getRGB();

            mc.fontRendererObj.drawStringWithShadow(
                    "Myau+",
                    (int) startX,
                    (int) textY,
                    accentRGB);

            String part1 = "  ·  " + username + "  ·  ";
            float part1Width = mc.fontRendererObj.getStringWidth("Myau+");
            mc.fontRendererObj.drawStringWithShadow(
                    part1,
                    (int) (startX + part1Width),
                    (int) textY,
                    0xFFFFFF);

            String part2 = ping + "ms";
            float part2Width = mc.fontRendererObj.getStringWidth("Myau+" + part1);
            mc.fontRendererObj.drawStringWithShadow(
                    part2,
                    (int) (startX + part2Width),
                    (int) textY,
                    accentRGB);

            String rest = " to " + server + "  ·  " + fps + "fps";
            float restWidth = mc.fontRendererObj.getStringWidth("Myau+" + part1 + part2);
            mc.fontRendererObj.drawStringWithShadow(
                    rest,
                    (int) (startX + restWidth),
                    (int) textY,
                    0xFFFFFF);
        }
    }
    
    private void updateSpotifyState() {
        if (!showSpotify.getValue()) {
            spotifyExpandProgress = 0f;
            return;
        }

        SpotifyIntegration si = SpotifyIntegration.INSTANCE;
        
        // Update playback state
        si.updatePlaybackState();
        
        boolean hasTrack = si.isAuthenticated() && !si.getCurrentTrackName().isEmpty();
        
        if (hasTrack) {
            showSpotifyInfo = true;
            spotifyLastActive = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - spotifyLastActive > SPOTIFY_HIDE_DELAY) {
            showSpotifyInfo = false;
        }
        
        // Smooth animation
        float targetProgress = showSpotifyInfo ? 1f : 0f;
        float speed = 0.15f;
        if (spotifyExpandProgress < targetProgress) {
            spotifyExpandProgress = Math.min(spotifyExpandProgress + speed, targetProgress);
        } else if (spotifyExpandProgress > targetProgress) {
            spotifyExpandProgress = Math.max(spotifyExpandProgress - speed, targetProgress);
        }
    }
    
    private void drawSpotifyIsland(ScaledResolution sr, float baseX, float baseY, float baseWidth, float progress) {
        SpotifyIntegration si = SpotifyIntegration.INSTANCE;
        
        float expandedWidth = Math.max(baseWidth, 280f);
        float expandedHeight = spotifyCompact.getValue() ? 50f : 80f;
        
        // Interpolate dimensions
        float currentWidth = baseWidth + (expandedWidth - baseWidth) * progress;
        float currentHeight = 26f + (expandedHeight - 26f) * progress;
        
        float x = sr.getScaledWidth() / 2f - currentWidth / 2f;
        float y = baseY;
        
        drawBackground(x, y, currentWidth, currentHeight);
        
        if (progress < 0.5f) return;
        
        float alpha = (progress - 0.5f) * 2f;
        int textColor = new Color(255, 255, 255, (int)(255 * alpha)).getRGB();
        int accentRGB = new Color(this.textColor.getValue()).getRGB();
        int accentWithAlpha = new Color(
            new Color(accentRGB).getRed(),
            new Color(accentRGB).getGreen(),
            new Color(accentRGB).getBlue(),
            (int)(255 * alpha)
        ).getRGB();
        
        // Draw Spotify icon/text
        String spotifyText = "§a♪ §fSpotify";
        mc.fontRendererObj.drawStringWithShadow(spotifyText, (int)(x + 12), (int)(y + 8), textColor);
        
        // Draw track info
        String trackName = si.getCurrentTrackName();
        String artistName = si.getCurrentArtistName();
        
        if (trackName.length() > 25) trackName = trackName.substring(0, 22) + "...";
        if (artistName.length() > 30) artistName = artistName.substring(0, 27) + "...";
        
        // Track name - larger and bold
        mc.fontRendererObj.drawStringWithShadow(
            "§l" + trackName,
            (int)(x + 12),
            (int)(y + 22),
            textColor
        );
        
        if (!spotifyCompact.getValue()) {
            // Artist name
            mc.fontRendererObj.drawStringWithShadow(
                "§7" + artistName,
                (int)(x + 12),
                (int)(y + 34),
                textColor
            );
            
            // Progress bar
            float progressBarWidth = currentWidth - 24;
            float progressPercent = si.getProgressPercent();
            
            // Background bar
            RoundedUtils.drawRoundedRect(
                x + 12, y + 52,
                progressBarWidth, 4,
                2,
                new Color(100, 100, 100, (int)(150 * alpha)).getRGB()
            );
            
            // Progress fill
            if (progressPercent > 0) {
                RoundedUtils.drawRoundedRect(
                    x + 12, y + 52,
                    progressBarWidth * progressPercent, 4,
                    2,
                    accentWithAlpha
                );
            }
            
            // Time labels
            String progressTime = formatTime(si.getProgressMs());
            String durationTime = formatTime(si.getDurationMs());
            
            mc.fontRendererObj.drawStringWithShadow(
                progressTime,
                (int)(x + 12),
                (int)(y + 58),
                new Color(200, 200, 200, (int)(200 * alpha)).getRGB()
            );
            
            mc.fontRendererObj.drawStringWithShadow(
                durationTime,
                (int)(x + currentWidth - 12 - mc.fontRendererObj.getStringWidth(durationTime)),
                (int)(y + 58),
                new Color(200, 200, 200, (int)(200 * alpha)).getRGB()
            );
            
            // Play/Pause indicator
            String statusIcon = si.isPlaying() ? "§a❚❚" : "§a▶";
            mc.fontRendererObj.drawStringWithShadow(
                statusIcon,
                (int)(x + currentWidth - 30),
                (int)(y + 22),
                accentWithAlpha
            );
        }
        
        // Handle clicks for playback control
        handleSpotifyClicks(x, y, currentWidth, currentHeight);
    }
    
    private void handleSpotifyClicks(float x, float y, float width, float height) {
        if (!Mouse.isButtonDown(0)) return;
        
        int mouseX = Mouse.getEventX() * mc.displayWidth / mc.displayWidth;
        int mouseY = mc.displayHeight - Mouse.getEventY() * mc.displayHeight / mc.displayHeight - 1;
        
        // Check if click is within island bounds
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            // Right side click = play/pause
            if (mouseX > x + width - 50) {
                SpotifyIntegration.INSTANCE.playPause();
            }
            // Left side click = previous
            else if (mouseX < x + 50) {
                SpotifyIntegration.INSTANCE.previousTrack();
            }
            // Middle click = next
            else if (mouseX > x + width / 2 - 25 && mouseX < x + width / 2 + 25) {
                SpotifyIntegration.INSTANCE.nextTrack();
            }
        }
    }
    
    private String formatTime(int ms) {
        int seconds = ms / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void drawBackground(float x, float y, float w, float h) {
        RenderUtil.enableRenderState();

        Color accent = new Color(this.textColor.getValue());

        // ── Drop shadow (dark, offset downward) ──
        GlowUtils.drawGlow(
                x + 2f, y + 4f,
                w, h,
                40,
                new Color(0, 0, 0, 120));

        if (this.enableGlow.getValue()) {
            // ── Outer bloom – large, faint ──
            GlowUtils.drawGlow(
                    x, y,
                    w, h,
                    90,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));

            // ── Mid bloom – medium, moderate ──
            GlowUtils.drawGlow(
                    x, y,
                    w, h,
                    55,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));

            // ── Inner bloom – tight, vibrant ──
            GlowUtils.drawGlow(
                    x, y,
                    w, h,
                    25,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110));
        }

        RoundedUtils.drawRoundedRect(
                x, y,
                w, h,
                this.radius,
                new Color(0, 0, 0, this.bgAlpha).getRGB());

        RoundedUtils.drawRoundedRect(
                x + 0.5f, y + 0.5f,
                w - 1f, h - 1f,
                this.radius - 0.5f,
                new Color(255, 255, 255, 18).getRGB());

        RenderUtil.disableRenderState();
    }

    private int getPing() {
        try {
            if (mc.thePlayer == null || mc.getNetHandler() == null) {
                return 0;
            }
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getName());
            if (playerInfo != null) {
                return playerInfo.getResponseTime();
            }
        } catch (Exception e) {
        }
        return 0;
    }

    private String getServerIP() {
        try {
            if (mc.theWorld != null) {
                if (mc.isIntegratedServerRunning()) {
                    return "SinglePlayer";
                } else if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
                    return mc.getCurrentServerData().serverIP;
                }
            }
        } catch (Exception e) {
        }
        return "SinglePlayer";
    }
}