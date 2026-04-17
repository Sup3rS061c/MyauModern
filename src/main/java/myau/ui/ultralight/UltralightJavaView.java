package myau.ui.ultralight;

import com.labymedia.ultralight.UltralightJava;
import com.labymedia.ultralight.UltralightLoadException;
import com.labymedia.ultralight.UltralightPlatform;
import com.labymedia.ultralight.UltralightRenderer;
import com.labymedia.ultralight.UltralightView;
import com.labymedia.ultralight.config.FontHinting;
import com.labymedia.ultralight.config.UltralightConfig;
import com.labymedia.ultralight.config.UltralightViewConfig;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Ultralight Java View wrapper for Minecraft.
 * Provides HTML/CSS UI rendering using Ultralight web engine.
 * 
 * This implementation uses CPU/software rendering to avoid LWJGL2/LWJGL3 OpenGL conflicts
 * that occur when trying to use GPU-accelerated Ultralight in Minecraft 1.8.9.
 * 
 * To enable GPU acceleration in the future (when LWJGL3 isolation is solved):
 * 1. Create a separate mod jar with LWJGL3, loaded in an isolated ClassLoader
 * 2. Use UltralightOpenGLGPUDriverNative when that ClassLoader is available
 */
public class UltralightJavaView {
    private static boolean initialized = false;
    private static UltralightRenderer renderer;
    private static long windowHandle = 0;

    private UltralightView view;
    private int width;
    private int height;

    /**
     * Initialize Ultralight platform and renderer.
     * Uses CPU rendering to avoid LWJGL conflicts.
     * 
     * @param windowHandle The GLFW window handle (unused in CPU mode, kept for future GPU mode)
     */
    public static synchronized void init(long windowHandle) throws UltralightLoadException {
        if (initialized) return;
        
        UltralightJavaView.windowHandle = windowHandle;

        Path nativesDir = extractNatives();

        // Load Ultralight Java native libraries
        UltralightJava.load(nativesDir);

        // Configure platform
        UltralightPlatform platform = UltralightPlatform.instance();
        platform.setConfig(new UltralightConfig()
                .forceRepaint(false)
                .fontHinting(FontHinting.SMOOTH));

        platform.usePlatformFontLoader();
        platform.setFileSystem(new MyauFileSystem());
        platform.setLogger(new MyauLogger());
        platform.setClipboard(new MyauClipboard());

        // CPU rendering mode - no GPU driver needed
        // Ultralight will render to an internal bitmap that we can blit to screen
        System.out.println("[Ultralight] Using CPU/software rendering mode");

        renderer = UltralightRenderer.create();

        initialized = true;
        System.out.println("[Ultralight] Initialized successfully");
    }

    /**
     * Extract native libraries from runtime_natives.zip in resources.
     */
    private static Path extractNatives() {
        try {
            Path nativesDir = Paths.get("ultralight-natives");
            Files.createDirectories(nativesDir);

            String zipPath = "/assets/minecraft.myau/runtime_natives.zip";
            InputStream zipStream = UltralightJavaView.class.getResourceAsStream(zipPath);

            if (zipStream == null) {
                throw new RuntimeException("runtime_natives.zip not found in resources at " + zipPath);
            }

            try (ZipInputStream zis = new ZipInputStream(zipStream)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path outPath = nativesDir.resolve(entry.getName());
                    if (!entry.isDirectory()) {
                        Files.createDirectories(outPath.getParent());
                        
                        // Skip extraction if file already exists (e.g., locked by another process)
                        if (Files.exists(outPath)) {
                            System.out.println("[Ultralight] Skipping existing file: " + entry.getName());
                            // Still need to consume the zip entry data
                            byte[] buffer = new byte[8192];
                            while (zis.read(buffer) != -1) { /* consume */ }
                            zis.closeEntry();
                            continue;
                        }
                        
                        try (FileOutputStream fos = new FileOutputStream(outPath.toFile())) {
                            copyStream(zis, fos);
                        }
                    }
                    zis.closeEntry();
                }
            }

            System.out.println("[Ultralight] Native libraries extracted to: " + nativesDir.toAbsolutePath());
            return nativesDir;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract native libraries", e);
        }
    }

    private static void copyStream(InputStream in, FileOutputStream out) throws IOException {
        ReadableByteChannel src = Channels.newChannel(in);
        FileChannel dst = out.getChannel();
        ByteBuffer buf = ByteBuffer.allocateDirect(8192);
        while (src.read(buf) != -1) {
            buf.flip();
            dst.write(buf);
            buf.clear();
        }
    }

    /**
     * Create a new Ultralight view.
     */
    public static UltralightJavaView create(int width, int height) {
        if (!initialized) {
            throw new IllegalStateException("UltralightJavaView not initialized. Call init() first.");
        }

        // CPU rendering mode - isAccelerated = false
        UltralightView ultralightView = renderer.createView(width, height,
                new UltralightViewConfig()
                        .isAccelerated(false)  // CPU mode
                        .initialDeviceScale(1.0)
                        .isTransparent(true));

        return new UltralightJavaView(ultralightView, width, height);
    }

    private UltralightJavaView(UltralightView view, int width, int height) {
        this.view = view;
        this.width = width;
        this.height = height;
    }

    /**
     * Load a URL or HTML content.
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
     */
    public void loadFromResource(String resourcePath) {
        try {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) throw new IOException("Resource not found: " + resourcePath);
            String html = readStream(is);
            loadContent(html);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }

    private String readStream(InputStream is) throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(4096);
        StringBuilder sb = new StringBuilder();
        ReadableByteChannel ch = Channels.newChannel(is);
        while (ch.read(buf) != -1) {
            buf.flip();
            byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            sb.append(new String(bytes));
            buf.clear();
        }
        return sb.toString();
    }

    /**
     * Evaluate JavaScript in the view.
     */
    public String evaluate(String script) {
        try {
            return view.evaluateScript(script);
        } catch (Exception e) {
            System.err.println("[Ultralight] JS evaluation error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Resize the view.
     */
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        view.resize(width, height);
    }

    /**
     * Update the renderer.
     */
    public void update() {
        if (renderer != null) {
            renderer.update();
        }
    }

    /**
     * Render the view.
     * In CPU mode, Ultralight handles rendering internally.
     * This method triggers the render loop.
     */
    public void render() {
        if (renderer != null) {
            renderer.render();
        }
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
            view = null;
        }
    }

    /**
     * Shutdown Ultralight renderer.
     */
    public static synchronized void shutdown() {
        if (renderer != null) {
            renderer.purgeMemory();
            renderer = null;
        }
        initialized = false;
        System.out.println("[Ultralight] Shutdown complete");
    }
}
