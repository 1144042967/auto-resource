package cn.sd.jrz.autoresource.generator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 方块生成机一键添加工具。
 * <p>
 * 用法：在下方常量区填入新方块的参数，然后在 IDEA 中右键运行本类即可。
 * 工具将自动生成所有 JSON 资源文件、合成纹理，并修改 Config.java / DataConfig.java /
 * Registration.java / ItemManager.java / lang/*.json。
 * <p>
 * 再次添加新方块时，修改常量区参数后重新运行即可。
 */
public class BlockGeneratorAdder {

    // ==================== 配置参数（每次添加新方块机时修改这里） ====================

    /**
     * 方块蛇形命名（小写+下划线），如 "diamond_block"
     */
    private static final String BLOCK_ID = "sandstone";

    /**
     * 英文显示名，如 "Diamond Block"
     */
    private static final String EN_NAME = "Sand Stone";

    /**
     * 中文显示名，如 "钻石块"
     */
    private static final String ZH_NAME = "砂岩";

    /**
     * 合成配方中的原料物品 ID，如 "minecraft:diamond_block"
     */
    private static final String SOURCE_ITEM = "minecraft:sandstone";

    /**
     * 源纹理文件路径（16×16 PNG），如 "C:/textures/diamond_block.png"
     */
    private static final String SOURCE_TEXTURE_PATH = "C:\\Users\\10177\\Pictures\\assets\\minecraft\\textures\\block\\sandstone_bottom.png";

    // ==================== 路径常量 ====================

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    private static final Path RESOURCES = PROJECT_ROOT.resolve("src/main/resources");
    private static final Path JAVA_SRC = PROJECT_ROOT.resolve("src/main/java");
    private static final Path PKG_PATH = Paths.get("cn/sd/jrz/autoresource");

    // 资源路径
    private static final Path BLOCKSTATES_DIR = RESOURCES.resolve("assets/autoresource/blockstates");
    private static final Path MODELS_BLOCK_DIR = RESOURCES.resolve("assets/autoresource/models/block");
    private static final Path MODELS_ITEM_DIR = RESOURCES.resolve("assets/autoresource/models/item");
    private static final Path LOOT_TABLES_DIR = RESOURCES.resolve("data/autoresource/loot_tables/blocks");
    private static final Path RECIPES_DIR = RESOURCES.resolve("data/autoresource/recipes");
    private static final Path TEXTURES_DIR = RESOURCES.resolve("assets/autoresource/textures/block");

    // 需要修改的 Java / JSON 文件
    private static final Path CONFIG_JAVA = JAVA_SRC.resolve(PKG_PATH).resolve("Config.java");
    private static final Path DATA_CONFIG_JAVA = JAVA_SRC.resolve(PKG_PATH).resolve("DataConfig.java");
    private static final Path REGISTRATION_JAVA = JAVA_SRC.resolve(PKG_PATH.resolve("setup")).resolve("Registration.java");
    private static final Path ITEM_MANAGER_JAVA = JAVA_SRC.resolve(PKG_PATH.resolve("items")).resolve("ItemManager.java");
    private static final Path EN_US_JSON = RESOURCES.resolve("assets/autoresource/lang/en_us.json");
    private static final Path ZH_CN_JSON = RESOURCES.resolve("assets/autoresource/lang/zh_cn.json");

    // 模板纹理
    private static final Path TEMPLATE_TEXTURE = TEXTURES_DIR.resolve("block_generator_cobblestone.png");

    // ==================== 派生常量 ====================

    /**
     * 例如: DIAMOND_BLOCK，用于 Blocks 字段引用和配置常量名
     */
    private static final String UPPER_ID = BLOCK_ID.toUpperCase();
    /**
     * 例如: BLOCK_GENERATOR_DIAMOND_BLOCK
     */
    private static final String REG_PREFIX = "BLOCK_GENERATOR_" + UPPER_ID;
    /**
     * 例如: block_generator_diamond_block
     */
    private static final String FULL_BLOCK_ID = "block_generator_" + BLOCK_ID;

