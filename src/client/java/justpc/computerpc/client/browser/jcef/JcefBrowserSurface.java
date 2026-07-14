package justpc.computerpc.client.browser.jcef;

import justpc.computerpc.browser.api.BrowserFrame;
import justpc.computerpc.browser.api.BrowserTexture;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.Nullable;

import java.awt.Rectangle;
import java.nio.ByteBuffer;

interface JcefBrowserSurface extends AutoCloseable {
	@Nullable
	BrowserTexture texture();

	@Nullable
	BrowserFrame latestFrame();

	void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height);

	@Override
	void close();
}
