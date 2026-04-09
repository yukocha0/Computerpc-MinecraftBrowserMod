package justpc.computerpc.client.render;

import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class BrowserRenderUtil {
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
		if (browser == null || width <= 0 || height <= 0 || !browser.isTextureReady()) {
			return;
		}

		Identifier texture = browser.getTextureIdentifier();
		if (texture == null) {
			return;
		}

		int textureWidth = Math.max(1, browser.getRenderer().getTextureWidth());
		int textureHeight = Math.max(1, browser.getRenderer().getTextureHeight());
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0f, 0.0f, width, height, textureWidth, textureHeight, textureWidth, textureHeight);
	}

	public static @Nullable Identifier syncWorldTexture(MCEFBrowser browser) {
		if (browser == null || !browser.isTextureReady()) {
			return null;
		}

		return browser.getTextureIdentifier();
	}

	public static void release(MCEFBrowser browser) {
	}

	public static void releaseAll() {
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
