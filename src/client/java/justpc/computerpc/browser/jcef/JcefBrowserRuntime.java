package justpc.computerpc.browser.jcef;

import justpc.computerpc.browser.api.BrowserInstance;
import justpc.computerpc.browser.api.BrowserManager;
import justpc.computerpc.browser.api.BrowserRenderBridge;
import justpc.computerpc.browser.api.BrowserRuntimeOptions;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.IProgressHandler;
import net.fabricmc.loader.api.FabricLoader;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.browser.CefRequestContext;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class JcefBrowserRuntime implements BrowserManager {
	public static final Path MOD_DIR = FabricLoader.getInstance().getConfigDir().resolve("computerpc-browser");
	public static final Path JCEF_PATH = MOD_DIR.resolve("jcef");
	public static final Path CACHE_PATH = MOD_DIR.resolve("cache");

	private static final int WINDOWLESS_FRAME_RATE = 60;
	private static volatile InitializationImpl initialization;

	private final CefApp cefApp;
	private final CefClient client;
	private final BrowserRenderBridge renderBridge;
	private final BrowserRuntimeOptions options;

	private JcefBrowserRuntime(InitializationImpl initialization, BrowserRenderBridge renderBridge) throws Exception {
		this.renderBridge = renderBridge;
		this.options = BrowserRuntimeOptions.STABLE_MIGRATION;
		ensureDirectories();

		CefAppBuilder cefAppBuilder = new CefAppBuilder();
		cefAppBuilder.setInstallDir(JCEF_PATH.toFile());
		cefAppBuilder.setProgressHandler(initialization);
		cefAppBuilder.addJcefArgs(
				"--autoplay-policy=no-user-gesture-required",
				"--disable-web-security",
				"--disable-background-timer-throttling",
				"--disable-backgrounding-occluded-windows",
				"--disable-breakpad",
				"--disable-component-update",
				"--disable-domain-reliability",
				"--disable-features=BackgroundSync,MediaRouter,OptimizationHints,PushMessaging",
				"--disable-notifications",
				"--disable-renderer-backgrounding",
				"--disable-sync",
				"--enable-widevine-cdm"
		);
		CefSettings cefSettings = cefAppBuilder.getCefSettings();
		cefSettings.user_agent_product = "Computerpc/1.0.0";
		cefSettings.root_cache_path = MOD_DIR.toAbsolutePath().toString();
		cefSettings.cache_path = CACHE_PATH.toAbsolutePath().toString();
		cefSettings.persist_session_cookies = true;
		cefApp = cefAppBuilder.build();

		client = cefApp.createClient();
	}

	public static Initialization initialize(BrowserRenderBridge renderBridge) {
		if (initialization == null) {
			synchronized (JcefBrowserRuntime.class) {
				if (initialization == null) {
					System.setProperty("java.awt.headless", "false");
					initialization = new InitializationImpl(renderBridge);
				}
			}
		}

		return initialization;
	}

	@Nullable
	public static Initialization getInitialization() {
		return initialization;
	}

	public java.util.concurrent.CompletableFuture<JcefBrowserRuntime> instanceFuture() {
		return initialization.getFuture().thenApply(JcefBrowserRuntime.class::cast);
	}

	@Override
	public BrowserInstance createBrowser(String url, boolean transparent) {
		JcefEmbeddedBrowser browser = new JcefEmbeddedBrowser(client, url, transparent, CefRequestContext.getGlobalContext(), renderBridge);
		browser.setCloseAllowed();
		browser.createImmediately();
		browser.setWindowlessFrameRate(WINDOWLESS_FRAME_RATE);
		return browser;
	}

	public void close() {
		cefApp.dispose();
	}

	public BrowserRuntimeOptions options() {
		return options;
	}

	private static void ensureDirectories() throws IOException {
		Files.createDirectories(JCEF_PATH);
		Files.createDirectories(CACHE_PATH);
	}

	private static final class InitializationImpl implements Initialization, IProgressHandler {
		private final CompletableFuture<BrowserManager> future;
		private volatile Stage stage = Stage.NOT_STARTED;
		private volatile float percentage = -1.0F;

		private InitializationImpl(BrowserRenderBridge renderBridge) {
			future = CompletableFuture.supplyAsync(() -> {
				try {
					return new JcefBrowserRuntime(this, renderBridge);
				} catch (Throwable e) {
					stage = Stage.DONE;
					percentage = -1.0F;
					throw new RuntimeException("Failed to initialize embedded Chromium", e);
				}
			});
		}

		@Override
		public Stage getStage() {
			return stage;
		}

		@Override
		public float getPercentage() {
			return percentage;
		}

		@Override
		public CompletableFuture<BrowserManager> getFuture() {
			return future;
		}

		@Override
		public void handleProgress(EnumProgress state, float percent) {
			stage = switch (state) {
				case LOCATING -> BrowserManager.Initialization.Stage.NOT_STARTED;
				case DOWNLOADING -> BrowserManager.Initialization.Stage.DOWNLOADING;
				case EXTRACTING -> BrowserManager.Initialization.Stage.EXTRACTING;
				case INSTALL -> BrowserManager.Initialization.Stage.INSTALL;
				case INITIALIZING -> BrowserManager.Initialization.Stage.INITIALIZING;
				case INITIALIZED -> BrowserManager.Initialization.Stage.DONE;
			};
			percentage = percent;
		}
	}
}
