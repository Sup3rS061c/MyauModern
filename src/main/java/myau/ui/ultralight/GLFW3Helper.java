package myau.ui.ultralight;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Helper to access LWJGL3 GLFW functions.
 * 
 * In production (shadowJar), LWJGL3 is relocated to minecraft.myau.deps.org.lwjgl
 * In development (IDE), LWJGL3 is in the regular package.
 * 
 * We detect which is available and use that.
 */
public final class GLFW3Helper {

    private static final String RELOCATED_PREFIX = "minecraft.myau.deps.org.lwjgl.";
    private static final String NATIVE_PREFIX = "org.lwjgl.";
    
    private static volatile Boolean available = null;
    private static String glfwClassName = null;
    private static String functionsClassName = null;

    private GLFW3Helper() {}

    /**
     * Initialize class names - try relocated first (production), then native (dev)
     */
    private static synchronized void initClassNames() {
        if (glfwClassName != null) {
            return;
        }

        // Try relocated names first (shadowJar relocates to minecraft.myau.deps.org.lwjgl)
        if (classExists(RELOCATED_PREFIX + "glfw.GLFW")) {
            glfwClassName = RELOCATED_PREFIX + "glfw.GLFW";
            functionsClassName = RELOCATED_PREFIX + "glfw.GLFW$Functions";
            System.out.println("[GLFW3Helper] Using relocated LWJGL3: " + glfwClassName);
        } else if (classExists(NATIVE_PREFIX + "glfw.GLFW")) {
            // Fallback to native names (development mode)
            glfwClassName = NATIVE_PREFIX + "glfw.GLFW";
            functionsClassName = NATIVE_PREFIX + "glfw.GLFW$Functions";
            System.out.println("[GLFW3Helper] Using native LWJGL3: " + glfwClassName);
        } else {
            System.err.println("[GLFW3Helper] LWJGL3 GLFW not found!");
        }
    }

    /**
     * Check if a class exists in the current classpath.
     */
    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if LWJGL3 is available (call this before using other methods).
     */
    public static boolean isAvailable() {
        if (available != null) {
            return available;
        }
        
        try {
            initClassNames();
            if (glfwClassName == null) {
                available = false;
                return false;
            }
            
            // Try to get the function pointer as a final check
            getProcAddressFunctionPointer();
            available = true;
            return true;
        } catch (Throwable t) {
            System.err.println("[GLFW3Helper] LWJGL3 not available: " + t.getMessage());
            available = false;
            return false;
        }
    }

    /**
     * Returns the GLFW3 glfwGetCurrentContext() result (the GLFW window handle).
     */
    public static long getCurrentContext() {
        if (!isAvailable()) {
            return 0L;
        }
        
        try {
            initClassNames();
            Class<?> glfwClass = Class.forName(glfwClassName);
            Method m = glfwClass.getMethod("glfwGetCurrentContext");
            Object result = m.invoke(null);
            return result == null ? 0L : (Long) result;
        } catch (Exception e) {
            throw new RuntimeException("[GLFW3Helper] Failed to get current GLFW context", e);
        }
    }

    /**
     * Returns the native function pointer for glfwGetProcAddress.
     */
    public static long getProcAddressFunctionPointer() {
        if (!isAvailable()) {
            throw new RuntimeException("[GLFW3Helper] LWJGL3 not available");
        }
        
        try {
            initClassNames();
            Class<?> functionsClass = Class.forName(functionsClassName);
            Field f = functionsClass.getField("GetProcAddress");
            return f.getLong(null);
        } catch (Exception e) {
            throw new RuntimeException("[GLFW3Helper] Failed to get GLFW GetProcAddress function pointer", e);
        }
    }
}
