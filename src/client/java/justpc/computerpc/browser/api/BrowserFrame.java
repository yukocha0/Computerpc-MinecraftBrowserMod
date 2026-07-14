package justpc.computerpc.browser.api;

import java.util.List;

public record BrowserFrame(ExternalTexture texture, int width, int height, List<DirtyRectangle> dirtyRectangles) {
	public BrowserFrame {
		dirtyRectangles = List.copyOf(dirtyRectangles);
	}
}
