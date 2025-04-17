package net.commoble.databuddy.examplecontent;

import java.util.HashMap;

import com.mojang.math.Quadrant;
import com.mojang.serialization.JsonOps;

import net.commoble.databuddy.datagen.BlockStateBuilder;
import net.commoble.databuddy.datagen.SimpleModel;
import net.minecraft.Util;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Example of using BlockStateFile and SimpleModel to datagen blockstate and model files
 */
@EventBusSubscriber(modid=DataBuddyExampleMod.MODID, bus=Bus.MOD)
public class BlockStateDataGenExampleMod
{
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DataBuddyExampleMod.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DataBuddyExampleMod.MODID);
	private static final DeferredHolder<Block,Block> RANDOM_SPONGE = BLOCKS.registerBlock("random_sponge", Block::new);
	private static final DeferredHolder<Block,SlabBlock> SPONGE_SLAB = BLOCKS.registerBlock("sponge_slab", SlabBlock::new);
	private static final DeferredHolder<Block,RedStoneWireBlock> WHITESTONE_DUST = BLOCKS.registerBlock("whitestone_wire", RedStoneWireBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_WIRE));
	
	static
	{
		ITEMS.registerItem("random_sponge", props ->
			new BlockItem(RANDOM_SPONGE.get(), props));
		ITEMS.registerItem("sponge_slab", props ->
			new BlockItem(SPONGE_SLAB.get(), props));
		ITEMS.registerItem("whitestone", props ->
			new BlockItem(WHITESTONE_DUST.get(), props));
	}
	
	@SubscribeEvent
	public static void onModConstruction(FMLConstructModEvent event)
	{
		IEventBus modBus = ((FMLModContainer)(ModList.get().getModContainerById(DataBuddyExampleMod.MODID).get())).getEventBus();
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
	}
	@SubscribeEvent
	public static void onGatherData(GatherDataEvent.Client event)
	{
		ResourceLocation blockSpongeSlab = ResourceLocation.fromNamespaceAndPath(DataBuddyExampleMod.MODID, "block/sponge_slab");
		ResourceLocation blockSpongeSlabTop = ResourceLocation.fromNamespaceAndPath(DataBuddyExampleMod.MODID, "block/sponge_slab_top");
		ResourceLocation blockSlab = ResourceLocation.withDefaultNamespace("block/slab");
		ResourceLocation blockSlabTop = ResourceLocation.withDefaultNamespace("block/slab_top");
		ResourceLocation blockSponge = ResourceLocation.withDefaultNamespace("block/sponge");
		ResourceLocation itemRandomSponge = ResourceLocation.fromNamespaceAndPath(DataBuddyExampleMod.MODID, "item/random_sponge");
		ResourceLocation itemSpongeSlab = ResourceLocation.fromNamespaceAndPath(DataBuddyExampleMod.MODID, "item/sponge_slab");
		ResourceLocation itemWhitestone = ResourceLocation.fromNamespaceAndPath(DataBuddyExampleMod.MODID, "item/whitestone");
		ResourceLocation itemGenerated = ResourceLocation.withDefaultNamespace("item/generated");
		ResourceLocation itemRedstone = ResourceLocation.withDefaultNamespace("item/redstone");
	
		// model generation example
		SimpleModel.addDataProvider(event, DataBuddyExampleMod.MODID, JsonOps.INSTANCE, Util.make(new HashMap<ResourceLocation, SimpleModel>(), map ->
		{
			map.put(blockSpongeSlab,
				SimpleModel.create(blockSlab)
					.addTexture("bottom", blockSponge)
					.addTexture("side", blockSponge)
					.addTexture("top", blockSponge));
			map.put(blockSpongeSlabTop,
				SimpleModel.create(blockSlabTop)
					.addTexture("bottom", blockSponge)
					.addTexture("side", blockSponge)
					.addTexture("top", blockSponge));
			map.put(itemRandomSponge,
				SimpleModel.create(blockSponge));
			map.put(itemSpongeSlab,
				SimpleModel.create(blockSpongeSlab));
			map.put(itemWhitestone,
				SimpleModel.create(itemGenerated)
					.addTexture("layer0", itemRedstone));
		}));

		// blockstate generation examples
		BlockStateBuilder.addDataProvider(event, Util.make(new HashMap<ResourceLocation, BlockModelDefinition>(), map ->
		{
			// random models example
			map.put(RANDOM_SPONGE.getId(), BlockStateBuilder.singleVariant(BlockStateBuilder.randomModels(
				new Weighted<>(BlockStateBuilder.model(blockSponge), 1),
				new Weighted<>(BlockStateBuilder.model(blockSponge, Quadrant.R0, Quadrant.R90), 1),
				new Weighted<>(BlockStateBuilder.model(blockSponge, Quadrant.R0, Quadrant.R180), 1),
				new Weighted<>(BlockStateBuilder.model(blockSponge, Quadrant.R0, Quadrant.R270), 1))));
			
			// variant example
			map.put(SPONGE_SLAB.getId(), BlockStateBuilder.variants(variants -> variants
				.addVariant(SlabBlock.TYPE, SlabType.BOTTOM, BlockStateBuilder.model(blockSpongeSlab))
				.addVariant(SlabBlock.TYPE, SlabType.DOUBLE, BlockStateBuilder.model(blockSponge))
				.addVariant(SlabBlock.TYPE, SlabType.TOP, BlockStateBuilder.model(blockSpongeSlabTop))));
			
			// multipart example
			map.put(WHITESTONE_DUST.getId(), BlockStateBuilder.multipart(multipart -> multipart
				.applyWhenOr(BlockStateBuilder.model(ResourceLocation.withDefaultNamespace("block/redstone_dust_dot")), or -> or
					.addAllPropertiesSubWhen(when -> when
						.addCondition(RedStoneWireBlock.EAST, RedstoneSide.NONE)
						.addCondition(RedStoneWireBlock.NORTH, RedstoneSide.NONE)
						.addCondition(RedStoneWireBlock.SOUTH, RedstoneSide.NONE)
						.addCondition(RedStoneWireBlock.WEST, RedstoneSide.NONE))
					.addAllPropertiesSubWhen(when -> when
						.addCondition(RedStoneWireBlock.EAST, RedstoneSide.SIDE, RedstoneSide.UP)
						.addCondition(RedStoneWireBlock.NORTH, RedstoneSide.SIDE, RedstoneSide.UP))
					.addAllPropertiesSubWhen(when -> when
						.addCondition(RedStoneWireBlock.EAST, RedstoneSide.SIDE, RedstoneSide.UP)
						.addCondition(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE, RedstoneSide.UP))
					.addAllPropertiesSubWhen(when -> when
						.addCondition(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE, RedstoneSide.UP)
						.addCondition(RedStoneWireBlock.WEST, RedstoneSide.SIDE, RedstoneSide.UP))
					.addAllPropertiesSubWhen(when -> when
						.addCondition(RedStoneWireBlock.NORTH, RedstoneSide.SIDE, RedstoneSide.UP)
						.addCondition(RedStoneWireBlock.WEST, RedstoneSide.SIDE, RedstoneSide.UP)))
				.applyWhen(BlockStateBuilder.model(ResourceLocation.withDefaultNamespace("block/redstone_dust_side0")), RedStoneWireBlock.NORTH, RedstoneSide.SIDE, RedstoneSide.UP)
				.applyWhen(BlockStateBuilder.model(ResourceLocation.withDefaultNamespace("block/redstone_dust_side_alt0")), RedStoneWireBlock.SOUTH, RedstoneSide.SIDE, RedstoneSide.UP)
				.applyWhen(BlockStateBuilder.model(ResourceLocation.withDefaultNamespace("block/redstone_dust_side_alt1"), Quadrant.R0, Quadrant.R270), RedStoneWireBlock.EAST, RedstoneSide.SIDE, RedstoneSide.UP)
				.applyWhen(BlockStateBuilder.model(ResourceLocation.withDefaultNamespace("block/redstone_dust_side1"), Quadrant.R0, Quadrant.R270), RedStoneWireBlock.WEST, RedstoneSide.SIDE, RedstoneSide.UP)
				.applyWhen(BlockStateBuilder.model(ResourceLocation.withDefaultNamespace("block/redstone_dust_up")), RedStoneWireBlock.NORTH, RedstoneSide.UP)
				.applyWhen(BlockStateBuilder.model(ResourceLocation.withDefaultNamespace("block/redstone_dust_up"), Quadrant.R0, Quadrant.R90), RedStoneWireBlock.EAST, RedstoneSide.UP)
				.applyWhen(BlockStateBuilder.model(ResourceLocation.withDefaultNamespace("block/redstone_dust_up"), Quadrant.R0, Quadrant.R180), RedStoneWireBlock.SOUTH, RedstoneSide.UP)
				.applyWhen(BlockStateBuilder.model(ResourceLocation.withDefaultNamespace("block/redstone_dust_up"), Quadrant.R0, Quadrant.R270), RedStoneWireBlock.WEST, RedstoneSide.UP)));			
		}));
	}
}
