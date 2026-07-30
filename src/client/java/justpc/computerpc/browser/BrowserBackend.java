package justpc.computerpc.browser;

import justpc.computerpc.browser.api.BrowserManager;
import justpc.computerpc.browser.api.BrowserRenderBridge;
import justpc.computerpc.browser.jcef.JcefBrowserRuntime;

import java.util.concurrent.CompletableFuture;

public final class BrowserBackend {
	private BrowserBackend() {
	}

	public static BrowserManager.Initialization initialize(BrowserRenderBridge renderBridge) {
		return JcefBrowserRuntime.initialize(renderBridge);
	}

	public static CompletableFuture<? extends BrowserManager> getInstanceFuture() {
		return JcefBrowserRuntime.getInitialization().getFuture();
	}

	public static BrowserManager getInstance() {
		return getInstanceFuture().join();
	}
}
