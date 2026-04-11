package myau.mixin;

import myau.module.modules.chatting.ChattingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.List;

@Mixin(value = GuiNewChat.class, priority = 999)
public abstract class MixinGuiNewChat extends Gui {

    @Shadow
    @Final
    private Minecraft mc;

    @Shadow
    @Final
    private List<ChatLine> drawnChatLines;

    @Shadow
    public abstract boolean getChatOpen();

    @Shadow
    public abstract int getLineCount();

    @Shadow
    private int scrollPos;

    @Shadow
    public abstract int getChatWidth();

    @Shadow
    public abstract float getChatScale();

    @Unique
    private int chatting$right = 0;

    @Unique
    private boolean chatting$isHovering = false;

    @Unique
    private boolean chatting$chatCheck = false;

    @Unique
    private int chatting$textOpacity = 255;

    @Unique
    private ChatLine chatting$chatLine = null;

    @Unique
    private int chatting$totalLines = 0;

    @Unique
    private boolean chatting$lastOpen = false;

    @Unique
    private long chatting$time = 0;

    @Unique
    private boolean chatting$closing = false;

    @Unique
    private int chatting$updateCounter = 0;

    @Unique
    private boolean chatting$lineInBounds = false;

    @Unique
    private int chatting$newLines = 0;

    @Unique
    private float chatting$animationPercent = 1.0f;

    @Unique
    private int chatting$lineBeingDrawn = 0;

    @Unique
    private long chatting$lastUniqueId = 0;

    @Unique
    private final java.util.HashSet<java.lang.ref.WeakReference<ChatLine>> chatting$chatLines = new java.util.HashSet<>();

    // ========== 聊天按钮功能 ==========

    @Inject(method = "drawChat", at = @At("HEAD"))
    private void onDrawChatStart(int updateCounter, CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        chatting$chatCheck = false;
        if (chatting$lastOpen != getChatOpen()) {
            if (chatting$lastOpen) chatting$time = Minecraft.getSystemTime();
            chatting$lastOpen = getChatOpen();
        }
        long duration = mod.smoothBackground.getValue() ? (long) mod.backgroundDuration.getValue() : 0;
        chatting$closing = (Minecraft.getSystemTime() - chatting$time <= duration);

        // 平滑消息动画
        if (mod.smoothChat.getValue() && !this.isScrolled) {
            chatting$animationPercent = Math.min(1.0f, (Minecraft.getSystemTime() - chatting$time) / ((1.0f - mod.messageSpeed.getValue().floatValue()) * 1000f));
        } else {
            chatting$animationPercent = 1.0f;
        }
    }

