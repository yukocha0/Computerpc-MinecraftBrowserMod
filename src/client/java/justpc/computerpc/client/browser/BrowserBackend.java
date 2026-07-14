package justpc.computerpc.client.browser;

import justpc.computerpc.browser.api.BrowserManager;
import justpc.computerpc.client.browser.jcef.JcefBrowserRuntime;

import java.util.concurrent.CompletableFuture;

public final class BrowserBackend {
	private BrowserBackend() {
	}

	public static BrowserManager.Initialization initialize() {
		return JcefBrowserRuntime.initialize();
	}

	public static CompletableFuture<? extends BrowserManager> getInstanceFuture() {
		return initialize().getFuture();
	}

	public static BrowserManager getInstance() {
		return getInstanceFuture().join();
	}
}
