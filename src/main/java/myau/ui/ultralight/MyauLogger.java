package myau.ui.ultralight;

import com.labymedia.ultralight.plugin.logging.UltralightLogLevel;
import com.labymedia.ultralight.plugin.logging.UltralightLogger;

/**
 * Logger implementation for Ultralight.
 */
public class MyauLogger implements UltralightLogger {

    @Override
    public void logMessage(UltralightLogLevel level, String message) {
        String prefix;
        switch (level) {
            case INFO:
                prefix = "[Ultralight] INFO";
                break;
            case WARNING:
                prefix = "[Ultralight] WARNING";
                break;
            case ERROR:
                prefix = "[Ultralight] ERROR";
                break;
            default:
                prefix = "[Ultralight] DEBUG";
                break;
        }
        System.out.println(prefix + ": " + message);
    }
}
