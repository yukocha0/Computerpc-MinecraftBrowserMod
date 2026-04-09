package justpc.computerpc.client;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFSettings;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class BrowserBootstrap {
	private static volatile String status = "Chromium is starting";

	private BrowserBootstrap() {
	}

	public static void initialize() {
		configurePersistentSettings();
		status = "Chromium is starting";
	}

	public static void tick(Minecraft client) {
		String nextStatus = MCEF.isInitialized()
				? "Chromium ready"
				: "Chromium is initializing or downloading its runtime";
		if (!nextStatus.equals(status)) {
			status = nextStatus;
		}
	}

	public static boolean isReady() {
		return MCEF.isInitialized();
	}

	public static String getStatus() {
		return status;
	}

	public static void shutdown() {
		if (MCEF.isInitialized()) {
			MCEF.shutdown();
		}
	}

	private static void configurePersistentSettings() {
		MCEFSettings settings = MCEF.getSettings();
		if (!settings.isUsingCache()) {
			settings.setUseCache(true);
		}

		boolean runtimeInstalled = hasInstalledRuntime();
		if (runtimeInstalled && !settings.isSkipDownload()) {
			settings.setSkipDownload(true);
		} else if (!runtimeInstalled && settings.isSkipDownload()) {
			settings.setSkipDownload(false);
		}
	}

	private static boolean hasInstalledRuntime() {
		String jcefPath = System.getProperty("jcef.path");
		if (jcefPath == null || jcefPath.isBlank()) {
			return false;
		}

		Path runtimePath = Path.of(jcefPath);
		if (!Files.isDirectory(runtimePath)) {
			return false;
		}

		try (Stream<Path> files = Files.list(runtimePath)) {
			return files.findAny().isPresent();
		} catch (Exception ignored) {
			return false;
		}
	}
}
