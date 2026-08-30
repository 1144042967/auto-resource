package cn.sd.jrz.autoresource.compat.energy;

import cn.sd.jrz.autoresource.blockentity.EnergyGeneratorEntity;
import cn.sd.jrz.autoresource.capability.EnergyConnection;
import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.core.Direction;
import team.reborn.energy.api.EnergyStorage;

/**
 * FE 发电机对外能量存储的 SIDED 注册。
 * 本类直接引用 teamreborn {@link EnergyStorage}，只能在 teamreborn energy 加载时
 * 由 {@link EnergyCompat#invokeEnergySetup()} 反射加载，否则 NoClassDefFoundError。
 */
public final class EnergySetup {
    private EnergySetup() {
    }

    public static void init() {
        // FE 发电机：六面输出能量
        EnergyStorage.SIDED.registerForBlockEntity(
                (EnergyGeneratorEntity entity, Direction direction) -> new EnergyConnection(entity),
                Registration.ENERGY_GENERATOR_FE_ENTITY);
    }
}
