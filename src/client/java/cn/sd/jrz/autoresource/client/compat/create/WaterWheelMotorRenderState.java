package cn.sd.jrz.autoresource.client.compat.create;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

/**
 * 水车马达的方块实体渲染状态：保存转速与输出方向，
 * 供渲染器在四面侧绘制转速文字（26.1.2 渲染管线重构后的状态对象）。
 */
public class WaterWheelMotorRenderState extends BlockEntityRenderState {
    /**
     * 当前转速（由机内水车数量决定）
     */
    public int speed;
    /**
     * 输出方向（决定哪些面显示转速文字）
     */
    public Direction outputFace = Direction.NORTH;
}