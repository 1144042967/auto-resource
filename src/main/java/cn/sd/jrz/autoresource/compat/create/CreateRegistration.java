package cn.sd.jrz.autoresource.compat.create;

import cn.sd.jrz.autoresource.setup.Registration;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/**
 * Create 联动的注册入口。本类直接引用 Create 类，只能由 CreateCompat.invokeRegistration
 * 经 Class.forName 反射在 Create 加载时调用，保证无 Create 时类加载安全。
 */
public final class CreateRegistration {

    // 1.21.11：Block 构造要求 Properties.setId(ResourceKey)，构建独立 Properties（共享无法区分 id）
    private static BlockBehaviour.Properties blockProperties(String path) {
        return BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Registration.id(path)))
                .mapColor(DyeColor.BLUE)
                .pushReaction(PushReaction.DESTROY)
                .strength(0.5f, 3.0f)
                // 侧面 LCD 显示窗自发光
                .lightLevel(state -> 7);
    }

    private CreateRegistration() {
    }

    /**
     * 注册水车马达（双端），仅由 Registration.init 反射调用
     */
    public static void register() {
        Block block = new WaterWheelMotorBlock(blockProperties("water_wheel_motor"));
        Registration.WATER_WHEEL_MOTOR = Registry.register(BuiltInRegistries.BLOCK, Registration.id("water_wheel_motor"), block);
        Registration.WATER_WHEEL_MOTOR_ITEM = Registry.register(BuiltInRegistries.ITEM, Registration.id("water_wheel_motor"),
                new WaterWheelMotorItem(block, ResourceKey.create(Registries.ITEM, Registration.id("water_wheel_motor"))));
        // 1.21.11：vanilla BlockEntityType.Builder 移除，改用 FabricBlockEntityTypeBuilder
        Registration.WATER_WHEEL_MOTOR_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Registration.id("water_wheel_motor"),
                FabricBlockEntityTypeBuilder.create(
                        (pos, state) -> new WaterWheelMotorEntity(Registration.WATER_WHEEL_MOTOR_ENTITY, pos, state),
                        block).build());
        Registration.WATER_WHEEL_MOTOR_MENU = Registry.register(BuiltInRegistries.MENU, Registration.id("water_wheel_motor"),
                new ExtendedScreenHandlerType<>((id, inv, pos) -> new WaterWheelMotorMenu(id, inv, pos), BlockPos.STREAM_CODEC));
    }
}
