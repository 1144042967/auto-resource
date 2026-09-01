# AutoResource — Minecraft Fabric 26.3 Mod

## 项目概述

一个添加自动资源生成机器的 Minecraft Fabric 模组（由同项目 Forge-1.20.1 分支迁移而来；**以已测试完成的 26.2 分支为权威实现基线**）。支持自动生成 FE（电力）、水、岩浆以及多种可配置方块。机器产量随时间增长，可通过放入特殊物品加速（FE 发电机）。

- **Mod ID**: `autoresource`
- **Group**: `cn.sd.jrz`
- **Minecraft 版本**: Loom 构件 `26.3-snapshot-10`，**游戏内版本字符串为 `26.3-alpha.10`**（fabric.mod.json 的 depends 必须写后者，否则 Fabric Loader 判定版本不匹配拒绝加载）
- **Loader**: Fabric Loader `0.19.3` + fabric-api `0.158.2+26.3`
- **Loom**: `net.fabricmc.fabric-loom:1.17-SNAPSHOT`（Gradle wrapper 9.5.1，JDK 25）
- **许可证**: `GNU LGPL v3`

> **与 26.2（权威基线）的 API 差异**：
> 1. **`Registry.get(Identifier)` 恢复返回 `Optional<T>`**：26.3 中 `BuiltInRegistries.ITEM.getOptional(loc)` 直接得 `Optional<Item>`；26.2 的 `get(loc).map(Holder::value)` 写法在 26.3 需改回 `getOptional(loc)`（`DataConfig` 两处）。
> 2. **`LivingEntity#drop` 增加 `Prediction` 参数**：`player.drop(stack, throwAround, Prediction.SERVER_ONLY)`（`Tool.dropItem`）。
> 3. **`PushReaction.DESTROY` 改名 `POPPED`**：语义不变（`Registration.blockProperties`）。
> 4. **`PoseStack#mulPose` 不再接受 `Quaternionf`**：须用 `new Matrix4f().rotateY(...)` 包装（`BlockGeneratorRenderer`）。
> 5. **GUI 渲染 API 变化**：`GuiGraphics` 更名 `GuiGraphicsExtractor`；`text()` 取消 shadow 布尔参数（5 参）；`GLFW` 类移除改为 `InputConstants`；`Item.getName(ItemStack.EMPTY)` 仍返回空（须用 `getDefaultInstance().getHoverName()`）。
> 6. **数据包目录为单数**：`data/autoresource/recipe/` 与 `data/autoresource/loot_table/`（`Registries` 注册表键为单数 `recipe`/`loot_table`）。recipe 用 1.21.5+ 新格式（`key` 字符串 ingredient、`result.id`）；loot 表只保留 `copy_name`（状态经 Java `getDrops` 写 `block_entity_data` 组件）。
> 7. **26.3 无水车马达**：机械动力（Create-Fly）无对应 26.3 版本，本分支已整体移除 compat/create 与 create 相关构建/资源/语言键，build.gradle 不含 create 检测。

## 构建和开发

```bash
# 运行客户端
./gradlew runClient

# 运行服务端（冒烟测试）
./gradlew runServer

# 构建 mod jar
./gradlew build
```

- **本分支无 Create 联动**：无需 libs/ 目录；build.gradle 无 create compileOnly / sourceSets 排除逻辑。
- 首次 runServer 前先写 `run/eula.txt`（内容 `eula=true`），否则服务端会因未接受 EULA 退出。

## 项目架构

