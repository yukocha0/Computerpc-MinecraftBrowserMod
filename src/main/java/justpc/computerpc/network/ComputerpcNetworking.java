package justpc.computerpc.network;

import com.mojang.serialization.DataResult;
import justpc.computerpc.blockentity.DisplayBlockEntity;
import justpc.computerpc.browser.DisplayStateData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;

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
		PayloadTypeRegistry.playC2S().register(ComputerpcPayloads.DisplayConfigC2S.TYPE, ComputerpcPayloads.DisplayConfigC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(ComputerpcPayloads.BrowserNavigateC2S.TYPE, ComputerpcPayloads.BrowserNavigateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(ComputerpcPayloads.BrowserInputC2S.TYPE, ComputerpcPayloads.BrowserInputC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(ComputerpcPayloads.BrowserInputS2C.TYPE, ComputerpcPayloads.BrowserInputS2C.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(ComputerpcPayloads.DisplayConfigC2S.TYPE, (payload, context) -> {
			if (!(context.player().level().getBlockEntity(payload.pos()) instanceof DisplayBlockEntity display)) {
				return;
			}

			if (!isControllingPlayer(context.player(), display)) {
				return;
			}

			DataResult<DisplayStateData> result = DisplayStateData.CODEC.parse(NbtOps.INSTANCE, payload.data());
			result.result().ifPresent(display::setClusterScreenState);
		});

		ServerPlayNetworking.registerGlobalReceiver(ComputerpcPayloads.BrowserNavigateC2S.TYPE, (payload, context) -> {
			if (!(context.player().level().getBlockEntity(payload.pos()) instanceof DisplayBlockEntity display)) {
				return;
			}

			if (!isControllingPlayer(context.player(), display)) {
				return;
			}

			display.pushClusterNavigation(payload.url());
		});

		ServerPlayNetworking.registerGlobalReceiver(ComputerpcPayloads.BrowserInputC2S.TYPE, (payload, context) -> {
			if (!(context.player().level().getBlockEntity(payload.pos()) instanceof DisplayBlockEntity display)) {
				return;
			}

			if (!isControllingPlayer(context.player(), display)) {
				return;
			}

			ComputerpcPayloads.BrowserInputS2C forwarded = new ComputerpcPayloads.BrowserInputS2C(
					payload.pos(),
					payload.eventType(),
					payload.x(),
					payload.y(),
					payload.button(),
					payload.keyCode(),
					payload.scanCode(),
					payload.modifiers(),
					payload.codePoint(),
					payload.scrollDelta()
			);

			for (ServerPlayer player : PlayerLookup.tracking(context.player().level(), payload.pos())) {
				if (player == context.player()) {
					continue;
				}
				ServerPlayNetworking.send(player, forwarded);
			}
		});
	}

	private static boolean isControllingPlayer(ServerPlayer player, DisplayBlockEntity display) {
		return player.distanceToSqr(display.getBlockPos().getCenter()) <= 2500.0;
	}
}
