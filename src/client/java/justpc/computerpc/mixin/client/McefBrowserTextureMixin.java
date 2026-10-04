package justpc.computerpc.mixin.client;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

@Pseudo
@Mixin(targets = "net.dimaskama.mcef.impl.MCEFBrowserImpl", remap = false)
abstract class McefBrowserTextureMixin {
	private static final long FRAME_UPLOAD_INTERVAL_NANOS = 1_000_000_000L / 30L;

	@Unique
	private long computerpc$lastFrameUploadNanos;

	@Shadow
	@Nullable
	private GpuTexture gpuTexture;

	@Shadow
	@Nullable
	private GpuTextureView gpuTextureView;

	@Inject(method = "onPaintInternal", at = @At("HEAD"), cancellable = true)
	private void computerpc$uploadBrowserFrame(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height, CallbackInfo callbackInfo) {
		if (popup || dirtyRects.length == 0) {
			return;
		}

		long now = System.nanoTime();
		if (computerpc$lastFrameUploadNanos != 0L
				&& now - computerpc$lastFrameUploadNanos < FRAME_UPLOAD_INTERVAL_NANOS) {
			MemoryUtil.memFree(buffer);
			callbackInfo.cancel();
			return;
		}

		try {
			if (gpuTexture == null || gpuTexture.getWidth(0) != width || gpuTexture.getHeight(0) != height) {
				if (gpuTextureView != null) {
					gpuTextureView.close();
				}
				if (gpuTexture != null) {
					gpuTexture.close();
				}
				gpuTexture = RenderSystem.getDevice().createTexture(
						"MCEFBrowser",
						GpuTexture.USAGE_COPY_DST
								| GpuTexture.USAGE_COPY_SRC
								| GpuTexture.USAGE_TEXTURE_BINDING
								| GpuTexture.USAGE_RENDER_ATTACHMENT,
						GpuFormat.RGBA8_UNORM,
						width,
						height,
						1,
						1
				);
				gpuTextureView = RenderSystem.getDevice().createTextureView(gpuTexture);
			}

			ByteBuffer rgba = MemoryUtil.memAlloc(width * height * Integer.BYTES);
			try {
				IntBuffer sourcePixels = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
				IntBuffer destinationPixels = rgba.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
				for (int i = 0; i < width * height; i++) {
					int bgra = sourcePixels.get(i);
					destinationPixels.put(i, (bgra & 0xFF00FF00) | ((bgra >> 16) & 0xFF) | ((bgra & 0xFF) << 16));
				}
				RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpuTexture, rgba, 0, 0, 0, 0, width, height);
			} finally {
				MemoryUtil.memFree(rgba);
			}

			computerpc$lastFrameUploadNanos = now;
			callbackInfo.cancel();
		} finally {
			MemoryUtil.memFree(buffer);
		}
	}
}
