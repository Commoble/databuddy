package commoble.databuddy.examplecontent;

import java.util.HashMap;

import com.mojang.serialization.JsonOps;

import net.commoble.databuddy.datagen.BlockStateFile;
import net.commoble.databuddy.datagen.SimpleModel;
import net.commoble.databuddy.datagen.BlockStateFile.Case;
import net.commoble.databuddy.datagen.BlockStateFile.Model;
import net.commoble.databuddy.datagen.BlockStateFile.Multipart;
import net.commoble.databuddy.datagen.BlockStateFile.OrCase;
import net.commoble.databuddy.datagen.BlockStateFile.PropertyValue;
import net.commoble.databuddy.datagen.BlockStateFile.Variants;
import net.commoble.databuddy.datagen.BlockStateFile.WhenApply;
import net.minecraft.Util;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
import net.neoforged.fml.common.Mod.EventBusSubscriber;
import net.neoforged.fml.common.Mod.EventBusSubscriber.Bus;
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
	private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, DataBuddyExampleMod.MODID);
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DataBuddyExampleMod.MODID);
	private static final DeferredHolder<Block,Block> RANDOM_SPONGE = BLOCKS.register("random_sponge", () ->
		new Block(BlockBehaviour.Properties.of()));
	private static final DeferredHolder<Block,SlabBlock> SPONGE_SLAB = BLOCKS.register("sponge_slab", () ->
		new SlabBlock(BlockBehaviour.Properties.of()));
	private static final DeferredHolder<Block,RedStoneWireBlock> WHITESTONE_DUST = BLOCKS.register("whitestone_wire", () ->
		new RedStoneWireBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.REDSTONE_WIRE)));
	
	static
	{
		ITEMS.register("random_sponge", () ->
			new BlockItem(RANDOM_SPONGE.get(), new Item.Properties()));
		ITEMS.register("sponge_slab", () ->
			new BlockItem(SPONGE_SLAB.get(), new Item.Properties()));
		ITEMS.register("whitestone", () ->
			new BlockItem(WHITESTONE_DUST.get(), new Item.Properties()));
	}
	
	@SubscribeEvent
	public static void onModConstruction(FMLConstructModEvent event)
	{
		IEventBus modBus = ((FMLModContainer)(ModList.get().getModContainerById(DataBuddyExampleMod.MODID).get())).getEventBus();
		BLOCKS.register(modBus);
		ITEMS.register(modBus);
	}
	@SubscribeEvent
	public static void onGatherData(GatherDataEvent event)
	{
		ResourceLocation blockSpongeSlab = new ResourceLocation(DataBuddyExampleMod.MODID, "block/sponge_slab");
		ResourceLocation blockSpongeSlabTop = new ResourceLocation(DataBuddyExampleMod.MODID, "block/sponge_slab_top");
		ResourceLocation blockSlab = new ResourceLocation("block/slab");
		ResourceLocation blockSlabTop = new ResourceLocation("block/slab_top");
		ResourceLocation blockSponge = new ResourceLocation("block/sponge");
		ResourceLocation itemRandomSponge = new ResourceLocation(DataBuddyExampleMod.MODID, "item/random_sponge");
		ResourceLocation itemSpongeSlab = new ResourceLocation(DataBuddyExampleMod.MODID, "item/sponge_slab");
		ResourceLocation itemWhitestone = new ResourceLocation(DataBuddyExampleMod.MODID, "item/whitestone");
		ResourceLocation itemGenerated = new ResourceLocation("item/generated");
		ResourceLocation itemRedstone = new ResourceLocation("item/redstone");
	
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
		BlockStateFile.addDataProvider(event, DataBuddyExampleMod.MODID, JsonOps.INSTANCE, Util.make(new HashMap<ResourceLocation, BlockStateFile>(), map ->
		{
			// random models example
			map.put(RANDOM_SPONGE.getId(),
				BlockStateFile.variants(
					Variants.always(
						Model.create(blockSponge),
						Model.create(blockSponge, BlockModelRotation.X0_Y90),
						Model.create(blockSponge, BlockModelRotation.X0_Y180),
						Model.create(blockSponge, BlockModelRotation.X0_Y270))));
			
			// variant example
			map.put(SPONGE_SLAB.getId(),
				BlockStateFile.variants(Variants.builder()
					.addVariant(
						PropertyValue.create(SlabBlock.TYPE, SlabType.BOTTOM),
						Model.create(blockSpongeSlab))
					.addVariant(
						PropertyValue.create(SlabBlock.TYPE, SlabType.DOUBLE),
						Model.create(blockSponge))
					.addVariant(
						PropertyValue.create(SlabBlock.TYPE, SlabType.TOP),
						Model.create(blockSpongeSlabTop))));
			
			// multipart example
			map.put(WHITESTONE_DUST.getId(),
				BlockStateFile.multipart(Multipart.builder()
					.addWhenApply(WhenApply.or(
						OrCase.builder()
							.addCase(Case.builder()
								.addCondition(RedStoneWireBlock.EAST, RedstoneSide.NONE)
								.addCondition(RedStoneWireBlock.NORTH, RedstoneSide.NONE)
								.addCondition(RedStoneWireBlock.SOUTH, RedstoneSide.NONE)
								.addCondition(RedStoneWireBlock.WEST, RedstoneSide.NONE))
							.addCase(Case.builder()
								.addCondition(RedStoneWireBlock.EAST, RedstoneSide.SIDE, RedstoneSide.UP)
								.addCondition(RedStoneWireBlock.NORTH, RedstoneSide.SIDE, RedstoneSide.UP))
							.addCase(Case.builder()
								.addCondition(RedStoneWireBlock.EAST, RedstoneSide.SIDE, RedstoneSide.UP)
								.addCondition(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE, RedstoneSide.UP))
							.addCase(Case.builder()
								.addCondition(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE, RedstoneSide.UP)
								.addCondition(RedStoneWireBlock.WEST, RedstoneSide.SIDE, RedstoneSide.UP))
							.addCase(Case.builder()
								.addCondition(RedStoneWireBlock.NORTH, RedstoneSide.SIDE, RedstoneSide.UP)
								.addCondition(RedStoneWireBlock.WEST, RedstoneSide.SIDE, RedstoneSide.UP)),
						Model.create(new ResourceLocation("block/redstone_dust_dot"))))
					.addWhenApply(WhenApply.when(
						Case.create(RedStoneWireBlock.NORTH, RedstoneSide.SIDE, RedstoneSide.UP),
						Model.create(new ResourceLocation("block/redstone_dust_side0"))))
					.addWhenApply(WhenApply.when(
						Case.create(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE, RedstoneSide.UP),
						Model.create(new ResourceLocation("block/redstone_dust_side_alt0"))))
					.addWhenApply(WhenApply.when(
						Case.create(RedStoneWireBlock.EAST, RedstoneSide.SIDE, RedstoneSide.UP),
						Model.create(new ResourceLocation("block/redstone_dust_side_alt1"), BlockModelRotation.X0_Y270)))
					.addWhenApply(WhenApply.when(
						Case.create(RedStoneWireBlock.WEST, RedstoneSide.SIDE, RedstoneSide.UP),
						Model.create(new ResourceLocation("block/redstone_dust_side1"), BlockModelRotation.X0_Y270)))
					.addWhenApply(WhenApply.when(
						Case.create(RedStoneWireBlock.NORTH, RedstoneSide.UP),
						Model.create(new ResourceLocation("block/redstone_dust_up"))))
					.addWhenApply(WhenApply.when(
						Case.create(RedStoneWireBlock.EAST, RedstoneSide.UP),
						Model.create(new ResourceLocation("block/redstone_dust_up"), BlockModelRotation.X0_Y90)))
					.addWhenApply(WhenApply.when(
						Case.create(RedStoneWireBlock.SOUTH, RedstoneSide.UP),
						Model.create(new ResourceLocation("block/redstone_dust_up"), BlockModelRotation.X0_Y180)))
					.addWhenApply(WhenApply.when(
						Case.create(RedStoneWireBlock.WEST, RedstoneSide.UP),
						Model.create(new ResourceLocation("block/redstone_dust_up"), BlockModelRotation.X0_Y270)))));
			
		}));
	}
}
