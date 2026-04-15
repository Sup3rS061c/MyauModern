package myau.ui.ultralight;

import com.labymedia.ultralight.databind.context.JavascriptContext;
import com.labymedia.ultralight.databind.context.JavascriptObject;
import com.labymedia.ultralight.databind.context.JavascriptProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * Ultralight file system implementation.
 * Reads files from resources or local filesystem.
 */
public class UltralightFileSystem extends com.labymedia.ultralight.UltralightFileSystem {

    @Override
    public boolean fileExists(String path) {
        // Check resources first
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            try {
                stream.close();
                return true;
            } catch (IOException ignored) {}
        }

        // Check local filesystem
        return new File(path).exists();
    }

    @Override
    public ByteBuffer createByteBuffer(String path) throws IOException {
        // Try resources first
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            return readStream(stream);
        }

        // Try filesystem
        try (FileInputStream fis = new FileInputStream(path);
             FileChannel channel = fis.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocateDirect((int) channel.size());
            channel.read(buffer);
            buffer.flip();
            return buffer;
        }
    }

    private ByteBuffer readStream(InputStream stream) throws IOException {
        try (ReadableByteChannel channel = Channels.newChannel(stream)) {
            int bufferSize = 1024;
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

            while (channel.read(buffer) != -1) {
                if (!buffer.hasRemaining()) {
                    // Double the buffer size
                    ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.capacity() * 2);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }

            buffer.flip();
            return buffer;
        }
    }

    @Override
    public String getFilePath(String identifier) {
        return identifier;
    }
}
