package cn.sd.jrz.autoresource.network;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.Config;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * 配置同步：Forge 版 SERVER 配置会自动镜像到客户端，Fabric 无此机制。
 * 本类把当前生效配置全量下发给每个登录的客户端，
 * 由客户端入口（AutoResourceClient）注册接收并替换本地快照；
 * 菜单进度计算与物品 tooltip 均依赖该快照数值。
 * <p>
 * 1.21.1：ServerPlayNetworking.send 现在接受 CustomPacketPayload，
 * 原先 (ServerPlayer, Identifier, FriendlyByteBuf) 三参签名已移除。
 */
public final class ConfigSync {

    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath(AutoResource.MODID, "config_sync");
    public static final CustomPacketPayload.Type<ConfigPayload> PAYLOAD_TYPE = new CustomPacketPayload.Type<>(CHANNEL);

    /**
     * 配置快照的网络负载（包装 Config.Data + FriendlyByteBuf 编解码）
     */
    public record ConfigPayload(FriendlyByteBuf buffer) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, ConfigPayload> CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBytes(payload.buffer.readBytes(payload.buffer.readableBytes())),
                buf -> {
                    // 直接复制整个 buffer 区域作为新 payload
                    FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer(buf.readableBytes()));
                    copy.writeBytes(buf.readBytes(buf.readableBytes()));
                    return new ConfigPayload(copy);
                }
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return PAYLOAD_TYPE;
        }
    }

    private ConfigSync() {
    }

    /**
     * 挂接服务器事件：注册 payload 类型并下发配置
     */
    public static void init() {
        // 注册 payload 类型（play 阶段双向）
        PayloadTypeRegistry.playC2S().register(PAYLOAD_TYPE, ConfigPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PAYLOAD_TYPE, ConfigPayload.CODEC);
        // 玩家进入游戏时下发最新配置
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendToPlayer(handler.getPlayer()));
    }

    /**
     * 向单个玩家下发当前配置快照
     */
    public static void sendToPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            Config.get().encode(buf);
            ServerPlayNetworking.send(player, new ConfigPayload(buf));
        } catch (Exception e) {
            AutoResource.LOGGER.warn("[AutoResource] 配置同步失败: {}", e.toString());
        }
    }
}
