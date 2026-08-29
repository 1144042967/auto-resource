package cn.sd.jrz.autoresource.compat.create;

import cn.sd.jrz.autoresource.AutoResource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.fabricmc.loader.api.FabricLoader;

import org.jetbrains.annotations.Nullable;

/**
 * Create 联动入口：判断 Create 是否加载、懒加载并缓存水车/大水车物品引用。
 * 本包其余类直接引用 Create 类，必须只在 Create 加载时被引用/实例化（见 Registration#init）；
 * 本类自身不引用任何 Create 类型，可无条件加载。
 */
public final class CreateCompat {
    public static final String CREATE_ID = "create";

    @Nullable
    private static Item waterWheelItem;
    @Nullable
    private static Item largeWaterWheelItem;
    /**
     * 未装 Create 时注册表查询恒返回 AIR，置位后不再重复查询
     */
    private static boolean wheelLookupFailed;
    private static boolean largeWheelLookupFailed;

    private CreateCompat() {
    }

    /**
     * Create 是否已加载（在 mod 构造阶段即可判断）
     */
    public static boolean isCreateLoaded() {
        return FabricLoader.getInstance().isModLoaded(CREATE_ID);
    }

    /**
     * 按类名反射调用 {@link CreateRegistration} 的静态方法。必须用 Class.forName 按字符串加载：
     * 直接引用会在调用方字节码产生类引用，JVM 链接时急切解析 Create 类，无 Create 时崩溃。
     * <p>当编译期未提供 create-fabric jar 时，本类及其依赖的 {@code WaterWheelMotorBlock/Entity/Item/Menu}
     * 会在打包阶段被排除，运行时反射必然找不到目标类——此时静默跳过（视为未启用 Create 联动），
     * 不要让游戏崩溃。
     */
    public static void invokeRegistration(String method, Class<?>[] paramTypes, Object[] args) {
        try {
            Class.forName("cn.sd.jrz.autoresource.compat.create.CreateRegistration")
                    .getMethod(method, paramTypes)
                    .invoke(null, args);
        } catch (ClassNotFoundException e) {
            // 编译时未引入 create-fabric jar，对应类未被打进本 mod；无 Create 联动，忽略
            AutoResource.LOGGER.info("Create 联动类未打入当前 jar（编译期未提供 create-fabric jar），跳过 register");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("机械动力联动初始化失败: " + method, e);
        }
    }

    /**
     * 按类名反射调用客户端注册类 {@code cn.sd.jrz.autoresource.client.compat.create.CreateRegistrationClient}
     * 的静态方法（屏幕与渲染器注册位于 client source set）。
     * 同样以字符串形式加载，避免通用侧字节码引用 client 类与 Create 类。
     * <p>对应 client 端 compat 类在编译期未提供 create-fabric jar 时同样会被排除，反射不到时静默跳过。
     */
    public static void invokeClient(String method, Class<?>[] paramTypes, Object[] args) {
        try {
            Class.forName("cn.sd.jrz.autoresource.client.compat.create.CreateRegistrationClient")
                    .getMethod(method, paramTypes)
                    .invoke(null, args);
        } catch (ClassNotFoundException e) {
            AutoResource.LOGGER.info("Create 联动客户端类未打入当前 jar（编译期未提供 create-fabric jar），跳过 " + method);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("机械动力联动客户端初始化失败: " + method, e);
        }
    }

    /**
     * create:water_wheel 物品（懒加载并缓存；Create 物品注册完成后调用才返回实际物品）
     */
    @Nullable
    public static Item waterWheelItem() {
        if (waterWheelItem == null && !wheelLookupFailed) {
            waterWheelItem = lookup(CREATE_ID, "water_wheel");
            wheelLookupFailed = waterWheelItem == null;
        }
        return waterWheelItem;
    }

    /**
     * create:large_water_wheel 物品（懒加载并缓存；Create 物品注册完成后调用才返回实际物品）
     */
    @Nullable
    public static Item largeWaterWheelItem() {
        if (largeWaterWheelItem == null && !largeWheelLookupFailed) {
            largeWaterWheelItem = lookup(CREATE_ID, "large_water_wheel");
            largeWheelLookupFailed = largeWaterWheelItem == null;
        }
        return largeWaterWheelItem;
    }

    @Nullable
    private static Item lookup(String namespace, String path) {
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(namespace, path);
        Item item = BuiltInRegistries.ITEM.get(loc);
        // Fabric 注册表查不到时返回 AIR 而非 null，此处归一化为 null
        return item == Items.AIR ? null : item;
    }
}
