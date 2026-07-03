package org.cef.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class CustomCefBrowserOsr extends CefBrowser_N implements CefRenderHandler {
	protected final Rectangle browserRect = new Rectangle(0, 0, 1, 1);
	protected final Component component = new Component() {
		@Override
		public String getName() {
			return "CustomCefBrowserOsr";
		}

		@Override
		public int getWidth() {
			return (int) browserRect.getWidth();
		}

		@Override
		public int getHeight() {
			return (int) browserRect.getHeight();
		}
	};

	protected final boolean transparent;
	private final List<Consumer<CefPaintEvent>> onPaintListeners = new CopyOnWriteArrayList<>();

	public CustomCefBrowserOsr(CefClient client, String url, boolean transparent, CefRequestContext context, CefBrowserSettings settings) {
		super(client, url, context, null, null, settings);
		this.transparent = transparent;
	}

	@Override
	public void createImmediately() {
		createBrowserIfRequired();
	}

	@Override
	public Component getUIComponent() {
		return component;
	}

	@Override
	public CefRenderHandler getRenderHandler() {
		return this;
	}

	@Override
	protected CefBrowser_N createDevToolsBrowser(CefClient client, String url, CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
		return null;
	}

	@Override
	public Rectangle getViewRect(CefBrowser browser) {
		return browserRect;
	}

	@Override
	public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
		return new Point(viewPoint);
	}

	@Override
	public void onPopupShow(CefBrowser browser, boolean show) {
	}

	@Override
	public void onPopupSize(CefBrowser browser, Rectangle size) {
	}

	@Override
	public void addOnPaintListener(Consumer<CefPaintEvent> listener) {
		onPaintListeners.add(listener);
	}

	@Override
	public void setOnPaintListener(Consumer<CefPaintEvent> listener) {
		onPaintListeners.clear();
		onPaintListeners.add(listener);
	}

	@Override
	public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {
		onPaintListeners.remove(listener);
	}

	@Override
	public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
		if (!onPaintListeners.isEmpty()) {
			CefPaintEvent paintEvent = new CefPaintEvent(browser, popup, dirtyRects, buffer, width, height);
			for (Consumer<CefPaintEvent> listener : onPaintListeners) {
				listener.accept(paintEvent);
			}
		}
	}

	@Override
	public boolean onCursorChange(CefBrowser browser, int cursorType) {
		return true;
	}

	@Override
	public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
		return true;
	}

	@Override
	public void updateDragCursor(CefBrowser browser, int operation) {
	}

	@Override
	public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
		screenInfo.Set(1.0, 32, 8, false, browserRect.getBounds(), browserRect.getBounds());
		return true;
	}

	@Override
	public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
		throw new UnsupportedOperationException();
	}

	private void createBrowserIfRequired() {
		if (getNativeRef("CefBrowser") == 0) {
			createBrowser(getClient(), 0, getUrl(), true, transparent, null, getRequestContext());
		}
	}
}
