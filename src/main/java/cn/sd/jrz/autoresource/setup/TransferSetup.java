package cn.sd.jrz.autoresource.setup;

import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import cn.sd.jrz.autoresource.blockentity.LiquidGeneratorEntity;
import cn.sd.jrz.autoresource.capability.BlockConnection;
import cn.sd.jrz.autoresource.capability.LiquidConnection;
import cn.sd.jrz.autoresource.compat.energy.EnergyCompat;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Direction;

/**
 * 机器对外的能量/流体/物品存储暴露（对应 Forge 版实体覆写 getCapability 的部分）：
 * 三台机器六面均可被相邻机器/管道访问，具体行为见各 Connection 类。
 */
public final class TransferSetup {

    private TransferSetup() {
    }

    public static void init() {
        // FE 发电机：六面输出能量（前置 teamreborn energy 加载时经反射注册，避免本类加载触发 EnergyStorage 类解析）
        if (EnergyCompat.isEnergyLoaded()) {
            EnergyCompat.invokeEnergySetup();
        }

        // 流体生成器：六面输出流体
        FluidStorage.SIDED.registerForBlockEntity(
                (LiquidGeneratorEntity entity, Direction direction) -> new LiquidConnection(entity),
                Registration.LIQUID_GENERATOR_WATER_ENTITY);
        FluidStorage.SIDED.registerForBlockEntity(
                (LiquidGeneratorEntity entity, Direction direction) -> new LiquidConnection(entity),
                Registration.LIQUID_GENERATOR_LAVA_ENTITY);

        // 流体生成器的物品管道视图：插入仅进输入槽、抽取仅来自输出槽
        ItemStorage.SIDED.registerForBlockEntity(
                (LiquidGeneratorEntity entity, Direction direction) -> entity.getPipeView(),
                Registration.LIQUID_GENERATOR_WATER_ENTITY);
        ItemStorage.SIDED.registerForBlockEntity(
                (LiquidGeneratorEntity entity, Direction direction) -> entity.getPipeView(),
                Registration.LIQUID_GENERATOR_LAVA_ENTITY);

        // 方块生成器：六面输出标记方块
        ItemStorage.SIDED.registerForBlockEntity(
                (BlockGeneratorEntity entity, Direction direction) -> new BlockConnection(entity),
                Registration.BLOCK_GENERATOR_ENTITY);
    }
}
