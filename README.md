
# AutoResource

Added some machines that automatically generate resources, supporting the generation of FE, Water, Lava, and various types of stones.

## Generator
- The maximum power generation is 9,223,372,036,854,775,807 FE/t
- The default initial power generation is 1 FE/t. Every second, the power generation increases by 1 FE/t, and you need a beacon to reach the max power
- The generator will actively transmit electricity evenly to the six veneers, or other MOD wires can be used to extract FE
- Synthetic materials require redstone and smooth stone
- In the case of redstone activation, if there is a beacon below, the increased power generation per second will be an additional 1% of the current power generation

## Fluid machine
- Support the generation of Water and Lava
- The maximum output is 9,223,372,036,854,775.807 B/t
- The default initial production is 0.05 B/t, which is 1 B/second. Every 10 seconds, the production increases by 0.05 B/t
- Fluid will actively transfer fluid evenly to the six veneers, or other MOD pipelines can be used to extract fluid
- Synthetic materials require water buckets/lava buckets and smooth stones
- In the case of redstone activation, 4 attempts will be made per second to place fluid into the air block below the machine

## Block machine
- Support the production of dirt, cobblestone, stone, smooth stone, clay, sand, gravel, granite, diorite, andesite, calcite, tuff, cobbled deepslate, prismarine, obsidian, netherrack, soul sand, soul soil, blackstone, basalt, and end stone
- The maximum output is 9,223,372,036,854,775.807 Block/t
- The default initial output is 0.05 Block/t, which is 1 Block/second. Every 10 seconds, the output increases by 0.05 Block/t
- The block machine actively transfers blocks evenly to the six veneers, or can use other MOD pipelines to extract blocks
- Synthetic materials require water buckets, lava buckets, corresponding products, and smooth stones
- In the case of redstone activation, 4 attempts will be made per second to place a block into the air block below the machine

## Image

![all machine image](https://github.com/1144042967/auto-resource/raw/master/docs/block.png)


# AutoResource

添加了一些自动生成资源的机器，支持生成FE、水、岩浆、多种石头。

## 发电机

- 最大发电量为 922,3372,0368,5477,5807 FE/t
- 默认初始发电量为 1 FE/t，每过 1 秒，发电量增加 1 FE/t，你需要一个信标来达到最大发电功率
- 发电机会主动向六个贴面均匀传输电量，也可以使用其他MOD的导线抽取电量
- 合成材料需要红石和平滑石头
- 在红石激活的情况下，如果在发电机下方有一个信标，发电量每秒将额外增加当前发电量的1%

## 流体机

- 支持产生水和岩浆
- 最大产量为 9223,3720,3685,4775.807 桶/t
- 默认初始产量为 0.05 桶/t，也就是 1 桶/秒，每过 10 秒，产量增加 0.05 桶/t
- 流体机会主动向六个贴面均匀传输流体，也可以使用其他MOD的管道抽取流体
- 合成材料需要水桶/岩浆桶和平滑石头
- 在红石激活的情况下，每秒将尝试 4 次，向机器下方的空气方块中放置流体

## 方块机

- 支持产生泥土、圆石、石头、平滑石头、粘土、沙子、沙砾、花岗岩、闪长岩、安山岩、方解石、凝灰岩、深板岩圆石、海晶石、黑曜石、下界岩、灵魂沙、灵魂土、黑石、玄武岩、末地石
- 最大产量为 9223,3720,3685,4775.807 个/t
- 默认初始产量为 0.05 个/t，也就是 1 个/秒，每过 10 秒，产量增加 0.05 个/t
- 方块机会主动向六个贴面均匀传输方块，也可以使用其他MOD的管道抽取方块
- 合成材料需要水桶、岩浆桶、对应产物和平滑石头
- 在红石激活的情况下，每秒将尝试 4 次，向机器下方的空气方块中放置方块



## 图片

![所有机器的图片](https://gitee.com/scrambled_egg_with_eek/auto-resource/raw/master/docs/block.png)