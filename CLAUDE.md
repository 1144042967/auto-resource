# AutoResource — Minecraft Fabric 1.21.1 Mod

## 项目概述

一个添加自动资源生成机器的 Minecraft Fabric 模组（由同项目 Forge-1.20.1 分支迁移而来）。支持自动生成 FE（电力）、水、岩浆以及多种可配置方块。机器产量随时间增长，可通过放入特殊物品加速（FE 发电机）。

- **Mod ID**: `autoresource`
- **Group**: `cn.sd.jrz`
- **Minecraft 版本**: `1.21.1`
- **Loader**: Fabric Loader `0.16.13` + fabric-api `0.116.15+1.21.1`
- **Loom**: `net.fabricmc.fabric-loom-remap:1.17-SNAPSHOT`（Gradle wrapper 9.5.1）
- **Java 版本**: `21`，Mappings 为 `official` (Mojang)
- **许可证**: `GNU LGPL v3`

> **与 Forge-1.20.1 版本的主要差异**：
> 1. 能量体系改用 Fabric Transfer API / teamreborn EnergyStorage（单位 FE=1:1）；流体对外以 droplets 计量（1 mB = 81 droplets），内部仍为 mB。
> 2. **EnergyBypass 反射绕过模块已移除**——其目标 MOD（Mekanism/Thermal/Industrial Foregoing/Draconic/Flux Networks）在 Fabric 上均无移植；Transfer API 循环 insert 可达同等效果。配置项 `bypass_enabled` 不复存在。
> 3. 配置系统改为 `config/autoresource.json` + 登录时 S2C 同步包（Forge 的 SERVER 配置客户端自动镜像，Fabric 无此机制）。

## 构建和开发

```bash
# 运行客户端
./gradlew runClient

# 运行服务端
./gradlew runServer

# 构建 mod jar
./gradlew build
```

## 项目架构

```
src/main/java/cn/sd/jrz/autoresource/          # 通用 source set
├── AutoResource.java              # 入口 (ModInitializer.onInitialize)
├── Config.java                    # JSON 配置 + 客户端同步快照（Data.encode/decode）
├── DataConfig.java                # 生成器数据配置（LongSupplier 挂接 Config 快照）
├── blocks/                        # 方块类（AbstractGeneratorBlock tick 分发，4 个实现）
├── blockentity/                   # BlockEntity 类
│   ├── AbstractGeneratorEntity.java # 基类（output/tickCount/六面开关/outputEnabled/markDirtyTick 节流）
│   │                                # 实现 ExtendedScreenHandlerFactory：openMenu 时附带坐标
│   ├── EnergyGeneratorEntity.java   # 发电机实体（无线充电分片扫描/装备栏背包充电/上方容器充电）
│   ├── LiquidGeneratorEntity.java   # 流体生成器实体（droplets↔mB 换算、桶特判、下方放流体）
│   └── BlockGeneratorEntity.java    # 方块生成器实体（标记槽 sendBlockUpdated 同步、下方放方块）
├── items/                         # 物品类 ×3（tooltip 读 BlockEntityTag；无需 @Environment）
├── capability/                    # 对外暴露的 Transfer API 连接（由 setup/TransferSetup 挂接 SIDED 查找）
│   ├── EnergyConnection.java       # 只出不进（teamreborn EnergyStorage；事务中止回补快照）
│   ├── LiquidConnection.java       # 只出不进的单资源 Storage<FluidVariant>
│   ├── BlockConnection.java        # 只出不进的标记方块输出
│   └── DualSlotPipeView.java       # 流体机物品管道视图（插入仅进输入槽、抽取仅来自输出槽）
├── storage/
│   ├── MachineSlotStorage.java     # 替代 Forge ItemStackHandler：Container 接口 + simulate 布尔 API
│   └── MachineSlot.java            # 替代 SlotItemHandler 的 GUI 槽位包装
├── menu/                          # 容器 ×4（DataSlot long 高低位拆分、clickMenuButton 按钮 id 与 Forge 版一致）
├── client/                        # （物理位置在 src/client/java，见下）
├── network/
│   └── ConfigSync.java             # 登录时下发配置快照（ServerPlayConnectionEvents.JOIN）
├── setup/
│   ├── Registration.java           # Registry.register 直注册四类对象
│   ├── ItemManager.java            # FabricItemGroup 创造标签页
│   └── TransferSetup.java          # Energy/Fluid/Item Storage.SIDED.registerForBlockEntity 集中挂接
├── util/
│   ├── Tool.java                   # 数值裁剪等工具方法（原样迁移）
│   ├── ItemEnergyIo.java           # 物品能量充电（teamreborn EnergyStorage.ITEM + ContainerItemContext 回写）
│   └── ItemFluidIo.java            # 物品流体灌装（FluidStorage.ITEM；81 droplets/mB；饱和乘法）
src/client/java/cn/sd/jrz/autoresource/client/   # 客户端 source set（splitEnvironmentSourceSets）
├── AutoResourceClient.java         # ClientModInitializer：MenuScreens×3 + EntityRendererRegistry + 配置接收器
├── AbstractGeneratorScreen.java    # GUI 基类（sendButton/growthPercent/StateButton）
├── EnergyGeneratorScreen.java      # FE发电机 GUI
├── LiquidGeneratorScreen.java      # 流体生成器 GUI（进度条配色随流体）
├── BlockGeneratorScreen.java       # 方块生成器 GUI（输出槽点击提取：单击/Shift/空格）
└── BlockGeneratorRenderer.java     # 标记物品四面渲染（getParticleIcon + cutout 平面矩形）
```

