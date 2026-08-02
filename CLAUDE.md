# AutoResource — Minecraft Forge 1.20.1 Mod

## 项目概述

一个添加自动资源生成机器的 Minecraft Forge 模组。支持自动生成 FE（电力）、水、岩浆以及 21 种不同类型的方块。机器产量会随时间逐渐增长，且可通过放入特殊物品加速（FE 发电机）。

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

### 方块生成机

方块生成机是**单个通用机器** `block_generator`，通过 GUI 标记槽放入任意合法产品（见 `DataConfig.BLOCK_GENERATOR_ITEMS`）决定输出方块种类，不再按方块分多个特化机器。

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
├── menu/                           # 容器
│   ├── EnergyGeneratorMenu.java    # FE发电机容器（数据槽同步 + 按钮交互）
│   ├── LiquidGeneratorMenu.java    # 流体生成器容器（输入/输出槽 + 六面开关按钮）
│   └── BlockGeneratorMenu.java     # 方块生成器容器（标记槽 + 输出展示槽 + 提取按钮）
├── client/                         # 客户端
│   ├── ClientSetup.java            # 客户端初始化（注册 GUI）
│   ├── EnergyGeneratorScreen.java  # FE发电机 GUI
│   ├── LiquidGeneratorScreen.java  # 流体生成器 GUI
│   ├── BlockGeneratorScreen.java   # 方块生成器 GUI（输出槽点击提取）
│   └── BlockGeneratorRenderer.java # 方块生成器方块实体渲染（四侧面显示标记物品）
├── setup/                          # 注册
│   └── Registration.java           # 所有方块/物品/实体/菜单的注册
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

**方块机（1种通用）**:
- `block_generator` — 通用可标记方块生成机（标记槽决定输出 21 种产品之一）

## 功能模块

### 1. 发电机 (`EnergyGeneratorBlock` / `EnergyGeneratorEntity`)

自动生成 FE 电力的机器，带有右键 GUI。

- **最大发电量**: `Long.MAX_VALUE` FE/t (9,223,372,036,854,775,807 FE/t)
- **初始发电量**: 1 FE/t，每 1 秒增加 1 FE/t
- 增长量无上限时，可通过**加速槽**达到最大功率

**增长机制**:
- 默认每次增长 `step`
- `nextIncrease` 变量存储**下次增长的发电量**，每 tick 重新计算（GUI 中实时展示）
- **加速槽**：放入配置指定的物品（默认下界之星）后，增长的发电量变为当前发电量的 1%（至少 1，避免低产量时停止增长）；加速物品可通过配置文件 `star_item` 修改
- 达最大发电量时，GUI 增长进度固定为 100%，下次增长显示"已达最大电量"

**能量传输**:
- 优先给充电槽中的物品充电（可放入任意可充电物品）；破坏时充电槽内容掉落（加速槽随物品 NBT 保留）
- 再给站在机器上方的玩家/生物全部槽位中可充电物品充电（覆盖物品栏、存储栏、装备栏，轮询 `ForgeCapabilities.ENERGY`）
- 若机器正上方是容器，容器内可充电物品也会被充电（轮询 `ForgeCapabilities.ITEM_HANDLER`）
- 剩余能量通过六个面均匀输出到相邻方块（轮询索引 `findIndex` 负载均衡）
- **输电面开关**：六个面可分别启用/禁用（GUI 中可逐台修改，默认全启用）

**无线充电**（默认关闭）:
- 通过 GUI 按钮开关（状态持久化到 NBT）
- **分片扫描**：整个扫描区域（1x1/3x3/5x5 区块）按扫描间隔秒数均分为若干片，**每 tick 扫描一片**，并记录支持电量接收的位置（`wirelessTargets`）
- **每 tick 输电**：遍历已记录位置，按重复传电次数循环输入电量；**有线传输优先级更高**
- 只扫描已加载区块，避免强制生成区块（`Level.isLoaded` 守卫）

**逐台独立配置**（GUI 中可修改，NBT 持久化）:
- 无线充电开关、扫描间隔（1-3600 秒）、区块范围（1x1/3x3/5x5）、重复传电次数（1-256）
- **重复传电次数对相邻输电和无线输电都生效**
- 六个输电面开关

**交互**:
- 右击打开 GUI（`EnergyGeneratorMenu` / `EnergyGeneratorScreen`），展示当前发电量、当前电量、下次增长的发电量、增长百分比（含进度条），并提供加速槽、充电槽以及所有逐台配置的按钮；大数值用 K/M/G/T/P/E 单位缩写展示

