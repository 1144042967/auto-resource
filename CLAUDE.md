# AutoResource — Minecraft Fabric 26.2 Mod

## 项目概述

一个添加自动资源生成机器的 Minecraft Fabric 模组（由同项目 Forge-1.20.1 分支迁移而来；**以已测试完成的 26.1.2 分支为权威实现基线**）。支持自动生成 FE（电力）、水、岩浆以及多种可配置方块。机器产量随时间增长，可通过放入特殊物品加速（FE 发电机）。

- **Mod ID**: `autoresource`
- **Group**: `cn.sd.jrz`
- **Minecraft 版本**: `26.2`
- **Loader**: Fabric Loader `0.19.3` + fabric-api `0.158.0+26.2`
- **Loom**: `net.fabricmc.fabric-loom:1.17-SNAPSHOT`（Gradle wrapper 9.5.1）
- **Java 版本**: `25`（26.x 系列以官方 deobfuscated 名称发布，不再需要显式 mapping）
- **许可证**: `GNU LGPL v3`

> **与 26.1.2（权威基线）的 API 差异**：
> 1. **`Registry.get(Identifier)` 返回 `Optional<Reference<T>>`**：26.2 中 `BuiltInRegistries.ITEM.get(loc)` 需 `.map(Holder::value)` 解包（`DataConfig` 两处已适配）；26.1.2 的 `getOptional(loc)` 直接返回 `Optional<T>`。
> 2. **`AbstractContainerMenu.getNeighborStack(Direction)` 不存在**：26.1.2/26.2 中均为自定义方法，`WaterWheelMotorMenu` 内**不能加 `@Override`**（26.2 早期尝试加 @Override 编译失败：方法不会覆盖或实现超类型的方法）。
> 3. 其余 API 与 26.1.2 完全一致（extractor 渲染流、menu.v1.ExtendedMenuType、ContainerStorage、ChunkPos、PayloadTypeRegistry、FabricCreativeModeTab、useWithoutItem/useItemOn、teamreborn energy 5.0.0、Create-Fly 6.0.9），26.1.2 全部代码可直接复用。

## 构建和开发

```bash
# 运行客户端
./gradlew runClient

# 运行服务端
./gradlew runServer

# 构建 mod jar
./gradlew build
```

### Create 联动编译条件

build.gradle 检测 `libs/create*.jar`：
- 存在 → 以 `compileOnly` 参与编译，compat/create 全部代码生效（含水车马达四件套）
- 不存在 → 从两个 source set 中排除 compat 相关类（CreateCompat 常驻，其余排除），保证无 jar 也能完整构建本体功能

jar 为 Create-Fly 26.2（`create-fly-26.2-rc-2-6.0.9-1.jar`，ZurrTum 移植版，已内置动能 API，无需单独 porting-lib）。`libs/*.jar` 已被 .gitignore 忽略。
（`scripts/merge_porting_lib.sh` 为旧 porting-lib 合并脚本，当前构建已不需要，保留作历史参考。）

## 项目架构

