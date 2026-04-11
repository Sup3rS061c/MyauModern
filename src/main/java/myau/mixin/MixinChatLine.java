package myau.mixin;

import myau.module.modules.chatting.ChattingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Pattern;

@Mixin(ChatLine.class)
public class MixinChatLine {

    @Unique
    private boolean chatting$detected = false;

    @Unique
    private boolean chatting$first = true;

    @Unique
    private NetworkPlayerInfo chatting$playerInfo = null;

    @Unique
    private NetworkPlayerInfo chatting$detectedPlayerInfo = null;

    @Unique
    private static NetworkPlayerInfo chatting$lastPlayerInfo = null;

    @Unique
    private static long chatting$lastUniqueId = 0;

    @Unique
    private long chatting$uniqueId = 0;

    @Unique
    private ChatLine chatting$fullMessage = null;

    @Unique
    private static ChatLine chatting$lastChatLine = null;

    @Unique
    private static final HashSet<WeakReference<ChatLine>> chatting$chatLines = new HashSet<>();

    @Unique
    private static final Pattern chatting$pattern = Pattern.compile("(\u00a7.)|\\W");

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int i, IChatComponent iChatComponent, int chatId, CallbackInfo ci) {
        ChattingModule mod = ChattingModule.getInstance();

        chatting$lastUniqueId++;
        chatting$uniqueId = chatting$lastUniqueId;
        chatting$chatLines.add(new WeakReference<>((ChatLine) (Object) this));

        // 聊天头检测
        if (mod == null || !mod.isEnabled() || !mod.chatHeads.getValue()) {
            return;
        }

        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();
        if (netHandler == null) return;

        Map<String, NetworkPlayerInfo> nicknameCache = new HashMap<>();
        try {
            for (String word : chatting$pattern.split(StringUtils.substringBefore(iChatComponent.getFormattedText(), ":"))) {
                if (word.isEmpty()) continue;
                chatting$playerInfo = netHandler.getPlayerInfo(word);
                if (chatting$playerInfo == null) {
                    chatting$playerInfo = chatting$getPlayerFromNickname(word, netHandler, nicknameCache);
                }
                if (chatting$playerInfo != null) {
                    chatting$detectedPlayerInfo = chatting$playerInfo;
                    chatting$detected = true;

                    // 检查是否是连续消息
                    if (chatting$lastPlayerInfo != null && chatting$playerInfo.getGameProfile().equals(chatting$lastPlayerInfo.getGameProfile())) {
                        chatting$first = false;
                        if (mod.hideChatHeadOnConsecutive.getValue()) {
                            chatting$playerInfo = null;
                        }
                    }
                    chatting$lastPlayerInfo = chatting$detectedPlayerInfo;
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Unique
    private static NetworkPlayerInfo chatting$getPlayerFromNickname(String word, NetHandlerPlayClient connection, Map<String, NetworkPlayerInfo> nicknameCache) {
        if (nicknameCache.isEmpty()) {
            for (NetworkPlayerInfo p : connection.getPlayerInfoMap()) {
                IChatComponent displayName = p.getDisplayName();
                if (displayName != null) {
                    String nickname = displayName.getUnformattedTextForChat();
                    if (word.equals(nickname)) {
                        nicknameCache.clear();
                        return p;
                    }
                    nicknameCache.put(nickname, p);
                }
            }
        } else {
            return nicknameCache.get(word);
        }
        return null;
    }

    @Unique
    public boolean chatting$hasDetected() {
        return chatting$detected;
    }

    @Unique
    public NetworkPlayerInfo chatting$getPlayerInfo() {
        return chatting$playerInfo;
    }

    @Unique
    public void chatting$updatePlayerInfo() {
        ChattingModule mod = ChattingModule.getInstance();
        if (mod != null && mod.hideChatHeadOnConsecutive.getValue() && !chatting$first) {
            chatting$playerInfo = null;
        } else {
            chatting$playerInfo = chatting$detectedPlayerInfo;
        }
    }

    @Unique
    public long chatting$getUniqueId() {
        return chatting$uniqueId;
    }

    @Unique
    public ChatLine chatting$getFullMessage() {
        return chatting$fullMessage;
    }
}
