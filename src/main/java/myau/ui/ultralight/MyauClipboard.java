package myau.ui.ultralight;

import com.labymedia.ultralight.plugin.clipboard.UltralightClipboard;

/**
 * Simple clipboard adapter for Ultralight.
 */
public class MyauClipboard implements UltralightClipboard {
    private String clipboardText = "";

    @Override
    public void clear() {
        this.clipboardText = "";
    }

    @Override
    public String readPlainText() {
        return clipboardText;
    }

    @Override
    public void writePlainText(String text) {
        this.clipboardText = text != null ? text : "";
    }
}
