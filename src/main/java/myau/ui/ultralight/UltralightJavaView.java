package myau.ui.ultralight;

import com.labymedia.ultralight.UltralightJava;
import com.labymedia.ultralight.UltralightLoadException;
import com.labymedia.ultralight.config.FontHinting;
import com.labymedia.ultralight.config.UltralightConfig;
import com.labymedia.ultralight.config.UltralightViewConfig;
import com.labymedia.ultralight.gpu.UltralightGPUDriverNativeUtil;
import com.labymedia.ultralight.gpu.UltralightOpenGLGPUDriverNative;
import com.labymedia.ultralight.javascript.JavascriptContextLock;
import com.labymedia.ultralight.UltralightPlatform;
import com.labymedia.ultralight.UltralightRenderer;
import com.labymedia.ultralight.UltralightView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Ultralight Java View wrapper for Minecraft.
 * Provides HTML/CSS UI rendering using Ultralight web engine.
 *
 * Note: Ultralight Java requires LWJGL 3, which is separate from Minecraft's LWJGL 2.
 * The native libraries are loaded and managed independently.
 */
public class UltralightJavaView {
    private static boolean initialized = false;
    private static UltralightRenderer renderer;
    private static UltralightPlatform platform;
    private static UltralightOpenGLGPUDriverNative gpuDriver;

    private UltralightView view;
    private int width;
    private int height;
    private long lastGarbageCollection;

    /**
     * Initialize Ultralight platform and renderer.
     * Must be called once before creating any views.
     */
    public static synchronized void init() throws UltralightLoadException {
        if (initialized) return;

        // Extract native libraries from runtime_natives.zip
        Path nativesDir = extractNatives();

        // Add natives to java.library.path
        String libraryPath = System.getProperty("java.library.path");
        String newPath = nativesDir.toAbsolutePath().toString();
        if (libraryPath != null && !libraryPath.isEmpty()) {
            newPath = libraryPath + File.pathSeparator + newPath;
        }
        System.setProperty("java.library.path", newPath);

        // Use Field to update the internal static library path (Java 8 workaround)
        try {
            java.lang.reflect.Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null); // Force reload on next System.loadLibrary call
        } catch (Exception e) {
            System.out.println("[UltralightJavaView] Warning: Could not reset library path: " + e);
        }

        // Load Ultralight Java native libraries
        UltralightGPUDriverNativeUtil.extractNativeLibrary(nativesDir);
        UltralightJava.extractNativeLibrary(nativesDir);

        // Load in correct order
        UltralightGPUDriverNativeUtil.load(nativesDir);
        UltralightJava.load(nativesDir);

        // Configure platform
        platform = UltralightPlatform.instance();
        platform.setConfig(new UltralightConfig()
                .forceRepaint(false)
                .fontHinting(FontHinting.SMOOTH));

        platform.usePlatformFontLoader();
        platform.setFileSystem(new UltralightFileSystem());
        platform.setLogger(new UltralightLogger());
        platform.setClipboard(new UltralightClipboard());

        // Create renderer with OpenGL GPU driver
        gpuDriver = new UltralightOpenGLGPUDriverNative(
                Minecraft.getMinecraft().getFramebuffer().framebufferTexture,
                addr -> {
                    // GetProcAddress implementation for LWJGL 3
                    return org.lwjgl.glfw.GLFW.glfwGetProcAddress(addr);
                }
        );

        platform.setGPUDriver(gpuDriver);
        renderer = UltralightRenderer.create();

