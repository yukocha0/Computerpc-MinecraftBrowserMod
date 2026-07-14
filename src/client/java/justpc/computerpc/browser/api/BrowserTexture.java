package justpc.computerpc.browser.api;

public interface BrowserTexture {
	ExternalTexture externalTexture();

	default int width() {
		return externalTexture().getWidth();
	}

	default int height() {
		return externalTexture().getHeight();
	}
}
