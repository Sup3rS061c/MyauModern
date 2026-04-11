package myau.mixin;

import com.google.common.collect.Lists;
import myau.module.modules.chatting.ChattingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.util.List;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {

    @Shadow
    protected GuiTextField inputField;

    @Shadow
    public abstract void drawScreen(int mouseX, int mouseY, float partialTicks);

    @Shadow
    private String defaultInputFieldText;

    @Shadow
    public abstract void sendChatMessage(String msg);

    @Unique
    private static final List<String> COPY_TOOLTIP = Lists.newArrayList(
            "\u00a7e\u00a7lCopy To Clipboard",
            "\u00a7b\u00a7lNORMAL CLICK\u00a7r \u00a78- \u00a77Full Message",
            "\u00a7b\u00a7lCTRL CLICK\u00a7r \u00a78- \u00a77Single Line",
            "\u00a7b\u00a7lSHIFT CLICK\u00a7r \u00a78- \u00a77Screenshot Message",
            "",
            "\u00a7e\u00a7lModifiers",
            "\u00a7b\u00a7lALT\u00a7r \u00a78- \u00a77Formatting Codes"
    );

    @Unique
    private static final List<String> DELETE_TOOLTIP = Lists.newArrayList(
            "\u00a7b\u00a7lNORMAL CLICK\u00a7r \u00a78- \u00a77Full Message",
            "\u00a7b\u00a7lCTRL CLICK\u00a7r \u00a78- \u00a77Single Line"
    );

    @Unique
    private String chatting$draft = "";

    @Unique
    private String chatting$commandDraft = "";

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        // 恢复草稿
        if (mod.inputFieldDraft.getValue()) {
            String command = (chatting$commandDraft.startsWith("/") ? "" : "/") + chatting$commandDraft;
            inputField.setText(inputField.getText().startsWith("/") ? command : chatting$draft);
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        // 聊天搜索 - Tab切换标签
        if ((Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) && keyCode == Keyboard.KEY_TAB) {
            mod.switchTab();
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        boolean copy = mod.chatCopy.getValue();
        boolean delete = mod.chatDelete.getValue();
        if (!copy && !delete) return;

        // 绘制按钮提示
        if (chatting$isHoveringButton()) {
            List<String> tooltip = chatting$isHoveringDelete() ? DELETE_TOOLTIP : COPY_TOOLTIP;
            GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, width, height, -1, fontRendererObj);
            GlStateManager.disableLighting();
        }
    }

    @Unique
    private boolean chatting$isHoveringButton() {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null) return false;

        MixinGuiNewChat hook = (MixinGuiNewChat) Minecraft.getMinecraft().ingameGUI.getChatGUI();
        int scale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        int x = Mouse.getX();
        int right = hook.chatting$right;

        return hook.chatting$isHovering && x > right * scale && x < (right + 9 * mod.chatScale.getValue().floatValue()) * scale;
    }

    @Unique
    private boolean chatting$isHoveringDelete() {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null) return false;

        int scale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        int x = Mouse.getX();
        MixinGuiNewChat hook = (MixinGuiNewChat) Minecraft.getMinecraft().ingameGUI.getChatGUI();
        int right = hook.chatting$right + (mod.chatCopy.getValue() ? 10 : 0);

        return hook.chatting$isHovering && x > right * scale && x < (right + 9 * mod.chatScale.getValue().floatValue()) * scale;
    }

    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiChat;drawRect(IIIII)V"))
    private void cancelDefaultBg(int left, int top, int right, int bottom, int color) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) {
            drawRect(left, top, right, bottom, color);
            return;
        }

        // 绘制自定义输入框背景
        int bgColor = mod.chatInputBackgroundColor.getColor().getRGB();
        drawRect(left, top, right, bottom, bgColor);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        MixinGuiNewChat hook = (MixinGuiNewChat) Minecraft.getMinecraft().ingameGUI.getChatGUI();
        if (!hook.chatting$isHovering) return;

        int scale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        int x = Mouse.getX();

        // 复制按钮点击
        if (mod.chatCopy.getValue() && chatting$isHoveringCopy(x, scale)) {
            Transferable message = hook.chatting$getChattingChatComponent(Mouse.getY(), mouseButton);
            if (message != null) {
                try {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(message, null);
                    mod.showNotification("Copied message to clipboard!");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // 删除按钮点击
        else if (mod.chatDelete.getValue() && chatting$isHoveringDelete(x, scale)) {
            ChatLine chatLine = hook.chatting$getHoveredLine(Mouse.getY());
            if (chatLine != null) {
                removeChatLine(chatLine);
            }
        }
    }

    @Unique
    private boolean chatting$isHoveringCopy(int x, int scale) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null) return false;

        MixinGuiNewChat hook = (MixinGuiNewChat) Minecraft.getMinecraft().ingameGUI.getChatGUI();
        int right = hook.chatting$right;
        return x > right * scale && x < (right + 9 * mod.chatScale.getValue().floatValue()) * scale;
    }

    @Unique
    private void removeChatLine(ChatLine chatLine) {
        GuiNewChat chatGUI = Minecraft.getMinecraft().ingameGUI.getChatGUI();

        // 使用反射获取私有字段
        try {
            java.lang.reflect.Field chatLinesField = GuiNewChat.class.getDeclaredField("chatLines");
            chatLinesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ChatLine> chatLines = (List<ChatLine>) chatLinesField.get(chatGUI);

            java.lang.reflect.Field drawnChatLinesField = GuiNewChat.class.getDeclaredField("drawnChatLines");
            drawnChatLinesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ChatLine> drawnChatLines = (List<ChatLine>) drawnChatLinesField.get(chatGUI);

            // 根据是否按下Ctrl键决定删除方式
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                // 删除单行
                drawnChatLines.remove(chatLine);
            } else {
                // 删除完整消息
                String fullText = chatLine.getChatComponent().getUnformattedText();
                drawnChatLines.removeIf(line -> line.getChatComponent().getUnformattedText().equals(fullText));
                chatLines.removeIf(line -> line.getChatComponent().getUnformattedText().equals(fullText));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ModifyArg(method = "keyTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiChat;sendChatMessage(Ljava/lang/String;)V"), index = 0)
    private String modifySentMessage(String original) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled() || !mod.chatShortcuts.getValue()) {
            return original;
        }

        // 处理聊天快捷方式
        if (original.startsWith("/")) {
            return "/" + mod.processShortcut(StringUtils.substringAfter(original, "/"));
        }
        return original;
    }

    @Inject(method = "keyTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiChat;sendChatMessage(Ljava/lang/String;)V"))
    private void onSendMessage(char typedChar, int keyCode, CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        // 清空草稿
        if (mod.inputFieldDraft.getValue()) {
            inputField.setText(inputField.getText().startsWith("/") ? "/" : "");
        }
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void onGuiClosed(CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        // 保存草稿
        if (mod.inputFieldDraft.getValue()) {
            if (inputField.getText().startsWith("/")) {
                chatting$commandDraft = inputField.getText();
            } else {
                if (inputField.getText().isEmpty() && defaultInputFieldText.equals("/")) return;
                chatting$draft = inputField.getText();
            }
        }
    }

    @Inject(method = "handleMouseInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;scroll(I)V"))
    private void onScroll(CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        // 平滑滚动
        mod.setShouldSmoothScroll(true);
    }
}
