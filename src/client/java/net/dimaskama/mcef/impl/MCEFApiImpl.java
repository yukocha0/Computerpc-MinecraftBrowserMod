package net.dimaskama.mcef.impl;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.EnumProgress;
import me.friwi.jcefmaven.IProgressHandler;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
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

public final class MCEFApiImpl implements MCEFApi {
	public static final Path MOD_DIR = FabricLoader.getInstance().getConfigDir().resolve("computerpc-mcef");
	public static final Path JCEF_PATH = MOD_DIR.resolve("jcef");
	public static final Path CACHE_PATH = MOD_DIR.resolve("cache");

	private static volatile InitializationImpl initialization;

	private final CefApp cefApp;
	private final CefClient client;

	private MCEFApiImpl(InitializationImpl initialization) throws Exception {
		ensureDirectories();

		CefAppBuilder cefAppBuilder = new CefAppBuilder();
		cefAppBuilder.setInstallDir(JCEF_PATH.toFile());
		cefAppBuilder.setProgressHandler(initialization);
		cefAppBuilder.addJcefArgs(
				"--autoplay-policy=no-user-gesture-required",
				"--disable-web-security",
				"--enable-widevine-cdm"
		);
		cefApp = cefAppBuilder.build();

		CefSettings cefSettings = new CefSettings();
		cefSettings.user_agent_product = "Computerpc/1.0.0";
		cefSettings.root_cache_path = CACHE_PATH.toAbsolutePath().toString();
		cefApp.setSettings(cefSettings);

		client = cefApp.createClient();
	}

	public static Initialization initialize() {
		if (initialization == null) {
			synchronized (MCEFApiImpl.class) {
				if (initialization == null) {
					System.setProperty("java.awt.headless", "false");
					initialization = new InitializationImpl();
				}
			}
		}

		return initialization;
	}

	@Nullable
	public static Initialization getInitialization() {
		return initialization;
	}

	@Override
	public MCEFBrowser createBrowser(String url, boolean transparent) {
		MCEFBrowserImpl browser = new MCEFBrowserImpl(client, url, transparent, CefRequestContext.getGlobalContext());
		browser.setCloseAllowed();
		browser.createImmediately();
		return browser;
	}

	public void close() {
		cefApp.dispose();
	}

	private static void ensureDirectories() throws IOException {
		Files.createDirectories(JCEF_PATH);
		Files.createDirectories(CACHE_PATH);
	}

	private static final class InitializationImpl implements Initialization, IProgressHandler {
		private final CompletableFuture<MCEFApi> future;
		private volatile Stage stage = Stage.NOT_STARTED;
		private volatile float percentage = -1.0F;

		private InitializationImpl() {
			future = CompletableFuture.supplyAsync(() -> {
				try {
					return new MCEFApiImpl(this);
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
		public CompletableFuture<MCEFApi> getFuture() {
			return future;
		}

		@Override
		public void handleProgress(EnumProgress state, float percent) {
			stage = switch (state) {
				case LOCATING -> Stage.NOT_STARTED;
				case DOWNLOADING -> Stage.DOWNLOADING;
				case EXTRACTING -> Stage.EXTRACTING;
				case INSTALL -> Stage.INSTALL;
				case INITIALIZING -> Stage.INITIALIZING;
				case INITIALIZED -> Stage.DONE;
			};
			percentage = percent;
		}
	}
}
