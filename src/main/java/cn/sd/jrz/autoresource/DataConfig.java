package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.setup.Registration;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public abstract class DataConfig {
    public static final DataConfig ENERGY_GENERATOR_FE = new DataConfig(1, Long.MAX_VALUE, 1, 1) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.ENERGY_GENERATOR_FE_ENTITY.get();
        }
    };
    public static final DataConfig LIQUID_GENERATOR_WATER = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.LIQUID_GENERATOR_WATER_ENTITY.get();
        }

        @Override
        public Fluid getFluid() {
            return Fluids.WATER;
        }
    };
    public static final DataConfig LIQUID_GENERATOR_LAVA = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.LIQUID_GENERATOR_LAVA_ENTITY.get();
        }

        @Override
        public Fluid getFluid() {
            return Fluids.LAVA;
        }
    };

    public static final DataConfig BLOCK_GENERATOR_DIRT = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_DIRT_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_COBBLESTONE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_COBBLESTONE_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_STONE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_STONE_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_SMOOTH_STONE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_SMOOTH_STONE_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_CLAY = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_CLAY_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_SAND = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_SAND_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_GRAVEL = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_GRAVEL_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_GRANITE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_GRANITE_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_DIORITE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_DIORITE_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_ANDESITE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_ANDESITE_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_CALCITE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_CALCITE_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_TUFF = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_TUFF_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_COBBLED_DEEPSLATE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_COBBLED_DEEPSLATE_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_PRISMARINE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_PRISMARINE_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_OBSIDIAN = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_OBSIDIAN_ENTITY.get();
        }
    };

    public static final DataConfig BLOCK_GENERATOR_NETHERRACK = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_NETHERRACK_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_SOUL_SAND = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_SOUL_SAND_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_SOUL_SOIL = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_SOUL_SOIL_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_BLACKSTONE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_BLACKSTONE_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_BASALT = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_BASALT_ENTITY.get();
        }
    };
    public static final DataConfig BLOCK_GENERATOR_END_STONE = new DataConfig(50, Long.MAX_VALUE, 10, 50) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_END_STONE_ENTITY.get();
        }
    };

    public final long min;
    public final long max;
    public final long second;
    public final long step;

    public DataConfig(long min, long max, long second, long step) {
        this.min = min;
        this.max = max;
        this.second = second;
        this.step = step;
    }

    public abstract BlockEntityType<?> getEntityType();

    public Fluid getFluid() {
        return Fluids.EMPTY;
    }
}
