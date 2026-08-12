package cn.sd.jrz.autoresource.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射式能量绕过工具类。
 * <p>
 * 当 FE 发电机向第三方 MOD 机器输送能量时，目标机器会因内部容量上限或接收速率限制而拒收
 * （正常 {@link IEnergyStorage#receiveEnergy} 返回 0 或注入量受限）。本类通过反射直接读取/修改
 * 这些机器内部的能量字段，把其内部能量"补满到容量"，从而绕过容量/接收限制，让机器始终满电运行。
 * <p>
 * 所有目标 MOD 均通过字符串类名 + 反射调用定位，编译期不依赖任何第三方 MOD 代码；
 * 若对应 MOD 未安装或类结构发生变化，相关反射会静默失败并返回 0，不影响原有功能。
 * 各 MOD 的反射只在首次命中时解析一次并缓存；未安装的 MOD 会记录"初始化失败"并永久跳过，
 * 避免每 tick 反复抛出反射异常影响性能。
 * <p>
 * 支持：Mekanism（通用机械）、Thermal Expansion（热力膨胀，CoFHCore）、
 * Industrial Foregoing（工业先锋，Titanium）、Draconic Evolution（龙之研究）、
 * Flux Networks（能量网络）。Modern Industrialization（现代化工业）1.20.1 仅提供 Fabric 版，
 * 与本 Forge 版不共存，故不在此处理（后续移植 NeoForge 版本时再补充）。
 */
public final class EnergyBypass {

    // 单位换算：Mekanism 默认 1 FE = 2.5 J（GeneralConfig.forgeConversionRate），即 1 J = 0.4 FE
    private static final double FE_PER_J = 0.4;
    // Flux 网络缓存无容量上限，这里把"补满"定义为补到 100 亿 FE（足够网络长期消耗供给）
    private static final long FLUX_FULL = 10_000_000_000L;

    // ---- Mekanism 反射缓存 ----
    private static boolean mekInitFailed;
    private static Class<?> mekTile;
    private static Method mekGetEnergy;
    private static Method mekGetMaxEnergy;
    private static Method mekSetEnergy;
    private static Method mekGetValue;
    private static Method mekSmaller;
    private static Method mekCreate;

    // ---- Flux Networks 反射缓存 ----
    private static boolean fluxInitFailed;
    private static Class<?> fluxTile;
    private static Method fluxGetTransferHandler;
    private static Field fluxBuffer;
    private static Field fluxDisableLimit;

    // ---- CoFH(Thermal) 反射缓存 ----
    private static boolean cofhInitFailed;
    private static Class<?> cofhStorage;
    private static Class<?> cofhWrapper;
    private static Field cofhEnergy;
    private static Field cofhCapacity;
    private static Field cofhWrapped;

    // ---- Titanium(Industrial Foregoing) 反射缓存 ----
    private static Field titaniumEnergy;

    // ---- Draconic Evolution 反射缓存（按目标类缓存字段/方法，避免每 tick 重复解析）----
    private static final Map<Class<?>, DraconicInfo> DRACONIC_CACHE = new ConcurrentHashMap<>();

    /**
     * 龙之研究 OP 存储的反射句柄缓存；字段为 null 表示该类型不可解析
     */
    private record DraconicInfo(Field field, Method getStored, Method getMax, Method modify) {
        static final DraconicInfo NONE = new DraconicInfo(null, null, null, null);
    }

    private EnergyBypass() {
    }

    /**
     * 尝试把目标方块实体的内部能量"补满到容量"，绕过其容量/接收限制。
     * 按已知 MOD 依次尝试，命中后返回并停止。
     *
     * @param target    目标方块实体
     * @param side      注入面（可为 null）
     * @param cap       目标暴露的 ENERGY 能力实例（可为 null，部分 MOD 不依赖它）
     * @param available 本机当前可用能量（FE），注入量不会超过该值
     * @return 实际消耗的 FE；0 表示无需补满或补满失败
     */
    public static long tryRefill(BlockEntity target, @Nullable Direction side, @Nullable IEnergyStorage cap, long available) {
        if (target == null || target.getLevel() == null || target.getLevel().isClientSide || available <= 0) {
            return 0;
        }
        // 1. Mekanism（机器：TileEntityMekanism 子类，能量 long 型 FloatingLong）
        long consumed = tryRefillMekanism(target, side, available);
        if (consumed > 0) {
            return consumed;
        }
        // 2. Draconic Evolution（机器 public OPStorage 字段 / 能量核心 public OPStorageOP 字段）
        consumed = tryRefillDraconic(target, available);
        if (consumed > 0) {
            return consumed;
        }
        // 3. Flux Networks（Plug/Point/Controller/Storage：TileFluxDevice 子类）
        consumed = tryRefillFlux(target, available);
        if (consumed > 0) {
            return consumed;
        }
        // 4. 通过 ENERGY 能力实例判断 CoFH(Thermal) 与 Titanium(Industrial Foregoing)
        if (cap != null) {
            consumed = tryRefillCapability(cap, available);
            if (consumed > 0) {
                return consumed;
            }
        }
        return 0;
    }

    /**
     * 初始化 Mekanism 反射缓存，只执行一次（成功或失败均记录状态）。返回是否可用。
     */
    private static boolean initMekanism() {
        if (mekTile != null || mekInitFailed) {
            return mekTile != null;
        }
        try {
            mekTile = Class.forName("mekanism.common.tile.base.TileEntityMekanism");
            Class<?> mekFloatingLong = Class.forName("mekanism.api.math.FloatingLong");
            mekGetEnergy = mekTile.getMethod("getEnergy", int.class, Direction.class);
            mekGetMaxEnergy = mekTile.getMethod("getMaxEnergy", int.class, Direction.class);
            mekSetEnergy = mekTile.getMethod("setEnergy", int.class, mekFloatingLong, Direction.class);
            mekGetValue = mekFloatingLong.getMethod("getValue");
            mekSmaller = mekFloatingLong.getMethod("smallerThan", mekFloatingLong);
            mekCreate = mekFloatingLong.getMethod("create", long.class);
            return true;
        } catch (Exception e) {
            mekInitFailed = true;
            return false;
        }
    }

    /**
     * Mekanism：反射调用 {@code TileEntityMekanism} 的接口默认方法
     * {@code getMaxEnergy(int, Direction)}/{@code getEnergy(int, Direction)}/{@code setEnergy(int, FloatingLong, Direction)}，
     * 把内部能量直接补到容量。能量以 FloatingLong（long 无符号整数部分）存储，可容纳超过 int 上限的量。
     * 单位换算：1 FE = 2.5 J，消耗按补满的 J 数换算回 FE。
     */
    private static long tryRefillMekanism(BlockEntity target, Direction side, long available) {
        if (!initMekanism()) {
            return 0;
        }
        try {
            if (!mekTile.isInstance(target)) {
                return 0;
            }
            Object cur = mekGetEnergy.invoke(target, 0, side);
            Object max = mekGetMaxEnergy.invoke(target, 0, side);
            if (cur == null || max == null) {
                return 0;
            }
            // 已满则无需补满
            if (!(boolean) mekSmaller.invoke(cur, max)) {
                return 0;
            }
            long curJ = (long) mekGetValue.invoke(cur);
            long maxJ = (long) mekGetValue.invoke(max);
            long needJ = maxJ - curJ;
            // 可用 FE 换算为 J（1 J = 0.4 FE）
            long availableJ = (long) (available / FE_PER_J);
            long giveJ = Math.min(needJ, Math.max(0, availableJ));
            if (giveJ <= 0) {
                return 0;
            }
            // 补到 curJ + giveJ；若本机能量足以补满，则直接用容量值（无需重新构造）
            Object newEnergy = giveJ >= needJ ? max : mekCreate.invoke(null, curJ + giveJ);
            mekSetEnergy.invoke(target, 0, newEnergy, side);
            long consumed = Math.max(1, (long) (giveJ * FE_PER_J));
            return Math.min(consumed, available);
        } catch (Exception ignored) {
            // 反射失败或目标类型变化：静默跳过
            return 0;
        }
    }

    /**
     * Draconic Evolution：机器（TileGrinder/TileDraconiumChest 等）持有 {@code public OPStorage opStorage} 字段，
     * 能量核心 TileEnergyCore 持有 {@code public OPStorageOP energy} 字段。反射调用
     * {@code getOPStored()}/{@code getMaxOPStored()}/{@code modifyEnergyStored(long)} 补满。
     * OP 与 RF/FE 1:1 换算；容量为 -1（T8 无限核心）或已满时跳过。
     * 字段与方法的解析按目标类缓存（{@link #DRACONIC_CACHE}），避免每 tick 重复反射解析。
     */
    private static long tryRefillDraconic(BlockEntity target, long available) {
        try {
            // 先按类名粗过滤，避免对普通方块实体做字段反射
            if (!target.getClass().getName().contains("draconicevolution")) {
                return 0;
            }
            DraconicInfo info = DRACONIC_CACHE.computeIfAbsent(target.getClass(), EnergyBypass::resolveDraconic);
            if (info.field == null) {
                return 0;
            }
            Object storage = info.field.get(target);
            if (storage == null) {
                return 0;
            }
            long cur = (long) info.getStored.invoke(storage);
            long max = (long) info.getMax.invoke(storage);
            // 容量无效（如 -1 无限）或已满则跳过
            if (max <= 0 || cur >= max) {
                return 0;
            }
            long give = Math.min(max - cur, available);
            if (give <= 0) {
                return 0;
            }
            info.modify.invoke(storage, give);
            return give;
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * 解析一个龙之研究方块实体的 OP 存储字段与方法；类型不含 OP 存储字段时返回 {@link DraconicInfo#NONE}。
     */
    private static DraconicInfo resolveDraconic(Class<?> clazz) {
        try {
            Field field;
            try {
                field = clazz.getField("opStorage");
            } catch (NoSuchFieldException ignored) {
                try {
                    field = clazz.getField("energy");
                } catch (NoSuchFieldException ignored2) {
                    return DraconicInfo.NONE;
                }
            }
            // 仅当字段类型是 OP 存储类时才处理，避免误伤同名非能量字段
            if (!field.getType().getName().contains("OPStorage")) {
                return DraconicInfo.NONE;
            }
            Method getStored = field.getType().getMethod("getOPStored");
            Method getMax = field.getType().getMethod("getMaxOPStored");
            Method modify = field.getType().getMethod("modifyEnergyStored", long.class);
            return new DraconicInfo(field, getStored, getMax, modify);
        } catch (Exception ignored) {
            return DraconicInfo.NONE;
        }
    }

    /**
     * 初始化 Flux Networks 反射缓存，只执行一次。返回是否可用。
     */
    private static boolean initFlux() {
        if (fluxTile != null || fluxInitFailed) {
            return fluxTile != null;
        }
        try {
            fluxTile = Class.forName("sonar.fluxnetworks.common.device.TileFluxDevice");
            fluxGetTransferHandler = fluxTile.getMethod("getTransferHandler");
            Class<?> handler = Class.forName("sonar.fluxnetworks.common.connection.TransferHandler");
            fluxBuffer = handler.getDeclaredField("mBuffer");
            fluxBuffer.setAccessible(true);
            fluxDisableLimit = handler.getDeclaredField("mDisableLimit");
            fluxDisableLimit.setAccessible(true);
            return true;
        } catch (Exception e) {
            fluxInitFailed = true;
            return false;
        }
    }

    /**
     * Flux Networks：网络能量存储在每个设备的 {@code TransferHandler.mBuffer}（long，无容量上限），
     * 每 tick 传输受限 {@code getLimit()}（默认 80 万 FE）。反射解除该限制（mDisableLimit=true）
     * 并把 mBuffer 补到 {@link #FLUX_FULL}，使网络拥有充足能量持续供给连接的机器。
     */
    private static long tryRefillFlux(BlockEntity target, long available) {
        if (!initFlux()) {
            return 0;
        }
        try {
            if (!fluxTile.isInstance(target)) {
                return 0;
            }
            Object transferHandler = fluxGetTransferHandler.invoke(target);
            if (transferHandler == null) {
                return 0;
            }
            // 解除每 tick 传输上限
            fluxDisableLimit.setBoolean(transferHandler, true);
            long cur = fluxBuffer.getLong(transferHandler);
            if (cur >= FLUX_FULL) {
                return 0;
            }
            long give = Math.min(FLUX_FULL - cur, available);
            if (give <= 0) {
                return 0;
            }
            fluxBuffer.setLong(transferHandler, cur + give);
            return give;
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * 初始化 CoFH(Thermal) 反射缓存，只执行一次。返回是否可用。
     */
    private static boolean initCoFH() {
        if ((cofhStorage != null && cofhWrapper != null) || cofhInitFailed) {
            return cofhStorage != null && cofhWrapper != null;
        }
        try {
            cofhWrapper = Class.forName("cofh.lib.common.energy.EnergyHandlerRestrictionWrapper");
            cofhWrapped = cofhWrapper.getDeclaredField("wrappedHandler");
            cofhWrapped.setAccessible(true);
            cofhStorage = Class.forName("cofh.lib.common.energy.EnergyStorageCoFH");
            cofhEnergy = cofhStorage.getDeclaredField("energy");
            cofhEnergy.setAccessible(true);
            cofhCapacity = cofhStorage.getDeclaredField("capacity");
            cofhCapacity.setAccessible(true);
            return true;
        } catch (Exception e) {
            cofhInitFailed = true;
            return false;
        }
    }

    /**
     * 通过 ENERGY 能力实例判断并补满 CoFH(Thermal) 与 Titanium(Industrial Foregoing) 的内部 int 能量字段。
     * 这两类机器内部存储均为 int（上限 Integer.MAX_VALUE ≈ 21.47 亿 FE），补满到当前容量即可。
     */
    private static long tryRefillCapability(IEnergyStorage cap, long available) {
        // 解包 CoFH 的侧面限制包装 EnergyHandlerRestrictionWrapper
        if (initCoFH()) {
            Object storage = unwrapCoFH(cap);
            if (cofhStorage.isInstance(storage)) {
                try {
                    int cur = cofhEnergy.getInt(storage);
                    int max = cofhCapacity.getInt(storage);
                    if (max > 0 && cur < max) {
                        long give = Math.min(max - cur, available);
                        if (give > 0) {
                            cofhEnergy.setInt(storage, cur + (int) give);
                            return give;
                        }
                    }
                    return 0;
                } catch (Exception ignored) {
                    return 0;
                }
            }
        }
        // Titanium(Industrial Foregoing)：EnergyStorageComponent 及子类（BigEnergyHandler）
        if (cap.getClass().getName().startsWith("com.hrznstudio.titanium.component.energy.")) {
            try {
                int cur = cap.getEnergyStored();
                int max = cap.getMaxEnergyStored();
                if (max <= 0 || cur >= max) {
                    return 0;
                }
                long give = Math.min(max - cur, available);
                if (give <= 0) {
                    return 0;
                }
                int targetValue = cur + (int) give;
                // 优先调用 setEnergyStored(int)（BigEnergyHandler 提供，内部 clamp 到容量）；否则反射写 energy 字段
                Method setter;
                try {
                    setter = cap.getClass().getMethod("setEnergyStored", int.class);
                } catch (NoSuchMethodException ignored) {
                    setter = null;
                }
                if (setter != null) {
                    setter.invoke(cap, targetValue);
                } else {
                    if (titaniumEnergy == null) {
                        titaniumEnergy = cap.getClass().getDeclaredField("energy");
                        titaniumEnergy.setAccessible(true);
                    }
                    titaniumEnergy.setInt(cap, targetValue);
                }
                return give;
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 解包 CoFH 的侧面限制包装 EnergyHandlerRestrictionWrapper，返回内部真正的能量存储实例；
     * 若实例不是该包装则原样返回。
     */
    @Nullable
    private static Object unwrapCoFH(IEnergyStorage cap) {
        try {
            if (cofhWrapper.isInstance(cap)) {
                return cofhWrapped.get(cap);
            }
        } catch (Exception ignored) {
            // 反射失败：原样返回
        }
        return cap;
    }
}
