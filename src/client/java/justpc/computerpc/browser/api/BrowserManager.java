package justpc.computerpc.browser.api;

public interface BrowserManager {
	BrowserInstance createBrowser(String url, boolean transparent);

	interface Initialization {
		Stage getStage();

		float getPercentage();

		java.util.concurrent.CompletableFuture<? extends BrowserManager> getFuture();

		default boolean isDone() {
			return getStage() == Stage.DONE;
		}

		enum Stage {
			NOT_STARTED,
			DOWNLOADING,
			EXTRACTING,
			INSTALL,
			INITIALIZING,
			DONE
		}
	}
}
