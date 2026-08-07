package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 方块生成机的方块实体渲染器（26.x 适配）。
 * <p>
 * 当机器标记槽中有物品时，在除上下外的四个侧面（北/南/东/西）各绘制一个标记方块的贴图矩形，
 * 表示该机器当前生成的方块种类。使用 26.x state-based 渲染模型
 * （createRenderState/extractRenderState/submit），贴图取自方块粒子精灵。
 */
public class BlockGeneratorRenderer implements BlockEntityRenderer<BlockGeneratorEntity, BlockGeneratorRenderer.BlockGeneratorRenderState> {
    /**
     * 旋转角度倍数（DOWN/UP/NORTH/SOUTH/WEST/EAST）：旋转后目标面朝向 +Z，
     * 与 StorageDrawers 的 alignRendering 一致
     */
    private static final float[] SIDE_ROT_Y = {0, 0, 2, 0, 3, 1};
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final float INSET = 0.275f;
    private static final int MIN_BLOCK_LIGHT = 15 << 4;
    /** 使用方块图集的 cutout 渲染类型（与方块本体共用方块图集） */
    private static final RenderType RENDER_TYPE = RenderTypes.cutoutMovingBlock();

    public BlockGeneratorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public BlockGeneratorRenderState createRenderState() {
        return new BlockGeneratorRenderState();
    }

    @Override
    public void extractRenderState(BlockGeneratorEntity entity, BlockGeneratorRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.markedItem = entity.getMarkedItem().copy();
    }

    @Override
    public void submit(BlockGeneratorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        ItemStack marked = state.markedItem;
        if (marked.isEmpty() || !(marked.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        BlockState blockState = blockItem.getBlock().defaultBlockState();
        Material.Baked particle = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(blockState);
        if (particle == null) {
            return;
        }
        TextureAtlasSprite sprite = particle.sprite();
        if (sprite == null) {
            return;
        }
        // 强制至少 15 级方块光照，保留环境天空光
        int blockLight = Math.max(state.lightCoords & 0xFFFF, MIN_BLOCK_LIGHT);
        int light = (state.lightCoords & 0xFFFF0000) | blockLight;
        for (Direction side : SIDES) {
            renderSpriteOnFace(sprite, side, poseStack, collector, light);
        }
    }

    /**
     * 在指定方块面上绘制一个居中矩形贴图
     */
    private void renderSpriteOnFace(TextureAtlasSprite sprite, Direction face, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
        // 绕 Y 旋转使目标面朝向 +Z（Y 方向不变，贴图保持竖直）
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPose(new Quaternionf().rotateY(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f));
        poseStack.translate(-0.5f, 0, -0.5f);
        // 在 +Z 面（z=1）绘制居中矩形，略高于面避免 z-fighting
        float x1 = 0.5f - INSET;
        float x2 = 0.5f + INSET;
        float y1 = 0.5f - INSET;
        float y2 = 0.5f + INSET;
        float z = 1.002f;
        final TextureAtlasSprite sp = sprite;
        final int l = light;
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
            Matrix4f mat = pose.pose();
            // 顶点顺序：左下、右下、右上、左上；V0=纹理顶部（对应 +Y），U0=纹理左侧
            addVertex(consumer, mat, x1, y1, z, sp.getU0(), sp.getV1(), l);
            addVertex(consumer, mat, x2, y1, z, sp.getU1(), sp.getV1(), l);
            addVertex(consumer, mat, x2, y2, z, sp.getU1(), sp.getV0(), l);
            addVertex(consumer, mat, x1, y2, z, sp.getU0(), sp.getV0(), l);
        });
        poseStack.popPose();
    }

    private void addVertex(VertexConsumer consumer, Matrix4f mat, float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(mat, x, y, z).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setUv2(light & 0xFFFF, light >> 16).setNormal(0, 0, 1);
    }

    /**
     * 渲染状态：保存标记物品（服务端→客户端提取，避免渲染线程访问实体）
     */
    public static class BlockGeneratorRenderState extends BlockEntityRenderState {
        public ItemStack markedItem = ItemStack.EMPTY;
    }
}
