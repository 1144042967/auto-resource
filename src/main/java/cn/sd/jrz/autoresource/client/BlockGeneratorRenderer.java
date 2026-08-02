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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/**
 * 方块生成机的方块实体渲染器。
 * <p>
 * 当机器标记槽中有物品时，在除上下外的四个侧面（北/南/东/西）各绘制一个标记方块的贴图矩形，
 * 表示该机器当前生成的方块种类。
 * <p>
 * 实现说明：
 * - 直接取标记方块的精灵（getParticleIcon）用 {@link RenderType#cutout()} 画平面四边形，
 * 与方块本体共用方块图集，避免 {@code ItemRenderer} 首次渲染时贴图未加载的问题；
 * - 强制至少 15 级方块光照，保证贴图清晰可见。
 */
@OnlyIn(Dist.CLIENT)
public class BlockGeneratorRenderer implements BlockEntityRenderer<BlockGeneratorEntity> {
    /**
     * 旋转角度倍数（DOWN/UP/NORTH/SOUTH/WEST/EAST）：旋转后目标面朝向 +Z，
     * 与 StorageDrawers 的 alignRendering 一致
     */
    private static final float[] SIDE_ROT_Y = {0, 0, 2, 0, 3, 1};
    /**
     * 需要显示物品的四个侧面（上下两面除外）
     */
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    /**
     * 面的四周留边比例（居中矩形约占面 55%，即贴图大小为面的 0.55 倍）
     */
    private static final float INSET = 0.275f;
    /**
     * 强制的最低方块光照（15 级 = 全亮），避免贴图太暗看不清
     */
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
        // 强制从当前方块图集重新解析精灵，确保首次渲染时贴图已加载
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
        poseStack.mulPoseMatrix((new Matrix4f()).rotateYXZ(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f, 0, 0));
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
        consumer.vertex(mat, x, y, z).color(1.0f, 1.0f, 1.0f, 1.0f).uv(u, v).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
    }
}
