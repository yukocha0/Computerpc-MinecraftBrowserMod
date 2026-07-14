package justpc.computerpc.browser.api;

import org.jetbrains.annotations.Nullable;

public interface BrowserRenderer {
	void resize(int width, int height);

	@Nullable
	BrowserTexture texture();

	@Nullable
	BrowserFrame latestFrame();
}
