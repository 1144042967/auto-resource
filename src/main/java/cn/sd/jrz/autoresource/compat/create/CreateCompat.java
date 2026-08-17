package cn.sd.jrz.autoresource.compat.create;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * 机械动力（Create）联动入口。
 * <p>
 * 本包内所有类都直接引用 Create 的类（编译期依赖 libs/create-*.jar），
 * 因此必须保证只在 Create 加载时才被引用/实例化（见 {@link cn.sd.jrz.autoresource.setup.Registration#init}）。
 * 本类只做两件事：判断 Create 是否加载、懒加载并缓存水车/大水车物品引用。
 */
public final class CreateCompat {
    public static final String CREATE_ID = "create";

    @Nullable
    private static Item waterWheelItem;
    @Nullable
    private static Item largeWaterWheelItem;

    private CreateCompat() {
    }

    /** Create 是否已加载（在 mod 构造阶段即可判断） */
    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded(CREATE_ID);
    }

    /**
     * 按类名反射调用 {@link CreateRegistration} 的静态方法（仅当 Create 已加载时调用）。
     * <p>
     * 不能用普通方法引用/直接调用：那些会在调用方类的字节码里产生对 {@code CreateRegistration} 的类引用，
     * 而该类的字节码引用了 Create 类，JVM 在链接调用方类时会急切解析它们——无 Create 时即崩溃。
     * 用 {@code Class.forName} 按字符串加载可彻底隔离类引用，只有 Create 存在时才真正加载。
     *
     * @param method     静态方法名（register / registerClient / registerRenderers）
     * @param paramTypes 方法参数类型
     * @param args       方法实参
     */
    public static void invokeRegistration(String method, Class<?>[] paramTypes, Object[] args) {
        try {
            Class.forName("cn.sd.jrz.autoresource.compat.create.CreateRegistration")
                    .getMethod(method, paramTypes)
                    .invoke(null, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("机械动力联动初始化失败: " + method, e);
        }
    }

    /** create:water_wheel 物品（懒加载并缓存；注册完成后调用才返回实际物品） */
    @Nullable
    public static Item waterWheelItem() {
        if (waterWheelItem == null) {
            waterWheelItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(CREATE_ID, "water_wheel"));
        }
        return waterWheelItem;
    }

    /** create:large_water_wheel 物品（懒加载并缓存；注册完成后调用才返回实际物品） */
    @Nullable
    public static Item largeWaterWheelItem() {
        if (largeWaterWheelItem == null) {
            largeWaterWheelItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(CREATE_ID, "large_water_wheel"));
        }
        return largeWaterWheelItem;
    }
}
