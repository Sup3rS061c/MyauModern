package myau.ui.ultralight;

import com.labymedia.ultralight.UltralightClipboard;

/**
 * Ultralight clipboard implementation.
 * Uses system clipboard via Java AWT.
 */
public class UltralightClipboard extends UltralightClipboard {

    private java.awt.datatransfer.Clipboard clipboard;

    public UltralightClipboard() {
        try {
            clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (Exception e) {
            // AWT may not be available in headless mode
            clipboard = null;
        }
    }

    @Override
    public void clear() {
        if (clipboard != null) {
            try {
                clipboard.setContents(new java.awt.datatransfer.StringSelection(""), null);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public String read() {
        if (clipboard == null) return "";

        try {
            return (String) clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void write(String text) {
        if (clipboard != null && text != null) {
            try {
                clipboard.setContents(new java.awt.datatransfer.StringSelection(text), null);
            } catch (Exception ignored) {}
        }
    }
}
