package justpc.computerpc.client.render;

import justpc.computerpc.Computerpc;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

public final class BrowserRenderUtil {
	private static final Map<MCEFBrowser, Identifier> REGISTERED_TEXTURES = new IdentityHashMap<>();
	private static final Map<Identifier, BrowserTextureBridge> TEXTURE_BRIDGES = new IdentityHashMap<>();
	private static int nextTextureId;

	private BrowserRenderUtil() {
	}

	public static AspectBox fitInside(int x, int y, int width, int height, int contentWidth, int contentHeight) {
		if (width <= 0 || height <= 0) {
			return new AspectBox(x, y, 0, 0);
		}

		double safeContentWidth = Math.max(1, contentWidth);
		double safeContentHeight = Math.max(1, contentHeight);
		double scale = Math.min(width / safeContentWidth, height / safeContentHeight);
		int fittedWidth = Math.max(1, Mth.floor(safeContentWidth * scale));
		int fittedHeight = Math.max(1, Mth.floor(safeContentHeight * scale));
		int fittedX = x + (width - fittedWidth) / 2;
		int fittedY = y + (height - fittedHeight) / 2;
		return new AspectBox(fittedX, fittedY, fittedWidth, fittedHeight);
	}

	public static AspectBoxF fitInside(float x, float y, float width, float height, int contentWidth, int contentHeight) {
		if (width <= 0.0f || height <= 0.0f) {
			return new AspectBoxF(x, y, 0.0f, 0.0f);
		}

		float safeContentWidth = Math.max(1, contentWidth);
		float safeContentHeight = Math.max(1, contentHeight);
		float scale = Math.min(width / safeContentWidth, height / safeContentHeight);
		float fittedWidth = safeContentWidth * scale;
		float fittedHeight = safeContentHeight * scale;
		float fittedX = x + (width - fittedWidth) * 0.5f;
		float fittedY = y + (height - fittedHeight) * 0.5f;
		return new AspectBoxF(fittedX, fittedY, fittedWidth, fittedHeight);
	}

	public static void drawGuiTexture(GuiGraphicsExtractor graphics, MCEFBrowser browser, int x, int y, int width, int height) {
		if (browser == null || width <= 0 || height <= 0) {
			return;
		}
		if (browser.getTexture() == null) {
			return;
		}

		Identifier textureId = syncWorldTexture(browser);
		if (textureId == null) {
			return;
		}

		int textureWidth = Math.max(1, browser.getTexture().getWidth(0));
		int textureHeight = Math.max(1, browser.getTexture().getHeight(0));
		graphics.blit(RenderPipelines.GUI_TEXTURED, textureId, x, y, 0.0f, 0.0f, width, height, textureWidth, textureHeight, textureWidth, textureHeight);
	}

	public static @Nullable Identifier syncWorldTexture(MCEFBrowser browser) {
		if (browser == null || browser.getTextureView() == null) {
			return null;
		}

		Identifier textureId = REGISTERED_TEXTURES.get(browser);
		BrowserTextureBridge bridge;
		if (textureId == null) {
			textureId = Identifier.fromNamespaceAndPath(Computerpc.MOD_ID, "runtime/browser_" + nextTextureId++);
			bridge = new BrowserTextureBridge();
			REGISTERED_TEXTURES.put(browser, textureId);
			TEXTURE_BRIDGES.put(textureId, bridge);
			Minecraft.getInstance().getTextureManager().register(textureId, bridge);
		} else {
			bridge = TEXTURE_BRIDGES.get(textureId);
			if (bridge == null) {
				bridge = new BrowserTextureBridge();
				TEXTURE_BRIDGES.put(textureId, bridge);
				Minecraft.getInstance().getTextureManager().register(textureId, bridge);
			}
		}

		bridge.update(browser);
		return textureId;
	}

	public static void release(MCEFBrowser browser) {
		Identifier textureId = REGISTERED_TEXTURES.remove(browser);
		if (textureId == null) {
			return;
		}

		TEXTURE_BRIDGES.remove(textureId);
		Minecraft.getInstance().getTextureManager().release(textureId);
	}

	public static void releaseAll() {
		for (Identifier textureId : TEXTURE_BRIDGES.keySet()) {
			Minecraft.getInstance().getTextureManager().release(textureId);
		}
		REGISTERED_TEXTURES.clear();
		TEXTURE_BRIDGES.clear();
	}

	public record AspectBox(int x, int y, int width, int height) {
		public int maxX() {
			return x + width;
		}

		public int maxY() {
			return y + height;
		}
	}

	public record AspectBoxF(float x, float y, float width, float height) {
		public float maxX() {
			return x + width;
		}

		public float maxY() {
			return y + height;
		}
	}
}
