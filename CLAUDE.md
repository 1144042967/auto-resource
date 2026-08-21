# AutoResource — Minecraft Forge 1.20.1 Mod

## 项目概述

一个添加自动资源生成机器的 Minecraft Forge 模组。支持自动生成 FE（电力）、水、岩浆以及多种不同类型的方块（可配置，默认含 21 种基础方块与推荐的建筑/自然方块）。机器产量会随时间逐渐增长，且可通过放入特殊物品加速（FE 发电机）。

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

方块生成机是**单个通用机器** `block_generator`，通过 GUI 标记槽放入任意合法产品（可生成产品由配置文件 `block_generator.items` 决定，支持物品 ID 与 `#` 标签）决定输出方块种类，不再按方块分多个特化机器。

## 项目架构

```
src/main/java/cn/sd/jrz/autoresource/
├── AutoResource.java              # 主 mod 类 (@Mod, Forge)
├── Config.java                    # 配置文件 (ForgeConfigSpec)
├── DataConfig.java                # 生成器数据配置（含 getStarItem 配置缓存）
├── blocks/                         # 方块类
│   ├── AbstractGeneratorBlock.java # 机器方块基类（config 持有、tick 分发）
│   ├── EnergyGeneratorBlock.java   # 发电机方块
│   ├── LiquidGeneratorBlock.java   # 流体生成器方块
│   └── BlockGeneratorBlock.java    # 方块生成器方块
├── blockentity/                    # BlockEntity 类
│   ├── AbstractGeneratorEntity.java# 机器实体基类（output/tickCount/六面开关/面NBT/markDirtyTick 节流）
│   ├── EnergyGeneratorEntity.java  # 发电机实体
│   ├── LiquidGeneratorEntity.java  # 流体生成器实体
│   └── BlockGeneratorEntity.java   # 方块生成器实体
├── items/                          # 物品类
│   ├── EnergyGeneratorItem.java    # 发电机物品
│   ├── LiquidGeneratorItem.java    # 流体生成器物品
│   └── BlockGeneratorItem.java     # 方块生成器物品（tooltip 支持物品集合缓存）
├── capability/                     # Forge Capability 实现
│   ├── EnergyConnection.java       # 能量 IEnergyStorage
│   ├── LiquidConnection.java       # 流体 IFluidHandler
│   └── BlockConnection.java        # 物品 IItemHandler
├── menu/                           # 容器
│   ├── AbstractGeneratorMenu.java  # 机器容器基类（实体泛型、DataSlot 工具、玩家背包布局、stillValid）
│   ├── EnergyGeneratorMenu.java    # FE发电机容器（数据槽同步 + 按钮交互）
│   ├── LiquidGeneratorMenu.java    # 流体生成器容器（输入/输出槽 + 六面开关按钮）
│   └── BlockGeneratorMenu.java     # 方块生成器容器（标记槽 + 输出展示槽 + 提取按钮）
├── client/                         # 客户端
│   ├── ClientSetup.java            # 客户端初始化（注册 GUI）
│   ├── AbstractGeneratorScreen.java# 机器 GUI 基类（sendButton/growthPercent/开关按钮/渲染循环）
│   ├── EnergyGeneratorScreen.java  # FE发电机 GUI
│   ├── LiquidGeneratorScreen.java  # 流体生成器 GUI
│   ├── BlockGeneratorScreen.java   # 方块生成器 GUI（输出槽点击提取）
│   └── BlockGeneratorRenderer.java # 方块生成器方块实体渲染（四侧面显示标记物品）
├── compat/create/                  # 机械动力（Create）联动（仅当 Create 加载时注册，未加载则整个包不被引用）
│   ├── CreateCompat.java           # isCreateLoaded 判断 + 水车/大水车物品懒加载缓存
│   ├── WaterWheelMotorBlock.java   # 水车马达方块（DirectionalKineticBlock + IBE，单向动力源）
│   ├── WaterWheelMotorEntity.java  # 水车马达实体（GeneratingKineticBlockEntity，转速/方向/应力容量）
│   ├── WaterWheelMotorItem.java    # 水车马达物品（水主题色名称 + tooltip 讲解）
│   ├── WaterWheelMotorMenu.java    # 水车马达容器（32 水车槽 + 转速/方向/六面按钮）
│   ├── WaterWheelMotorScreen.java  # 水车马达 GUI（客户端）
│   └── WaterWheelMotorRenderer.java# 水车马达方块实体渲染（四面侧显示转速文字）
├── compat/energybypass/            # 第三方MOD能量反射绕过（零编译期依赖）
│   └── EnergyBypass.java           # 反射补满第三方MOD机器能量到容量（龙之研究反射按类缓存）
├── setup/                          # 注册
│   ├── Registration.java           # 所有方块/物品/实体/菜单的注册（含 Create 联动条件注册）
│   └── ItemManager.java            # 创造标签注册
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
- `block_generator` — 通用可标记方块生成机（标记槽决定输出配置的多种产品之一）

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

**反射能量绕过**（`bypass_enabled` 配置，默认开启）:
- 相邻输电与无线输电统一走 `outputTo()`：先走标准 `ForgeCapabilities.ENERGY` 注入；若目标机器因**容量/接收速率限制**拒收（如第三方 MOD 机器已满），且配置开启，则通过反射把目标内部能量**直接补满到容量**，使其满电运行（不超额）。
- 纯反射实现（`compat/energybypass/EnergyBypass.java`），字符串类名定位，**编译期零依赖**；未安装的 MOD 记录 `initFailed` 永久跳过，避免每 tick 抛异常。
- 支持：Mekanism（`TileEntityMekanism` 接口 `getMaxEnergy/setEnergy`，FloatingLong 可超 int）、Thermal Expansion（`EnergyStorageCoFH.energy/capacity` int）、Industrial Foregoing（Titanium `EnergyStorageComponent.energy` int）、Draconic Evolution（public `OPStorage`/`OPStorageOP` 字段）、Flux Networks（`TransferHandler.mBuffer` + 解除速率限制）。
- Modern Industrialization 1.20.1 仅 Fabric，与 Forge 不共存，未支持。

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

单个通用可标记生成方块机 `block_generator`，通过标记槽支持配置的多种方块的自动生成（输出种类由标记槽决定）。

- **最大产量**: `Long.MAX_VALUE / 1000` Block/t
- **初始产量**: 0.05 Block/t (即 1 Block/s)，每 10 秒增加 0.05 Block/t
- 内部存储单位为 Block/1000

**GUI 交互**:
- 右击打开 GUI（`BlockGeneratorMenu` / `BlockGeneratorScreen`，与流体机同款布局），展示存量、产量、下次增长量、增长百分比（含进度条），并提供六个传输面开关和"下方生成方块"开关
- 展示值与进度增长机制与发电机/流体机一致；大数值用 K/M/G/T/P/E 单位缩写
- **标记槽**（槽位 0）：放入任意合法方块生成机产品（合法性由配置文件 `block_generator.items` 决定，支持物品 ID 与 `#` 标签，默认含 21 种基础方块及推荐的建筑/自然方块）后锁定（菜单槽 `mayPickup` 返回 false，不可取出/更换），决定机器输出的方块种类；自动生成一直计算，未标记时无法取出/传输/放置；破坏时标记槽内容随物品 NBT 保留（不掉落），物品 tooltip 显示标记内容（兼容为空）
- **侧面显示**：`BlockGeneratorRenderer`（BlockEntityRenderer，仿 StorageDrawers）在四个侧面（北/南/东/西，上下除外）用 `ItemRenderer` 把标记物品拍扁后各渲染一次，指示机器输出的方块种类
- **输出展示槽**（槽位 1）：显示标记槽的物品（无实际库存），不支持插入；点击提取通过客户端拦截 + `clickMenuButton` 实现——单击提取 1 个、Shift+单击提取一组（标记物品堆叠上限）、空格+单击提取到背包满（提取逻辑在菜单 `extractBlocks`，背包放不下部分退回存量）
- **下方生成方块**：输出槽下方有"下方生成方块"开关按钮（替代原红石激活判断，逐台保存到 NBT，默认关闭）；开启后每 5 ticks 尝试向机器下方空气方块放置标记的方块，每次消耗 1000 单位