**数据持久化 (NBT)**:
- `output`(long), `energy`(long), `tickCount`(long), `nextIncrease`(long)（旧存档 `beaconIncrease` 兼容读取）
- `wirelessOn`(boolean), `wirelessInterval`(int), `wirelessRange`(int), `transferRepeat`(int)；`scanCursor`（扫描游标）与 `wirelessTargets` 仅存内存，不持久化
- `transferDown/Up/North/South/West/East`(boolean), `starSlot`(CompoundTag), `chargeSlot`(CompoundTag)
- 加载时通过 `Tool.suit()` 防负数处理

### 2. 流体生成器 (`LiquidGeneratorBlock` / `LiquidGeneratorEntity`)

支持水和岩浆的自动生成。

- **最大产量**: `Long.MAX_VALUE / 1000` B/t
- **初始产量**: 0.05 B/t (即 1 B/s)，每 10 秒增加 0.05 B/t
- 内部存储单位为 mB/1000（即 B）

**流体传输**:
- 通过六个面输出到相邻方块（轮询索引负载均衡），可在 GUI 中逐面独立开关（默认全启用，保存到 NBT）
- 检查目标方块的 `IFluidHandler` 是否接受该流体类型

**GUI 交互**:
- 右击打开 GUI（`LiquidGeneratorMenu` / `LiquidGeneratorScreen`），紧凑布局展示当前流体量、产量、下次增长量、增长百分比（含进度条），并提供六个传输面开关
- 展示值与进度增长机制与发电机一致；大数值用 K/M/G/T/P/E 单位缩写；进度条填充色随对应流体变化（水=蓝、岩浆=橙）
- **输入/输出槽**：输入槽放入空桶或可容纳流体的物品（通过 `FLUID_HANDLER_ITEM` 能力判断），可放一组物品、组的大小由物品自身堆叠上限决定（`getStackLimit` 返回 `stack.getMaxStackSize()`）；机器每 tick 优先填充输入槽物品（铁桶需 1000 mB），填满后转移到输出槽；输出槽只能放 1 个（`getSlotLimit`/`getStackLimit` 均为 1），且 `isItemValid` 返回 false（不可主动放入，只能由机器放入、玩家/管道抽取）；有待填充的铁桶时保留液体、暂不向六面输出；破坏时输入/输出槽内容掉落（方块 `getDrops`）
- **下方生成流体**：输出槽下方有"下方生成流体"开关按钮（替代原红石激活判断，逐台保存到 NBT，默认关闭）；开启后每 5 ticks 尝试向机器下方空气方块放置对应流体，每次消耗 1000 mB
- **物品管道能力**：实体额外暴露 `ITEM_HANDLER`（包装输入/输出槽），插入的物品只能进输入槽，抽取的物品只能来自输出槽（输入槽不可抽取、输出槽不可插入）
- **上方容器充液**：机器正上方容器内的空桶/可容纳流体物品也会被填充（空桶直接转换为流体桶）
- 保留空桶右击直接提取一桶液体（主手或副手，消耗 1000 mB）

### 3. 方块生成器 (`BlockGeneratorBlock` / `BlockGeneratorEntity`)

单个通用可标记生成方块机 `block_generator`，通过标记槽支持 21 种方块的自动生成（输出种类由标记槽决定）。

- **最大产量**: `Long.MAX_VALUE / 1000` Block/t
- **初始产量**: 0.05 Block/t (即 1 Block/s)，每 10 秒增加 0.05 Block/t
- 内部存储单位为 Block/1000

**GUI 交互**:
- 右击打开 GUI（`BlockGeneratorMenu` / `BlockGeneratorScreen`，与流体机同款布局），展示存量、产量、下次增长量、增长百分比（含进度条），并提供六个传输面开关和"下方生成方块"开关
- 展示值与进度增长机制与发电机/流体机一致；大数值用 K/M/G/T/P/E 单位缩写
- **标记槽**（槽位 0）：放入任意合法方块生成机产品（`DataConfig.BLOCK_GENERATOR_ITEMS`，即现有 21 种方块生成机的产品）后锁定（菜单槽 `mayPickup` 返回 false，不可取出/更换），决定机器输出的方块种类；自动生成一直计算，未标记时无法取出/传输/放置；破坏时标记槽内容随物品 NBT 保留（不掉落），物品 tooltip 显示标记内容（兼容为空）
- **侧面显示**：`BlockGeneratorRenderer`（BlockEntityRenderer，仿 StorageDrawers）在四个侧面（北/南/东/西，上下除外）用 `ItemRenderer` 把标记物品拍扁后各渲染一次，指示机器输出的方块种类
- **输出展示槽**（槽位 1）：显示标记槽的物品（无实际库存），不支持插入；点击提取通过客户端拦截 + `clickMenuButton` 实现——单击提取 1 个、Shift+单击提取一组（标记物品堆叠上限）、空格+单击提取到背包满（提取逻辑在菜单 `extractBlocks`，背包放不下部分退回存量）
- **下方生成方块**：输出槽下方有"下方生成方块"开关按钮（替代原红石激活判断，逐台保存到 NBT，默认关闭）；开启后每 5 ticks 尝试向机器下方空气方块放置标记的方块，每次消耗 1000 单位

