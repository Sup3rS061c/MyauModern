package myau.ui.ultralight;

/**
 * Simple clipboard adapter for Ultralight.
 */
public abstract class UltralightClipboard implements com.labymedia.ultralight.plugin.clipboard.UltralightClipboard {
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
