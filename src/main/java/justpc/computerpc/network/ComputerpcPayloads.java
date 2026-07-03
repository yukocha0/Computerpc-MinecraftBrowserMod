package justpc.computerpc.network;

import justpc.computerpc.Computerpc;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ComputerpcPayloads {
	private ComputerpcPayloads() {
	}

	public record DisplayConfigC2S(BlockPos pos, CompoundTag data) implements CustomPacketPayload {
		public static final Type<DisplayConfigC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, "display_config"));
		public static final StreamCodec<RegistryFriendlyByteBuf, DisplayConfigC2S> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, DisplayConfigC2S::pos,
				ByteBufCodecs.COMPOUND_TAG, DisplayConfigC2S::data,
				DisplayConfigC2S::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record BrowserNavigateC2S(BlockPos pos, String url) implements CustomPacketPayload {
		public static final Type<BrowserNavigateC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, "browser_navigate"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BrowserNavigateC2S> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, BrowserNavigateC2S::pos,
				ByteBufCodecs.STRING_UTF8, BrowserNavigateC2S::url,
				BrowserNavigateC2S::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record BrowserInputC2S(BlockPos pos, int eventType, int x, int y, int button, int keyCode, int scanCode, int modifiers, int codePoint, double scrollDelta) implements CustomPacketPayload {
		public static final Type<BrowserInputC2S> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, "browser_input"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BrowserInputC2S> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, BrowserInputC2S::pos,
				ByteBufCodecs.VAR_INT, BrowserInputC2S::eventType,
				ByteBufCodecs.VAR_INT, BrowserInputC2S::x,
				ByteBufCodecs.VAR_INT, BrowserInputC2S::y,
				ByteBufCodecs.VAR_INT, BrowserInputC2S::button,
				ByteBufCodecs.VAR_INT, BrowserInputC2S::keyCode,
				ByteBufCodecs.VAR_INT, BrowserInputC2S::scanCode,
				ByteBufCodecs.VAR_INT, BrowserInputC2S::modifiers,
				ByteBufCodecs.VAR_INT, BrowserInputC2S::codePoint,
				ByteBufCodecs.DOUBLE, BrowserInputC2S::scrollDelta,
				BrowserInputC2S::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public record BrowserInputS2C(BlockPos pos, int eventType, int x, int y, int button, int keyCode, int scanCode, int modifiers, int codePoint, double scrollDelta) implements CustomPacketPayload {
		public static final Type<BrowserInputS2C> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, "browser_input_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, BrowserInputS2C> CODEC = StreamCodec.composite(
				BlockPos.STREAM_CODEC, BrowserInputS2C::pos,
				ByteBufCodecs.VAR_INT, BrowserInputS2C::eventType,
				ByteBufCodecs.VAR_INT, BrowserInputS2C::x,
				ByteBufCodecs.VAR_INT, BrowserInputS2C::y,
				ByteBufCodecs.VAR_INT, BrowserInputS2C::button,
				ByteBufCodecs.VAR_INT, BrowserInputS2C::keyCode,
				ByteBufCodecs.VAR_INT, BrowserInputS2C::scanCode,
				ByteBufCodecs.VAR_INT, BrowserInputS2C::modifiers,
				ByteBufCodecs.VAR_INT, BrowserInputS2C::codePoint,
				ByteBufCodecs.DOUBLE, BrowserInputS2C::scrollDelta,
				BrowserInputS2C::new
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
