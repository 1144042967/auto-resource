package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/**
 * 方块生成机的方块实体渲染器。
 * <p>
 * 26.x 适配：使用 {@code TextureAtlas.LOCATION_BLOCKS} 仍按 26.x 方式获取贴图；
 * 渲染逻辑与 1.21.1 保持一致。
 */
@OnlyIn(Dist.CLIENT)
public class BlockGeneratorRenderer implements BlockEntityRenderer<BlockGeneratorEntity> {
    private static final float[] SIDE_ROT_Y = {0, 0, 2, 0, 3, 1};
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final float INSET = 0.275f;
    private static final int MIN_BLOCK_LIGHT = 15 << 4;

    public BlockGeneratorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@Nonnull BlockGeneratorEntity entity, float partialTick, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        ItemStack marked = entity.getMarkedItem();
        if (marked.isEmpty() || !(marked.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        BlockState state = blockItem.getBlock().defaultBlockState();
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer().getBlockModel(state).getParticleIcon(ModelData.EMPTY);
        if (sprite == null) {
            return;
        }
        sprite = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(sprite.contents().name());
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        int blockLight = Math.max(combinedLight & 0xFFFF, MIN_BLOCK_LIGHT);
        int light = (combinedLight & 0xFFFF0000) | blockLight;
        for (Direction side : SIDES) {
            renderSpriteOnFace(sprite, side, poseStack, buffer, light, combinedOverlay);
        }
    }

    private void renderSpriteOnFace(TextureAtlasSprite sprite, Direction face, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPose((new Matrix4f()).rotateYXZ(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f, 0, 0));
        poseStack.translate(-0.5f, 0, -0.5f);
        float x1 = 0.5f - INSET;
        float x2 = 0.5f + INSET;
        float y1 = 0.5f - INSET;
        float y2 = 0.5f + INSET;
        float z = 1.002f;
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        Matrix4f mat = poseStack.last().pose();
        addVertex(consumer, mat, x1, y1, z, sprite.getU0(), sprite.getV1(), light, overlay);
        addVertex(consumer, mat, x2, y1, z, sprite.getU1(), sprite.getV1(), light, overlay);
        addVertex(consumer, mat, x2, y2, z, sprite.getU1(), sprite.getV0(), light, overlay);
        addVertex(consumer, mat, x1, y2, z, sprite.getU0(), sprite.getV0(), light, overlay);
        poseStack.popPose();
    }

    private void addVertex(VertexConsumer consumer, Matrix4f mat, float x, float y, float z, float u, float v, int light, int overlay) {
        consumer.addVertex(mat, x, y, z).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(u, v).setOverlay(overlay).setUv2(light & 0xFFFF, light >> 16).setNormal(0, 0, 1);
    }
}
