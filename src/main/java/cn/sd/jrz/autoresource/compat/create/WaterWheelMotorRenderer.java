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
import org.joml.Matrix4f;
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
 * 转速由 {@link WaterWheelMotorEntity#currentSpeed()} 读取——水车槽内容随 Create 的
 * {@code SmartBlockEntity} 同步包下发，客户端无需额外网络同步。
 */
@OnlyIn(Dist.CLIENT)
public class WaterWheelMotorRenderer implements BlockEntityRenderer<WaterWheelMotorEntity> {
    /**
     * 强制的最低方块光照（15 级 = 全亮），避免文字太暗看不清
     */
    private static final int MIN_BLOCK_LIGHT = 15 << 4;
    /**
     * 世界文字缩放（1 字体像素 = 0.01 方块）
     */
    private static final float TEXT_SCALE = 0.01f;
    /**
     * 文字平面略凸出表面的距离（方块面位于中心 +0.5，文字取 0.51）
     */
    private static final float TEXT_OFFSET = 0.51f;

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
     * 用 (右=U, 上=V=输出方向, 法线=N) 三个基向量直接构建变换矩阵，
     * 对六个面统一处理，无需按面分别旋转。
     */
    private void renderTextOnFace(Component text, Direction output, Direction face, PoseStack poseStack, MultiBufferSource buffer, int light) {
        // 面法线与输出方向（输出方向 ⊥ 面法线，位于面平面内）
        Vector3f n = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
        Vector3f v = new Vector3f(output.getStepX(), output.getStepY(), output.getStepZ());
        // 文字右方 U = V × N（保证 (U,V,N) 右手系，文字不镜像）
        Vector3f u = new Vector3f(v).cross(n);

        poseStack.pushPose();
        // 文字中心：面中心 + 略凸出表面，避免与贴图 z-fighting
        float px = 0.5f + TEXT_OFFSET * n.x();
        float py = 0.5f + TEXT_OFFSET * n.y();
        float pz = 0.5f + TEXT_OFFSET * n.z();
        // 局部坐标映射到世界：x 轴=文字右方 U，y 轴=文字下方 -V（字体 y 向下，取反后文字上方=输出方向）
        float s = TEXT_SCALE;
        Matrix4f mat = new Matrix4f();
        mat.set(0, 0, u.x() * s); mat.set(0, 1, u.y() * s); mat.set(0, 2, u.z() * s);
        mat.set(1, 0, -v.x() * s); mat.set(1, 1, -v.y() * s); mat.set(1, 2, -v.z() * s);
        mat.set(2, 0, n.x() * s); mat.set(2, 1, n.y() * s); mat.set(2, 2, n.z() * s);
        mat.set(3, 0, px); mat.set(3, 1, py); mat.set(3, 2, pz);
        poseStack.mulPoseMatrix(mat);
        // 居中：水平按文字实际宽度，垂直按 8px 字高中心
        float textWidth = this.font.width(text);
        this.font.drawInBatch(text, -textWidth / 2f, -4f, 0xFFFFFF, true, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, light);
        poseStack.popPose();
    }
}
