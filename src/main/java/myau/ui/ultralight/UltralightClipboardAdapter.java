package myau.ui.ultralight;

import java.nio.ByteBuffer;

/**
 * Simple clipboard adapter for Ultralight.
 */
public class UltralightClipboardAdapter extends com.labymedia.ultralight.databind.config.clipboard.Clipboard {
    private String clipboardText = "";

    @Override
    public String read() {
        return clipboardText;
    }

    @Override
    public void write(String text) {
        this.clipboardText = text != null ? text : "";
    }
}