## 注册体系

`Registration.java` 使用 Fabric 的 `Registry.register(BuiltInRegistries.X, id, obj)` 直接静态字段注册：

- 注册的机器：`energy_generator_fe`、`liquid_generator_water`、`liquid_generator_lava`、`block_generator`
- 菜单类型为 `ExtendedScreenHandlerType`，打开时附带机器 `BlockPos`（对应 Forge IForgeMenuType + NetworkHooks）
- 方块属性保持 Forge 版一致：蓝色、活塞推动销毁、硬度 0.5/抗性 3、光照 7

## 功能要点（与 Forge 版对齐）

### 1. 发电机（energy_generator_fe）

- 最大发电量 `Long.MAX_VALUE` FE/t；初始 1/t，每秒 +step；加速槽内放配置物品后每步增量为当前发电量 1%
- 充电顺序：充电槽物品 → 上方玩家/生物（Player 经 InventoryStorage 全槽位；生物经 EquipmentSlot standalone 回写）→ 上方容器 → 六面 push（轮询 findIndex）→ 无线扫描分片输电
- 六面开关、输出总开关、无线参数逐台 NBT 持久化；`scanCursor`/`wirelessTargets` 仅存内存
- 区块加载守卫：`level.getChunkSource().hasChunk(cx, cz)`（对应 Forge Level.isLoaded）
- 注意：Fabric 版无第三方能量反射绕过，限速接收方靠 transferRepeat 循环逐步灌满

### 2. 流体生成器（liquid_generator_water/lava）

- 内部单位 mB；对外 FluidStorage droplets（×81）；产量显示除以 1000 折算桶
- 输入槽接受空桶（特判）或 `FluidStorage.ITEM` 可容纳流体容器；空桶消耗 1000 mB 且要求液体≥1000 才转移输出
- 上方容器充液走 InventoryStorage 槽位视图；上方的空桶在同一事务内"取空桶放入流体桶"
- "下方生成流体"每 5 ticks 放置一次（1000 mB/次）；有待填充桶时暂停六面输出（isBucketPending）

### 3. 方块生成器（block_generator）

- 标记槽锁定后决定输出种类；`onContentsChanged` 触发 sendBlockUpdated 刷新客户端渲染
- 输出展示槽为 vanilla SimpleContainer(1) ghost slot；提取经 clickMenuButton（1 个/一组/背包满）
- 内部 存量=方块×1000；六面 push 用 ItemStorage.SIDED（insert 自动多槽合并，语义≈insertItemStacked）