```
src/main/java/cn/sd/jrz/autoresource/          # 通用 source set
├── AutoResource.java              # 入口 (ModInitializer.onInitialize)
├── Config.java                    # JSON 配置 + 客户端同步快照（Data.encode/decode）
├── DataConfig.java                # 生成器数据配置（LongSupplier 挂接 Config 快照）
├── blocks/                        # 方块类（AbstractGeneratorBlock tick 分发，4 个实现）
├── blockentity/                   # BlockEntity 类
│   ├── AbstractGeneratorEntity.java # 基类（output/tickCount/六面开关/outputEnabled/markDirtyTick 节流）
│   │                                # 实现 ExtendedMenuProvider：openMenu 时附带坐标
│   ├── EnergyGeneratorEntity.java   # 发电机实体（无线充电分片扫描/装备栏背包充电/上方容器充电）
│   ├── LiquidGeneratorEntity.java   # 流体生成器实体（droplets↔mB 换算、桶特判、下方放流体）
│   └── BlockGeneratorEntity.java    # 方块生成器实体（标记槽 sendBlockUpdated 同步、下方放方块）
├── items/                         # 物品类 ×3（tooltip 读 block_entity_data 组件）
├── capability/                    # 对外暴露的 Transfer API 连接（由 setup/TransferSetup 挂接 SIDED 查找）
│   ├── EnergyConnection.java       # 只出不进（teamreborn EnergyStorage；事务中止回补快照）
│   ├── LiquidConnection.java       # 只出不进的单资源 Storage<FluidVariant>
│   ├── BlockConnection.java        # 只出不进的标记方块输出
│   └── DualSlotPipeView.java       # 流体机物品管道视图（插入仅进输入槽、抽取仅来自输出槽）
├── storage/
│   ├── MachineSlotStorage.java     # Container 接口 + simulate 布尔 API（+serializeNBT/deserializeNBT）
│   └── MachineSlot.java            # GUI 槽位包装
├── menu/                          # 容器 ×4（DataSlot long 高低位拆分、clickMenuButton 按钮 id 与 Forge 版一致）
├── client/                        # （物理位置在 src/client/java，见下）
├── compat/create/                 # 机械动力联动（仅当 Create 加载时经反射注册；需 libs/ jar 参与编译）
│   ├── CreateCompat.java           # isCreateLoaded 判断 + 水车物品懒加载缓存（自身零 Create 引用）
│   ├── CreateRegistration.java     # 反射入口 register()：注册 water_wheel_motor 四件套
│   └── WaterWheelMotor{Block,Entity,Item,Menu}.java
├── compat/energy/                 # teamreborn energy 动态前置（EnergyCompat.isEnergyLoaded 判定）
├── network/
│   └── ConfigSync.java             # 登录时下发配置快照 + sendToAll 运行期广播
├── setup/
│   ├── Registration.java           # Registry.register 直注册四类对象 + Create 反射触发点
│   ├── ItemManager.java            # FabricCreativeModeTab 创造标签页（FE 前置 null 安全）
│   └── TransferSetup.java          # Energy/Fluid/Item Storage.SIDED.registerForBlockEntity 集中挂接
├── util/
│   ├── Tool.java                   # 数值裁剪等工具方法（原样迁移）
│   ├── ItemEnergyIo.java           # 物品能量充电（teamreborn EnergyStorage.ITEM + ContainerItemContext 回写）
│   └── ItemFluidIo.java            # 物品流体灌装（FluidStorage.ITEM；81 droplets/mB；饱和乘法）
src/client/java/cn/sd/jrz/autoresource/client/   # 客户端 source set（splitEnvironmentSourceSets）
├── AutoResourceClient.java         # ClientModInitializer：MenuScreens×3（FE 前置 null 检查） + 渲染器 + 配置接收器
├── AbstractGeneratorScreen.java    # GUI 基类（extractContents/extractLabels/extractRenderState + 按钮体系）
├── EnergyGeneratorScreen.java      # FE发电机 GUI
├── LiquidGeneratorScreen.java      # 流体生成器 GUI（进度条配色随流体）
├── BlockGeneratorScreen.java       # 方块生成器 GUI（输出槽点击提取：单击/Shift/空格）
├── BlockGeneratorRenderer.java     # 标记物品四面渲染（getParticleMaterial().sprite() + cutout 平面矩形）
└── compat/create/
    ├── CreateRegistrationClient.java # 反射入口 registerScreens/registerRenderers
    ├── WaterWheelMotorScreen.java    # 水车马达 GUI（方向按钮+转速显示；自包含，不继承机器基类）
    ├── WaterWheelMotorRenderer.java  # 四面转速文字渲染（Font + Quaternionf 旋转）
    └── WaterWheelMotorRenderState.java
```

## 注册体系

`Registration.java` 使用 Fabric 的 `Registry.register(BuiltInRegistries.X, id, obj)` 直接静态字段注册：

- 注册的机器：`energy_generator_fe`、`liquid_generator_water`、`liquid_generator_lava`、`block_generator`
- 菜单类型为 `ExtendedMenuType`，打开时附带机器 `BlockPos`（`BlockPos.STREAM_CODEC`）
- 方块属性保持 Forge 版一致：蓝色、活塞推动销毁、硬度 0.5/抗性 3、光照 7
- Create 联动字段（WATER_WHEEL_MOTOR 四个 @Nullable 字段）由 `CreateRegistration.register()` 反射填充
- FE 发电机四件套（@Nullable）由 `registerEnergyGenerator()` 在 teamreborn energy 加载时填充

## 功能要点（与 26.1.2 对齐）

### 1. 发电机（energy_generator_fe）

