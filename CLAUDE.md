# AutoResource — Minecraft Forge 1.20.1 Mod

## 项目概述

一个添加自动资源生成机器的 Minecraft Forge 模组。支持自动生成 FE（电力）、水、岩浆以及 21 种不同类型的方块。机器产量会随时间逐渐增长，且可通过信标加速。

- **Mod ID**: `autoresource`
- **Group**: `cn.sd.jrz`
- **Minecraft 版本**: `1.20.1`
- **Forge 版本**: `47.x`
- **Java 版本**: `17`
- **Mappings**: `official` (Mojang)
- **许可证**: `GNU LGPL v3`

> **与 1.21.1 版本的区别**: 本版本基于 Forge，使用 ForgeConfigSpec 配置系统和 NBT BlockEntityTag 数据持久化。1.21.1 版本基于 NeoForge，使用 DataComponentType 系统。功能层面两个版本相同。详见项目 master 分支中的合并文档。

## 构建和开发

```bash
# 运行客户端
./gradlew runClient

# 运行服务端
./gradlew runServer

# 构建 mod jar
./gradlew build
```

### 添加新方块生成机

使用 `BlockGeneratorAdder` 工具一键生成所有文件：

1. 在 IDEA 中打开 `src/test/java/cn/sd/jrz/autoresource/generator/BlockGeneratorAdder.java`
2. 修改文件顶部 `配置参数` 区域的常量：`BLOCK_ID`、`EN_NAME`、`ZH_NAME`、`SOURCE_ITEM`、`SOURCE_TEXTURE_PATH`
3. 准备好目标方块的 16×16 纹理 PNG 文件
4. 右键 `BlockGeneratorAdder` → Run 'BlockGeneratorAdder.main()'
5. 工具自动完成：生成 5 个 JSON（blockstate、model×2、loot_table、recipe）+ 合成纹理 PNG + 修改 6 个 Java/config 文件（Config.java、DataConfig.java、Registration.java、ItemManager.java、en_us.json、zh_cn.json）
6. 运行 `./gradlew runClient` 验证

## 项目架构

```
src/main/java/cn/sd/jrz/autoresource/
├── AutoResource.java              # 主 mod 类 (@Mod, Forge)
├── Config.java                    # 配置文件 (ForgeConfigSpec)
├── DataConfig.java                # 生成器数据配置
├── blocks/                         # 方块类
│   ├── EnergyGeneratorBlock.java   # 发电机方块
│   ├── LiquidGeneratorBlock.java   # 流体生成器方块
│   └── BlockGeneratorBlock.java    # 方块生成器方块
├── entities/                       # BlockEntity 类
│   ├── EnergyGeneratorEntity.java  # 发电机实体
│   ├── LiquidGeneratorEntity.java  # 流体生成器实体
│   └── BlockGeneratorEntity.java   # 方块生成器实体
├── items/                          # 物品类
│   ├── ItemManager.java            # 物品事件管理
│   ├── EnergyGeneratorItem.java    # 发电机物品
│   ├── LiquidGeneratorItem.java    # 流体生成器物品
│   └── BlockGeneratorItem.java     # 方块生成器物品
├── connection/                     # Forge Capability 实现
│   ├── EnergyConnection.java       # 能量 IEnergyStorage
│   ├── LiquidConnection.java       # 流体 IFluidHandler
│   └── BlockConnection.java        # 物品 IItemHandler
├── setup/                          # 注册
│   └── Registration.java           # 所有方块/物品/实体的注册
└── util/                           # 工具类
    └── Tool.java                   # 数值裁剪等工具方法
```

## 注册体系

`Registration.java` 是中心注册文件，使用 Forge 的 `DeferredRegister` 模式：

- 4 个 `DeferredRegister`: BLOCKS, ITEMS, BLOCK_ENTITIES, CONTAINERS
- 在 `init(FMLJavaModLoadingContext)` 中注册到 `modEventBus`
- `RegistryObject<Block>` / `RegistryObject<Item>` / `RegistryObject<BlockEntityType<?>>`
- 方块属性: 蓝色、活塞推动时销毁、硬度 2.5、抗性 15

### 注册的机器类型

**发电机（1种）**:
- `energy_generator_fe` — FE发电机

**流体机（2种）**:
- `liquid_generator_water` — 水生成器
- `liquid_generator_lava` — 岩浆生成器

**方块机（21种）**:
- 泥土、圆石、石头、平滑石头、粘土、沙子、沙砾
- 花岗岩、闪长岩、安山岩、方解石、凝灰岩、深板岩圆石
- 海晶石、黑曜石、下界岩、灵魂沙、灵魂土、黑石、玄武岩、末地石

## 功能模块

### 1. 发电机 (`EnergyGeneratorBlock` / `EnergyGeneratorEntity`)

自动生成 FE 电力的机器。

- **最大发电量**: `Long.MAX_VALUE` FE/t (9,223,372,036,854,775,807 FE/t)
- **初始发电量**: 1 FE/t，每 1 秒增加 1 FE/t
- 发电量无上限时，需要信标来达到最大功率

**信标加速机制**:
- 红石激活时，检查机器下方的方块
- 若下方为信标 (`Blocks.BEACON`)，每秒额外增加当前发电量的 `beacon_step / 10000` (默认1%)
- `beaconIncrease` 变量存储每次增加量

**能量传输**:
- 优先给站在机器上方的玩家物品栏中可充电物品充电（轮询 `ForgeCapabilities.ENERGY`）
- 剩余能量通过六个面均匀输出到相邻方块（使用轮询索引 `findIndex` 实现负载均衡）

