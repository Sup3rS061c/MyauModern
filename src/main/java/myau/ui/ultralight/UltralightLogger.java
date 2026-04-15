package myau.ui.ultralight;

import com.labymedia.ultralight.UltralightLogLevel;
import com.labymedia.ultralight.UltralightLogger;

/**
 * Ultralight logger implementation.
 * Logs Ultralight messages to Minecraft's logger.
 */
public class UltralightLogger extends UltralightLogger {

    @Override
    public void logMessage(UltralightLogLevel level, String message) {
        String prefix = "[Ultralight] ";
        switch (level) {
            case ERROR:
                System.err.println(prefix + "ERROR: " + message);
                break;
            case WARNING:
                System.err.println(prefix + "WARNING: " + message);
                break;
            case INFO:
                System.out.println(prefix + "INFO: " + message);
                break;
            default:
                System.out.println(prefix + message);
        }
    }
}
