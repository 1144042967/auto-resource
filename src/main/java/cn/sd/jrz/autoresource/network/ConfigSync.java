package cn.sd.jrz.autoresource.network;

import cn.sd.jrz.autoresource.AutoResource;
import cn.sd.jrz.autoresource.Config;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 配置同步：Forge 版 SERVER 配置会自动镜像到客户端，Fabric 无此机制。
 * 本类把当前生效配置全量下发给每个登录的客户端，
 * 由客户端入口（AutoResourceClient）注册接收并替换本地快照；
 * 菜单进度计算与物品 tooltip 均依赖该快照数值。
 */
public final class ConfigSync {

    public static final Identifier CHANNEL_ID = Identifier.fromNamespaceAndPath(AutoResource.MODID, "config_sync");
    public static final CustomPacketPayload.Type<Payload> TYPE = new CustomPacketPayload.Type<>(CHANNEL_ID);

    /**
     * 自定义 payload 类型（MC 26.x Fabric networking v6+ 强制使用 payload 记录传递数据包）
     */
    public record Payload(Config.Data data) implements CustomPacketPayload {

        public static final StreamCodec<FriendlyByteBuf, Payload> CODEC = StreamCodec.of(
                (buf, payload) -> payload.data.encode(buf),
                buf -> new Payload(Config.Data.decode(buf))
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private ConfigSync() {
    }

    /**
     * 挂接 Payload 类型并注册服务端事件：玩家进入游戏时下发最新配置
     */
    public static void init() {
        // 注册 payload 类型（Fabric networking v6+ 强制要求）
        PayloadTypeRegistry.serverboundPlay().register(TYPE, Payload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TYPE, Payload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendToPlayer(handler.getPlayer()));
    }

    /**
     * 向单个玩家下发当前配置快照
     */
    public static void sendToPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        try {
            ServerPlayNetworking.send(player, new Payload(Config.get()));
        } catch (Exception e) {
            AutoResource.LOGGER.warn("[AutoResource] 配置同步失败: {}", e.toString());
        }
    }

    /**
     * 向所有在线玩家广播（供运行期修改配置后刷新）
     */
    public static void sendToAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer(player);
        }
    }
}