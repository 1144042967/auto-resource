package cn.sd.jrz.autoresource.setup;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import javax.annotation.Nonnull;

/**
 * 创造模式物品栏页。
 * <p>
 * <b>1.19.2 没有创意标签的注册表</b>（既没有 {@code CreativeModeTabEvent}，也没有
 * {@code ForgeRegistries.CREATIVE_MODE_TABS}）。Forge 43 的做法是给 {@code CreativeModeTab}
 * 打了个补丁：{@code new CreativeModeTab(String)} 会把自身追加进 {@code CreativeModeTab.TABS}
 * 末尾并自动扩容，所以这里直接用匿名子类，在静态初始化里完成“注册”。
 * 内容用 {@link #fillItemList} 填充（对应 1.20.1 的 {@code displayItems}）。
 */
public class ItemManager {

    public static final CreativeModeTab CREATIVE_MODE_TAB = new CreativeModeTab("autoresource") {

        @Override
        @Nonnull
        public ItemStack makeIcon() {
            return new ItemStack(Registration.ENERGY_GENERATOR_FE_ITEM.get());
        }

        @Override
        public void fillItemList(@Nonnull NonNullList<ItemStack> items) {
            items.add(new ItemStack(Registration.ENERGY_GENERATOR_FE_ITEM.get()));
            items.add(new ItemStack(Registration.LIQUID_GENERATOR_WATER_ITEM.get()));
            items.add(new ItemStack(Registration.LIQUID_GENERATOR_LAVA_ITEM.get()));
            items.add(new ItemStack(Registration.BLOCK_GENERATOR_ITEM.get()));
            // Create 联动：仅当 Create 加载时显示水车马达
            if (Registration.WATER_WHEEL_MOTOR_ITEM != null) {
                items.add(new ItemStack(Registration.WATER_WHEEL_MOTOR_ITEM.get()));
            }
        }
    };

    /**
     * 标签页在 {@link #CREATIVE_MODE_TAB} 的静态初始化里已经自我注册，这里无须再做别的——
     * 调用本方法本身即触发类初始化，必须在客户端建出创意物品栏之前完成。
     */
    public static void init(FMLJavaModLoadingContext context) {
        // 见方法注释：只为触发类初始化
    }
}
