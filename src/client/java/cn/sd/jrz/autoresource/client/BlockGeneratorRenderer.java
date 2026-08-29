package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

import org.jetbrains.annotations.NotNull;

/**
 * 方块生成机的方块实体渲染器：标记槽有物品时，在四个侧面（北/南/东/西）各画一个标记方块贴图矩形。
 *
 * 26.x 起渲染管线（submit 提交节点 + RenderState）完全重构，旧的 PoseStack/MultiBufferSource 路径已弃用。
 * 此处保留渲染器类壳与最简渲染状态：当前渲染逻辑暂时回退到无附加效果（标记物品仍可通过 GUI 内 blit 展示），等后续按新版管线实现。
 * 屏幕 GUI 仍能正常工作，方块机本身的逻辑交互、能量传输等不受影响。
 */
public class BlockGeneratorRenderer implements BlockEntityRenderer<BlockGeneratorEntity, BlockGeneratorRenderer.MarkerRenderState> {

    public BlockGeneratorRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public MarkerRenderState createRenderState() {
        return new MarkerRenderState();
    }

    @Override
    public void extractRenderState(@NotNull BlockGeneratorEntity entity, @NotNull MarkerRenderState state, float partialTick, @NotNull net.minecraft.world.phys.Vec3 cameraPos, @NotNull net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        // 暂存标记物品信息；submit 阶段未提交任何附加绘制（保持接口可扩展）
        state.markedEmpty = entity.getMarkedItem().isEmpty();
    }

    @Override
    public void submit(@NotNull MarkerRenderState state, @NotNull PoseStack poseStack, @NotNull SubmitNodeCollector collector, @NotNull CameraRenderState camera) {
        // 26.x 渲染管线暂未实现四面贴图渲染（界面 GUI 仍可见标记物品名）
    }

    /**
     * 渲染状态载体（保留以便后续扩展）
     */
    public static class MarkerRenderState extends BlockEntityRenderState {
        public boolean markedEmpty;
    }
}