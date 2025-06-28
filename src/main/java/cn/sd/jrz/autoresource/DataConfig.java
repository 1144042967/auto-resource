package cn.sd.jrz.autoresource;

import cn.sd.jrz.autoresource.setup.Registration;
import cn.sd.jrz.autoresource.util.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeConfigSpec;

public abstract class DataConfig {
    public static final DataConfig ENERGY_GENERATOR_FE = new DataConfig(Config.FE_MIN, Config.FE_MAX, Config.FE_SECOND, Config.FE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.ENERGY_GENERATOR_FE_ENTITY.get();
        }
    };
    public static final DataConfig LIQUID_GENERATOR_WATER = new DataConfig(Config.WATER_MIN, Config.WATER_MAX, Config.WATER_SECOND, Config.WATER_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.LIQUID_GENERATOR_WATER_ENTITY.get();
        }

        @Override
        public Fluid getFluid() {
            return Fluids.WATER;
        }

        @Override
        public Block getBlock() {
            return Blocks.WATER;
        }
    };
    public static final DataConfig LIQUID_GENERATOR_LAVA = new DataConfig(Config.LAVA_MIN, Config.LAVA_MAX, Config.LAVA_SECOND, Config.LAVA_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.LIQUID_GENERATOR_LAVA_ENTITY.get();
        }

        @Override
        public Fluid getFluid() {
            return Fluids.LAVA;
        }

        @Override
        public Block getBlock() {
            return Blocks.LAVA;
        }
    };

    public static final DataConfig BLOCK_GENERATOR_DIRT = new DataConfig(Config.DIRT_MIN, Config.DIRT_MAX, Config.DIRT_SECOND, Config.DIRT_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_DIRT_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.DIRT;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_COBBLESTONE = new DataConfig(Config.COBBLESTONE_MIN, Config.COBBLESTONE_MAX, Config.COBBLESTONE_SECOND, Config.COBBLESTONE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_COBBLESTONE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.COBBLESTONE;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_STONE = new DataConfig(Config.STONE_MIN, Config.STONE_MAX, Config.STONE_SECOND, Config.STONE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_STONE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.STONE;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_SMOOTH_STONE = new DataConfig(Config.SMOOTH_STONE_MIN, Config.SMOOTH_STONE_MAX, Config.SMOOTH_STONE_SECOND, Config.SMOOTH_STONE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_SMOOTH_STONE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.SMOOTH_STONE;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_CLAY = new DataConfig(Config.CLAY_MIN, Config.CLAY_MAX, Config.CLAY_SECOND, Config.CLAY_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_CLAY_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.CLAY;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_SAND = new DataConfig(Config.SAND_MIN, Config.SAND_MAX, Config.SAND_SECOND, Config.SAND_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_SAND_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.SAND;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_GRAVEL = new DataConfig(Config.GRAVEL_MIN, Config.GRAVEL_MAX, Config.GRAVEL_SECOND, Config.GRAVEL_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_GRAVEL_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.GRAVEL;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_GRANITE = new DataConfig(Config.GRANITE_MIN, Config.GRANITE_MAX, Config.GRANITE_SECOND, Config.GRANITE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_GRANITE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.GRANITE;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_DIORITE = new DataConfig(Config.DIORITE_MIN, Config.DIORITE_MAX, Config.DIORITE_SECOND, Config.DIORITE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_DIORITE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.DIORITE;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_ANDESITE = new DataConfig(Config.ANDESITE_MIN, Config.ANDESITE_MAX, Config.ANDESITE_SECOND, Config.ANDESITE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_ANDESITE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.ANDESITE;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_CALCITE = new DataConfig(Config.CALCITE_MIN, Config.CALCITE_MAX, Config.CALCITE_SECOND, Config.CALCITE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_CALCITE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.CALCITE;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_TUFF = new DataConfig(Config.TUFF_MIN, Config.TUFF_MAX, Config.TUFF_SECOND, Config.TUFF_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_TUFF_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.TUFF;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_COBBLED_DEEPSLATE = new DataConfig(Config.COBBLED_DEEPSLATE_MIN, Config.COBBLED_DEEPSLATE_MAX, Config.COBBLED_DEEPSLATE_SECOND, Config.COBBLED_DEEPSLATE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_COBBLED_DEEPSLATE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.COBBLED_DEEPSLATE;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_PRISMARINE = new DataConfig(Config.PRISMARINE_MIN, Config.PRISMARINE_MAX, Config.PRISMARINE_SECOND, Config.PRISMARINE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_PRISMARINE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.PRISMARINE;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_OBSIDIAN = new DataConfig(Config.OBSIDIAN_MIN, Config.OBSIDIAN_MAX, Config.OBSIDIAN_SECOND, Config.OBSIDIAN_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_OBSIDIAN_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.OBSIDIAN;
        }
    };

    public static final DataConfig BLOCK_GENERATOR_NETHERRACK = new DataConfig(Config.NETHERRACK_MIN, Config.NETHERRACK_MAX, Config.NETHERRACK_SECOND, Config.NETHERRACK_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_NETHERRACK_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.NETHERRACK;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_SOUL_SAND = new DataConfig(Config.SOUL_SAND_MIN, Config.SOUL_SAND_MAX, Config.SOUL_SAND_SECOND, Config.SOUL_SAND_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_SOUL_SAND_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.SOUL_SAND;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_SOUL_SOIL = new DataConfig(Config.SOUL_SOIL_MIN, Config.SOUL_SOIL_MAX, Config.SOUL_SOIL_SECOND, Config.SOUL_SOIL_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_SOUL_SOIL_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.SOUL_SOIL;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_BLACKSTONE = new DataConfig(Config.BLACKSTONE_MIN, Config.BLACKSTONE_MAX, Config.BLACKSTONE_SECOND, Config.BLACKSTONE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_BLACKSTONE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.BLACKSTONE;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_BASALT = new DataConfig(Config.BASALT_MIN, Config.BASALT_MAX, Config.BASALT_SECOND, Config.BASALT_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_BASALT_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.BASALT;
        }
    };
    public static final DataConfig BLOCK_GENERATOR_END_STONE = new DataConfig(Config.END_STONE_MIN, Config.END_STONE_MAX, Config.END_STONE_SECOND, Config.END_STONE_STEP) {
        @Override
        public BlockEntityType<?> getEntityType() {
            return Registration.BLOCK_GENERATOR_END_STONE_ENTITY.get();
        }

        @Override
        public Block getBlock() {
            return Blocks.END_STONE;
        }
    };

    private final ForgeConfigSpec.LongValue min;
    private final ForgeConfigSpec.LongValue max;
    private final ForgeConfigSpec.LongValue second;
    private final ForgeConfigSpec.LongValue step;

    public DataConfig(ForgeConfigSpec.LongValue min, ForgeConfigSpec.LongValue max, ForgeConfigSpec.LongValue second, ForgeConfigSpec.LongValue step) {
        this.min = min;
        this.max = max;
        this.second = second;
        this.step = step;
    }

    public long getMin() {
        return Tool.suit(min.get());
    }

    public long getMax() {
        return Tool.suit(max.get());
    }

    public long getSecond() {
        return Tool.suit(second.get());
    }

    public long getStep() {
        return Tool.suit(step.get());
    }

    public abstract BlockEntityType<?> getEntityType();

    public Fluid getFluid() {
        return Fluids.EMPTY;
    }

    public Block getBlock() {
        return Blocks.AIR;
    }
}
