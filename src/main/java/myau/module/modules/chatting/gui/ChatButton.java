package myau.module.modules.chatting.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * Generic Chat Button
 * Used for Copy, Delete, Screenshot, Search buttons
 */
public class ChatButton {
    
    public enum ButtonType {
        COPY,
        DELETE,
        DELETE_HISTORY,
        SCREENSHOT,
        SEARCH,
        CLEAR
    }
    
    private ButtonType type;
    private int x, y, width, height;
    private boolean hovered = false;
    private boolean enabled = true;
    private int color = 0xFFFFFFFF;
    private int hoveredColor = 0xFFFFFFA0;
    private int backgroundColor = 0x80000000;
    private int hoveredBackgroundColor = 0xA0000000;
    
    // 圆角半径
    public static int cornerRadius = 2;
    
    public ChatButton(ButtonType type, int x, int y, int width, int height) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * 绘制按钮
     */
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        updateHover(mouseX, mouseY);
        
        // 获取当前背景色
        int bgColor = hovered ? hoveredBackgroundColor : backgroundColor;
        if (!enabled) {
            bgColor = 0x80404040;
        }
        
        // 获取文字颜色
        int textColor = getCurrentColor();
        
        // 绘制圆角背景
        GuiHelper.drawRoundedRect(x, y, x + width, y + height, cornerRadius, bgColor);
        
        // 绘制图标/文字
        FontRenderer fr = mc.fontRendererObj;
        String icon = getIcon();
        if (!icon.isEmpty()) {
            int iconWidth = fr.getStringWidth(icon);
            int iconX = x + (width - iconWidth) / 2;
            int iconY = y + (height - fr.FONT_HEIGHT) / 2 + 1;
            fr.drawStringWithShadow(icon, iconX, iconY, textColor);
        }
    }
    
    /**
     * Check if mouse is hovering
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && 
               mouseY >= y && mouseY <= y + height;
    }
    
    /**
     * Update hover state
     */
    public void updateHover(int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY) && enabled;
    }
    
    /**
     * Handle click
     */
    public boolean onClick(int mouseX, int mouseY) {
        return enabled && isMouseOver(mouseX, mouseY);
    }
    
    /**
     * Get button icon character
     */
    public String getIcon() {
        switch (type) {
            case COPY:
                return "📋";
            case DELETE:
                return "🗑️";
            case DELETE_HISTORY:
                return "🗑️";
            case SCREENSHOT:
                return "📷";
            case SEARCH:
                return "🔍";
            case CLEAR:
                return "✕";
            default:
                return "";
        }
    }
    
    /**
     * Get button tooltip
     */
    public String getTooltip() {
        switch (type) {
            case COPY:
                return "Copy Message";
            case DELETE:
                return "Delete Message";
            case DELETE_HISTORY:
                return "Clear Chat History";
            case SCREENSHOT:
                return "Take Screenshot";
            case SEARCH:
                return "Search Messages";
            case CLEAR:
                return "Clear";
            default:
                return "";
        }
    }
    
    // Getters and Setters
    public ButtonType getType() { return type; }
    public void setType(ButtonType type) { this.type = type; }
    
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    
    public boolean isHovered() { return hovered; }
    public void setHovered(boolean hovered) { this.hovered = hovered; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getColor() { return enabled ? color : 0xFF808080; }
    public void setColor(int color) { this.color = color; }
    
    public int getHoveredColor() { return hoveredColor; }
    public void setHoveredColor(int color) { this.hoveredColor = color; }
    
    public int getBackgroundColor() { return enabled ? backgroundColor : 0xFF404040; }
    public void setBackgroundColor(int color) { this.backgroundColor = color; }
    
    public int getHoveredBackgroundColor() { return hoveredBackgroundColor; }
    public void setHoveredBackgroundColor(int color) { this.hoveredBackgroundColor = color; }
    
    public int getCurrentColor() {
        return hovered ? hoveredColor : (enabled ? color : 0xFF808080);
    }
    
    public int getCurrentBackgroundColor() {
        return hovered ? hoveredBackgroundColor : backgroundColor;
    }
}