```
src/main/java/cn/sd/jrz/autoresource/          # 通用 source set
├── AutoResource.java              # 入口 (ModInitializer.onInitialize)
├── Config.java                    # JSON 配置 + 客户端同步快照（Data.encode/decode）
├── DataConfig.java                # 生成器数据配置（LongSupplier 挂接 Config 快照；getOptional 解包）
├── blocks/                        # 方块类（AbstractGeneratorBlock tick 分发，4 个实现）
├── blockentity/                   # BlockEntity 类
│   ├── AbstractGeneratorEntity.java # 基类（output/tickCount/六面开关/outputEnabled/markDirtyTick 节流）
│   │                                # 实现 ExtendedMenuProvider：openMenu 时附带坐标
│   ├── EnergyGeneratorEntity.java   # 发电机实体（无线充电分片扫描/装备栏背包充电/上方容器充电）
│   ├── LiquidGeneratorEntity.java   # 流体生成器实体（droplets↔mB 换算、桶特判、下方放流体）
│   │                                # fillContainersAbove 用 vanilla Container 直读写（ContainerStorage 对 Chest 不稳定）
│   └── BlockGeneratorEntity.java    # 方块生成器实体（标记槽 sendBlockUpdated 同步、下方放方块）
├── items/                         # 物品类 ×3（tooltip 读 block_entity_data 组件；用 getDefaultInstance().getHoverName() 取名）
├── capability/                    # 对外暴露的 Transfer API 连接（由 setup/TransferSetup 挂接 SIDED 查找）
│   ├── EnergyConnection.java       # 只出不进（teamreborn EnergyStorage；事务中止回补快照）
│   ├── LiquidConnection.java       # 只出不进的单资源 Storage<FluidVariant>
│   ├── BlockConnection.java        # 只出不进的标记方块输出
│   └── DualSlotPipeView.java       # 流体机物品管道视图（插入仅进输入槽、抽取仅来自输出槽）
├── storage/
│   ├── MachineSlotStorage.java     # Container 接口 + simulate 布尔 API（+saveTo/loadFrom 流式持久化）
│   └── MachineSlot.java            # GUI 槽位包装
├── menu/                          # 容器 ×4（DataSlot long 高低位拆分、clickMenuButton 按钮 id 与 Forge 版一致）
├── client/                        # （物理位置在 src/client/java，见下）
├── compat/energy/                 # teamreborn energy 动态前置（EnergyCompat.isEnergyLoaded 判定）
├── network/
│   └── ConfigSync.java             # 登录时下发配置快照 + sendToAll 运行期广播
├── setup/
│   ├── Registration.java           # Registry.register 直注册四类对象（FE 前置 null 安全）
│   ├── ItemManager.java            # FabricCreativeModeTab（图标用方块生成机，FE 前置 null 安全）
│   └── TransferSetup.java          # Energy/Fluid/Item Storage.SIDED.registerForBlockEntity 集中挂接
├── util/
│   ├── Tool.java                   # 数值裁剪等工具方法（drop 带 Prediction）
│   ├── ItemEnergyIo.java           # 物品能量充电（teamreborn EnergyStorage.ITEM + ContainerItemContext 回写）
│   └── ItemFluidIo.java            # 物品流体灌装（FluidStorage.ITEM；81 droplets/mB；饱和乘法）
src/client/java/cn/sd/jrz/autoresource/client/   # 客户端 source set（splitEnvironmentSourceSets）
├── AutoResourceClient.java         # ClientModInitializer：MenuScreens×3（FE 前置 null 检查） + 渲染器 + 配置接收器
├── AbstractGeneratorScreen.java    # GUI 基类（extractContents/extractLabels/extractRenderState + 按钮体系 + 悬浮提示）
├── EnergyGeneratorScreen.java      # FE发电机 GUI
├── LiquidGeneratorScreen.java      # 流体生成器 GUI（进度条配色随流体）
├── BlockGeneratorScreen.java       # 方块生成器 GUI（输出槽点击提取：单击/Shift/空格）
├── BlockGeneratorRenderer.java     # 标记物品四面渲染（getParticleMaterial().sprite() + cutout 平面矩形）
└── BlockGeneratorRenderState.java
```

## 注册体系

`Registration.java` 使用 Fabric 的 `Registry.register(BuiltInRegistries.X, id, obj)` 直接静态字段注册：

- 注册的机器：`energy_generator_fe`、`liquid_generator_water`、`liquid_generator_lava`、`block_generator`
- 菜单类型为 `ExtendedMenuType`，打开时附带机器 `BlockPos`（`BlockPos.STREAM_CODEC`）
- 方块属性保持 Forge 版一致：蓝色、活塞推动销毁（`PushReaction.POPPED`）、硬度 0.5/抗性 3、光照 7
- **FE 发电机四件套（@Nullable）**：由 `registerEnergyGenerator()` 在 teamreborn energy 加载时填充；未加载时 `ENERGY_GENERATOR_FE_*` 均为 null，ItemManager/AutoResourceClient 均判空，禁止直接引用