## 配置系统

`config/autoresource.json`（Gson，缺失字段回退默认并写回）：

```jsonc
{
  "energy": { "fe": { "min": 1, "max": ..., "second": 1, "step": 1, "star_item": "minecraft:nether_star" } },
  "liquid": { "water": { min/max/second/step }, "lava": { ... } },
  "block": { "generator": { min/max/second/step, "items": [45 项默认] } }
}
```

- 数值范围矫正与 Forge defineInRange 一致（min/max/second ≥1，step ≥0）
- 服务端加载 `Config.load()`；**每个玩家登录时经 ConfigSync 全量下发**，客户端 `Config.applyRemote` 原子替换快照（菜单 getMax、tooltip 等读快照，与 Forge SERVER 配置镜像行为等价）

## 数据持久化

所有 Entity 通过 `saveAdditional`/`load` 持久化，NBT 键位与 Forge 版完全兼容：

- EnergyGeneratorEntity: `output/energy/tickCount/nextIncrease`(+旧 beaconIncrease)、无线参数、六面开关、`starSlot/chargeSlot`（MachineSlotStorage.serializeNBT，Size+Items[{Slot,id,Count}]）
- LiquidGeneratorEntity: `output/liquid/tickCount`、六面、`outputEnabled`、`placeFluidBelow`、`inputSlot/outputSlot`
- BlockGeneratorEntity: `output/block/tickCount`、六面、`outputEnabled`、`placeBlockBelow`、`markerSlot`

物品 tooltip 从 `DataComponents.BLOCK_ENTITY_DATA`（`minecraft:block_entity_data`）读取状态。

**掉落状态保留（1.21.1）**：数据包目录为**单数** `data/autoresource/recipe/` 与 `data/autoresource/loot_table/blocks/`（1.20.x 用复数）。loot 表只负责掉落方块自身与自定义名称（`copy_name`）；机器状态经 `AbstractGeneratorBlock.getDrops` 覆写，用 `BlockEntity.saveCustomOnly` 序列化后写入掉落物品的 `block_entity_data` 组件，放置时经 `CustomData.loadInto` → `loadCustomOnly`（即 `loadAdditional`）恢复。原因：1.21.1 的 `copy_nbt` 已更名为 `copy_custom_data` 且写入 `custom_data` 组件（非本 mod 读取的 `block_entity_data`），无法直接用于状态保留。

## 事务安全说明（Fabric 特有）

- 对外 Connection 类的 extract 均 `txn.addCloseCallback(result.wasAborted() → 回补)` 保证回滚
- 液体扣减一律 `insertedDroplets / 81` 向下取整（<1 mB 零头归耗损），防止内部精度膨胀
- ItemFluidIo.safeMul 做 droplets 换算的饱和乘法防溢出

## 命名规范

- 注册名：`<type>_generator_<material>`
- 语言键：`block.autoresource.<name>`、`item.autoresource.<name>`、`screen.autoresource.<name>`
- 语言文件共 48 种（en_us/zh_cn 最全；其余语言后续同步）

## 依赖

- **Fabric Loader** ≥0.16.13（唯一硬加载器依赖）
- **fabric-api** *（transfer/screen/itemgroup/networking/rendering 各子模块按需使用）
- 可选：teamreborn energy 4.1.0（发电机前置；未安装时不加载发电机，build.gradle 用 modImplementation 提供开发期依赖）

## 待验证清单（首个构建批次逐项核对）

1. `ExtendedScreenHandlerType` 工厂签名与 `ExtendedScreenHandlerFactory.writeScreenOpeningData` 形态（screenhandler.v1 包）
2. teamreborn `EnergyStorage.ITEM.find(variant, context)` + `ContainerItemContext.forSlot(withInitial)`
3. `FluidStorage.ITEM` 存在性（若缺失：通用容器灌装分支静默降级，桶特判不受影响）
4. `BlockApiLookup.registerForBlockEntity((be, dir)->..., type)` 参数序
5. `MachineSlot` 中 vanilla `Slot.container` 字段名与方法可见性
