package cn.sd.jrz.autoresource.compat.energy;

import net.fabricmc.loader.api.FabricLoader;

/**
 * FE 能量前置 mod（teamreborn energy）加载门面：判断前置是否安装。
 * 发电机（energy_generator_fe）依赖 teamreborn EnergyStorage，未装前置时不注册发电机。
 * 本类自身不引用任何 teamreborn 类型，可无条件加载；引用 EnergyStorage 的类
 * （EnergyGeneratorEntity/EnergyConnection/ItemEnergyIo/EnergySetup）
 * 只能在 {@code isEnergyLoaded()} 为 true 时被引用/实例化，否则 NoClassDefFoundError。
 */
public final class EnergyCompat {
    /**
     * teamreborn energy 的 mod id（Fabric 命名中下划线）
     */
    public static final String ENERGY_MOD_ID = "team_reborn_energy";

    private EnergyCompat() {
    }

    /**
     * teamreborn energy 是否已加载（在 mod 构造阶段即可判断）
     */
    public static boolean isEnergyLoaded() {
        return FabricLoader.getInstance().isModLoaded(ENERGY_MOD_ID);
    }

    /**
     * 反射调用 {@link EnergySetup#init()} 注册发电机对外能量存储。
     * 必须用 Class.forName 按字符串加载：字节码引用 EnergySetup 会触发 EnergyStorage 类加载。
     * 仅当 {@link #isEnergyLoaded()} 为 true 时调用。
     */
    public static void invokeEnergySetup() {
        try {
            Class.forName("cn.sd.jrz.autoresource.compat.energy.EnergySetup")
                    .getMethod("init")
                    .invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to register energy SIDED lookup", e);
        }
    }
}