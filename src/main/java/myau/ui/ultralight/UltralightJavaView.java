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
import net.minecraft.client.shader.Framebuffer;

import java.io.File;
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

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Ultralight Java View wrapper for Minecraft.
 * Provides HTML/CSS UI rendering using Ultralight web engine.
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
     */
    public static synchronized void init() throws UltralightLoadException {
        if (initialized) return;

        Path nativesDir = extractNatives();

        // Add natives to java.library.path
        String libraryPath = System.getProperty("java.library.path");
        String newPath = nativesDir.toAbsolutePath().toString();
        if (libraryPath != null && !libraryPath.isEmpty()) {
            newPath = libraryPath + File.pathSeparator + newPath;
        }
        System.setProperty("java.library.path", newPath);

        // Reset library path for Java 8
        try {
            java.lang.reflect.Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);
        } catch (Exception e) {
            System.out.println("[Ultralight] Warning: Could not reset library path: " + e);
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
        Framebuffer framebuffer = Minecraft.getMinecraft().getFramebuffer();
        gpuDriver = new UltralightOpenGLGPUDriverNative(
                framebuffer.framebufferTexture,
                true,
                addr -> org.lwjgl.glfw.GLFW.glfwGetProcAddress(addr)
        );

        platform.setGPUDriver(gpuDriver);
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

            String zipPath = "/assets/myau/runtime_natives.zip";
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
        try (JavascriptContextLock lock = view.lockJavascriptContext()) {
            return lock.getContext().evaluateString(lock.getContext().getGlobalObject(), script, "eval", 0);
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
        renderer.update();

        if (System.currentTimeMillis() - lastGarbageCollection > 1000) {
            try (JavascriptContextLock lock = view.lockJavascriptContext()) {
                lock.getContext().garbageCollect();
            }
            lastGarbageCollection = System.currentTimeMillis();
        }
    }

    /**
     * Render the view to the screen.
     */
    public void render() {
        renderer.render();

        glPushAttrib(GL_ENABLE_BIT | GL_COLOR_BUFFER_BIT | GL_TRANSFORM_BIT);

        if (gpuDriver.hasCommandsPending()) {
            gpuDriver.drawCommandList();
        }

        glPopAttrib();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
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
            view.delete();
            view = null;
        }
    }

    /**
     * Shutdown Ultralight renderer.
     */
    public static synchronized void shutdown() {
        if (renderer != null) {
            renderer.delete();
            renderer = null;
        }
        if (gpuDriver != null) {
            gpuDriver.delete();
            gpuDriver = null;
        }
        initialized = false;
    }
}