## 功能要点（与 26.2 对齐）

### 1. 发电机（energy_generator_fe）

- 最大发电量 `Long.MAX_VALUE` FE/t；初始 1/t，每秒 +step；加速槽内放配置物品后每步增量为当前发电量 1%
- 充电顺序：充电槽物品 → 上方玩家/生物 → 上方容器 → 六面 push（轮询 findIndex）→ 无线扫描分片输电
- 六面开关、输出总开关、无线参数逐台 NBT 持久化；`scanCursor`/`wirelessTargets` 仅存内存
- 区块加载守卫：`level.getChunkSource().hasChunk(cx, cz)`
- 无第三方能量反射绕过，限速接收方靠 transferRepeat 循环逐步灌满

### 2. 流体生成器（liquid_generator_water/lava）

- 内部单位 mB；对外 FluidStorage droplets（×81）；产量显示除以 1000 折算桶
- 输入槽接受空桶（特判）或 `FluidStorage.ITEM` 可容纳流体容器；空桶消耗 1000 mB 且要求液体≥1000 才转移输出
- 上方容器充液走 vanilla Container 直接读写（**勿用 `ContainerStorage.of`**：对 Chest 槽位封装不稳定）；**可堆叠空 cell 特殊处理**（findEmptySlot 取 1 个单独灌装，避免整堆灌装丢 cell）
- "下方生成流体"每 5 ticks 放置一次（1000 mB/次）；有待填充桶时暂停六面输出（isBucketPending）

### 3. 方块生成器（block_generator）

- 标记槽锁定后决定输出种类；`onContentsChanged` 触发 sendBlockUpdated 刷新客户端渲染
- 输出展示槽为 vanilla SimpleContainer(1) ghost slot；提取经 clickMenuButton（1 个/一组/背包满）
- 内部 存量=方块×1000；六面 push 用 ItemStorage.SIDED（insert 自动多槽合并）

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

所有 Entity 通过 `saveAdditional(ValueOutput)` / `loadAdditional(ValueInput)` 流式持久化（26.x 沿用流式 API），NBT 键位与 Forge 版完全兼容：

- EnergyGeneratorEntity: `output/energy/tickCount/nextIncrease`(+旧 beaconIncrease)、无线参数、六面开关、`starSlot/chargeSlot`
- LiquidGeneratorEntity: `output/liquid/tickCount`、六面、`outputEnabled`、`placeFluidBelow`、`inputSlot/outputSlot`
- BlockGeneratorEntity: `output/block/tickCount`、六面、`outputEnabled`、`placeBlockBelow`、`markerSlot`

基础类型用 `putLong/getLongOr` 等；子结构用 `child()/childOrEmpty()`；槽位容器（MachineSlotStorage）提供 `saveTo(ValueOutput)/loadFrom(ValueInput)`。读取时 `getXOr(key, 当前值)` 缺字段保持当前值。

物品 tooltip 从 `DataComponents.BLOCK_ENTITY_DATA`（`minecraft:block_entity_data`，为 `TypedEntityData`）读取状态。

**掉落状态保留**：数据包目录为**单数** `recipe/` 与 `loot_table/blocks/`。loot 表只负责掉落方块自身与自定义名称（`copy_name`）；机器状态经 `AbstractGeneratorBlock.getDrops` 覆写，用 `BlockEntity.saveWithFullMetadata` 序列化后写入掉落物品的 `block_entity_data` 组件，放置时经 `loadCustomOnly` 恢复。`removeDroppedSlots` 钩子从 block_entity_data 移除会被 `getDrops` 单独掉落的槽位（流体机输入/输出槽、能量机充电槽），避免"掉落+重放"重复；方块机标记槽、能量机加速槽随物品保留。

## 已知 26.3 注意事项（相对 26.2）

