package myau.ui.ultralight;

import com.labymedia.ultralight.plugin.clipboard.UltralightClipboard;

import java.nio.ByteBuffer;

/**
 * Simple clipboard adapter for Ultralight.
 */
public abstract class UltralightClipboardAdapter implements UltralightClipboard {
    private String clipboardText = "";

    @Override
    public String readPlainText() {
        return clipboardText;
    }

    @Override
    public void writePlainText(String text) {
        this.clipboardText = text != null ? text : "";
    }
}
