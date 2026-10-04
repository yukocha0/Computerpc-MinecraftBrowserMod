package justpc.computerpc.client;

import net.dimaskama.mcef.api.MCEFApi;
import net.minecraft.client.Minecraft;

import java.util.Locale;

public final class BrowserBootstrap {
	private static volatile String status = "MCEF Modern is required to start the browser";
	private static volatile MCEFApi.Initialization initialization;

	private BrowserBootstrap() {
	}

	public static void initialize() {
		initialization = MCEFApi.initialize();
		status = "Chromium is starting";
	}

	public static void tick(Minecraft client) {
		MCEFApi.Initialization currentInitialization = initialization;
		if (currentInitialization == null) {
			return;
		}

		if (currentInitialization.getFuture().isCompletedExceptionally()) {
			status = "MCEF Modern failed to initialize; check the game log";
			return;
		}

		String nextStatus = switch (currentInitialization.getStage()) {
			case DONE -> "Chromium ready";
			case DOWNLOADING -> downloadingStatus(currentInitialization.getPercentage());
			case EXTRACTING -> "Chromium is extracting its runtime";
			case INSTALL -> "Chromium is installing its runtime";
			case INITIALIZING -> "Chromium is initializing";
			case NOT_STARTED -> "Chromium is starting";
		};
		status = nextStatus;
	}

	public static boolean isReady() {
		MCEFApi.Initialization currentInitialization = initialization;
		return currentInitialization != null
				&& currentInitialization.isDone()
				&& currentInitialization.getFuture().isDone()
				&& !currentInitialization.getFuture().isCompletedExceptionally();
	}

	public static String getStatus() {
		return status;
	}

	private static String downloadingStatus(float percentage) {
		if (Float.isFinite(percentage) && percentage >= 0.0F && percentage <= 100.0F) {
			return String.format(Locale.ROOT, "Chromium is downloading its runtime (%.0f%%)", percentage);
		}
		return "Chromium is downloading its runtime";
	}
}
