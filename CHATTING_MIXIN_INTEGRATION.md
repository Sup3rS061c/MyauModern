# Chatting Mixin 集成指南

本指南描述了 Chatting 模组的 Mixin 如何集成到 MyauModern 项目中。

## 已创建的 Mixin 文件

### 1. MixinGuiNewChat.java
**目标类**: `net.minecraft.client.gui.GuiNewChat`

**功能**:
- 聊天消息平滑动画
- 聊天按钮（复制/删除）
- 聊天搜索过滤
- 聊天标签过滤
- 平滑滚动
- 消息淡入淡出
- 悬停高亮效果
- 获取悬停行

### 2. MixinGuiChat.java
**目标类**: `net.minecraft.client.gui.GuiChat`

**功能**:
- 聊天按钮交互（点击复制/删除）
- 按钮提示显示
- 聊天快捷方式处理
- 草稿保存和恢复
- Tab 切换标签页
- 平滑滚动触发

### 3. MixinChatLine.java
**目标类**: `net.minecraft.client.gui.ChatLine`

**功能**:
- 聊天头检测
- 玩家信息关联
- 唯一ID管理
- 连续消息隐藏聊天头

## 模块文件

### ChattingModule.java
主模块类，包含所有配置选项:
- 聊天窗口设置（位置、大小、缩放）
- 动画设置（平滑消息、滚动、背景）
- 按钮设置（复制、删除、截图、搜索）
- 聊天标签设置
- 聊天头设置
- 快捷方式设置
- 垃圾信息屏蔽设置
- 颜色设置

### ChatTabs.java
聊天标签系统:
- ALL - 全部消息
- PARTY - 队伍消息
- GUILD - 公会消息
- PM - 私聊消息
- SYSTEM - 系统消息

### ChatShortcuts.java
聊天快捷方式:
- 常用命令别名（p, r, cc, gc 等）
- 变量替换（坐标、FPS、延迟）
- 快捷命令处理

## 使用方法

1. **在 ModuleManager 中注册模块**:
```java
modules.add(new ChattingModule());
```

2. **模块会自动加载 Mixin**，无需额外配置。

3. **配置选项**:
   - 通过 ClickGUI 或配置文件修改设置
   - 所有设置都有对应的 get/set 方法

## 功能说明

### 聊天按钮
- 复制按钮: 点击复制聊天消息到剪贴板
- 删除按钮: 删除选中的聊天消息
- Ctrl+点击: 复制单行
- Shift+点击: 截图
- Alt+点击: 保留格式代码

### 聊天标签
- Ctrl+Tab: 切换标签页
- 标签过滤: 只显示对应类型的消息

### 聊天快捷方式
- 输入快捷命令后自动替换为完整命令
- 支持变量: {x}, {y}, {z}, {fps}, {ping}, {ip}

### 平滑动画
- 新消息滑入动画
- 滚动平滑插值
- 背景淡入淡出

## 注意事项

1. Mixin 优先级设置为 999，确保在大多数情况下正确注入
2. 所有功能都可以通过配置选项开启/关闭
3. 模块禁用时 Mixin 逻辑会自动跳过
4. 与其他模组的兼容性需要测试

## 依赖

- Mixin 0.7+
- Minecraft 1.8.9 Forge
- Java 8
