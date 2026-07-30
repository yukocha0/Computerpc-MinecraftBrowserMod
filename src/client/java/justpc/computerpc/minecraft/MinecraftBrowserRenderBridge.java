package justpc.computerpc.minecraft;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import justpc.computerpc.browser.api.BrowserRenderBridge;
import justpc.computerpc.browser.api.ExternalTexture;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;

public final class MinecraftBrowserRenderBridge implements BrowserRenderBridge {
	public static final MinecraftBrowserRenderBridge INSTANCE = new MinecraftBrowserRenderBridge();

	private MinecraftBrowserRenderBridge() {
	}

	@Override
	public void executeOnRenderThread(Runnable task) {
		Minecraft.getInstance().execute(task);
	}

	@Override
	public ExternalTexture createTexture(String debugName, int width, int height) {
		GpuTexture texture = RenderSystem.getDevice().createTexture(
				debugName,
				GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
				GpuFormat.RGBA8_UNORM,
				width,
				height,
				1,
				1
		);
		return new MinecraftExternalTexture(texture, RenderSystem.getDevice().createTextureView(texture));
	}

	@Override
	public void uploadBgra(ExternalTexture texture, ByteBuffer pixels, int bytes, int width, int height) {
		texture.bind();
		GlStateManager._pixelStore(GlConst.GL_UNPACK_ROW_LENGTH, width);
		GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_PIXELS, 0);
		GlStateManager._pixelStore(GlConst.GL_UNPACK_SKIP_ROWS, 0);
		GlStateManager._pixelStore(GlConst.GL_UNPACK_ALIGNMENT, 4);
		GlStateManager._texSubImage2D(
				GlConst.GL_TEXTURE_2D,
				0,
				0,
				0,
				width,
				height,
				GL12.GL_BGRA,
				GlConst.GL_UNSIGNED_BYTE,
				pixels
		);
	}
}
