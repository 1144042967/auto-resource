package cn.sd.jrz.autoresource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置系统：Forge 版使用 ForgeConfigSpec（SERVER 型，客户端自动镜像），
 * Fabric 版改为 config/autoresource.json + 登录时经 ConfigSync 同步到客户端快照，
 * 客户端读取逻辑（菜单进度计算、物品 tooltip 展示产品列表）不变。
 */
public class Config {

    /**
     * 可整体传输的配置快照（Gson 反序列化时缺失字段保留默认值）
     */
    public static class Data {

        public static class Energy {
            public FE fe = new FE();
        }

        public static class FE {
            /**
             * 最小产量
             */
            public long min = 1;
            /**
             * 最大产量
             */
            public long max = Long.MAX_VALUE;
            /**
             * 每次增长间隔秒数
             */
            public long second = 1;
            /**
             * 每次产量增量
             */
            public long step = 1;
            /**
             * 放入加速槽的增长加速物品（加速后增长量=当前发电量 1%）
             */
            @SerializedName("star_item")
            public String starItem = "minecraft:nether_star";
        }

        public static class Liquid {
            public Rate water = new Rate(50, Long.MAX_VALUE, 10, 50);
            public Rate lava = new Rate(50, Long.MAX_VALUE, 10, 50);
        }

        /**
         * 流体/方块机的共同参数组（实际数值需除以 1000）
         */
        public static class Rate {
            public long min;
            public long max;
            public long second;
            public long step;

            public Rate() {
            }

            public Rate(long min, long max, long second, long step) {
                this.min = min;
                this.max = max;
                this.second = second;
                this.step = step;
            }
        }

        public static class BlockGroup {
            public Rate generator = new Rate(50, Long.MAX_VALUE, 10, 50);
            /**
             * 方块生成机可生成产品列表：物品 ID 或 # 开头的标签
             */
            public List<String> items = defaultItems();
        }

        @SerializedName("energy")
        public Energy energy = new Energy();
        @SerializedName("liquid")
        public Liquid liquid = new Liquid();
        @SerializedName("block")
        public BlockGroup block = new BlockGroup();

        /**
         * 数值越界矫正（与 Forge defineInRange 的下限一致：min/max/second ≥1，step ≥0）
         */
        public void sanitize() {
            feSanitize();
            sanitizeRate(liquid.water, 1);
            sanitizeRate(liquid.lava, 1);
            sanitizeRate(block.generator, 1);
            if (block.items == null || block.items.isEmpty()) {
                block.items = defaultItems();
            } else {
                block.items = new ArrayList<>(block.items);
            }
            if (energy.fe.starItem == null || energy.fe.starItem.isBlank()) {
                energy.fe.starItem = "minecraft:nether_star";
            }
        }

        private void feSanitize() {
            FE fe = energy.fe;
            fe.min = Math.max(1, fe.min);
            fe.max = Math.max(1, fe.max);
            fe.second = Math.max(1, fe.second);
            fe.step = Math.max(0, fe.step);
        }

        private static void sanitizeRate(Rate rate, int minBound) {
            rate.min = Math.max(minBound, rate.min);
            rate.max = Math.max(minBound, rate.max);
            rate.second = Math.max(minBound, rate.second);
            rate.step = Math.max(0, rate.step);
        }

        /**
         * 方块机默认可生成产品列表（按主世界/下界/末地分类、类内按常见程度排序；与 Forge 版逐项一致）
         */
        public static List<String> defaultItems() {
            return List.of(
                    // 主世界 — 自然生成/基础方块
                    "minecraft:stone", "minecraft:deepslate", "minecraft:dirt", "minecraft:sand", "minecraft:gravel",
                    "minecraft:cobblestone", "minecraft:andesite", "minecraft:granite", "minecraft:diorite", "minecraft:tuff",
                    "minecraft:cobbled_deepslate", "minecraft:clay", "minecraft:red_sand", "minecraft:moss_block", "minecraft:rooted_dirt",
                    "minecraft:mud", "minecraft:dripstone_block", "minecraft:calcite", "minecraft:amethyst_block", "minecraft:obsidian",
                    "minecraft:prismarine",
                    // 主世界 — 建筑加工方块
                    "minecraft:bricks", "minecraft:smooth_stone",
                    // 标签优先：#minecraft:stone_bricks 覆盖全部石砖变种
                    "#minecraft:stone_bricks",
                    "minecraft:mossy_cobblestone", "minecraft:sandstone", "minecraft:smooth_sandstone", "minecraft:red_sandstone",
                    "minecraft:polished_andesite", "minecraft:polished_granite", "minecraft:polished_diorite",
                    "minecraft:polished_deepslate", "minecraft:deepslate_bricks", "minecraft:mud_bricks",
                    // 下界
                    "minecraft:netherrack", "minecraft:basalt", "minecraft:smooth_basalt", "minecraft:soul_sand", "minecraft:soul_soil",
                    "minecraft:blackstone", "minecraft:polished_blackstone_bricks", "minecraft:polished_blackstone",
                    "minecraft:nether_bricks", "minecraft:magma_block",
                    // 末地
                    "minecraft:end_stone", "minecraft:end_stone_bricks");
        }

