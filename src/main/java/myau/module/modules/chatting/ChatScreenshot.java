package myau.module.modules.chatting;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Chat Screenshot System
 * Takes screenshots of chat
 */
public class ChatScreenshot {
    
    public enum ScreenshotMode {
        SAVE_TO_SYSTEM,
        ADD_TO_CLIPBOARD,
        BOTH
    }
    
    private static ScreenshotMode mode = ScreenshotMode.SAVE_TO_SYSTEM;
    
    /**
     * Take a screenshot of the chat area
     */
    public static void captureChat(BufferedImage chatImage) {
        if (chatImage == null) return;
        
        switch (mode) {
            case SAVE_TO_SYSTEM:
                saveToFile(chatImage);
                break;
            case ADD_TO_CLIPBOARD:
                copyToClipboard(chatImage);
                break;
            case BOTH:
                saveToFile(chatImage);
                copyToClipboard(chatImage);
                break;
        }
    }
    
    /**
     * Save screenshot to file
     */
    private static void saveToFile(BufferedImage image) {
        try {
            // Create screenshots directory
            File screenshotsDir = new File(System.getProperty("user.home"), "Pictures/MyauChat");
            if (!screenshotsDir.exists()) {
                screenshotsDir.mkdirs();
            }
            
            // Generate filename with timestamp
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
            File outputFile = new File(screenshotsDir, "chat_" + timestamp + ".png");
            
            // Save image
            ImageIO.write(image, "png", outputFile);
            
            // Notify user
            System.out.println("Chat screenshot saved to: " + outputFile.getAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("Failed to save chat screenshot: " + e.getMessage());
        }
    }
    
    /**
     * Copy screenshot to clipboard
     */
    private static void copyToClipboard(BufferedImage image) {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            ImageTransferable transferable = new ImageTransferable(image);
            clipboard.setContents(transferable, null);
            
            System.out.println("Chat screenshot copied to clipboard");
            
        } catch (Exception e) {
            System.err.println("Failed to copy chat screenshot to clipboard: " + e.getMessage());
        }
    }
    
    public static ScreenshotMode getMode() {
        return mode;
    }
    
    public static void setMode(ScreenshotMode mode) {
        ChatScreenshot.mode = mode;
    }
    
    /**
     * Transferable wrapper for clipboard image
     */
    private static class ImageTransferable implements Transferable {
        private final BufferedImage image;
        
        public ImageTransferable(BufferedImage image) {
            this.image = image;
        }
        
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }
        
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }
        
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
