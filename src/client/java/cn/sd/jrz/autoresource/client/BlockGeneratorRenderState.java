package cn.sd.jrz.autoresource.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块生成机的方块实体渲染状态：保存标记槽中物品对应的方块状态，
 * 供渲染器在四个侧面绘制标记方块贴图（1.21.11 渲染管线重构后的状态对象）。
 */
public class BlockGeneratorRenderState extends BlockEntityRenderState {
    /**
     * 标记槽物品对应的方块状态（未标记/非方块物品时为 null，此时不渲染）
     */
    public BlockState markedBlockState;
}
