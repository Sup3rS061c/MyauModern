package myau.ui.ultralight;

import com.labymedia.ultralight.UltralightLoadException;

/**
 * File system implementation for Ultralight.
 * Uses classpath resources for file access.
 */
public class UltralightFileSystem extends com.labymedia.ultralight.plugin.FileSystem {

    @Override
    public boolean fileExists(String path) {
        return getClass().getResource(path) != null;
    }

    @Override
    public com.labymedia.ultralight.plugin.FileSystem.File openFile(String path, int flags) throws UltralightLoadException {
        return null;
    }

    @Override
    public void closeFile(com.labymedia.ultralight.plugin.FileSystem.File file) {
    }

    @Override
    public long readFromFile(com.labymedia.ultralight.plugin.FileSystem.File file, long data, long length) {
        return 0;
    }

    @Override
    public long getFileSize(com.labymedia.ultralight.plugin.FileSystem.File file) {
        return 0;
    }

    @Override
    public boolean seekFile(com.labymedia.ultralight.plugin.FileSystem.File file, long position) {
        return false;
    }

    @Override
    public long tellFile(com.labymedia.ultralight.plugin.FileSystem.File file) {
        return 0;
    }
}
