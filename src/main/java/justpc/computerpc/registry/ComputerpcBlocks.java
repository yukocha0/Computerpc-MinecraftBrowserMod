package justpc.computerpc.registry;

import justpc.computerpc.Computerpc;
import justpc.computerpc.block.DisplayBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ComputerpcBlocks {
	public static final DisplayBlock DISPLAY_BLOCK = new DisplayBlock(blockProperties("display")
			.strength(3.5f)
			.requiresCorrectToolForDrops());

	private ComputerpcBlocks() {
	}

	public static void register() {
		register("display", DISPLAY_BLOCK);
	}

	private static <T extends Block> T register(String path, T block) {
		return Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, path), block);
	}

	private static BlockBehaviour.Properties blockProperties(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, path);
		return BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id));
	}
}