**方块传输**:
- 标记后，通过六个面输出标记方块到相邻方块的 `IItemHandler` 管道（轮询索引负载均衡），可在 GUI 中逐面独立开关
- 使用 `ItemHandlerHelper.insertItemStacked` 插入

**管道输出（BlockConnection）**:
- 未标记时 `getStackInSlot`/`extractItem` 返回空；标记后按标记物品输出存量方块
- `insertItem` 返回原物品（禁止输入）

### 4. 水车马达（Create 联动，可选）

一个机械动力（Create）动力源，**仅当 Create 加载时注册**，无 Create 时该方块/物品完全不存在（`compat/create/` 包整体不被引用，类加载安全）。

- **依赖**：`libs/create-1.20.1-0.5.1.j.jar`（`compileOnly files(...)`，见 build.gradle；可从 Modrinth 下载）。mods.toml 声明 `create` 软依赖（`mandatory=false`, `after`）。
- **双版本兼容（0.5.1.j 与 6.0.8 通用）**：`compat/create` 的代码**只使用两版共享的 API**——Create 6.0.8（1.20.1）保留了 `com.simibubi.create.foundation.*` 包，经 javap 逐项核验 `DirectionalKineticBlock/IBE/GeneratingKineticBlockEntity/KineticBlockEntity 动力方法/SmartBlockEntity NBT 钩子/ScrollValueBehaviour/KineticScrollValueBehaviour/ValueBoxTransform$Sided` 两版签名一致。**同一 jar 可同时运行在 Create 0.5.1.j 与 6.0.8（Flywheel 0.6/1.0）环境**。规避点：转速框标签用 vanilla `Component.translatable`（避开 `Lang`→`CreateLang`）；值框定位直接写 `new Vec3(...)`（避开 `VecHelper`）。
- **条件注册（关键：类加载安全）**：Create 相关注册全部集中在 `compat/create/CreateRegistration`（自持 4 个 DeferredRegister），由 `Registration.init()` / `ClientSetup` 在 `CreateCompat.isCreateLoaded()` 为真时**经 `CreateCompat.invokeRegistration()` 反射按类名加载**。⚠️ 不能在无条件加载类的字节码里直接引用 Create/Flywheel 类（即使写在 `if` 分支内也不行）——JVM 在链接这些类时会急切解析常量池里的类引用，无 Create 时直接 `NoClassDefFoundError`。本模块已用 `javap -v` 核验：Registration/ItemManager/ClientSetup/AutoResource/Config/DataConfig/CreateCompat 常量池 Create/Flywheel/Catnip/Ponder 引用均为 0。
- **动力**：方块 `extends DirectionalKineticBlock implements IBE`（FACING=输出面，`hasShaftTowards` 仅输出面）；实体 `extends GeneratingKineticBlockEntity`。
  - **转速由水车数量决定**：`currentSpeed()` = 小水车数量 ×1 RPM、大水车数量 ×4 RPM（槽内只能放一种）；`getGeneratedSpeed()` 返回 `convertToDirection(±speed, FACING)`，**未放入水车时返回 0（马达不转）**。旋转方向可切换（`counterClockwise`），**转速不可手动调节**。
  - **应力容量动态**：重写 `calculateAddedStressCapacity()`，按**单个水车槽**内堆叠数量累加（水车 256 SU/个、大水车 512 SU/个），放入/取出即 `updateGeneratedRotation()` 通知网络重算；`onContentsChanged` 还会**兜底显式**调用 `network.updateCapacityFor/updateStressFor/updateStress` 推送最新容量与应力（Create 的 `updateGeneratedRotation` 内部受 `hasNetwork() && 转速≠0` 守卫，网络重挂后可能跳过导致容量不更新）。此外把公开字段 `updateSpeed` 置 `true`，强制下一 tick 走 `attachKinetics()` 以最新转速重新传播（容器事务上下文中 `updateGeneratedRotation` 的 detach/attach 传播可能被 Create 跳过——表现为"加水车转速不更新、但切换旋转方向/输出面却生效"）。
