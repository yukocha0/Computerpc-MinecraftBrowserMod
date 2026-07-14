package justpc.computerpc.browser.api;

public record BrowserRuntimeOptions(boolean sharedTextureEnabled, boolean externalBeginFrameEnabled) {
	public static final BrowserRuntimeOptions STABLE_MIGRATION = new BrowserRuntimeOptions(false, false);
}
