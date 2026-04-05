package justpc.computerpc.browser;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record BrowserTabData(String title, List<String> history, int historyIndex) {
	private static final String SEARCH_URL_PREFIX = "https://www.google.com/search?q=";
	private static final String DEFAULT_URL = "about:blank";

	public static final Codec<BrowserTabData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("title").forGetter(BrowserTabData::title),
			Codec.STRING.listOf().fieldOf("history").forGetter(BrowserTabData::history),
			Codec.INT.fieldOf("history_index").forGetter(BrowserTabData::historyIndex)
	).apply(instance, BrowserTabData::new));

	public BrowserTabData {
		history = List.copyOf(history);
	}

	public static BrowserTabData create(String input) {
		String normalized = normalizeInput(input);
		return new BrowserTabData(titleFromUrl(normalized), List.of(normalized), 0);
	}

	public static String defaultUrl() {
		return DEFAULT_URL;
	}

	public BrowserTabData sanitize() {
		if (history.isEmpty()) {
			return create(DEFAULT_URL);
		}

		List<String> cleanedHistory = history;
		ArrayList<String> mutableHistory = null;
		for (int i = 0; i < history.size(); i++) {
			String original = history.get(i);
			String normalized = normalizeInput(original);
			if (normalized.equals(original)) {
				continue;
			}

			if (mutableHistory == null) {
				mutableHistory = new ArrayList<>(history);
				cleanedHistory = mutableHistory;
			}
			mutableHistory.set(i, normalized);
		}

		int clampedIndex = Math.max(0, Math.min(historyIndex, cleanedHistory.size() - 1));
		String currentUrl = cleanedHistory.get(clampedIndex);
		String sanitizedTitle = title == null || title.isBlank() ? titleFromUrl(currentUrl) : title;
		if (cleanedHistory == history && clampedIndex == historyIndex && Objects.equals(sanitizedTitle, title)) {
			return this;
		}

		return new BrowserTabData(sanitizedTitle, cleanedHistory, clampedIndex);
	}

	public String currentUrl() {
		BrowserTabData sanitized = sanitize();
		return sanitized.history().get(sanitized.historyIndex());
	}

	public BrowserTabData navigate(String input) {
		String normalized = normalizeInput(input);
		BrowserTabData sanitized = sanitize();
		if (sanitized.currentUrl().equals(normalized)) {
			return sanitized;
		}

		ArrayList<String> updatedHistory = new ArrayList<>(sanitized.history().subList(0, sanitized.historyIndex() + 1));
		updatedHistory.add(normalized);
		return new BrowserTabData(titleFromUrl(normalized), updatedHistory, updatedHistory.size() - 1);
	}

	public BrowserTabData stepHistory(int amount) {
		BrowserTabData sanitized = sanitize();
		int updatedIndex = Math.max(0, Math.min(sanitized.historyIndex() + amount, sanitized.history().size() - 1));
		String currentUrl = sanitized.history().get(updatedIndex);
		return new BrowserTabData(titleFromUrl(currentUrl), sanitized.history(), updatedIndex);
	}

	public static String normalizeInput(String input) {
		String trimmed = input == null ? "" : input.trim();
		if (trimmed.isEmpty()) {
			return DEFAULT_URL;
		}

		String lowered = trimmed.toLowerCase(Locale.ROOT);
		if (lowered.startsWith("http://")
				|| lowered.startsWith("https://")
				|| lowered.startsWith("about:")
				|| lowered.startsWith("data:")
				|| lowered.startsWith("mod:")) {
			return trimmed;
		}

		if (trimmed.contains(".") && !trimmed.contains(" ")) {
			return "https://" + trimmed;
		}

		return SEARCH_URL_PREFIX + trimmed.replace(" ", "+");
	}

	public static String titleFromUrl(String url) {
		if (DEFAULT_URL.equals(url)) {
			return "Home";
		}

		try {
			URI uri = URI.create(url);
			String host = uri.getHost();
			if (host == null || host.isBlank()) {
				return url;
			}

			String cleaned = host.startsWith("www.") ? host.substring(4) : host;
			return cleaned.isBlank() ? url : cleaned;
		} catch (IllegalArgumentException ignored) {
			return url;
		}
	}
}
