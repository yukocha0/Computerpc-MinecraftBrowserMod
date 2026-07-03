package justpc.computerpc.registry;

import justpc.computerpc.Computerpc;
import justpc.computerpc.blockentity.DisplayBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ComputerpcBlockEntities {
	public static final BlockEntityType<DisplayBlockEntity> DISPLAY = FabricBlockEntityTypeBuilder.create(DisplayBlockEntity::new, ComputerpcBlocks.DISPLAY_BLOCK).build();

	private ComputerpcBlockEntities() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, "display"), DISPLAY);
	}
}
