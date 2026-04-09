package justpc.computerpc;

import justpc.computerpc.block.DisplayBlock;
import justpc.computerpc.client.BrowserBootstrap;
import justpc.computerpc.client.DisplayBrowserManager;
import justpc.computerpc.client.render.DisplayBlockEntityRenderer;
import justpc.computerpc.client.screen.RemoteBrowserScreen;
import justpc.computerpc.item.RemoteItem;
import justpc.computerpc.network.ComputerpcPayloads;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import justpc.computerpc.registry.ComputerpcBlockEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ComputerpcClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BrowserBootstrap.initialize();
		BlockEntityRendererRegistry.register(ComputerpcBlockEntities.DISPLAY, DisplayBlockEntityRenderer::new);

		ClientPlayNetworking.registerGlobalReceiver(ComputerpcPayloads.BrowserInputS2C.TYPE, (payload, context) ->
				context.client().execute(() -> DisplayBrowserManager.applyRemoteInput(payload)));

		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (!(player.getItemInHand(hand).getItem() instanceof RemoteItem)) {
				return InteractionResult.PASS;
			}

			if (!level.isClientSide()) {
				return InteractionResult.PASS;
			}

			if (Minecraft.getInstance().hitResult instanceof BlockHitResult hitResult) {
				BlockState state = level.getBlockState(hitResult.getBlockPos());
				if (player.isShiftKeyDown() && state.getBlock() instanceof DisplayBlock && hitResult.getDirection() == state.getValue(DisplayBlock.FACING)) {
					return InteractionResult.PASS;
				}
			}

			Minecraft.getInstance().setScreen(new RemoteBrowserScreen());
			return InteractionResult.SUCCESS;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			BrowserBootstrap.tick(client);
			DisplayBrowserManager.tick(client);
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> BrowserBootstrap.shutdown());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DisplayBrowserManager.closeAll());
	}
}