- 最大发电量 `Long.MAX_VALUE` FE/t；初始 1/t，每秒 +step；加速槽内放配置物品后每步增量为当前发电量 1%
- 充电顺序：充电槽物品 → 上方玩家/生物 → 上方容器 → 六面 push（轮询 findIndex）→ 无线扫描分片输电
- 六面开关、输出总开关、无线参数逐台 NBT 持久化；`scanCursor`/`wirelessTargets` 仅存内存
- 区块加载守卫：`level.getChunkSource().hasChunk(cx, cz)`
- Fabric 版无第三方能量反射绕过，限速接收方靠 transferRepeat 循环逐步灌满

### 2. 流体生成器（liquid_generator_water/lava）

- 内部单位 mB；对外 FluidStorage droplets（×81）；产量显示除以 1000 折算桶
- 输入槽接受空桶（特判）或 `FluidStorage.ITEM` 可容纳流体容器；空桶消耗 1000 mB 且要求液体≥1000 才转移输出
- 上方容器充液走 vanilla Container 直接读写；空桶特判同槽"取空桶放流体桶"；**可堆叠空 cell 特殊处理**（findEmptySlot 取 1 个单独灌装，避免整堆灌装丢 cell）
- "下方生成流体"每 5 ticks 放置一次（1000 mB/次）；有待填充桶时暂停六面输出（isBucketPending）

### 3. 方块生成器（block_generator）

- 标记槽锁定后决定输出种类；`onContentsChanged` 触发 sendBlockUpdated 刷新客户端渲染
- 输出展示槽为 vanilla SimpleContainer(1) ghost slot；提取经 clickMenuButton（1 个/一组/背包满）
- 内部 存量=方块×1000；六面 push 用 ItemStorage.SIDED（insert 自动多槽合并）

### 4. 水车马达（water_wheel_motor，可选）

- `DirectionalKineticBlock + IBE` / `GeneratingKineticBlockEntity`，转速=水车数×(1|4)，容量=数量×(256|512) SU
- `handleWheelContentsChanged()` 三重兜底保证加水车后动力网络正确重建（updateGeneratedRotation + 显式 updateStressFor/updateCapacityFor + detach/setNetwork(null)/setSpeed(0)/再传播）
- Menu 构造时 `(MenuType<WaterWheelMotorMenu>) Registration.WATER_WHEEL_MOTOR_MENU` 强转读取
- 槽位 setChanged 补发 handleWheelContentsChanged（moveItemStackTo 合并场景不触发 onContentsChanged）
- 纹理坐标约定：水车槽 8,57；玩家槽基准 y=97（GUI png 已烘焙槽框）
- `WaterWheelMotorScreen` 自包含（继承 AbstractContainerScreen）：`WaterWheelMotorMenu` 非 `AbstractGeneratorMenu` 子类，不满足机器基类泛型约束

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
- 服务端加载 `Config.load()`；**每个玩家登录时经 ConfigSync 全量下发**（含 `sendToAll` 运行期广播），客户端 `Config.applyRemote` 原子替换快照

## 数据持久化

所有 Entity 通过 `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)` 流式持久化（26.2 保持 26.1.2 的流式 API），NBT 键位与 Forge 版完全兼容：

- EnergyGeneratorEntity: `output/energy/tickCount/nextIncrease`(+旧 beaconIncrease)、无线参数、六面开关、`starSlot/chargeSlot`
- LiquidGeneratorEntity: `output/liquid/tickCount`、六面、`outputEnabled`、`placeFluidBelow`、`inputSlot/outputSlot`
- BlockGeneratorEntity: `output/block/tickCount`、六面、`outputEnabled`、`placeBlockBelow`、`markerSlot`
- WaterWheelMotorEntity: `counterClockwise`、`wheelSlots`（SmartBlockEntity 的 write/read(ValueOutput/ValueInput)）

基础类型用 `putLong/getLongOr` 等；子结构用 `child()/childOrEmpty()`；槽位容器（MachineSlotStorage）提供 `saveTo(ValueOutput)/loadFrom(ValueInput)`。读取时 `getXOr(key, 当前值)` 缺字段保持当前值。

物品 tooltip 从 `DataComponents.BLOCK_ENTITY_DATA`（`minecraft:block_entity_data`，为 `TypedEntityData`）读取状态。

