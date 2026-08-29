package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.blockentity.BlockGeneratorEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import org.jetbrains.annotations.NotNull;

/**
 * 方块生成机的方块实体渲染器：标记槽有物品时，在四个侧面（北/南/东/西）各画一个标记方块贴图矩形。
 * 实现要点：取 getParticleIcon 用 cutout 画平面四边形（共用方块图集，避免首帧贴图未加载），强制至少 15 级方块光照。
 */
public class BlockGeneratorRenderer implements BlockEntityRenderer<BlockGeneratorEntity> {
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

    @SuppressWarnings("deprecation")
    @Override
    public void render(@NotNull BlockGeneratorEntity entity, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        ItemStack marked = entity.getMarkedItem();
        if (marked.isEmpty() || !(marked.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        BlockState state = blockItem.getBlock().defaultBlockState();
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(state).getParticleIcon();
        // 强制从当前方块图集重新解析精灵，确保首次渲染时贴图已加载
        //noinspection resource
        sprite = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(sprite.contents().name());
        // 显式绑定方块纹理图集（与物品 GUI 渲染 renderGuiItem 一致）
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        // 强制至少 15 级方块光照，保留环境天空光
        int blockLight = Math.max(combinedLight & 0xFFFF, MIN_BLOCK_LIGHT);
        int light = (combinedLight & 0xFFFF0000) | blockLight;
        for (Direction side : SIDES) {
            renderSpriteOnFace(sprite, side, poseStack, buffer, light, combinedOverlay);
        }
    }

    /**
     * 在指定方块面上绘制一个居中矩形贴图
     */
    private void renderSpriteOnFace(TextureAtlasSprite sprite, Direction face, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        poseStack.pushPose();
        // 绕 Y 旋转使目标面朝向 +Z（Y 方向不变，贴图保持竖直）
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPose((new Matrix4f()).rotateYXZ(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f, 0, 0));
        poseStack.translate(-0.5f, 0, -0.5f);
        // 在 +Z 面（z=1）绘制居中矩形，略高于面避免 z-fighting
        float x1 = 0.5f - INSET;
        float x2 = 0.5f + INSET;
        float y1 = 0.5f - INSET;
        float y2 = 0.5f + INSET;
        float z = 1.002f;
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        Matrix4f mat = poseStack.last().pose();
        // 顶点顺序：左下、右下、右上、左上；V0=纹理顶部（对应 +Y），U0=纹理左侧
        addVertex(consumer, mat, x1, y1, z, sprite.getU0(), sprite.getV1(), light, overlay);
        addVertex(consumer, mat, x2, y1, z, sprite.getU1(), sprite.getV1(), light, overlay);
        addVertex(consumer, mat, x2, y2, z, sprite.getU1(), sprite.getV0(), light, overlay);
        addVertex(consumer, mat, x1, y2, z, sprite.getU0(), sprite.getV0(), light, overlay);
        poseStack.popPose();
    }

    private void addVertex(VertexConsumer consumer, Matrix4f mat, float x, float y, float z, float u, float v, int light, int overlay) {
        // 1.21.1：VertexConsumer API 重命名：vertex→addVertex、color→setColor、uv→setUv、
        // overlayCoords→setOverlay、uv2→setUv1、normal→setNormal，去除 endVertex() 终结调用
        consumer.addVertex(mat, x, y, z).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
    }
}