    // ==================== main ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   方块生成机一键添加工具                  ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  方块ID: " + padRight(FULL_BLOCK_ID, 30) + " ║");
        System.out.println("║  英文名: " + padRight(EN_NAME, 30) + " ║");
        System.out.println("║  中文名: " + padRight(ZH_NAME, 30) + " ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        Path sourcePath = Paths.get(SOURCE_TEXTURE_PATH);
        if (!Files.exists(sourcePath)) {
            throw new RuntimeException("源纹理文件不存在：" + SOURCE_TEXTURE_PATH);
        }

        // 1. 生成 JSON 资源文件
        generateJsonFiles();

        // 2. 生成纹理
        generateTexture();

        // 3. 修改 Java 源文件
        insertConfig();
        insertDataConfig();
        insertRegistration();
        insertItemManager();

        // 4. 修改语言文件
        insertLang(EN_US_JSON, "en_us", "\"" + EN_NAME + " Generator\"");
        insertLang(ZH_CN_JSON, "zh_cn", "\"" + ZH_NAME + "机\"");

        System.out.println();
        System.out.println("══════════════════════════════════════════");
        System.out.println("  全部完成！请执行 ./gradlew runClient 验证");
        System.out.println("══════════════════════════════════════════");
    }

    // ==================== JSON 文件生成 ====================

    private static void generateJsonFiles() throws IOException {
        System.out.println("[1/5] 生成 JSON 资源文件...");

        // blockstate
        writeJson(BLOCKSTATES_DIR.resolve(FULL_BLOCK_ID + ".json"),
                "{\n" +
                        "  \"variants\": {\n" +
                        "    \"\": {\n" +
                        "      \"model\": \"autoresource:block/" + FULL_BLOCK_ID + "\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}");

        // block model
        writeJson(MODELS_BLOCK_DIR.resolve(FULL_BLOCK_ID + ".json"),
                "{\n" +
                        "  \"parent\": \"minecraft:block/cube_all\",\n" +
                        "  \"textures\": {\n" +
                        "    \"all\": \"autoresource:block/" + FULL_BLOCK_ID + "\"\n" +
                        "  }\n" +
                        "}");

        // item model
        writeJson(MODELS_ITEM_DIR.resolve(FULL_BLOCK_ID + ".json"),
                "{\n" +
                        "  \"parent\": \"autoresource:block/" + FULL_BLOCK_ID + "\"\n" +
                        "}");

        // loot table
        writeJson(LOOT_TABLES_DIR.resolve(FULL_BLOCK_ID + ".json"),
                "{\n" +
                        "  \"type\": \"minecraft:block\",\n" +
                        "  \"pools\": [\n" +
                        "    {\n" +
                        "      \"bonus_rolls\": 0.0,\n" +
                        "      \"entries\": [\n" +
                        "        {\n" +
                        "          \"type\": \"minecraft:item\",\n" +
                        "          \"functions\": [\n" +
                        "            {\n" +
                        "              \"function\": \"minecraft:copy_name\",\n" +
                        "              \"source\": \"block_entity\"\n" +
                        "            },\n" +
                        "            {\n" +
                        "              \"function\": \"minecraft:copy_nbt\",\n" +
                        "              \"ops\": [\n" +
                        "                {\n" +
                        "                  \"op\": \"replace\",\n" +
                        "                  \"source\": \"energy\",\n" +
                        "                  \"target\": \"BlockEntityTag.energy\"\n" +
                        "                },\n" +
                        "                {\n" +
                        "                  \"op\": \"replace\",\n" +
                        "                  \"source\": \"liquid\",\n" +
                        "                  \"target\": \"BlockEntityTag.liquid\"\n" +
                        "                },\n" +
                        "                {\n" +
                        "                  \"op\": \"replace\",\n" +
                        "                  \"source\": \"block\",\n" +
                        "                  \"target\": \"BlockEntityTag.block\"\n" +
                        "                },\n" +
                        "                {\n" +
                        "                  \"op\": \"replace\",\n" +
                        "                  \"source\": \"output\",\n" +
                        "                  \"target\": \"BlockEntityTag.output\"\n" +
                        "                },\n" +
                        "                {\n" +
                        "                  \"op\": \"replace\",\n" +
                        "                  \"source\": \"tickCount\",\n" +
                        "                  \"target\": \"BlockEntityTag.tickCount\"\n" +
                        "                }\n" +
                        "              ],\n" +
                        "              \"source\": \"block_entity\"\n" +
                        "            }\n" +
                        "          ],\n" +
                        "          \"name\": \"autoresource:" + FULL_BLOCK_ID + "\"\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"name\": \"" + FULL_BLOCK_ID.replace("_", "") + "\",\n" +
                        "      \"rolls\": 1.0\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}");

        // recipe（合成配方）
        writeJson(RECIPES_DIR.resolve(FULL_BLOCK_ID + ".json"),
                "{\n" +
                        "  \"type\": \"minecraft:crafting_shaped\",\n" +
                        "  \"group\": \"" + FULL_BLOCK_ID + "\",\n" +
                        "  \"key\": {\n" +
                        "    \"#\": {\n" +
                        "      \"item\": \"minecraft:smooth_stone\"\n" +
                        "    },\n" +
                        "    \"W\": {\n" +
                        "      \"item\": \"minecraft:water_bucket\"\n" +
                        "    },\n" +
                        "    \"L\": {\n" +
                        "      \"item\": \"minecraft:lava_bucket\"\n" +
                        "    },\n" +
                        "    \"S\": {\n" +
                        "      \"item\": \"" + SOURCE_ITEM + "\"\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"pattern\": [\n" +
                        "    \"#L#\",\n" +
                        "    \"#S#\",\n" +
                        "    \"#W#\"\n" +
                        "  ],\n" +
                        "  \"result\": {\n" +
                        "    \"item\": \"autoresource:" + FULL_BLOCK_ID + "\"\n" +
                        "  }\n" +
                        "}");

        System.out.println("  已生成: blockstate, block model, item model, loot_table, recipe");
    }

