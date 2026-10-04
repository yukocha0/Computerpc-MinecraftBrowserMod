package justpc.computerpc.network;

import justpc.computerpc.Computerpc;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ComputerpcPayloads {
	private ComputerpcPayloads() {
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
}
