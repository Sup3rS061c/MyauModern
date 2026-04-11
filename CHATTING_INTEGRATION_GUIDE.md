# Chatting Module Integration Guide

## Overview
The Chatting module from Polyfrost/Chatting has been integrated into MyauModern.

## Files Added

### Core Module
- `src/main/java/myau/module/modules/chatting/ChattingModule.java` - Main module class

### Chat Features
- `src/main/java/myau/module/modules/chatting/ChatTabs.java` - Chat tab system
- `src/main/java/myau/module/modules/chatting/ChatShortcuts.java` - Chat shortcuts
- `src/main/java/myau/module/modules/chatting/ChatSpamBlock.java` - Spam blocker
- `src/main/java/myau/module/modules/chatting/ChatScreenshot.java` - Screenshot system
- `src/main/java/myau/module/modules/chatting/ChatSearch.java` - Chat search

### GUI Components
- `src/main/java/myau/module/modules/chatting/gui/TabButton.java` - Tab buttons
- `src/main/java/myau/module/modules/chatting/gui/ChatButton.java` - Chat buttons (Copy, Delete, Screenshot, Search)

### Utilities
- `src/main/java/myau/module/modules/chatting/util/EaseOutQuad.java` - Animation utility
- `src/main/java/myau/module/modules/chatting/util/EaseOutQuart.java` - Animation utility

## Integration Steps

### 1. Register Module in ModuleManager

Edit `src/main/java/myau/module/ModuleManager.java` and add:

```java
import myau.module.modules.chatting.ChattingModule;

// In the constructor or initialization method:
modules.add(new ChattingModule());
```

### 2. Add Event Listeners

You need to hook into Minecraft's chat events. Create mixins or event handlers for:

- Chat message received event
- Chat GUI open/close
- Chat input handling
- Key press events for peek mode

### 3. Create Mixins

Create the following mixin classes:

#### GuiNewChatMixin.java
```java
@Mixin(GuiNewChat.class)
public class GuiNewChatMixin {
    // Handle chat rendering
    // Add tab buttons
    // Add copy/delete buttons
}
```

#### GuiChatMixin.java
```java
@Mixin(GuiChat.class)
public class GuiChatMixin {
    // Handle chat input shortcuts
    // Handle tab switching
}
```

#### ChatLineMixin.java
```java
@Mixin(ChatLine.class)
public class ChatLineMixin {
    // Store player info for chat heads
}
```

### 4. Chat Configuration

Add to your config system (if using a config library):

```java
// Example configuration values
public class ChatConfig {
    public boolean chatTabs = true;
    public boolean chatCopy = true;
    public boolean chatDelete = true;
    public boolean chatScreenshot = true;
    public boolean chatSearch = true;
    public boolean smoothChat = true;
    public boolean showChatHeads = true;
    public boolean spamBlocker = true;
    public float spamThreshold = 95.0f;
}
```

## Features Implemented

### 1. Chat Tabs
- Filter messages by tabs
- Default tabs: ALL, PARTY, GUILD, PM
- Custom tab creation support
- Tab switching with keybinds

### 2. Chat Buttons
- Copy message button
- Delete message button
- Clear chat history button
- Screenshot button
- Search button

### 3. Chat Shortcuts
- Custom shortcut definitions
- Default shortcuts for common messages

### 4. Spam Blocker
- Pattern-based spam detection
- Message similarity detection
- Player message frequency tracking
- Configurable threshold

### 5. Chat Search
- Regex and literal search
- Case-sensitive option
- Result navigation

### 6. Screenshot
- Save to file
- Copy to clipboard
- Both options

### 7. Visual Features
- Smooth chat animations
- Smooth scrolling
- Chat peek mode
- Custom colors

## Usage Example

```java
// Enable the module
ChattingModule module = (ChattingModule) Myau.moduleManager.modules.get(ChattingModule.class);
module.setEnabled(true);

// Configure settings
module.setChatTabs(true);
module.setChatCopy(true);
module.setSmoothChat(true);
module.setShowChatHeads(true);

// Access chat tabs
ChatTab currentTab = ChatTabs.getSelectedTab();
ChatTabs.nextTab(); // Switch to next tab

// Use search
ChatSearch search = new ChatSearch();
search.setQuery("hello");
search.setUseRegex(false);
String result = search.nextResult();

// Take screenshot
ChatScreenshot.setMode(ChatScreenshot.ScreenshotMode.SAVE_TO_SYSTEM);
ChatScreenshot.captureChat(chatImage);
```

## Translation from Kotlin

The original Chatting mod was written in Kotlin. Key translations:

| Kotlin Feature | Java Equivalent |
|---------------|-----------------|
| `data class` | Regular class with getters/setters |
| `object` | Singleton pattern with static methods |
| `val` | `final` variables |
| `var` | Regular variables |
| `?:` | Null coalescing with ternary |
| `?.` | Null-safe calls with null checks |
| Extension functions | Static utility methods |
| Lambda expressions | Anonymous inner classes or method references |

## Notes

- The module is designed to be self-contained within the `chatting` package
- All animations use simple interpolation (EaseOutQuad, EaseOutQuart)
- Thread safety should be considered for chat message handling
- The module follows MyauModern's existing module pattern

## Testing Checklist

- [ ] Module enables/disables correctly
- [ ] Chat tabs filter messages
- [ ] Tab buttons render and work
- [ ] Copy button copies messages
- [ ] Delete button removes messages
- [ ] Screenshot saves/copies correctly
- [ ] Search finds messages
- [ ] Spam blocker filters spam
- [ ] Smooth animations work
- [ ] Chat peek mode works
- [ ] Settings save/load correctly
