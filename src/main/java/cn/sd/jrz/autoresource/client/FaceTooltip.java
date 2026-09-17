package cn.sd.jrz.autoresource.client;

import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 六方向按钮的 hover tooltip 统一构建（与 alltheimbaium 的同名类保持同一版式）。
 * <p>
 * 所有带六方向按钮的界面（FE 发电机 / 流体生成机 / 方块生成机 / 水车马达）都走这里，保证观感一致：
 * <pre>
 *   §f&lt;当前状态&gt;                 ① 无标签，最亮白色（启用 / 禁用 / 选中 / 未选中）
 *   §7输出方向：§e&lt;方向&gt;            ②
 *   §7输出目标：§e&lt;指向的方块&gt;       ③ 无目标时显示 §8无
 * </pre>
 * 标签用暗色 {@code §7}、内容用亮色，是这一组提示的统一读法。
 * <p>
 * 拼装一律用字符串拼接，不用 {@code Component.append}：{@code §} 的格式状态不跨兄弟组件传递，
 * 放进独立组件的前缀会被丢弃（与 alltheimbaium 的 {@code item/Tip.java} 处理一致）。
 */
public final class FaceTooltip {

    /** 内容行没有标签，直接用最亮的白色 */
    private static final String CONTENT_COLOR = "§f";

    private FaceTooltip() {
    }

    /**
     * @param directionName 方向名
     * @param targetName    该方向指向的方块名；无目标传 {@code null}
     * @param outputContent 当前面的状态文案（启用 / 禁用 / 选中 / 未选中）。
     *                      传 {@code null} 表示该机器没有可选的状态，不显示内容行
     */
    @Nonnull
    public static List<Component> build(@Nonnull String directionName,
                                        @Nullable String targetName,
                                        @Nullable String outputContent) {
        List<Component> lines = new ArrayList<>();
        if (outputContent != null && !outputContent.isEmpty()) {
            lines.add(Component.literal(CONTENT_COLOR + outputContent));
        }
        lines.add(Component.translatable("screen.autoresource.output.direction", directionName));
        lines.add(targetName != null
                ? Component.translatable("screen.autoresource.output.target", targetName)
                : Component.translatable("screen.autoresource.output.no_target"));
        return lines;
    }
}
