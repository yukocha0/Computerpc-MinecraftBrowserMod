package justpc.computerpc.network;

import justpc.computerpc.Computerpc;
import justpc.computerpc.blockentity.DisplayBlockEntity;
import justpc.computerpc.browser.DisplayStateData;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class ComputerpcNetworking {
	public static final int EVENT_MOUSE_MOVE = 0;
	public static final int EVENT_MOUSE_PRESS = 1;
	public static final int EVENT_MOUSE_RELEASE = 2;
	public static final int EVENT_MOUSE_SCROLL = 3;
	public static final int EVENT_KEY_PRESS = 4;
	public static final int EVENT_KEY_RELEASE = 5;
	public static final int EVENT_CHAR_TYPED = 6;
	private static final double CONTROL_RANGE_SQR = 2500.0D;
	private static final StreamCodec<RegistryFriendlyByteBuf, DisplayStateData> DISPLAY_STATE_CODEC = ByteBufCodecs.fromCodec(DisplayStateData.CODEC).cast();

	private ComputerpcNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(DisplayConfigC2S.TYPE, DisplayConfigC2S.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(BrowserNavigateC2S.TYPE, BrowserNavigateC2S.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(DisplayConfigC2S.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!canControl(player, payload.pos())) {
				return;
			}

			if (player.level().getBlockEntity(payload.pos()) instanceof DisplayBlockEntity display) {
				display.setClusterScreenState(payload.state());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(BrowserNavigateC2S.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!canControl(player, payload.pos())) {
				return;
			}

			if (player.level().getBlockEntity(payload.pos()) instanceof DisplayBlockEntity display) {
				display.pushClusterNavigation(payload.url());
			}
		});
	}

	private static boolean canControl(ServerPlayer player, BlockPos pos) {
		return pos.getCenter().distanceToSqr(player.position()) <= CONTROL_RANGE_SQR;
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, path);
	}

	public record DisplayConfigC2S(BlockPos pos, DisplayStateData state) implements CustomPacketPayload {
		public static final Type<DisplayConfigC2S> TYPE = new Type<>(id("display_config"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DisplayConfigC2S> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC,
				DisplayConfigC2S::pos,
				DISPLAY_STATE_CODEC,
				DisplayConfigC2S::state,
				DisplayConfigC2S::new
		);

		@Override
		public Type<DisplayConfigC2S> type() {
			return TYPE;
		}
	}

	public record BrowserNavigateC2S(BlockPos pos, String url) implements CustomPacketPayload {
		public static final Type<BrowserNavigateC2S> TYPE = new Type<>(id("browser_navigate"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BrowserNavigateC2S> STREAM_CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC,
				BrowserNavigateC2S::pos,
				ByteBufCodecs.stringUtf8(2048),
				BrowserNavigateC2S::url,
				BrowserNavigateC2S::new
		);

		@Override
		public Type<BrowserNavigateC2S> type() {
			return TYPE;
		}
	}
}
