package myau.ui;

import com.labymedia.ultralight.UltralightLoadException;
import com.labymedia.ultralight.databind.context.JavascriptContext;
import myau.ui.ultralight.MyauJSBinding;
import myau.ui.ultralight.UltralightJavaView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

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

    // Mouse tracking for Ultralight input
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    private boolean mouseDown = false;
    private int currentMouseButton = -1;

    // Keyboard state
    private boolean[] keyStates = new boolean[256];

    /**
     * Initialize Ultralight.
     */
    @Override
    public void initGui() {
        try {
            // Initialize Ultralight if not already done
            if (!isUltralightInitialized()) {
                UltralightJavaView.init();
            }

            // Create view matching screen size
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int width = sr.getScaledWidth();
            int height = sr.getScaledHeight();

            ultralightView = UltralightJavaView.create(width, height);

            // Load HTML content
            ultralightView.loadFromResource(HTML_RESOURCE);

            // Bind JavaScript interface
            ultralightView.bind("myau", new MyauJSBinding());

            // Bind databind context if available
            try {
                bindJavascriptContext(ultralightView);
            } catch (Exception e) {
                // Databind may not be available, continue without it
                System.out.println("[GuiMainMenuUltralight] Databind not available: " + e.getMessage());
            }

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

    private void bindJavascriptContext(UltralightJavaView view) {
        // This requires ultralight-java-databind
        // The binding is done automatically when databind is on classpath
    }

    private boolean isUltralightInitialized() {
        // Check if we can create a view (indicates renderer exists)
        return false; // Always return false to ensure proper initialization
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
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (ultralightView != null && !initFailed) {
            // Convert screen coordinates to view coordinates
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int viewX = (int) (mouseX * sr.getScaledWidth() / (double) width);
            int viewY = (int) (mouseY * sr.getScaledHeight() / (double) height);

            ultralightView.fireMouseEvent(viewX, viewY, mouseButton, true);
        }
    }

    /**
     * Handle mouse release.
     */
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);

        if (ultralightView != null && !initFailed) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int viewX = (int) (mouseX * sr.getScaledWidth() / (double) width);
            int viewY = (int) (mouseY * sr.getScaledHeight() / (double) height);

            ultralightView.fireMouseEvent(viewX, viewY, state, false);
        }
    }

    /**
     * Handle mouse movement.
     */
    @Override
    public void handleMouseInput() throws java.io.IOException {
        super.handleMouseInput();

        if (ultralightView != null && !initFailed) {
            int mouseX = Mouse.getEventX() * width / Minecraft.getMinecraft().displayWidth;
            int mouseY = height - Mouse.getEventY() * height / Minecraft.getMinecraft().displayHeight - 1;

            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int viewX = (int) (mouseX * sr.getScaledWidth() / (double) width);
            int viewY = (int) (mouseY * sr.getScaledHeight() / (double) height);

            ultralightView.fireMouseMoveEvent(viewX, viewY);
        }
    }

    /**
     * Handle keyTyped for character input.
     */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
        super.keyTyped(typedChar, keyCode);

        if (ultralightView != null && !initFailed) {
            ultralightView.fireKeyEvent(keyCode, true, typedChar);

            // Handle ESC key to return to standard menu
            if (keyCode == Keyboard.KEY_ESCAPE) {
                Minecraft.getMinecraft().displayGuiScreen(null);
            }
        }
    }

    /**
     * Handle keyboard events.
     */
    @Override
    public void handleInput() throws java.io.IOException {
        super.handleInput();

        if (ultralightView != null && !initFailed) {
            // Process keyboard events
            while (Keyboard.next()) {
                int keyCode = Keyboard.getEventKey();
                char keyChar = Keyboard.getEventCharacter();
                boolean pressed = Keyboard.getEventKeyState();

                ultralightView.fireKeyEvent(keyCode, pressed, keyChar);

                // Handle ESC
                if (keyCode == Keyboard.KEY_ESCAPE && pressed) {
                    Minecraft.getMinecraft().displayGuiScreen(null);
                }
            }
        }
    }

    /**
     * Handle scroll wheel.
     */
    @Override
    public void onMouseWheel() {
        super.onMouseWheel();

        if (ultralightView != null && !initFailed) {
            int scrollDelta = Mouse.getEventDWheel();
            if (scrollDelta != 0) {
                ultralightView.fireScrollEvent(0, scrollDelta > 0 ? 1 : -1);
            }
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