**交互**:
- 右击查看当前状态（存储能量、输出功率、增长百分比、信标加成）

**数据持久化 (NBT)**:
- `output`(long), `energy`(long), `tickCount`(long), `beaconIncrease`(long)
- 加载时通过 `Tool.suit()` 防负数处理

### 2. 流体生成器 (`LiquidGeneratorBlock` / `LiquidGeneratorEntity`)

支持水和岩浆的自动生成。

- **最大产量**: `Long.MAX_VALUE / 1000` B/t
- **初始产量**: 0.05 B/t (即 1 B/s)，每 10 秒增加 0.05 B/t
- 内部存储单位为 mB/1000（即 B）

**流体传输**:
- 通过六个面输出到相邻方块（轮询索引负载均衡）
- 检查目标方块的 `IFluidHandler` 是否接受该流体类型

**红石模式 - 放置流体**:
- 红石激活时，每 5 ticks 尝试向下方的空气方块放置对应流体
- 每次放置消耗 1000 mB

**桶提取**:
- 主手或副手持有空桶右击，消耗 1000 mB 返回对应桶（水桶/岩浆桶）
- 支持堆叠桶的批量提取

**交互**:
- 空桶右击提取液体
- 空手/其他物品右击查看状态

### 3. 方块生成器 (`BlockGeneratorBlock` / `BlockGeneratorEntity`)

支持 21 种方块的自动生成。

- **最大产量**: `Long.MAX_VALUE / 1000` Block/t
- **初始产量**: 0.05 Block/t (即 1 Block/s)，每 10 秒增加 0.05 Block/t
- 内部存储单位为 Block/1000

**方块传输**:
- 通过六个面输出到相邻方块的 `IItemHandler` 管道（轮询索引负载均衡）
- 使用 `ItemHandlerHelper.insertItemStacked` 插入

**红石模式 - 放置方块**:
- 红石激活时，每 5 ticks 尝试向下方的空气方块放置对应方块
- 每次放置消耗 1000 单位

**方块提取**:
- 主手为空或手持同种方块时右击，消耗 1000 单位取出 1 个方块

**交互**:
- 空手/同种方块右击提取
- 其他物品右击查看状态

## Capability 系统

使用 Forge Capability 实现与其他 Mod 的互操作：

- **EnergyConnection** (`IEnergyStorage`): 
  - 实现 `receiveEnergy` (返回 0，禁止输入) 和 `extractEnergy` (输出当前存储能量)
  - `canReceive` 返回 `false`, `canExtract` 返回 `true`

- **LiquidConnection** (`IFluidHandler`):
  - 实现 `fill` (返回 0，禁止输入) 和 `drain` (输出流体)
  - 使用机器的 `FluidStack` 类型

- **BlockConnection** (`IItemHandler`):
  - 实现 `extractItem` (提取方块物品)
  - `insertItem` 返回原物品（禁止输入）

## 配置系统

`Config.java` 使用 `ForgeConfigSpec` 实现所有机器的参数配置：

**发电机配置**:
- `FE_MIN/MAX/SECOND/STEP/BEACON_STEP` — 最小/最大产量、增长速度间隔、每次增量、信标加成比例

**每种流体/方块机配置** (以 Water 为例):
- `WATER_MIN/MAX/SECOND/STEP` — 最小/最大产量、增长速度间隔、每次增量

配置类型: `ModConfig.Type.SERVER`（服务端配置，世界间不共享）

配置注册: `context.registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG)`

## Tool 工具方法

- `suit(long)`: 防溢出裁剪，检测 `value + step < value` (溢出) 或 `value < 0` → 返回 `Long.MAX_VALUE`
- `suitInt(long)`: 同上但返回 int，负值返回 `Integer.MAX_VALUE`
- `suit(String)`: 解析字符串为 long，调用 `suit(long)`
- `takeItem(Player, ItemStack)`: 尝试给玩家物品，失败则丢到世界

## 数据持久化

所有 Entity 通过 `saveAdditional`/`load` 持久化数据：

- **EnergyGeneratorEntity**: `output`, `energy`, `tickCount`, `beaconIncrease`
- **LiquidGeneratorEntity**: `output`, `liquid`, `tickCount`
- **BlockGeneratorEntity**: `output`, `block`, `tickCount`

物品 hover 信息从 NBT `BlockEntityTag` 中读取机器状态并显示。

## 资源文件

- `src/main/resources/assets/autoresource/` — 纹理、语言文件、模型
- `src/generated/resources/` — 数据生成产物 (blockstates, models, loot_tables, recipes)

## 依赖

- **Forge** 1.20.1-47.x (唯一硬依赖)
- 无其他 Mod 依赖

## 命名规范

- 所有注册名格式: `<type>_generator_<material>` (如 `energy_generator_fe`, `block_generator_dirt`)
- 语言键格式: `block.autoresource.<name>`, `item.autoresource.<name>`, `screen.autoresource.<name>`
- 类名驼峰命名，包名全小写
- 所有 `use()` 方法标记 `@SuppressWarnings("deprecation")`

## 代码风格约定

- 所有文件使用 UTF-8 编码
- 使用 `@Nonnull`/`@Nullable` 注解标记参数
- tick 逻辑内嵌在 Block 类中，通过匿名 lambda 直接实现
- 使用 `findIndex` 轮询索引实现六面均匀输出
- 产量使用 scaled long 存储（*1000 避免浮点运算）
