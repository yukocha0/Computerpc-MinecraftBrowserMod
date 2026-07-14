package justpc.computerpc.browser.api;

import org.jetbrains.annotations.Nullable;

public interface BrowserInstance extends AutoCloseable {
	BrowserRenderer renderer();

	BrowserInput input();

	void setFocus(boolean focused);

	void setVisible(boolean visible);

	void navigate(String url);

	void goBack();

	void goForward();

	void reload();

	void stop();

	void executeJavaScript(String script);

	boolean isLoading();

	String currentUrl();

	BrowserCursor cursor();

	@Override
	void close();

	@Nullable
	default BrowserTexture texture() {
		return renderer().texture();
	}
}
