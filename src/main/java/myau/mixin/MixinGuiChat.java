package myau.mixin;

import myau.Myau;
import myau.module.modules.chatting.ChattingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {

    @Shadow
    protected GuiTextField inputField;

    @Shadow
    private String defaultInputFieldText;

    @Shadow
    private boolean waitingOnAutocomplete;

    @Shadow
    protected abstract void onAutocompleteResponse(String[] p_onAutocompleteResponse_1_);

    @Shadow
    private int sentHistoryCursor;

    @Unique
    private static final List<String> COPY_TOOLTIP = Arrays.asList(
            "\u00a7e\u00a7lCopy To Clipboard",
            "\u00a7b\u00a7lNORMAL CLICK\u00a7r \u00a78- \u00a77Full Message",
            "\u00a7b\u00a7lCTRL CLICK\u00a7r \u00a78- \u00a77Single Line",
            "",
            "\u00a7e\u00a7lModifiers",
            "\u00a7b\u00a7lALT\u00a7r \u00a78- \u00a77Formatting Codes"
    );

    @Unique
    private static final List<String> DELETE_TOOLTIP = Arrays.asList(
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

        // Restore draft
        if (mod.inputFieldDraft.getValue()) {
            String command = (chatting$commandDraft.startsWith("/") ? "" : "/") + chatting$commandDraft;
            inputField.setText(inputField.getText().startsWith("/") ? command : chatting$draft);
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        // Chat tabs - Ctrl+Tab to switch
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

        // Draw button tooltips
        if (chatting$isHoveringButton(mouseX, mouseY)) {
            List<String> tooltip = chatting$isHoveringDelete(mouseX, mouseY) ? DELETE_TOOLTIP : COPY_TOOLTIP;
            GuiUtils.drawHoveringText(tooltip, mouseX, mouseY, width, height, -1, fontRendererObj);
        }
    }

    @Unique
    private boolean chatting$isHoveringButton(int mouseX, int mouseY) {
        // Simplified hover detection
        return mouseY < 20;
    }

    @Unique
    private boolean chatting$isHoveringDelete(int mouseX, int mouseY) {
        // Simplified hover detection for delete button
        return mouseX > width / 2 + 50 && mouseY < 20;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        // Handle copy/delete button clicks
        if (mod.chatCopy.getValue() && chatting$isHoveringCopy(mouseX, mouseY)) {
            // Copy functionality would go here
        }
        else if (mod.chatDelete.getValue() && chatting$isHoveringDelete(mouseX, mouseY)) {
            // Delete functionality would go here
        }
    }

    @Unique
    private boolean chatting$isHoveringCopy(int mouseX, int mouseY) {
        return mouseX > width / 2 - 60 && mouseX < width / 2 - 40 && mouseY < 20;
    }

    @Unique
    private static final String[] EMPTY_COMPLETE = new String[0];

    @Unique
    private String[] myau$latestAutoComplete = EMPTY_COMPLETE;

    /**
     * 处理客户端命令自动补全
     * 拦截原版服务器的自动补全请求，改为客户端命令补全
     */
    @Inject(method = "sendAutocompleteRequest", at = @At("HEAD"), cancellable = true)
    private void onSendAutocompleteRequest(String full, String ignored, CallbackInfo ci) {
        if (Myau.commandManager.isTypingCommand(full)) {
            // 获取自动补全建议
            List<String> completions = Myau.commandManager.getCompletions(full);

            if (!completions.isEmpty()) {
                // 将补全建议转换为Minecraft格式
                myau$latestAutoComplete = completions.toArray(new String[0]);

                // 标记为等待补全结果
                waitingOnAutocomplete = true;

                // 调用Minecraft的自动补全处理
                onAutocompleteResponse(myau$latestAutoComplete);

                // 取消原版请求
                ci.cancel();
            }
        }
    }

    @Inject(method = "keyTyped", at = @At("TAIL"))
    private void onKeyTypedEnd(char typedChar, int keyCode, CallbackInfo ci) {
        // Chat shortcuts processed before message sent
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void onGuiClosed(CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        // Save draft
        if (mod.inputFieldDraft.getValue()) {
            if (inputField.getText().startsWith("/")) {
                chatting$commandDraft = inputField.getText();
            } else {
                if (inputField.getText().isEmpty() && defaultInputFieldText.equals("/")) return;
                chatting$draft = inputField.getText();
            }
        }
    }
}
