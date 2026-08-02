package cn.sd.jrz.autoresource.client;

import cn.sd.jrz.autoresource.entities.BlockGeneratorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/**
 * 方块生成机的方块实体渲染器。
 * <p>
 * 当机器标记槽中有物品时，在除上下外的四个侧面（北/南/东/西）各渲染一次标记物品，
 * 表示该机器当前生成的方块种类。渲染方式参考 StorageDrawers：把物品模型拍扁后贴在方块面上。
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

    public BlockGeneratorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@Nonnull BlockGeneratorEntity entity, float partialTick, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        ItemStack marked = entity.getMarkedItem();
        if (marked.isEmpty()) {
            return;
        }
        for (Direction side : SIDES) {
            renderItemOnFace(marked, side, poseStack, buffer, combinedLight, combinedOverlay);
        }
    }

    /**
     * 把一个物品拍扁后渲染在指定方块面上（居中，约占面的 70%）
     */
    private void renderItemOnFace(ItemStack item, Direction face, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(item, null, null, 0);

        poseStack.pushPose();
        // 绕 Y 旋转使目标面朝向 +Z（Y 方向不变，物品保持竖直）
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPoseMatrix((new Matrix4f()).rotateYXZ(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f, 0, 0));
        poseStack.translate(-0.5f, 0, -0.5f);
        // 移到目标面表面并居中、拍扁
        poseStack.translate(0.5f, 0.5f, 1.0025f);
        poseStack.scale(0.6f, 0.6f, 0.001f);
        try {
            itemRenderer.render(item, ItemDisplayContext.GUI, false, poseStack, buffer, combinedLight, combinedOverlay, model);
        } catch (Exception ignored) {
            // 忽略渲染异常，避免影响其他渲染
        }
        poseStack.popPose();
    }
}
