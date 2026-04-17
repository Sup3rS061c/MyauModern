package myau.ui.ultralight;

import com.labymedia.ultralight.plugin.filesystem.UltralightFileSystem;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * File system implementation for Ultralight.
 * Uses classpath resources for file access.
 */
public class MyauFileSystem implements UltralightFileSystem {

    @Override
    public boolean fileExists(String path) {
        return getClass().getResource(path) != null;
    }

    @Override
    public long getFileSize(long fileHandle) {
        return -1;
    }

    @Override
    public String getFileMimeType(String path) {
        if (path.endsWith(".html") || path.endsWith(".htm")) {
            return "text/html";
        } else if (path.endsWith(".css")) {
            return "text/css";
        } else if (path.endsWith(".js")) {
            return "application/javascript";
        } else if (path.endsWith(".png")) {
            return "image/png";
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (path.endsWith(".gif")) {
            return "image/gif";
        } else if (path.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (path.endsWith(".json")) {
            return "application/json";
        } else if (path.endsWith(".xml")) {
            return "application/xml";
        } else if (path.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }

    @Override
    public long openFile(String path, boolean forWriting) {
        URL resource = getClass().getResource(path);
        if (resource != null && !forWriting) {
            return System.identityHashCode(resource);
        }
        return -1;
    }

    @Override
    public void closeFile(long fileHandle) {
        // Nothing to close
    }

    @Override
    public long readFromFile(long fileHandle, ByteBuffer data, long length) {
        URL resource = getClass().getResource("/assets/minecraft.myau/html/mainmenu.html");
        if (resource == null) {
            return -1;
        }
        
        try (InputStream is = resource.openStream()) {
            byte[] bytes = new byte[(int) Math.min(length, 65536)];
            int read = is.read(bytes);
            if (read > 0) {
                data.put(bytes, 0, read);
                return read;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
