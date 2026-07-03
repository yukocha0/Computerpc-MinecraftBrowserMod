package justpc.computerpc.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public final class RemoteItem extends Item {
	public RemoteItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> textAdder, TooltipFlag flag) {
		textAdder.accept(Component.literal("Right click to manage nearby screens").withStyle(ChatFormatting.GRAY));
		textAdder.accept(Component.literal("Sneak right click the front of a display to power it").withStyle(ChatFormatting.DARK_GRAY));
	}
}
