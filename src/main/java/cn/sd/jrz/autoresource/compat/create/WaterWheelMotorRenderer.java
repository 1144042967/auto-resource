package cn.sd.jrz.autoresource.compat.create;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;

/**
 * 水车马达方块实体渲染器（仅当机械动力 Create 加载时使用）。
 * <p>
 * 在<b>垂直于应力输出方向</b>的四个侧面上渲染当前转速文字（如 {@code 004 RPM}），
 * 叠放在侧面贴图中央的 LCD 显示窗上，且<b>文字上方始终指向应力输出方向</b>
 * （输出面由顶面水车轮贴图随 FACING 旋转指示，LCD 侧贴图分布在其四周）。
 * 例如输出朝北时，文字出现在东西与上下四面，文字顶部朝北。
 * <p>
 * 变换只用标准的 {@link PoseStack} 方法（translate/mulPose/scale）与 {@link Quaternionf}，
 * 与既有可用的渲染器（BlockGeneratorRenderer）结构一致，不直接构造 Matrix4f
 * （JOML 1.10.5 字段为包私有，且避免手写矩阵的风险）。
 * 转速由 {@link WaterWheelMotorEntity#currentSpeed()} 读取——水车槽内容随 Create 的
 * {@code SmartBlockEntity} 同步包下发，客户端无需额外网络同步。
 */
@OnlyIn(Dist.CLIENT)
public class WaterWheelMotorRenderer implements BlockEntityRenderer<WaterWheelMotorEntity> {
    /**
     * 旋转角度倍数（DOWN/UP/NORTH/SOUTH/WEST/EAST）：仅 N/S/E/W 用 Y 轴旋转把面转到 +Z，
     * 上下两面用 X 轴旋转（见 {@link #faceRotation}）
     */
    private static final float[] SIDE_ROT_Y = {0, 0, 2, 0, 3, 1};
    /**
     * 强制的最低方块光照（15 级 = 全亮），避免文字太暗看不清
     */
    private static final int MIN_BLOCK_LIGHT = 15 << 4;
    /**
     * 世界文字缩放（1 字体像素 = 0.01 方块）
     */
    private static final float TEXT_SCALE = 0.01f;
    private static final float DEG_TO_RAD = (float) Math.PI / 180f;

    private final Font font;

    public WaterWheelMotorRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(@Nonnull WaterWheelMotorEntity entity, float partialTick, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        int speed = entity.currentSpeed();
        Component text = Component.translatable("screen.autoresource.water_wheel_motor.block_speed", String.format("%03d", speed));
        // 强制至少 15 级方块光照，保留环境天空光
        int blockLight = Math.max(combinedLight & 0xFFFF, MIN_BLOCK_LIGHT);
        int light = (combinedLight & 0xFFFF0000) | blockLight;
        Direction output = entity.getOutputFace();
        for (Direction side : Direction.values()) {
            // 只在垂直于输出方向的四个侧面显示（输出面与其对面是水车轮/底座贴图）
            if (side.getAxis() == output.getAxis()) {
                continue;
            }
            renderTextOnFace(text, output, side, poseStack, buffer, light);
        }
    }

    /**
     * 在指定侧面绘制居中转速文字，文字上方指向应力输出方向。
     * <p>
     * 旋转中心必须是方块中心 (0.5,0.5,0.5)：此前误用 y=0（底面），
     * 导致上下两面（UP/DOWN，绕 X 轴旋转）的文字被转到方块内部/错误位置，
     * 表现为输出面非上下方向时顶底侧文字错位。
     */
    private void renderTextOnFace(Component text, Direction output, Direction face, PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        // 绕方块中心旋转，使目标面朝向 +Z
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(faceRotation(face));
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        // 移到面中心并略微凸出表面，避免与贴图 z-fighting
        poseStack.translate(0.5f, 0.5f, 1.01f);
        // 在面平面内旋转文字，使文字上方指向输出方向
        poseStack.mulPose(new Quaternionf().rotateZ(textAngle(output, face)));
        // 缩放字体（Y 取反，保证文字不镜像）
        poseStack.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        // 居中：水平按文字实际宽度，垂直按 8px 字高中心
        float textWidth = this.font.width(text);
        this.font.drawInBatch(text, -textWidth / 2f, -4f, 0xFFFFFF, true,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, light);
        poseStack.popPose();
    }

    /** 把目标面转到 +Z 的旋转（N/S/E/W 用 Y 轴，上下两面用 X 轴） */
    private static Quaternionf faceRotation(Direction face) {
        return switch (face) {
            case UP -> new Quaternionf().rotateX(-90f * DEG_TO_RAD);
            case DOWN -> new Quaternionf().rotateX(90f * DEG_TO_RAD);
            default -> new Quaternionf().rotateY(SIDE_ROT_Y[face.ordinal()] * 90f * DEG_TO_RAD);
        };
    }

    /**
     * 计算使"文字上方=输出方向"的面内旋转角（绕面法线）。
     * 把输出方向旋转到面局部坐标系（面法线=+Z），取其在面平面内的分量反算绕 Z 角度。
     */
    private static float textAngle(Direction output, Direction face) {
        Vector3f f = new Vector3f(output.getStepX(), output.getStepY(), output.getStepZ());
        f.rotate(faceRotation(face).conjugate());
        // F'=(fx, fy) 位于面平面内；文字上方=该方向 => θ = atan2(-fx, fy)
        return (float) Math.atan2(-f.x(), f.y());
    }
}
