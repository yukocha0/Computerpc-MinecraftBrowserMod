package justpc.computerpc.registry;

import justpc.computerpc.Computerpc;
import justpc.computerpc.item.RemoteItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ComputerpcItems {
	public static final Item DISPLAY_BLOCK = new BlockItem(ComputerpcBlocks.DISPLAY_BLOCK, itemProperties("display").useBlockDescriptionPrefix());
	public static final Item REMOTE = new RemoteItem(itemProperties("remote").stacksTo(1));

	private ComputerpcItems() {
	}

	public static void register() {
		register("display", DISPLAY_BLOCK);
		register("remote", REMOTE);
	}

	private static <T extends Item> T register(String path, T item) {
		return Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, path), item);
	}

	private static Item.Properties itemProperties(String path) {
		Identifier id = Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, path);
		return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
	}
}
