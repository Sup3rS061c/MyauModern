package myau.ui.ultralight;

import com.labymedia.ultralight.databind.context.JavascriptObject;
import com.labymedia.ultralight.databind.context.JavascriptProperty;
import net.minecraft.client.Minecraft;

/**
 * JavaScript binding for Minecraft GUI actions.
 * Exposed to JavaScript as window.myau
 *
 * Usage in HTML:
 * <script>
 *   myau.playSingleplayer();      // Open singleplayer world selection
 *   myau.playMultiplayer();       // Open multiplayer server list
 *   myau.openOptions();           // Open game options
 *   myau.openAltManager();        // Open alt account manager
 *   myau.openMods();             // Open mod list
 *   myau.quit();                  // Quit game
 *   myau.getVersion();           // Get MC version string
 *   myau.getModVersion();        // Get mod version string
 * </script>
 */
@JavascriptObject
public class MyauJSBinding {

    /**
     * Open singleplayer world selection screen.
     */
    @JavascriptProperty
    public void playSingleplayer() {
        Minecraft.getMinecraft().displayGuiScreen(
                new net.minecraft.client.gui.GuiSelectWorld(null));
    }

    /**
     * Open multiplayer server list screen.
     */
    @JavascriptProperty
    public void playMultiplayer() {
        Minecraft.getMinecraft().displayGuiScreen(
                new net.minecraft.client.gui.GuiMultiplayer(null));
    }

    /**
     * Open game options screen.
     */
    @JavascriptProperty
    public void openOptions() {
        Minecraft.getMinecraft().displayGuiScreen(
                new net.minecraft.client.gui.GuiOptions(null,
                        Minecraft.getMinecraft().gameSettings));
    }

    /**
     * Open alt account manager.
     */
    @JavascriptProperty
    public void openAltManager() {
        Minecraft.getMinecraft().displayGuiScreen(
                new myau.accountmanager.gui.GuiAccountManager(null));
    }

    /**
     * Open mods list screen.
     */
    @JavascriptProperty
    public void openMods() {
        Minecraft.getMinecraft().displayGuiScreen(
                new net.minecraft.client.gui.GuiScreenMods(null,
                        Minecraft.getMinecraft().gameSettings));
    }

    /**
     * Quit the game.
     */
    @JavascriptProperty
    public void quit() {
        Minecraft.getMinecraft().shutdown();
    }

    /**
     * Get Minecraft version.
     */
    @JavascriptProperty
    public String getMinecraftVersion() {
        return "1.8.9";
    }

    /**
     * Get mod version.
     */
    @JavascriptProperty
    public String getModVersion() {
        return "1.6+5";
    }

    /**
     * Get mod name.
     */
    @JavascriptProperty
    public String getModName() {
        return "OpenMyau+";
    }

    /**
     * Get current username.
     */
    @JavascriptProperty
    public String getUsername() {
        return Minecraft.getMinecraft().getSession().getUsername();
    }

    /**
     * Check if connected to a server.
     */
    @JavascriptProperty
    public boolean isOnline() {
        return Minecraft.getMinecraft().getCurrentServerData() != null;
    }

    /**
     * Get current server name (if online).
     */
    @JavascriptProperty
    public String getCurrentServer() {
        if (Minecraft.getMinecraft().getCurrentServerData() != null) {
            return Minecraft.getMinecraft().getCurrentServerData().serverName;
        }
        return "";
    }
}