        // ==================== 网络编解码（ConfigSync 使用，long 用 zigzag 变长编码压缩） ====================

        public void encode(net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeVarLong(energy.fe.min);
            buf.writeVarLong(energy.fe.max);
            buf.writeVarLong(energy.fe.second);
            buf.writeVarLong(energy.fe.step);
            buf.writeUtf(energy.fe.starItem);
            encodeRate(buf, liquid.water);
            encodeRate(buf, liquid.lava);
            encodeRate(buf, block.generator);
            buf.writeVarInt(block.items.size());
            for (String entry : block.items) {
                buf.writeUtf(entry);
            }
        }

        public static Data decode(net.minecraft.network.FriendlyByteBuf buf) {
            Data data = new Data();
            data.energy.fe.min = buf.readVarLong();
            data.energy.fe.max = buf.readVarLong();
            data.energy.fe.second = buf.readVarLong();
            data.energy.fe.step = buf.readVarLong();
            data.energy.fe.starItem = buf.readUtf();
            decodeRate(buf, data.liquid.water);
            decodeRate(buf, data.liquid.lava);
            decodeRate(buf, data.block.generator);
            int size = buf.readVarInt();
            List<String> items = new ArrayList<>(Math.min(size, 1024));
            for (int i = 0; i < size; i++) {
                items.add(buf.readUtf());
            }
            data.block.items = items;
            return data;
        }

        private static void encodeRate(net.minecraft.network.FriendlyByteBuf buf, Rate rate) {
            buf.writeVarLong(rate.min);
            buf.writeVarLong(rate.max);
            buf.writeVarLong(rate.second);
            buf.writeVarLong(rate.step);
        }

        private static void decodeRate(net.minecraft.network.FriendlyByteBuf buf, Rate rate) {
            rate.min = buf.readVarLong();
            rate.max = buf.readVarLong();
            rate.second = buf.readVarLong();
            rate.step = buf.readVarLong();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /**
     * 当前生效配置：服务端为磁盘文件内容，客户端为服务端下发的快照
     */
    private static volatile Data current = new Data();

    public static Data get() {
        return current;
    }

    /**
     * 从磁盘加载配置（文件不存在或损坏时写出默认值）；仅应在服务端调用
     */
    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("autoresource.json");
        Data data = new Data();
        boolean parseFailed = false;
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                Data parsed = GSON.fromJson(reader, Data.class);
                if (parsed != null) {
                    data = parsed;
                }
            } catch (Exception e) {
                LOGGER.warn("[AutoResource] 读取配置失败，使用默认值并回写: {}", e.toString());
                parseFailed = true;
            }
        }
        data.sanitize();
        current = data;

        // 缺失/损坏/新增字段时回写，使文件始终包含完整可编辑项
        if (!Files.isRegularFile(file) || parseFailed) {
            save(file, data);
        }
    }

    /**
     * 将当前配置写回磁盘（供运行期修改后持久化）
     */
    public static void save() {
        save(FabricLoader.getInstance().getConfigDir().resolve("autoresource.json"), current);
    }

    private static void save(Path file, Data data) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            LOGGER.warn("[AutoResource] 写出配置失败: {}", e.toString());
        }
    }

    /**
     * 客户端接收服务端下发配置时替换本地快照
     */
    public static void applyRemote(Data remote) {
        remote.sanitize();
        current = remote;
    }
}
