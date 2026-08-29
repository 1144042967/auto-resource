package cn.sd.jrz.autoresource.compat.energybypass;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 反射式能量绕过工具：当第三方 MOD 机器因容量/接收速率限制拒收能量时，反射读取/修改其内部
 * 能量字段，把能量补满到容量。全部通过字符串类名 + 反射定位，编译期零依赖；反射失败静默返回 0，
 * 各 MOD 首次解析后缓存（未安装的 MOD 记录失败并永久跳过）。支持：Mekanism、Thermal(CoFH)、
 * Industrial Foregoing(Titanium)、Draconic Evolution、Flux Networks。
 * <p>
 * 26.x 适配：能力参数由 {@code Object} 承载（26.x 新传输 API 为 {@code EnergyHandler}，旧接口
 * {@code IEnergyStorage} 仍可用），本类只做反射，不直接依赖具体能量接口类型。
 */
public final class EnergyBypass {

    // 单位换算：Mekanism 默认 1 FE = 2.5 J（GeneralConfig.forgeConversionRate），即 1 J = 0.4 FE
    private static final double FE_PER_J = 0.4;
    // Flux 网络缓存无容量上限，这里把"补满"定义为补到 100 亿 FE（足够网络长期消耗供给）
    private static final long FLUX_FULL = 10_000_000_000L;

    // ---- Mekanism 反射缓存 ----
    private static boolean mekInitFailed;
    private static Class<?> mekTile;
    private static Method mekGetEnergyContainers;
    private static Method mekGetEnergy;
    private static Method mekGetMaxEnergy;
    private static Method mekSetEnergy;

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
     * 尝试把目标内部能量补满到容量，绕过其容量/接收限制。按已知 MOD 依次尝试，命中即返回。
     *
     * @param target    目标方块实体
     * @param side      注入面（可为 null）
     * @param cap       目标暴露的能量能力实例（可为 null，部分 MOD 不依赖它）
     * @param available 本机可用能量（FE），注入量不会超过该值
     * @return 实际消耗的 FE；0 表示无需补满或补满失败
     */
    public static long tryRefill(BlockEntity target, @Nullable Direction side, @Nullable Object cap, long available) {
        net.minecraft.world.level.Level level = target != null ? target.getLevel() : null;
        if (level == null || level.isClientSide() || available <= 0) {
            return 0;
        }
        // 1. Mekanism（机器：TileEntityMekanism 子类，能量经 IEnergyContainer 读取/写入，单位 J）
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
        // 4. 通过能量能力实例判断 CoFH(Thermal) 与 Titanium(Industrial Foregoing)
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
            mekGetEnergyContainers = mekTile.getMethod("getEnergyContainers", Direction.class);
            Class<?> mekContainer = Class.forName("mekanism.api.energy.IEnergyContainer");
            mekGetEnergy = mekContainer.getMethod("getEnergy");
            mekGetMaxEnergy = mekContainer.getMethod("getMaxEnergy");
            mekSetEnergy = mekContainer.getMethod("setEnergy", long.class);
            return true;
        } catch (Exception e) {
            mekInitFailed = true;
            return false;
        }
    }

    /**
     * Mekanism（1.21.x 新 API）：反射 getEnergyContainers(Direction) 取所有 IEnergyContainer，
     * 逐容器 getEnergy/getMaxEnergy/setEnergy(long) 把能量补到容量；能量以 J 存储，1 FE = 2.5 J
     */

    private static long tryRefillMekanism(BlockEntity target, Direction side, long available) {
        if (!initMekanism()) {
            return 0;
        }
        try {
            if (!mekTile.isInstance(target)) {
                return 0;
            }
            Object containers = mekGetEnergyContainers.invoke(target, side);
            if (containers == null) {
                return 0;
            }
            long consumed = 0;
            // 遍历所有能量容器（主容器 + 辅助）依次补满
            for (Object container : (Iterable<?>) containers) {
                if (available - consumed <= 0) {
                    break;
                }
                long curJ = (long) mekGetEnergy.invoke(container);
                long maxJ = (long) mekGetMaxEnergy.invoke(container);
                // 容量无效（如无限）或已满则跳过
                if (maxJ <= 0 || curJ >= maxJ) {
                    continue;
                }
                long needJ = maxJ - curJ;
                // 可用 FE 换算为 J（1 J = 0.4 FE）
                long availableJ = (long) ((available - consumed) / FE_PER_J);
                long giveJ = Math.clamp(availableJ, 0, needJ);
                if (giveJ <= 0) {
                    continue;
                }
                mekSetEnergy.invoke(container, curJ + giveJ);
                long fe = Math.max(1, (long) (giveJ * FE_PER_J));
                consumed += Math.min(fe, available - consumed);
            }
            return consumed;
        } catch (Exception ignored) {
            // 反射失败或目标类型变化：静默跳过
            return 0;
        }
    }

    /**
     * Draconic Evolution：反射 OP 存储字段（opStorage/energy）调用 getOPStored/getMaxOPStored/modifyEnergyStored 补满；OP 与 FE 1:1，按类缓存
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
     * Flux Networks：反射解除 TransferHandler 每 tick 传输上限（mDisableLimit）并把 mBuffer 补到 FLUX_FULL
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
     * 通过能量能力实例判断并补满 CoFH(Thermal) 与 Titanium(Industrial Foregoing) 的内部 int 能量字段。
     * 这两类机器内部存储均为 int（上限 Integer.MAX_VALUE ≈ 21.47 亿 FE），补满到当前容量即可。
     * <p>
     * 26.x 适配：能力参数为 Object，Titanium 的读取/写入全部经反射，不依赖旧版 IEnergyStorage 接口。
     */
    private static long tryRefillCapability(Object cap, long available) {
        if (cap == null) {
            return 0;
        }
        // 解包 CoFH 的侧面限制包装 EnergyHandlerRestrictionWrapper
        if (initCoFH()) {
            Object storage = unwrapCoFH(cap);
            if (storage != null && cofhStorage.isInstance(storage)) {
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
                // 反射读取当前能量与容量（不依赖具体能量接口类型，兼容 26.x 新传输 API）
                int cur = (int) cap.getClass().getMethod("getEnergyStored").invoke(cap);
                int max = (int) cap.getClass().getMethod("getMaxEnergyStored").invoke(cap);
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
    private static Object unwrapCoFH(Object cap) {
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