**掉落状态保留**：数据包目录为**单数** `recipe/` 与 `loot_table/blocks/`。loot 表只负责掉落方块自身与自定义名称（`copy_name`）；机器状态经 `AbstractGeneratorBlock.getDrops` 覆写，用 `BlockEntity.saveWithFullMetadata` 序列化后写入掉落物品的 `block_entity_data` 组件（组件类型为 `TypedEntityData`，type 由组件自身承载），放置时经 `loadCustomOnly` 恢复。`removeDroppedSlots` 钩子从 block_entity_data 移除会被 `getDrops` 单独掉落的槽位（流体机输入/输出槽、能量机充电槽），避免"掉落+重放"重复；方块机标记槽、能量机加速槽随物品保留。

## 已知 26.2 注意事项（相对 26.1.2）

- **`Registry.get(Identifier)` 返回 `Optional<Reference<T>>`**：`BuiltInRegistries.ITEM.get(loc).map(net.minecraft.core.Holder::value)` 解包（`DataConfig.getStarItem` / `isBlockGeneratorItem` 两处）。26.1.2 用 `getOptional(loc)` 直接得 `Optional<T>`，两版本写法不通用。
- **`getNeighborStack` 非覆写**：`AbstractContainerMenu` 无该方法，`WaterWheelMotorMenu.getNeighborStack(Direction)` 是自定义方法，**不要加 `@Override`**。
- 其余注意事项全部沿用 26.1.2（fabric-api 菜单扩展包 menu.v1、extractor 渲染流、按钮体系、AbstractContainerScreen 尺寸 final、ContainerStorage/PlayerInventoryStorage、ChunkPos 无 BlockPos 构造器、RegistryFriendlyByteBuf + PayloadTypeRegistry.serverboundPlay/clientboundPlay、useWithoutItem/useItemOn、数据槽 16 位拆分、数据包目录单数、Block/Item 构造 setId、Identifier、Level.isClientSide() 方法、FabricBlockEntityTypeBuilder、区块加载守卫、teamreborn energy 5.0.0 动态前置、BlockEntityRenderer 三阶段）。

## 事务安全说明（Fabric 特有）

- 对外 Connection 类的 extract 均 `txn.addCloseCallback(result.wasAborted() → 回补)` 保证回滚
- 液体扣减一律 `insertedDroplets / 81` 向下取整（<1 mB 零头归耗损），防止内部精度膨胀
- ItemFluidIo.safeMul 做 droplets 换算的饱和乘法防溢出；ItemEnergyIo 插入前 `Math.min(maxAmount, Long.MAX_VALUE)` 防溢出

## 命名规范

- 注册名：`<type>_generator_<material>` / `water_wheel_motor`
- 语言键：`block.autoresource.<name>`、`item.autoresource.<name>`、`screen.autoresource.<name>`
- 语言文件共 48 种（en_us/zh_cn 最全，含水车马达全部键；其余语言后续同步）

## 依赖

- **Fabric Loader** ≥0.19.3（唯一硬加载器依赖）
- **fabric-api** *（transfer/screen/itemgroup/networking/rendering 各子模块按需使用）
- 可选：teamreborn energy 5.0.0（发电机前置；build.gradle 用 implementation 提供开发期依赖）
- 可选：create-fly 26.2（Create-Fly 6.0.9；联动，运行时缺 create 时水车马达不注册）

## 已验证清单（26.2 首个构建批次）

1. ✅ `ExtendedMenuType` 工厂签名与 `ExtendedMenuProvider`（menu.v1 包，复用 26.1.2）
2. ✅ teamreborn energy 5.0.0 + `EnergyStorage.ITEM.find` + `ContainerItemContext`（含 ItemEnergyIo 防溢出）
3. ✅ `ContainerStorage.of`/`PlayerInventoryStorage.of` 取代 InventoryStorage
4. ✅ `BlockApiLookup` / `Storage.SIDED.registerForBlockEntity` 挂接
5. ✅ 配方 `fabric:load_conditions`（all_mods_loaded → create）被 resource-conditions 识别
6. ✅ `MachineSlot` 中 vanilla `Slot.container` 字段名与方法可见性
7. ✅ Screen extractor 渲染流（extractContents/extractLabels/extractRenderState）实际编译与按钮体系
8. ✅ `DataConfig` 的 `Registry.get(loc).map(Holder::value)` 解包编译通过（26.2 特有 API）
9. ✅ `WaterWheelMotorMenu.getNeighborStack` 移除错误 @Override 后编译通过
10. ✅ 有 create jar 与无 create jar 两条构建路径均 BUILD SUCCESSFUL；jar 含水车马达全部类与 items/ 资源
11. ✅ `runServer` 冒烟测试：mod 正常加载、`config/autoresource.json` 生成、服务端 `Done` 无异常
