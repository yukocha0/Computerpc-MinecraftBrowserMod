package justpc.computerpc.minecraft;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import justpc.computerpc.browser.api.ExternalTexture;

public final class MinecraftExternalTexture implements ExternalTexture {
	private final GpuTexture texture;
	private final GpuTextureView textureView;

	public MinecraftExternalTexture(GpuTexture texture, GpuTextureView textureView) {
		this.texture = texture;
		this.textureView = textureView;
	}

	@Override
	public void bind() {
		GlStateManager._bindTexture((int) getNativeHandle());
	}

	@Override
	public void release() {
		textureView.close();
		texture.close();
	}

	@Override
	public void resize(int width, int height) {
		if (getWidth() != width || getHeight() != height) {
			throw new UnsupportedOperationException("Minecraft GPU textures are resized by replacement");
		}
	}

	@Override
	public int getWidth() {
		return texture.getWidth(0);
	}

	@Override
	public int getHeight() {
		return texture.getHeight(0);
	}

	@Override
	public long getNativeHandle() {
		return ((GlTexture) texture).glId();
	}

	public GpuTexture gpuTexture() {
		return texture;
	}

	public GpuTextureView textureView() {
		return textureView;
	}
}
