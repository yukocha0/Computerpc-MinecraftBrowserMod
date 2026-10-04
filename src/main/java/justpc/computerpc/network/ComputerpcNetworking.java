package justpc.computerpc.network;

import justpc.computerpc.blockentity.DisplayBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class ComputerpcNetworking {
	public static final int EVENT_MOUSE_MOVE = 0;
	public static final int EVENT_MOUSE_PRESS = 1;
	public static final int EVENT_MOUSE_RELEASE = 2;
	public static final int EVENT_MOUSE_SCROLL = 3;
	public static final int EVENT_KEY_PRESS = 4;
	public static final int EVENT_KEY_RELEASE = 5;
	public static final int EVENT_CHAR_TYPED = 6;

	private ComputerpcNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(ComputerpcPayloads.BrowserNavigateC2S.TYPE, ComputerpcPayloads.BrowserNavigateC2S.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ComputerpcPayloads.BrowserNavigateC2S.TYPE, (payload, context) -> {
			if (!(context.player().level().getBlockEntity(payload.pos()) instanceof DisplayBlockEntity display)) {
				return;
			}

			if (!isControllingPlayer(context.player(), display)) {
				return;
			}

			display.pushClusterNavigation(payload.url());
		});
	}

	private static boolean isControllingPlayer(ServerPlayer player, DisplayBlockEntity display) {
		return player.distanceToSqr(Vec3.atCenterOf(display.getBlockPos())) <= 2500.0;
	}
}