**方块传输**:
- 标记后，通过六个面输出标记方块到相邻方块的 `IItemHandler` 管道（轮询索引负载均衡），可在 GUI 中逐面独立开关
- 使用 `ItemHandlerHelper.insertItemStacked` 插入

**管道输出（BlockConnection）**:
- 未标记时 `getStackInSlot`/`extractItem` 返回空；标记后按标记物品输出存量方块
- `insertItem` 返回原物品（禁止输入）

## Capability 系统

使用 Forge Capability 实现与其他 Mod 的互操作：

- **EnergyConnection** (`IEnergyStorage`): 
  - 实现 `receiveEnergy` (返回 0，禁止输入) 和 `extractEnergy` (输出当前存储能量)
  - `canReceive` 返回 `false`, `canExtract` 返回 `true`

- **LiquidConnection** (`IFluidHandler`):
  - 实现 `fill` (返回 0，禁止输入) 和 `drain` (输出流体)
  - 使用机器的 `FluidStack` 类型

- **BlockConnection** (`IItemHandler`):
  - 实现 `extractItem` (按标记物品提取存量方块，未标记时返回空)
  - `insertItem` 返回原物品（禁止输入）

## 配置系统

`Config.java` 使用 `ForgeConfigSpec` 实现所有机器的参数配置：

**发电机配置**:
- `FE_MIN/MAX/SECOND/STEP` — 最小/最大产量、增长速度间隔、每次增量
- `FE_STAR_ITEM` — 加速增长所需物品（默认 `minecraft:nether_star`，方便其他作者改为更难的物品）
- 无线充电参数、输电面开关等均为**每台发电机独立**配置，在 GUI 中修改并保存到 NBT，不属于全局配置

**流体机配置** (以 Water 为例):
- `WATER_MIN/MAX/SECOND/STEP` — 最小/最大产量、增长速度间隔、每次增量
- `LAVA_MIN/MAX/SECOND/STEP` — 岩浆机同结构

**方块机配置**:
- `BLOCK_MIN/MAX/SECOND/STEP` — 通用方块生成机的产量参数（输出种类由标记槽决定）

配置类型: `ModConfig.Type.SERVER`（服务端配置，世界间不共享）

配置注册: `context.registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG)`

## Tool 工具方法

- `suit(long)`: 防溢出裁剪，检测 `value + step < value` (溢出) 或 `value < 0` → 返回 `Long.MAX_VALUE`
- `suitInt(long)`: 同上但返回 int，负值返回 `Integer.MAX_VALUE`
- `suit(String)`: 解析字符串为 long，调用 `suit(long)`
- `takeItem(Player, ItemStack)`: 尝试给玩家物品，失败则丢到世界

## 数据持久化

所有 Entity 通过 `saveAdditional`/`load` 持久化数据：

- **EnergyGeneratorEntity**: `output`, `energy`, `tickCount`, `nextIncrease`（旧存档 `beaconIncrease` 兼容）
- **LiquidGeneratorEntity**: `output`, `liquid`, `tickCount`，六面开关 `transferDown/Up/North/South/West/East`，`placeFluidBelow`，`inputSlot`、`outputSlot`
- **BlockGeneratorEntity**: `output`, `block`, `tickCount`，六面开关 `transferDown/Up/North/South/West/East`，`placeBlockBelow`，`markerSlot`

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
- tick 逻辑内嵌在 Block 类中，通过匿名 lambda 直接实现（FE 发电机的具体逻辑在 `EnergyGeneratorEntity.serverTick()` 中）
- 使用 `findIndex` 轮询索引实现六面均匀输出
- 产量使用 scaled long 存储（*1000 避免浮点运算）
- GUI 使用 vanilla `MenuType` + `Menu` + `AbstractContainerScreen` 实现；数据同步用 `DataSlot`（long 拆高低 32 位）；按钮交互用 `clickMenuButton` + `ServerboundContainerButtonClickPacket`
- 客户端类放在 `client/` 包，仅通过 `@Mod.EventBusSubscriber(Dist.CLIENT)` 注册，避免服务端加载
