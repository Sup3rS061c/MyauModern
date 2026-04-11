package myau.module.modules.chatting.gui;

import myau.module.modules.chatting.ChatTabs;

/**
 * Tab Button for Chat Tabs UI
 */
public class TabButton {
    
    private ChatTabs.ChatTab tab;
    private int x, y, width, height;
    private boolean hovered = false;
    private boolean selected = false;
    
    public static int color = 0xFFFFFFFF;
    public static int hoveredColor = 0xFFFFFFA0;
    public static int selectedColor = 0xFFFFFF00;
    
    public TabButton(ChatTabs.ChatTab tab, int x, int y, int width, int height) {
        this.tab = tab;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    /**
     * Check if mouse is hovering over button
     */
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && 
               mouseY >= y && mouseY <= y + height;
    }
    
    /**
     * Update hover state
     */
    public void updateHover(int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY);
    }
    
    /**
     * Handle click
     */
    public boolean onClick(int mouseX, int mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            ChatTabs.setActiveTab(tab);
            return true;
        }
        return false;
    }
    
    // Getters and Setters
    public ChatTabs.ChatTab getTab() { return tab; }
    public void setTab(ChatTabs.ChatTab tab) { this.tab = tab; }
    
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
    
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    
    public int getCurrentColor() {
        if (selected) return selectedColor;
        if (hovered) return hoveredColor;
        return color;
    }
}