        initialized = true;
    }

    /**
     * Extract native libraries from runtime_natives.zip in resources.
     */
    private static Path extractNatives() {
        try {
            // Create a permanent directory for natives (not temp, so it persists)
            Path nativesDir = Paths.get("ultralight-natives");
            Files.createDirectories(nativesDir);

            // Extract from runtime_natives.zip in resources
            String zipPath = "/assets/myau/runtime_natives.zip";
            InputStream zipStream = UltralightJavaView.class.getResourceAsStream(zipPath);

            if (zipStream == null) {
                throw new RuntimeException("runtime_natives.zip not found in resources at " + zipPath);
            }

            try (ZipInputStream zis = new ZipInputStream(zipStream)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path outPath = nativesDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(outPath);
                    } else {
                        Files.createDirectories(outPath.getParent());
                        try (FileOutputStream fos = new FileOutputStream(outPath.toFile())) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }

            System.out.println("[UltralightJavaView] Native libraries extracted to: " + nativesDir.toAbsolutePath());
            return nativesDir;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract native libraries", e);
        }
    }

    /**
     * Create a new Ultralight view.
     *
     * @param width  View width in pixels
     * @param height View height in pixels
     * @return The created view
     */
    public static UltralightJavaView create(int width, int height) {
        if (!initialized) {
            throw new IllegalStateException("UltralightJavaView not initialized. Call init() first.");
        }

        UltralightView ultralightView = renderer.createView(width, height,
                new UltralightViewConfig()
                        .isAccelerated(true)
                        .initialDeviceScale(1.0)
                        .isTransparent(true));

        return new UltralightJavaView(ultralightView, width, height);
    }

    private UltralightJavaView(UltralightView view, int width, int height) {
        this.view = view;
        this.width = width;
        this.height = height;
        this.lastGarbageCollection = System.currentTimeMillis();
    }

    /**
     * Load a URL or HTML content.
     *
     * @param content HTML content or URL
     */
    public void loadContent(String content) {
        if (content.startsWith("http://") || content.startsWith("https://") ||
                content.startsWith("file://")) {
            view.loadURL(content);
        } else {
            view.loadHTML(content);
        }
    }

    /**
     * Load HTML from resources.
     *
     * @param resourcePath Path to HTML file in resources
     */
    public void loadFromResource(String resourcePath) {
        try {
            String html = new String(getClass().getResourceAsStream(resourcePath)
                    .readAllBytes());
            loadContent(html);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }

    /**
     * Evaluate JavaScript in the view.
     *
     * @param script JavaScript code to execute
     * @return Result as string (if any)
     */
    public String evaluate(String script) {
        try (JavascriptContextLock lock = view.lockJavascriptContext()) {
            return lock.getContext().evaluate(script);
        }
    }

    /**
     * Bind a Java object to JavaScript window object.
     *
     * @param name  Name in JavaScript (e.g., "myau")
     * @param object Java object to bind
     */
    public void bind(String name, Object object) {
        try (JavascriptContextLock lock = view.lockJavascriptContext()) {
            lock.getContext().makeObjectGlobal(object);
            lock.getContext().setProperty(lock.getContext().getGlobalObject(), name, object);
        }
    }

    /**
     * Resize the view.
     *
     * @param width  New width
     * @param height New height
     */
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        view.resize(width, height);
    }

    /**
     * Fire a mouse event.
     */
    public void fireMouseEvent(int x, int y, int button, boolean pressed) {
        com.labymedia.ultralight.UltralightMouseEvent event =
                new com.labymedia.ultralight.UltralightMouseEvent()
                        .x(x)
                        .y(y)
                        .button(com.labymedia.ultralight.UltralightMouseEventButton.forCode(button))
                        .type(pressed ?
                                com.labymedia.ultralight.UltralightMouseEventType.PRESSED :
                                com.labymedia.ultralight.UltralightMouseEventType.RELEASED);
        view.fireMouseEvent(event);
    }

    /**
     * Fire a mouse move event.
     */
    public void fireMouseMoveEvent(int x, int y) {
        com.labymedia.ultralight.UltralightMouseEvent event =
                new com.labymedia.ultralight.UltralightMouseEvent()
                        .x(x)
                        .y(y)
                        .type(com.labymedia.ultralight.UltralightMouseEventType.MOVED);
        view.fireMouseEvent(event);
    }

    /**
     * Fire a scroll event.
     *
     * @param deltaX Horizontal scroll delta
     * @param deltaY Vertical scroll delta
     */
    public void fireScrollEvent(int deltaX, int deltaY) {
        com.labymedia.ultralight.UltralightScrollEvent event =
                new com.labymedia.ultralight.UltralightScrollEvent()
                        .deltaX(deltaX * 32)
                        .deltaY(deltaY * 32)
                        .type(com.labymedia.ultralight.UltralightScrollEventType.BY_PIXEL);
        view.fireScrollEvent(event);
    }

    /**
     * Fire a key event.
     */
    public void fireKeyEvent(int key, boolean pressed, char character) {
        // Map LWJGL 2 key to Ultralight key
        com.labymedia.ultralight.UltralightKeyEvent event =
                new com.labymedia.ultralight.UltralightKeyEvent()
                        .type(pressed ?
                                com.labymedia.ultralight.UltralightKeyEventType.KEY_DOWN :
                                com.labymedia.ultralight.UltralightKeyEventType.KEY_UP)
                        .key(com.labymedia.ultralight.UltralightKey.forCode(mapKeyCode(key)))
                        .text(String.valueOf(character));
        view.fireKeyEvent(event);
    }

    private int mapKeyCode(int lwjglKey) {
        // Map Minecraft LWJGL 2 key codes to Ultralight key codes
        // This is a simplified mapping
        return lwjglKey;
    }

    /**
     * Update the renderer and perform garbage collection.
     */
    public void update() {
        renderer.update();

        // JavaScript garbage collection every second
        if (System.currentTimeMillis() - lastGarbageCollection > 1000) {
            try (JavascriptContextLock lock = view.lockJavascriptContext()) {
                lock.getContext().garbageCollect();
            }
            lastGarbageCollection = System.currentTimeMillis();
        }
    }

    /**
     * Render the view to the screen using OpenGL.
     * Should be called from an active OpenGL context.
     */
    public void render() {
        renderer.render();

        glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT | GL_TRANSFORM_BIT);

        if (gpuDriver.hasCommandsPending()) {
            gpuDriver.drawCommandList();
        }

        // Render the HTML texture as a fullscreen quad
        renderHtmlTexture();

        glPopAttrib();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
    }

    private void renderHtmlTexture() {
        long textureId = view.renderTarget().getTextureId();
        if (textureId == 0) return;

        glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT | GL_TRANSFORM_BIT);

        glBindTexture(GL_TEXTURE_2D, textureId);
        glEnable(GL_TEXTURE_2D);

        glUseProgram(0);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_LIGHTING);
        glDisable(GL_SCISSOR_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glColor4f(1, 1, 1, 1);

        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2i(0, 0);
        glTexCoord2f(0, 1);
        glVertex2i(0, height);
        glTexCoord2f(1, 1);
        glVertex2i(width, height);
        glTexCoord2f(1, 0);
        glVertex2i(width, 0);
        glEnd();

        glBindTexture(GL_TEXTURE_2D, 0);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);

        glDisable(GL_TEXTURE_2D);
        glPopAttrib();
    }

    /**
     * Get the underlying UltralightView.
     */
    public UltralightView getView() {
        return view;
    }

    /**
     * Get view width.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get view height.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Cleanup and dispose the view.
     */
    public void dispose() {
        if (view != null) {
            view.destroy();
            view = null;
        }
    }

    /**
     * Shutdown Ultralight renderer.
     * Call when closing the game.
     */
    public static synchronized void shutdown() {
        if (renderer != null) {
            renderer.destroy();
            renderer = null;
        }
        if (gpuDriver != null) {
            gpuDriver.dispose();
            gpuDriver = null;
        }
        initialized = false;
    }
}
