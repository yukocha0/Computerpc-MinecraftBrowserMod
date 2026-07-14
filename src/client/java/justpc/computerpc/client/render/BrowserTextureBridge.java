package justpc.computerpc.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import justpc.computerpc.browser.api.BrowserTexture;
import justpc.computerpc.minecraft.MinecraftExternalTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;

final class BrowserTextureBridge extends AbstractTexture {
	void update(BrowserTexture browserTexture) {
		if (!(browserTexture.externalTexture() instanceof MinecraftExternalTexture minecraftTexture)) {
			texture = null;
			textureView = null;
			sampler = null;
			return;
		}
		if (texture == minecraftTexture.gpuTexture() && textureView == minecraftTexture.textureView() && sampler != null) {
			return;
		}

		texture = minecraftTexture.gpuTexture();
		textureView = minecraftTexture.textureView();
		sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
	}

	@Override
	public void close() {
		// The browser owns its GPU resources; the texture manager should only drop the handle.
		texture = null;
		textureView = null;
		sampler = null;
	}
}
