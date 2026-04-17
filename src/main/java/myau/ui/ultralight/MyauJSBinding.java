package myau.ui.ultralight;

import myau.ui.impl.clickgui.normal.ClickGuiScreen;
import net.minecraft.client.Minecraft;

/**
 * JavaScript binding for Minecraft GUI actions.
 * Use view.evaluate() to call methods from JavaScript.
 *
 * Usage in HTML/JS:
 *   window.minecraft.myau.playSingleplayer();
 *   window.minecraft.myau.playMultiplayer();
 *   window.minecraft.myau.openOptions();
 *   window.minecraft.myau.quit();
 *   window.minecraft.myau.getUsername();
 */
public class MyauJSBinding {

    public void playSingleplayer() {
        Minecraft.getMinecraft().displayGuiScreen(
                new net.minecraft.client.gui.GuiSelectWorld(null));
    }

    public void playMultiplayer() {
        Minecraft.getMinecraft().displayGuiScreen(
                new net.minecraft.client.gui.GuiMultiplayer(null));
    }

    public void openOptions() {
        Minecraft.getMinecraft().displayGuiScreen(
                new net.minecraft.client.gui.GuiOptions(null,
                        Minecraft.getMinecraft().gameSettings));
    }

    public void openAltManager() {
        Minecraft.getMinecraft().displayGuiScreen(
                new myau.accountmanager.gui.GuiAccountManager(null));
    }

    public void openMods() {
        Minecraft.getMinecraft().displayGuiScreen(
                ClickGuiScreen.getInstance());
    }

    public void quit() {
        Minecraft.getMinecraft().shutdown();
    }

    public String getMinecraftVersion() {
        return "1.8.9";
    }

    public String getModVersion() {
        return "1.6+5";
    }

    public String getModName() {
        return "OpenMyau+";
    }

    public String getUsername() {
        return Minecraft.getMinecraft().getSession().getUsername();
    }

    public boolean isOnline() {
        return Minecraft.getMinecraft().getCurrentServerData() != null;
    }

    public String getCurrentServer() {
        if (Minecraft.getMinecraft().getCurrentServerData() != null) {
            return Minecraft.getMinecraft().getCurrentServerData().serverName;
        }
        return "";
    }
}
