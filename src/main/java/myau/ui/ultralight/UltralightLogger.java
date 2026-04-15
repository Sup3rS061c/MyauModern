package myau.ui.ultralight;

/**
 * Logger implementation for Ultralight.
 */
public class UltralightLogger extends com.labymedia.ultralight.databind.config.logger.Logger {

    @Override
    public void logMessage(int level, String message) {
        String prefix;
        switch (level) {
            case 0: prefix = "[Ultralight] INFO"; break;
            case 1: prefix = "[Ultralight] WARNING"; break;
            default: prefix = "[Ultralight] ERROR"; break;
        }
        System.out.println(prefix + ": " + message);
    }

    @Override
    public void developerMessage(String message) {
        System.out.println("[Ultralight] DEV: " + message);
    }
}