    @ModifyVariable(method = "drawChat", at = @At("HEAD"), argsOnly = true)
    private int setUpdateCounter(int updateCounter) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return updateCounter;
        return (chatting$updateCounter = updateCounter);
    }

    @ModifyVariable(method = "drawChat", at = @At("STORE"), index = 2)
    private int setChatLimit(int linesToDraw) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return linesToDraw;
        return calculateChatboxHeight(mc.gameSettings.chatHeightFocused) / 9;
    }

    @ModifyVariable(method = "drawChat", at = @At(value = "STORE", ordinal = 0), ordinal = 6)
    private int fadeTime(int value) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return value;
        return value + 200 - (int) (mod.fadeTime.getValue() * 20);
    }

    @Redirect(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;getChatOpen()Z"))
    private boolean noFade(GuiNewChat instance) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return instance.getChatOpen();
        return !mod.fadeChat.getValue() || instance.getChatOpen() || chatting$closing;
    }

    @ModifyArgs(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V", ordinal = 0))
    private void captureDrawRect(Args args) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        args.set(4, 0); // 透明背景

        if (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) {
            int left = args.get(0);
            int top = args.get(1);
            int right = args.get(2);

            if (chatting$isHovered(left, top, right - left, 9)) {
                chatting$isHovering = true;
                chatting$lineInBounds = true;
                args.set(4, mod.hoveredChatBackgroundColor.getValue());
            }
        }
    }

    @ModifyArgs(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private void drawChatBox(Args args) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;

        if (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) {
            int left = 0;
            int top = (int) ((float) args.get(2) - 1);
            int right = getChatWidth() + 4;

            if ((chatting$isHovering && chatting$lineInBounds) || chatting$isHovered(left, top, right + 20, 9)) {
                chatting$isHovering = true;
                chatting$drawCopyChatBox(right, top);
            }
        }
        chatting$lineInBounds = false;
    }

    @Unique
    private void chatting$drawCopyChatBox(int right, int top) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null) return;

        chatting$chatCheck = true;
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();

        int posLeft = right + 1;
        int posRight = right + 10;

        // 复制按钮
        if (mod.chatCopy.getValue()) {
            chatting$right = right;
            boolean hovered = chatting$isHovered(posLeft, top, posRight - posLeft, 9);
            int bgColor = hovered ? mod.chatButtonHoveredBackgroundColor.getValue() : mod.chatButtonBackgroundColor.getValue();
            int btnColor = hovered ? mod.chatButtonHoveredColor.getValue() : mod.chatButtonColor.getValue();

            drawRect(posLeft, top, posRight, top + 9, bgColor);
            drawRect(posLeft + 1, top + 1, posRight - 1, top + 8, btnColor);

            posLeft += 10;
            posRight += 10;
        }

        // 删除按钮
        if (mod.chatDelete.getValue()) {
            boolean hovered = chatting$isHovered(posLeft, top, posRight - posLeft, 9);
            int bgColor = hovered ? mod.chatButtonHoveredBackgroundColor.getValue() : mod.chatButtonBackgroundColor.getValue();
            int btnColor = hovered ? mod.chatButtonHoveredColor.getValue() : mod.chatButtonColor.getValue();

            drawRect(posLeft, top, posRight, top + 9, bgColor);
            drawRect(posLeft + 1, top + 1, posRight - 1, top + 8, btnColor);
        }

        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
    }

    @Unique
    private boolean chatting$isHovered(int x, int y, int width, int height) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null) return false;

        net.minecraft.client.gui.ScaledResolution scaleResolution = new net.minecraft.client.gui.ScaledResolution(mc);
        int scale = scaleResolution.getScaleFactor();
        int mouseX = Mouse.getX();
        int mouseY = mc.displayHeight - Mouse.getY();
        float chatScale = mod.chatScale.getValue().floatValue();

        int paddingX = mod.chatPaddingX.getValue().intValue();
        int paddingY = mod.chatPaddingY.getValue().intValue();
        int chatX = mod.chatPosX.getValue().intValue();
        int chatY = mod.chatPosY.getValue().intValue();

        int actualX = (int) (((int) chatX + (x + paddingX) * chatScale) * scale);
        int actualY = (int) (((int) chatY + (y - paddingY) * chatScale) * scale);

        return mouseX > actualX && mouseX < actualX + width * chatScale * scale
                && mouseY > actualY && mouseY < actualY + height * chatScale * scale;
    }

    @Inject(method = "drawChat", at = @At("RETURN"))
    private void onDrawChatEnd(int updateCounter, CallbackInfo ci) {
        if (!chatting$chatCheck && chatting$isHovering) {
            chatting$isHovering = false;
        }
    }

    @ModifyVariable(method = "setChatLine", at = @At("STORE"), ordinal = 2)
    private int wrap(int value) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return value;
        return mod.customChatWidth.getValue() ? mod.chatWidth.getValue().intValue() : calculateChatboxWidth(mc.gameSettings.chatWidth);
    }

    @Unique
    private static int calculateChatboxWidth(float chatWidth) {
        return MathHelper.floor_float(chatWidth * 280.0f + 40.0f);
    }

    @Unique
    private static int calculateChatboxHeight(float chatHeight) {
        return MathHelper.floor_float(chatHeight * 180.0f + 20.0f);
    }

    @Shadow
    private boolean isScrolled;

    // ========== 聊天搜索功能 ==========

    @Redirect(method = "drawChat", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GuiNewChat;drawnChatLines:Ljava/util/List;"))
    private List<ChatLine> filterMessages(GuiNewChat instance) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled() || mod.searchQuery.isEmpty()) {
            return drawnChatLines;
        }

        java.util.List<ChatLine> filtered = new java.util.ArrayList<>();
        String search = mod.searchQuery.toLowerCase();
        for (ChatLine line : drawnChatLines) {
            String text = line.getChatComponent().getUnformattedText().toLowerCase();
            if (text.contains(search)) {
                filtered.add(line);
            }
        }
        return filtered;
    }

    @Inject(method = "setChatLine", at = @At("HEAD"))
    private void onSetChatLine(IChatComponent chatComponent, int chatLineId, int updateCounter, boolean displayOnly, CallbackInfo ci) {
        // 聊天标签过滤
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled() || !mod.chatTabs.getValue()) return;

        String text = chatComponent.getUnformattedText();
        ChattingModule.ChatTab currentTab = mod.getCurrentTab();

        if (currentTab != ChattingModule.ChatTab.ALL) {
            boolean shouldShow = false;
            switch (currentTab) {
                case PARTY:
                    shouldShow = text.contains("Party") || text.contains("party");
                    break;
                case GUILD:
                    shouldShow = text.contains("Guild") || text.contains("guild");
                    break;
                case PM:
                    shouldShow = text.contains("->") || text.contains("<-");
                    break;
            }
            if (!shouldShow) {
                ci.cancel();
            }
        }
    }

    // ========== 平滑滚动 ==========

    @Unique
    private float chatting$smoothScrollPos = 0;

    @Redirect(method = "drawChat", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GuiNewChat;scrollPos:I"))
    private int redirectScrollPos(GuiNewChat instance) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled() || !mod.smoothScrolling.getValue()) {
            return scrollPos;
        }

        // 平滑滚动插值
        float target = scrollPos;
        chatting$smoothScrollPos += (target - chatting$smoothScrollPos) * 0.3f;
        return (int) chatting$smoothScrollPos;
    }

    // ========== 平滑消息动画 ==========

    @ModifyArg(method = "drawChat", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;", ordinal = 0, remap = false), index = 0)
    private int getLineBeingDrawn(int line) {
        chatting$lineBeingDrawn = line;
        return line;
    }

    @ModifyArg(method = "drawChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I"))
    private int modifyTextOpacity(int original) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled() || !mod.smoothChat.getValue()) return original;

        if (chatting$lineBeingDrawn <= chatting$newLines) {
            int opacity = (original >> 24) & 0xFF;
            opacity *= chatting$animationPercent;
            return (original & ~(0xFF << 24)) | (opacity << 24);
        }
        return original;
    }

    @ModifyVariable(method = "setChatLine", at = @At("STORE"), ordinal = 0)
    private List<IChatComponent> setNewLines(List<IChatComponent> original) {
        chatting$newLines = original.size() - 1;
        return original;
    }

    @Inject(method = "printChatMessageWithOptionalDeletion", at = @At("HEAD"))
    private void onPrintChatMessage(IChatComponent chatComponent, int chatLineId, CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null || !mod.isEnabled()) return;
        chatting$time = Minecraft.getSystemTime();
    }

    // ========== 获取悬停行 ==========

    public ChatLine chatting$getHoveredLine(int mouseY) {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod == null) return null;

        if (this.getChatOpen()) {
            net.minecraft.client.gui.ScaledResolution scaledresolution = new net.minecraft.client.gui.ScaledResolution(this.mc);
            int i = scaledresolution.getScaleFactor();
            float f = mod.chatScale.getValue().floatValue();
            int paddingY = mod.chatPaddingY.getValue().intValue();
            int chatY = mod.chatPosY.getValue().intValue();

            int k = (int) (mouseY / i - (scaledresolution.getScaledHeight() - chatY + paddingY * f));
            k = MathHelper.floor_float((float) k / f);

            if (k >= 0) {
                int l = Math.min(this.getLineCount(), this.drawnChatLines.size());

                if (k < this.mc.fontRendererObj.FONT_HEIGHT * l + l) {
                    int i1 = k / this.mc.fontRendererObj.FONT_HEIGHT + this.scrollPos;

                    if (i1 >= 0 && i1 < this.drawnChatLines.size()) {
                        return this.drawnChatLines.get(i1);
                    }
                }
            }
        }
        return null;
    }

    public Transferable chatting$getChattingChatComponent(int mouseY, int mouseButton) {
        ChatLine subLine = chatting$getHoveredLine(mouseY);
        if (subLine != null) {
            net.minecraft.client.gui.GuiScreen gui = mc.currentScreen;
            String message = subLine.getChatComponent().getFormattedText();

            if (net.minecraft.client.gui.GuiScreen.isShiftKeyDown() && mouseButton == 0) {
                // 截图功能
                return null;
            }

            String actualMessage = net.minecraft.client.gui.GuiScreen.isAltKeyDown() ? message : EnumChatFormatting.getTextWithoutFormattingCodes(message);
            return new StringSelection(actualMessage);
        }
        return null;
    }
}