- **fabric.mod.json 的 minecraft 依赖写 `~26.3-alpha.10`**（游戏内版本字符串），不是 `~26.3-snapshot-10`（Loom 构件名）。写错则 Fabric Loader 报"Incompatible mods found"。
- **数据包目录单数**：`recipe/`、`loot_table/`。勿改回复数 `recipes/`/`loot_tables/`（26.3 `Registries` 键为单数，复数为旧版目录）。
- **recipe JSON 用新格式**：`key` 的 ingredient 直接写字符串 `"minecraft:smooth_stone"`；`result` 用 `{ "id": "autoresource:xxx", "count": 1 }`。旧格式（`{"item": ...}` / `result.item`）会解析失败。
- **GUI 面板纹理**：必须用 10 参 `guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0f, 0.0f, w, h, w, h)`。9 参 `blit(Identifier, ...)` 是 UV 语义，传 int 会被自动加宽到 float 版本导致参数错位（面板画不出来）。
- **GUI 文字颜色必须带 alpha**：`text()` 对 `ARGB.alpha==0` 直接跳过绘制，`TEXT_COLOR` 须为 `0xFF404040`。
- **`extractContents` 必须先调 `super.extractContents()`**：父类在其中 pushMatrix+translate(leftPos,topPos) 渲染按钮/槽位/标签并 popMatrix 还原；不调 super 会丢失槽位与按钮渲染。
- **`Item.getName(ItemStack.EMPTY)` 返回空组件**：取名/取 hover name 一律用 `getDefaultInstance().getHoverName()`（`BlockGeneratorItem`、`EnergyGeneratorScreen`）。
- **`ContainerStorage.of` 对 Chest 不稳定**：流体机上方容器充液用 vanilla Container 直读写 + `findEmptySlot`/`fillCellInContainer`（26.2 验证过的方案）。

## 事务安全说明（Fabric 特有）

- 对外 Connection 类的 extract 均 `txn.addCloseCallback(result.wasAborted() → 回补)` 保证回滚
- 液体扣减一律 `insertedDroplets / 81` 向下取整（<1 mB 零头归耗损），防止内部精度膨胀
- ItemFluidIo.safeMul 做 droplets 换算的饱和乘法防溢出；ItemEnergyIo 插入前 `Math.min(maxAmount, Long.MAX_VALUE)` 防溢出

## 命名规范

- 注册名：`<type>_generator_<material>`（26.3 无水车马达）
- 语言键：`block.autoresource.<name>`、`item.autoresource.<name>`、`screen.autoresource.<name>`
- 语言文件共 48 种（en_us/zh_cn 最全；其余语言后续同步）

## 依赖

- **Fabric Loader** ≥0.19.3（唯一硬加载器依赖）
- **fabric-api** *（transfer/screen/itemgroup/networking/rendering 各子模块按需使用）
- 可选：teamreborn energy 5.0.0（发电机前置；build.gradle 用 implementation 提供开发期依赖，运行时缺失则 FE 发电机不注册）

## 已验证清单（26.3 适配完成）

1. ✅ 26.2 → 26.3 全部 API 差异适配（Registry.getOptional / Prediction / PushReaction.POPPED / mulPose Matrix4f / GuiGraphicsExtractor / text 5 参 / InputConstants）
2. ✅ 数据包目录迁移为单数 recipe/、loot_table/，recipe 改用新格式（字符串 ingredient + result.id），loot 表去掉旧 copy_nbt
3. ✅ 资源补齐：items/ 物品模型 ×4、lang/ ×48、models/ ×8、textures/block ×12、textures/gui ×3（全部来自 26.2，无水车马达）
4. ✅ GUI 渲染修复：面板纹理 10 参 RenderPipelines blit、extractContents 调 super、TEXT_COLOR alpha FF、悬浮提示恢复
5. ✅ 动态前置判空恢复：ItemManager 图标用方块生成机 + FE null 检查、AutoResourceClient FE 菜单 null 检查
6. ✅ 流体机上方容器充液恢复 26.2 直接 Container 方案（findEmptySlot/fillCellInContainer，可堆叠 cell 特殊处理）
7. ✅ 移除 create 联动：build.gradle 无 create 逻辑、fabric.mod.json 无 suggests、compat/create 与 resources 无残留
8. ✅ `gradlew build` BUILD SUCCESSFUL；jar 含水车马达外全部类与资源
9. ✅ `runServer` 冒烟测试：44 mods 加载（autoresource 1.1.1）、`config/autoresource.json` 生成、服务端 `Done (1.538s)` 无异常
