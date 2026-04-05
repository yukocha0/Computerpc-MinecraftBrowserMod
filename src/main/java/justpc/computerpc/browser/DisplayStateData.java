package justpc.computerpc.browser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;

public record DisplayStateData(List<BrowserTabData> tabs, int activeTab, int resolutionWidth, int resolutionHeight, float volume) {
	private static final BrowserTabData DEFAULT_TAB = BrowserTabData.create(BrowserTabData.defaultUrl());
	private static final List<BrowserTabData> DEFAULT_TABS = List.of(DEFAULT_TAB);

	public static final Codec<DisplayStateData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			BrowserTabData.CODEC.listOf().fieldOf("tabs").forGetter(DisplayStateData::tabs),
			Codec.INT.fieldOf("active_tab").forGetter(DisplayStateData::activeTab),
			Codec.INT.fieldOf("resolution_width").forGetter(DisplayStateData::resolutionWidth),
			Codec.INT.fieldOf("resolution_height").forGetter(DisplayStateData::resolutionHeight),
			Codec.FLOAT.fieldOf("volume").forGetter(DisplayStateData::volume)
	).apply(instance, DisplayStateData::new));

	public static final DisplayStateData DEFAULT = new DisplayStateData(DEFAULT_TABS, 0, 960, 540, 1.0f);

	public DisplayStateData {
		tabs = List.copyOf(tabs);
	}

	public DisplayStateData sanitize() {
		List<BrowserTabData> cleanedTabs = tabs;
		ArrayList<BrowserTabData> mutableTabs = null;
		for (int i = 0; i < tabs.size(); i++) {
			BrowserTabData original = tabs.get(i);
			BrowserTabData sanitizedTab = original.sanitize();
			if (sanitizedTab.equals(original)) {
				continue;
			}

			if (mutableTabs == null) {
				mutableTabs = new ArrayList<>(tabs);
				cleanedTabs = mutableTabs;
			}
			mutableTabs.set(i, sanitizedTab);
		}

		if (cleanedTabs.isEmpty()) {
			cleanedTabs = DEFAULT_TABS;
		}

		int safeActive = Math.max(0, Math.min(activeTab, cleanedTabs.size() - 1));
		int safeWidth = Math.max(1, Math.min(resolutionWidth, 3840));
		int safeHeight = Math.max(1, Math.min(resolutionHeight, 2160));
		float safeVolume = Math.max(0.0f, Math.min(volume, 1.0f));
		if (cleanedTabs == tabs
				&& safeActive == activeTab
				&& safeWidth == resolutionWidth
				&& safeHeight == resolutionHeight
				&& Float.compare(safeVolume, volume) == 0) {
			return this;
		}

		return new DisplayStateData(cleanedTabs, safeActive, safeWidth, safeHeight, safeVolume);
	}

	public BrowserTabData activeTabData() {
		DisplayStateData sanitized = sanitize();
		return sanitized.tabs().get(sanitized.activeTab());
	}

	public DisplayStateData withActiveTab(int index) {
		DisplayStateData sanitized = sanitize();
		int safeActive = Math.max(0, Math.min(index, sanitized.tabs().size() - 1));
		return new DisplayStateData(sanitized.tabs(), safeActive, sanitized.resolutionWidth(), sanitized.resolutionHeight(), sanitized.volume());
	}

	public DisplayStateData withResolution(int width, int height) {
		DisplayStateData sanitized = sanitize();
		return new DisplayStateData(sanitized.tabs(), sanitized.activeTab(), width, height, sanitized.volume()).sanitize();
	}

	public DisplayStateData adaptToAspect(int aspectWidth, int aspectHeight) {
		DisplayStateData sanitized = sanitize();
		int adaptedWidth = adaptedWidth(sanitized.resolutionHeight(), aspectWidth, aspectHeight);
		if (adaptedWidth == sanitized.resolutionWidth()) {
			return sanitized;
		}

		return new DisplayStateData(
				sanitized.tabs(),
				sanitized.activeTab(),
				adaptedWidth,
				sanitized.resolutionHeight(),
				sanitized.volume()
		).sanitize();
	}

	public static int adaptedWidth(int height, int aspectWidth, int aspectHeight) {
		int safeHeight = Math.max(1, height);
		int safeAspectWidth = Math.max(1, aspectWidth);
		int safeAspectHeight = Math.max(1, aspectHeight);
		double exactWidth = safeHeight * (safeAspectWidth / (double) safeAspectHeight);
		int roundedWidth = Math.max(1, (int) Math.round(exactWidth));
		if (roundedWidth > 1 && (roundedWidth & 1) != 0) {
			int lowerEven = roundedWidth - 1;
			int upperEven = roundedWidth + 1;
			roundedWidth = Math.abs(exactWidth - lowerEven) <= Math.abs(exactWidth - upperEven) ? lowerEven : upperEven;
		}
		return Math.max(1, roundedWidth);
	}

	public DisplayStateData withVolume(float newVolume) {
		DisplayStateData sanitized = sanitize();
		return new DisplayStateData(sanitized.tabs(), sanitized.activeTab(), sanitized.resolutionWidth(), sanitized.resolutionHeight(), newVolume).sanitize();
	}

	public DisplayStateData withAddedTab(String input) {
		DisplayStateData sanitized = sanitize();
		ArrayList<BrowserTabData> updated = new ArrayList<>(sanitized.tabs());
		updated.add(BrowserTabData.create(input));
		return new DisplayStateData(updated, updated.size() - 1, sanitized.resolutionWidth(), sanitized.resolutionHeight(), sanitized.volume()).sanitize();
	}

	public DisplayStateData withRemovedActiveTab() {
		return withRemovedTab(activeTab);
	}

	public DisplayStateData withRemovedTab(int index) {
		DisplayStateData sanitized = sanitize();
		if (sanitized.tabs().size() <= 1) {
			return new DisplayStateData(
					DEFAULT_TABS,
					0,
					sanitized.resolutionWidth(),
					sanitized.resolutionHeight(),
					sanitized.volume()
			);
		}

		ArrayList<BrowserTabData> updated = new ArrayList<>(sanitized.tabs());
		int safeIndex = Math.max(0, Math.min(index, updated.size() - 1));
		updated.remove(safeIndex);
		int nextIndex = Math.max(0, Math.min(safeIndex, updated.size() - 1));
		return new DisplayStateData(updated, nextIndex, sanitized.resolutionWidth(), sanitized.resolutionHeight(), sanitized.volume()).sanitize();
	}

	public DisplayStateData navigateActive(String input) {
		return replaceActive(activeTabData().navigate(input));
	}

	public DisplayStateData goBack() {
		return replaceActive(activeTabData().stepHistory(-1));
	}

	public DisplayStateData goForward() {
		return replaceActive(activeTabData().stepHistory(1));
	}

	public DisplayStateData syncActiveUrl(String input) {
		DisplayStateData sanitized = sanitize();
		String normalized = BrowserTabData.normalizeInput(input);
		if (sanitized.activeTabData().currentUrl().equals(normalized)) {
			return sanitized;
		}
		return replaceActive(sanitized.activeTabData().navigate(normalized));
	}

	private DisplayStateData replaceActive(BrowserTabData replacement) {
		DisplayStateData sanitized = sanitize();
		ArrayList<BrowserTabData> updated = new ArrayList<>(sanitized.tabs());
		updated.set(sanitized.activeTab(), replacement.sanitize());
		return new DisplayStateData(updated, sanitized.activeTab(), sanitized.resolutionWidth(), sanitized.resolutionHeight(), sanitized.volume()).sanitize();
	}
}
