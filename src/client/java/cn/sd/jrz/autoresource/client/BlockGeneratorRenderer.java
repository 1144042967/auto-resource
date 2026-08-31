package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 方块生成机的方块实体渲染器：标记槽有物品时，在四个侧面（北/南/东/西）各画一个标记方块贴图矩形。
 * 26.1.2 渲染管线与 1.21.11 一致：拆分为 createRenderState/extractRenderState/submit 三阶段，
 * 顶点几何经 {@link SubmitNodeCollector#submitCustomGeometry} 提交。
 * 实现要点：取 particleIcon 用 cutout 画平面四边形（共用方块图集，避免首帧贴图未加载），强制至少 15 级方块光照。
 */
public class BlockGeneratorRenderer implements BlockEntityRenderer<BlockGeneratorEntity, BlockGeneratorRenderState> {
    /**
     * 旋转角度倍数（DOWN/UP/NORTH/SOUTH/WEST/EAST）：旋转后目标面朝向 +Z（与 StorageDrawers 一致）
     */
    private static final float[] SIDE_ROT_Y = {0, 0, 2, 0, 3, 1};
    /**
     * 需要显示物品的四个侧面（上下两面除外）
     */
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    /**
     * 面的四周留边比例（居中矩形约占面 55%）
     */
    private static final float INSET = 0.275f;
    /**
     * 强制的最低方块光照（15 级 = 全亮），避免贴图太暗
     */
    private static final int MIN_BLOCK_LIGHT = 15 << 4;

    public BlockGeneratorRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public BlockGeneratorRenderState createRenderState() {
        return new BlockGeneratorRenderState();
    }

    @Override
    public void extractRenderState(BlockGeneratorEntity entity, BlockGeneratorRenderState state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        // 先填充基础字段（blockPos/lightCoords/blockState 等）
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPos, crumblingOverlay);
        ItemStack marked = entity.getMarkedItem();
        if (!marked.isEmpty() && marked.getItem() instanceof BlockItem blockItem) {
            state.markedBlockState = blockItem.getBlock().defaultBlockState();
        } else {
            state.markedBlockState = null;
        }
    }

    @Override
    public void submit(BlockGeneratorRenderState state, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState) {
        BlockState markedState = state.markedBlockState;
        if (markedState == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(markedState).sprite();
        // 强制从当前方块图集重新解析精灵，确保首次渲染时贴图已加载
        // 26.1.2：AtlasManager 查找键用 AtlasIds.*（不带 .png 后缀），而 TextureAtlas.LOCATION_BLOCKS 带 .png 会抛 Invalid atlas id
        sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(sprite.contents().name());
        // 强制至少 15 级方块光照，保留环境天空光
        int light = Math.max(state.lightCoords & 0xFFFF, MIN_BLOCK_LIGHT) | (state.lightCoords & 0xFFFF0000);
        // RenderType 的纹理绑定（Sampler0）用带 .png 的图集位置；getAtlasOrThrow 才用 AtlasIds（不带 .png）
        RenderType renderType = RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS);
        for (Direction side : SIDES) {
            // lambda 捕获需 effectively final
            final TextureAtlasSprite spriteForFace = sprite;
            final int lightForFace = light;
            poseStack.pushPose();
            // 绕 Y 旋转使目标面朝向 +Z（Y 方向不变，贴图保持竖直）
            // 26.3 起 PoseStack#mulPose 不再接受 Quaternionf，需用 Matrix4f 包装
            poseStack.translate(0.5f, 0, 0.5f);
            poseStack.mulPose(new org.joml.Matrix4f().rotateY(SIDE_ROT_Y[side.ordinal()] * 90f * (float) Math.PI / 180f));
            poseStack.translate(-0.5f, 0, -0.5f);
            nodeCollector.submitCustomGeometry(poseStack, renderType, (pose, consumer) ->
                    renderSpriteOnFace(spriteForFace, pose, consumer, lightForFace));
            poseStack.popPose();
        }
    }

    /**
     * 在 +Z 面（z=1）绘制居中矩形贴图，略高于面避免 z-fighting
     */
    private void renderSpriteOnFace(TextureAtlasSprite sprite, PoseStack.Pose pose, VertexConsumer consumer, int light) {
        // 顶点顺序：左下、右下、右上、左上；V0=纹理顶部（对应 +Y），U0=纹理左侧
        float x1 = 0.5f - INSET;
        float x2 = 0.5f + INSET;
        float y1 = 0.5f - INSET;
        float y2 = 0.5f + INSET;
        float z = 1.002f;
        Matrix4f mat = pose.pose();
        addVertex(consumer, mat, x1, y1, z, sprite.getU0(), sprite.getV1(), light);
        addVertex(consumer, mat, x2, y1, z, sprite.getU1(), sprite.getV1(), light);
        addVertex(consumer, mat, x2, y2, z, sprite.getU1(), sprite.getV0(), light);
        addVertex(consumer, mat, x1, y2, z, sprite.getU0(), sprite.getV0(), light);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f mat, float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(mat, x, y, z).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
    }
}