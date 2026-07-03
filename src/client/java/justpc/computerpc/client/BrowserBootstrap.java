package justpc.computerpc.client;

import net.dimaskama.mcef.api.MCEFApi;
import net.minecraft.client.Minecraft;

public final class BrowserBootstrap {
	private static volatile String status = "Chromium is starting";
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

		String nextStatus = switch (currentInitialization.getStage()) {
			case DONE -> "Chromium ready";
			case DOWNLOADING -> "Chromium is downloading its runtime";
			case EXTRACTING -> "Chromium is extracting its runtime";
			case INSTALL -> "Chromium is installing its runtime";
			case INITIALIZING -> "Chromium is initializing";
			case NOT_STARTED -> "Chromium is starting";
		};
		if (!nextStatus.equals(status)) {
			status = nextStatus;
		}
	}

	public static boolean isReady() {
		return initialization != null && initialization.isDone();
	}

	public static String getStatus() {
		return status;
	}
}