    // ==================== 纹理合成 ====================

    private static void generateTexture() throws IOException {
        System.out.println("[2/5] 合成纹理...");

        Path sourcePath = Paths.get(SOURCE_TEXTURE_PATH);
        if (!Files.exists(sourcePath)) {
            System.out.println("  ⚠ 警告：源纹理文件不存在: " + SOURCE_TEXTURE_PATH);
            System.out.println("  请手动复制纹理文件到: " + TEXTURES_DIR.resolve(FULL_BLOCK_ID + ".png"));
            return;
        }

        // 读取模板图和源图
        BufferedImage template = ImageIO.read(TEMPLATE_TEXTURE.toFile());
        BufferedImage source = ImageIO.read(sourcePath.toFile());

        if (template.getWidth() != 16 || template.getHeight() != 16) {
            throw new IOException("模板纹理尺寸不是 16x16！");
        }
        if (source.getWidth() != 16 || source.getHeight() != 16) {
            throw new IOException("源纹理尺寸不是 16x16！实际: " + source.getWidth() + "x" + source.getHeight());
        }

        // 创建结果图并从模板复制全部像素
        BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                result.setRGB(x, y, template.getRGB(x, y));
            }
        }

        // 替换中间 8×8 区域为源图对应像素（坐标：x∈[4,11], y∈[4,11]）
        for (int y = 4; y < 12; y++) {
            for (int x = 4; x < 12; x++) {
                result.setRGB(x, y, source.getRGB(x, y));
            }
        }

        // 保存
        Path outputPath = TEXTURES_DIR.resolve(FULL_BLOCK_ID + ".png");
        ImageIO.write(result, "PNG", outputPath.toFile());
        System.out.println("  已生成纹理: " + outputPath.getFileName());
    }

    // ==================== Config.java 修改 ====================

    private static void insertConfig() throws IOException {
        System.out.println("[3/5] 修改 Config.java...");
        String content = readFile(CONFIG_JAVA);

        // 插入点1：字段声明 — 在 END_STONE_STEP; 之后插入
        String fieldInsert = "\n" +
                "    public static ForgeConfigSpec.LongValue " + UPPER_ID + "_MIN;\n" +
                "    public static ForgeConfigSpec.LongValue " + UPPER_ID + "_MAX;\n" +
                "    public static ForgeConfigSpec.LongValue " + UPPER_ID + "_SECOND;\n" +
                "    public static ForgeConfigSpec.LongValue " + UPPER_ID + "_STEP;\n";

        String fieldMarker = "public static ForgeConfigSpec.LongValue END_STONE_STEP;";
        content = insertAfter(content, fieldMarker, fieldInsert);

        // 插入点2：配置块 — 在 End Stone 的 pop 之后、"Block" 的 pop 之前
        String blockInsert = "\n\n" +
                "        SERVER_BUILDER.push(\"" + EN_NAME + "\");\n" +
                "        " + UPPER_ID + "_MIN = SERVER_BUILDER.comment(\"Control the minimum rate of production.The actual data needs to be divided by 1000.\").defineInRange(\"min\", 50, 1, Long.MAX_VALUE);\n" +
                "        " + UPPER_ID + "_MAX = SERVER_BUILDER.comment(\"Control the maximum rate of production.The actual data needs to be divided by 1000.\").defineInRange(\"max\", Long.MAX_VALUE, 1, Long.MAX_VALUE);\n" +
                "        " + UPPER_ID + "_SECOND = SERVER_BUILDER.comment(\"Control the number of seconds it takes to increase production each time.\").defineInRange(\"second\", 10, 1, Long.MAX_VALUE);\n" +
                "        " + UPPER_ID + "_STEP = SERVER_BUILDER.comment(\"Control the numerical increase in production each time.The actual data needs to be divided by 1000.\").defineInRange(\"step\", 50, 0, Long.MAX_VALUE);\n" +
                "        SERVER_BUILDER.pop();";

        // 定位：找到 End Stone 的 pop，在其后插入
        // Config.java 结构：SERVER_BUILDER.push("End Stone"); ... END_STONE_STEP... SERVER_BUILDER.pop();
        // 我们需要在 End Stone 的 pop() 和 Block 的 pop() 之间插入
        String blockMarker = "SERVER_BUILDER.push(\"End Stone\");";
        int endStoneIdx = content.indexOf(blockMarker);
        if (endStoneIdx == -1) {
            throw new IOException("无法定位 Config.java 中的 End Stone 配置块！");
        }
        // 从 End Stone push 之后找到第一个 pop();
        int afterEndStone = content.indexOf("SERVER_BUILDER.pop();", endStoneIdx);
        if (afterEndStone == -1) {
            throw new IOException("无法定位 End Stone 的 pop()！");
        }
        int popEnd = afterEndStone + "SERVER_BUILDER.pop();".length();
        content = content.substring(0, popEnd) + blockInsert + content.substring(popEnd);

        writeFile(CONFIG_JAVA, content);
        System.out.println("  已插入配置字段和配置块");
    }

    // ==================== DataConfig.java 修改 ====================

    private static void insertDataConfig() throws IOException {
        System.out.println("[4/5] 修改 DataConfig.java...");
        String content = readFile(DATA_CONFIG_JAVA);

        // 在 BLOCK_GENERATOR_END_STONE 条目之后插入新条目
        String marker = "BLOCK_GENERATOR_END_STONE =";
        int idx = content.indexOf(marker);
        if (idx == -1) {
            throw new IOException("无法定位 DataConfig.java 中的 BLOCK_GENERATOR_END_STONE！");
        }

        // 找到这个条目的结尾：匿名类的 };
        // 结构：public static final DataConfig BLOCK_GENERATOR_END_STONE = new DataConfig(...) { ... };
        // 需要找到对应的 };
        int braceLevel = 0;
        boolean inBlock = false;
        int insertIdx = -1;
        for (int i = idx; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                braceLevel++;
                inBlock = true;
            }
            if (c == '}') {
                braceLevel--;
            }
            if (inBlock && c == ';' && braceLevel == 0) {
                insertIdx = i + 1;
                break;
            }
        }
        if (insertIdx == -1) {
            throw new IOException("无法定位 BLOCK_GENERATOR_END_STONE 条目的结尾！");
        }

        String insert = "\n" +
                "    public static final DataConfig " + REG_PREFIX + " = new DataConfig(Config." + UPPER_ID + "_MIN, Config." + UPPER_ID + "_MAX, Config." + UPPER_ID + "_SECOND, Config." + UPPER_ID + "_STEP) {\n" +
                "        @Override\n" +
                "        public BlockEntityType<?> getEntityType() {\n" +
                "            return Registration." + REG_PREFIX + "_ENTITY.get();\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        public Block getBlock() {\n" +
                "            return Blocks." + UPPER_ID + ";\n" +
                "        }\n" +
                "    };\n";

        content = content.substring(0, insertIdx) + insert + content.substring(insertIdx);
        writeFile(DATA_CONFIG_JAVA, content);
        System.out.println("  已插入 DataConfig 条目");
    }

    // ==================== Registration.java 修改 ====================

    private static void insertRegistration() throws IOException {
        System.out.println("[5/5] 修改 Registration.java...");
        String content = readFile(REGISTRATION_JAVA);

        // 插入 Block 注册（在 BLOCK_GENERATOR_END_STONE 行之后）
        String blockInsert = "    public static final RegistryObject<Block> " + REG_PREFIX + " = BLOCKS.register(\"" + FULL_BLOCK_ID + "\", () -> new BlockGeneratorBlock(BLOCK_PROPERTIES, DataConfig." + REG_PREFIX + "));\n";

        String blockMarker = "BLOCK_GENERATOR_END_STONE =";
        content = insertAfter(content, blockMarker, blockInsert);

        // 插入 Item 注册（在 BLOCK_GENERATOR_END_STONE_ITEM 行之后）
        String itemInsert = "\n" +
                "    public static final RegistryObject<Item> " + REG_PREFIX + "_ITEM = ITEMS.register(\"" + FULL_BLOCK_ID + "\", () -> new BlockGeneratorItem(" + REG_PREFIX + ".get(), DataConfig." + REG_PREFIX + "));\n";

        String itemMarker = "BLOCK_GENERATOR_END_STONE_ITEM =";
        content = insertAfter(content, itemMarker, itemInsert);

        // 插入 Entity 注册（在 BLOCK_GENERATOR_END_STONE_ENTITY 行之后）
        String entityInsert = "\n" +
                "    public static final RegistryObject<BlockEntityType<BlockGeneratorEntity>> " + REG_PREFIX + "_ENTITY = BLOCK_ENTITIES.register(\"" + FULL_BLOCK_ID + "\", () -> BlockEntityType.Builder.of((pos, state) -> new BlockGeneratorEntity(pos, state, DataConfig." + REG_PREFIX + "), " + REG_PREFIX + ".get()).build(null));\n";

        String entityMarker = "BLOCK_GENERATOR_END_STONE_ENTITY =";
        content = insertAfter(content, entityMarker, entityInsert);

        writeFile(REGISTRATION_JAVA, content);
        System.out.println("  已插入 Block / Item / Entity 注册");
    }

    // ==================== ItemManager.java 修改 ====================

    private static void insertItemManager() throws IOException {
        System.out.println("[6/5] 修改 ItemManager.java...");
        String content = readFile(ITEM_MANAGER_JAVA);

        String marker = "Registration.BLOCK_GENERATOR_END_STONE_ITEM.get()";
        String insert = "                output.accept(Registration." + REG_PREFIX + "_ITEM.get());\n";

        content = insertAfter(content, marker, insert);
        writeFile(ITEM_MANAGER_JAVA, content);
        System.out.println("  已插入创造模式标签条目");
    }

    // ==================== 语言文件修改 ====================

    private static void insertLang(Path langFile, String langName, String displayValue) throws IOException {
        System.out.println("  修改语言文件: " + langFile.getFileName());
        String content = readFile(langFile);

        String marker = "\"block.autoresource.block_generator_end_stone\"";
        String insert = "  \"block.autoresource." + FULL_BLOCK_ID + "\": " + displayValue + ",\n";

        content = insertAfter(content, marker, insert);
        writeFile(langFile, content);
        System.out.println("    [" + langName + "] 已插入翻译条目");
    }

    // ==================== 工具方法 ====================

    /**
     * 读取文件全部内容（UTF-8）
     */
    private static String readFile(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /**
     * 写入文件（UTF-8）
     */
    private static void writeFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 写入 JSON 文件，自动处理目录创建和存在性检查
     */
    private static void writeJson(Path path, String content) throws IOException {
        if (Files.exists(path)) {
            System.out.println("  ⚠ 跳过已存在文件: " + path.getFileName());
            return;
        }
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 在指定标记所在行之后插入内容。找到 marker 后，在该行末尾（含换行符）之后插入。
     */
    private static String insertAfter(String content, String marker, String insert) {
        int idx = content.indexOf(marker);
        if (idx == -1) {
            throw new RuntimeException("无法定位标记: " + marker);
        }
        // 找到 marker 所在行的结尾
        int lineEnd = content.indexOf('\n', idx);
        if (lineEnd == -1) lineEnd = content.length() - 1;
        return content.substring(0, lineEnd + 1) + insert + content.substring(lineEnd + 1);
    }

    /**
     * 右侧填充空格
     */
    private static String padRight(String s, int len) {
        if (s.length() >= len) return s;
        return s + " ".repeat(len - s.length());
    }
}
