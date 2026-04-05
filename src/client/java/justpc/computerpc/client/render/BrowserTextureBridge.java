package justpc.computerpc.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.renderer.texture.AbstractTexture;

final class BrowserTextureBridge extends AbstractTexture {
	void update(MCEFBrowser browser) {
		texture = browser.getTexture();
		textureView = browser.getTextureView();
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