- **GUI**：上部大框正中放六面输出方向按钮；中部左侧水车槽位（右侧提示"水车槽位"，同排右侧为旋转方向开关，无标签）；下部物品栏行：左侧物品栏名称、右侧当前转速（3 位补零，如 `004 RPM`）。**无转速调节按钮**；不挂 `ScrollValueBehaviour`（无方块表面转速显示与滚动调节）。面板与槽位背景烘焙在 `textures/gui/water_wheel_motor_gui.png`（用户 PS 修改；槽位坐标需与 `WaterWheelMotorMenu` 一致：水车槽 7,56，玩家槽 `7+col*18`，y=96/114/132/150）。
- **命中形状**：整方块 `Shapes.block()`，与整方块模型一致。
- **渲染**：整方块模型（`parent: minecraft:block/cube`，无镂空，方块不设 `.noOcclusion()`）；`WaterWheelMotorRenderer`（BlockEntityRenderer）在**垂直于应力输出方向的四个面**中央用 1.20.1 的 `Font.drawInBatch`（世界文字统一走该方法）渲染当前转速文字（`%03d RPM`，`screen.autoresource.water_wheel_motor.block_speed` 键），白色带阴影、强制 15 级方块光照，叠放在 LCD 显示窗上；**文字上方始终指向应力输出方向**。变换只用标准 `PoseStack.translate/mulPose/scale` + `Quaternionf`（与 BlockGeneratorRenderer 一致，不手写 Matrix4f——JOML 1.10.5 字段包私有）：`faceRotation` 把目标面转到 +Z（N/S/E/W 用 Y 轴 `SIDE_ROT_Y`，上下两面用 X 轴），`textAngle` 把输出方向转到面局部坐标求绕 Z 的旋转角，使文字顶部朝向输出方向；输出面与其对面（水车轮/底座贴图）不显示文字。转速由实体 `currentSpeed()` 读取（经 `SmartBlockEntity` 同步包下发到客户端）。渲染器由 `ClientSetup.onRegisterRenderers` 经 `CreateCompat.invokeRegistration("registerRenderers", ...)` 反射注册，避免无条件类字节码引用 Create 依赖类。不使用 Flywheel/实例化/`ScrollValueBehaviour`（双版本 API 不兼容）。
- **持久化**：重写 `write(tag, clientPacket)`/`read(tag, clientPacket)`（Create `SmartBlockEntity` 的钩子，`load` 为 final 不可覆写）保存 `speed`/`counterClockwise`/`wheelSlots`。
- **模型/朝向**：整方块模型（`parent: minecraft:block/cube`），六面贴图由 `tools/make_water_wheel_motor_textures.py`（Python/PIL）生成并写入 `textures/block/water_wheel_motor/`：`side` 灰色机器面板 + 中央 LCD 显示窗（上下水蓝强调条，风格对齐其他机器，六面侧贴图）、`top` 俯视水车轮（外环 + 8 辐条 + 轮毂 + 青色状态灯，即输出面标记）、`bottom` 深色底座板（中心轴承盘 + 四角铆钉）。`blockstates` 按 FACING 旋转模型：水平朝向（north/south/east/west）用 `x:90` + `y` 使**顶面水车轮贴图朝向输出方向**，朝上/朝下用 `x:0`/`x:180`；`front` 箭头贴图与旧 createaddition 模型/黄铜贴图（`*_old.json`、`brass_*.png`）保留备份未使用。
- **配方**：`data/autoresource/recipes/water_wheel_motor.json`——6 个铁锭（顶行 3 + 底行 3） + 中央活塞 + 左右各一水车（`create:water_wheel`）合成 1 个马达；材料含 Create 物品，仅 Create 加载时可用。
- **物品/tooltip**：`WaterWheelMotorItem`（`BlockItem` 子类）——物品名用水主题色（`ChatFormatting.AQUA`，与水生成机一致）；tooltip 用 `item.autoresource.water_wheel_motor.tooltip.*` 语言键讲解用途（Create 动力源、转速/应力容量由水车数量决定、GUI 可调方向、破坏掉落内部水车）。
- **破坏掉落**：`data/autoresource/loot_tables/blocks/water_wheel_motor.json`（掉落方块自身）；`WaterWheelMotorBlock.getDrops` 覆写把水车槽内放入的水车/大水车一并掉落（不随物品 NBT 保留槽内容，避免重复）。
- 语言键：`block.autoresource.water_wheel_motor`、`screen.autoresource.water_wheel_motor.*`（目前仅 en_us/zh_cn 两个语言文件加入，其余语言后续同步）。

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
- `BLOCK_GENERATOR_ITEMS` — 方块生成机可生成的产品列表（`block_generator.items`），每项支持物品注册 ID（如 `minecraft:dirt`）或物品标签（以 `#` 开头，如 `#minecraft:stone_bricks`）；默认列表按主世界/下界/末地分类、各类内按常见程度排序；物品 tooltip 会展示该列表（最多前 100 种，超过在尾部提示总数量）

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
- 三类机器共享逻辑抽取到基类（`AbstractGeneratorBlock`/`AbstractGeneratorEntity`/`AbstractGeneratorMenu`/`AbstractGeneratorScreen`），机器特有逻辑留在子类；实体每 tick 用 `markDirtyTick()` 节流存档标记
- tick 逻辑内嵌在 Block 类中，通过匿名 lambda 直接实现（FE 发电机的具体逻辑在 `EnergyGeneratorEntity.serverTick()` 中）
- 使用 `findIndex` 轮询索引实现六面均匀输出
- 产量使用 scaled long 存储（*1000 避免浮点运算）
- GUI 使用 vanilla `MenuType` + `Menu` + `AbstractContainerScreen` 实现；数据同步用 `DataSlot`（long 拆高低 32 位）；按钮交互用 `clickMenuButton` + `ServerboundContainerButtonClickPacket`
- 客户端类放在 `client/` 包，通过 `@Mod.EventBusSubscriber(Dist.CLIENT)` 或手动注册到 Forge 事件总线（`MinecraftForge.EVENT_BUS.addListener`）注册，避免服务端加载
