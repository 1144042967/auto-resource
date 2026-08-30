package cn.sd.jrz.autoresource.client.compat.create;

import cn.sd.jrz.autoresource.compat.create.WaterWheelMotorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 水车马达方块实体渲染器（仅 Create 加载时使用）。在垂直于输出方向的四面侧
 * 渲染当前转速文字（叠放在 LCD 显示窗上），文字上方始终指向输出方向。
 * 1.21.11 渲染管线重构：旧 render() 拆分为 createRenderState/extractRenderState/submit 三阶段，
 * 文字经 SubmitNodeCollector#submitText 提交（替代旧 Font.drawInBatch）。
 */
public class WaterWheelMotorRenderer implements BlockEntityRenderer<WaterWheelMotorEntity, WaterWheelMotorRenderState> {
    /**
     * 各面绕 Y 轴旋转倍数（N/S/E/W 用 Y 轴，上下两面用 X 轴，见 faceRotation）
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
        // 1.21.11：BlockEntityRendererProvider.Context 无 getFont，经 Minecraft 获取字体
        this.font = Minecraft.getInstance().font;
    }

    @Override
    public WaterWheelMotorRenderState createRenderState() {
        return new WaterWheelMotorRenderState();
    }

    @Override
    public void extractRenderState(WaterWheelMotorEntity entity, WaterWheelMotorRenderState state, float partialTick, Vec3 cameraPos,
                                   @NotNull ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPos, crumblingOverlay);
        state.speed = entity.currentSpeed();
        state.outputFace = entity.getOutputFace();
    }

    @Override
    public void submit(WaterWheelMotorRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        int speed = state.speed;
        Component text = Component.translatable("screen.autoresource.water_wheel_motor.block_speed", String.format("%03d", speed));
        // 强制至少 15 级方块光照，保留环境天空光
        int light = Math.max(state.lightCoords & 0xFFFF, MIN_BLOCK_LIGHT) | (state.lightCoords & 0xFFFF0000);
        Direction output = state.outputFace;
        for (Direction side : Direction.values()) {
            // 只在垂直于输出方向的四个侧面显示（输出面与其对面是水车轮/底座贴图）
            if (side.getAxis() == output.getAxis()) {
                continue;
            }
            renderTextOnFace(text, output, side, poseStack, nodeCollector, light);
        }
    }

    /**
     * 在指定侧面绘制居中转速文字，文字上方指向输出方向。
     * 旋转中心必须是方块中心 (0.5,0.5,0.5)：此前误用 y=0 导致上下两面文字错位。
     */
    private void renderTextOnFace(Component text, Direction output, Direction face, PoseStack poseStack, SubmitNodeCollector nodeCollector, int light) {
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
        // 1.21.11：submitText(pose, x, y, text, shadow, mode, color, bg, 0, light)
        nodeCollector.submitText(poseStack, -textWidth / 2f, -4f, text.getVisualOrderText(), true,
                Font.DisplayMode.NORMAL, 0xFFFFFF, 0, 0, light);
        poseStack.popPose();
    }

    /**
     * 把目标面转到 +Z 的旋转（N/S/E/W 用 Y 轴，上下两面用 X 轴）
     */
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
