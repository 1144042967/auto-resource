package cn.sd.jrz.autoresource.compat.create;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLLoader;

import javax.annotation.Nullable;

/**
 * Create 联动入口：判断 Create 是否加载、懒加载并缓存水车/大水车物品引用。
 * 本包类直接引用 Create 类，必须只在 Create 加载时被引用/实例化（见 Registration#init）。
 */
public final class CreateCompat {
    public static final String CREATE_ID = "create";

    @Nullable
    private static Item waterWheelItem;
    @Nullable
    private static Item largeWaterWheelItem;

    private CreateCompat() {
    }

    /**
     * Create 是否已加载（在 mod 构造阶段即可判断）。
     * 用 FMLLoader.getLoadingModList() 而非 ModList.get()：NeoForge 在 mod 构造早期 ModList 可能未填充完，
     * 而 loadingModList 基于已扫描的 mods.toml 声明，能可靠判断 Create 是否声明加载。
     */
    public static boolean isCreateLoaded() {
        boolean loaded = FMLLoader.getLoadingModList().getModFileById(CREATE_ID) != null;
        // 调试：确认 Create 检测结果（看 logs 里 [AutoResource] 行）
        System.out.println("[AutoResource] CreateCompat.isCreateLoaded = " + loaded);
        return loaded;
    }

    /**
     * 按类名反射调用 {@link CreateRegistration} 的静态方法。必须用 Class.forName 按字符串加载：
     * 直接引用会在调用方字节码产生类引用，JVM 链接时急切解析 Create 类，无 Create 时崩溃。
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

    /**
     * create:water_wheel 物品（懒加载并缓存；注册完成后调用才返回实际物品）
     */
    @Nullable
    public static Item waterWheelItem() {
        if (waterWheelItem == null) {
            waterWheelItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CREATE_ID, "water_wheel"));
        }
        return waterWheelItem;
    }

    /**
     * create:large_water_wheel 物品（懒加载并缓存；注册完成后调用才返回实际物品）
     */
    @Nullable
    public static Item largeWaterWheelItem() {
        if (largeWaterWheelItem == null) {
            largeWaterWheelItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(CREATE_ID, "large_water_wheel"));
        }
        return largeWaterWheelItem;
    }
}
