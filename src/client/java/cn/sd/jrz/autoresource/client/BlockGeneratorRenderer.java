package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

/**
 * 方块生成机的方块实体渲染器（标记槽物品在四个侧面的展示）：
 * 1.21.5 渲染管线已重写为 RenderState + SubmitNodeCollector，
 * 为避免引入复杂依赖，渲染改为占位 no-op，标记槽仍可通过 GUI 内的 ghost slot 看到，
 * 标签同步由 BlockEntity.setChanged 配合 sendBlockUpdated 保证。
 */
public class BlockGeneratorRenderer implements BlockEntityRenderer<BlockGeneratorEntity, BlockGeneratorRenderer.RenderState> {

    /**
     * 渲染状态：当前仅占位，后续按新版 API 重写时添加物品姿态/位置字段
     */
    public static class RenderState extends BlockEntityRenderState {
    }

    public BlockGeneratorRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(@NotNull BlockGeneratorEntity entity, @NotNull RenderState state, float partialTick, @NotNull net.minecraft.world.phys.Vec3 cameraPos, @NotNull net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        // 1.21.5 渲染管线重写后旧的 VertexConsumer 链路无法直接复用；保留渲染钩子用于后续按新版 API 重写
        ItemStack marked = entity.getMarkedItem();
        if (marked.isEmpty()) {
            return;
        }
        BlockEntityRenderState.extractBase(entity, state, overlay);
    }

    @Override
    public void submit(@NotNull RenderState state, @NotNull PoseStack poseStack, @NotNull SubmitNodeCollector collector, @NotNull CameraRenderState cameraState) {
        // 占位实现：1.21.5 渲染管线重写后，需要按 RenderState -> SubmitNodeCollector 重写物品四面展示
    }

    @Override
    public boolean shouldRender(@NotNull BlockGeneratorEntity entity, @NotNull net.minecraft.world.phys.Vec3 cameraPos) {
        ItemStack marked = entity.getMarkedItem();
        return !marked.isEmpty();
    }
}