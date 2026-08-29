package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 方块生成机的方块实体渲染器：标记槽有物品时，在四个侧面（北/南/东/西）各画一个标记方块贴图矩形。
 *
 * 26.3 起 BlockEntityRenderer 接口签名变更为 {@code BlockEntityRenderer<T, S>} 并要求实现
 * createRenderState/extractRenderState/submit 等方法；旧的 getBlockRenderer/MultiBufferSource/
 * RenderType/RenderSystem.setShaderTexture 等 API 也被替换。完整重写较为侵入且与机器功能正交，
 * 暂以最小存根保证编译通过——标记物品贴图的视觉延后到后续迭代补齐。
 */
public class BlockGeneratorRenderer implements BlockEntityRenderer<BlockGeneratorEntity, BlockGeneratorRenderer.State> {

    public BlockGeneratorRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BlockGeneratorEntity entity, State state, float partialTick, net.minecraft.world.phys.Vec3 cameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        BlockEntityRenderState.extractBase(entity, state, overlay);
        // 暂存实体引用供 submit 使用（26.3 渲染管线即将迁移为新的模型特征渲染器，简化处理）
        state.entity = entity;
    }

    @Override
    public void submit(State state, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, CameraRenderState camera) {
        // 占位实现：完整四侧贴图渲染逻辑延后到下一批；当前仅保留编译兼容
    }

    public static class State extends BlockEntityRenderState {
        public BlockGeneratorEntity entity;
    }
}