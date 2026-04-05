package justpc.computerpc;

import justpc.computerpc.block.DisplayBlock;
import justpc.computerpc.blockentity.DisplayBlockEntity;
import justpc.computerpc.item.RemoteItem;
import justpc.computerpc.network.ComputerpcNetworking;
import justpc.computerpc.registry.ComputerpcBlockEntities;
import justpc.computerpc.registry.ComputerpcBlocks;
import justpc.computerpc.registry.ComputerpcItems;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class Computerpc implements ModInitializer {
	public static final String MOD_ID = "computerpc";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final List<ResourceKey<Recipe<?>>> STARTING_RECIPE_IDS = List.of(
			ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MOD_ID, "display")),
			ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MOD_ID, "remote"))
	);

	@Override
	public void onInitialize() {
		ComputerpcBlocks.register();
		ComputerpcItems.register();
		ComputerpcBlockEntities.register();
		ComputerpcNetworking.register();
		registerItemGroups();
		registerInteractionHooks();
		registerRecipeUnlocks();
	}

	private static void registerItemGroups() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(output -> output.accept(ComputerpcBlocks.DISPLAY_BLOCK));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(output -> output.accept(ComputerpcItems.REMOTE));
	}

	private static void registerInteractionHooks() {
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}

			BlockState state = level.getBlockState(hitResult.getBlockPos());
			if (!(state.getBlock() instanceof DisplayBlock)) {
				return InteractionResult.PASS;
			}

			Direction front = state.getValue(DisplayBlock.FACING);
			if (hitResult.getDirection() != front) {
				return InteractionResult.PASS;
			}

			if (player.isShiftKeyDown()) {
				if (!level.isClientSide() && level.getBlockEntity(hitResult.getBlockPos()) instanceof DisplayBlockEntity display) {
					display.toggleClusterPower();
				}
				return InteractionResult.SUCCESS;
			}

			if (player.getItemInHand(hand).getItem() instanceof RemoteItem) {
				return InteractionResult.PASS;
			}

			return InteractionResult.FAIL;
		});

		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}

			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof DisplayBlock)) {
				return InteractionResult.PASS;
			}

			return direction == state.getValue(DisplayBlock.FACING) ? InteractionResult.FAIL : InteractionResult.PASS;
		});
	}

	private static void registerRecipeUnlocks() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ArrayList<RecipeHolder<?>> recipes = new ArrayList<>(STARTING_RECIPE_IDS.size());
			for (ResourceKey<Recipe<?>> recipeId : STARTING_RECIPE_IDS) {
				server.getRecipeManager().byKey(recipeId).ifPresent(recipes::add);
			}

			if (!recipes.isEmpty()) {
				handler.player.awardRecipes(recipes);
			}
		});
	}
}
