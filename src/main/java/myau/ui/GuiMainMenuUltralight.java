package myau.ui;

import com.labymedia.ultralight.UltralightLoadException;
import myau.ui.ultralight.MyauJSBinding;
import myau.ui.ultralight.UltralightJavaView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * GuiMainMenu using Ultralight HTML UI.
 * Replaces the traditional Java-drawn main menu with an HTML/CSS interface.
 *
 * To enable: Set USE_ULTRALIGHT = true in GuiMainMenu
 */
public class GuiMainMenuUltralight extends GuiScreen {

    private static final String HTML_RESOURCE = "/assets/myau/html/mainmenu.html";

    private UltralightJavaView ultralightView;
    private boolean initFailed = false;
    private String initError = null;

    /**
     * Initialize Ultralight.
     */
    @Override
    public void initGui() {
        try {
            // Initialize Ultralight with GLFW window handle
            long windowHandle = GLFW.glfwGetCurrentContext();
            UltralightJavaView.init(windowHandle);

            // Create view matching screen size
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int viewWidth = sr.getScaledWidth();
            int viewHeight = sr.getScaledHeight();

            ultralightView = UltralightJavaView.create(viewWidth, viewHeight);

            // Load HTML content
            ultralightView.loadFromResource(HTML_RESOURCE);

            System.out.println("[GuiMainMenuUltralight] Loaded successfully");

        } catch (UltralightLoadException e) {
            initFailed = true;
            initError = "Failed to load Ultralight: " + e.getMessage();
            System.err.println("[GuiMainMenuUltralight] " + initError);
            e.printStackTrace();
        } catch (Exception e) {
            initFailed = true;
            initError = "Unexpected error: " + e.getMessage();
            System.err.println("[GuiMainMenuUltralight] " + initError);
            e.printStackTrace();
        }
    }

    /**
     * Render the Ultralight view.
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // If init failed, draw error message and return to normal menu
        if (initFailed) {
            drawDefaultBackground();
            drawCenteredString(fontRendererObj, "Ultralight UI failed to load:", width / 2, height / 2 - 20, 0xffffff);
            drawCenteredString(fontRendererObj, initError, width / 2, height / 2, 0xff5555);
            drawCenteredString(fontRendererObj, "Press ESC to use standard menu", width / 2, height / 2 + 40, 0xaaaaaa);
            return;
        }

        if (ultralightView == null) {
            drawDefaultBackground();
            return;
        }

        // Update Ultralight
        ultralightView.update();

        // Render Ultralight view
        GlStateManager.disableAlpha();
        ultralightView.render();
        GlStateManager.enableAlpha();
    }

    /**
     * Handle mouse clicks.
     */
    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (ultralightView != null && !initFailed) {
            // Ultralight handles its own mouse events via GPU driver
        }
    }

    /**
     * Handle mouse movement.
     */
    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();

        if (ultralightView != null && !initFailed) {
            // Ultralight handles mouse input internally
        }
    }

    /**
     * Handle keyTyped for character input.
     */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        super.keyTyped(typedChar, keyCode);

        // Handle ESC key to return to standard menu
        if (keyCode == Keyboard.KEY_ESCAPE) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
    }

    /**
     * Don't pause the game in menu.
     */
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * Cleanup on close.
     */
    @Override
    public void onGuiClosed() {
        if (ultralightView != null) {
            ultralightView.dispose();
            ultralightView = null;
        }
    }
}
