package justpc.computerpc.client;

import justpc.computerpc.blockentity.DisplayBlockEntity;
import justpc.computerpc.browser.BrowserTabData;
import justpc.computerpc.browser.DisplayStateData;
import justpc.computerpc.client.render.BrowserRenderUtil;
import justpc.computerpc.network.ComputerpcNetworking;
import justpc.computerpc.network.ComputerpcPayloads;
import justpc.computerpc.util.DisplayCluster;
import net.dimaskama.mcef.api.MCEFApi;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DisplayBrowserManager {
	private static final long PREVIEW_SYNC_GRACE_TICKS = 40L;
	private static final Map<DisplayKey, DisplayBrowserSession> SESSIONS = new HashMap<>();
	private static long tickCounter;

	private DisplayBrowserManager() {
	}

	public static void tick(Minecraft client) {
		tickCounter++;
		if (client.level == null) {
			closeAll();
			return;
		}

		Iterator<Map.Entry<DisplayKey, DisplayBrowserSession>> iterator = SESSIONS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<DisplayKey, DisplayBrowserSession> entry = iterator.next();
			DisplayBrowserSession session = entry.getValue();
			if (!session.key.dimension.equals(client.level.dimension())) {
				session.close();
				iterator.remove();
				continue;
			}

			if (!(client.level.getBlockEntity(session.key.rootPos) instanceof DisplayBlockEntity display)) {
				session.close();
				iterator.remove();
				continue;
			}

			if (!display.isPowered()) {
				session.suspend();
			} else {
				session.resume();
			}

			if (tickCounter - session.lastAccessTick > 200L) {
				session.close();
				iterator.remove();
				continue;
			}

			session.refreshBrowserState();
		}
	}

	public static void closeAll() {
		SESSIONS.values().forEach(DisplayBrowserSession::close);
		SESSIONS.clear();
	}

	public static void applyRemoteInput(ComputerpcPayloads.BrowserInputS2C payload) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) {
			return;
		}

		DisplayBrowserSession session = getOrCreateSession(client.level, payload.pos());
		if (session == null) {
			return;
		}

		session.applyInput(payload.eventType(), payload.x(), payload.y(), payload.button(), payload.keyCode(), payload.scanCode(), payload.modifiers(), payload.codePoint(), payload.scrollDelta());
	}

	public static @Nullable DisplayBrowserSession getSession(ClientLevel level, BlockPos rootPos) {
		return getOrCreateSession(level, rootPos);
	}

	public static @Nullable DisplayBrowserSession previewState(ClientLevel level, BlockPos rootPos, DisplayStateData state) {
		DisplayBrowserSession session = getOrCreateSession(level, rootPos);
		if (session != null) {
			DisplayStateData adaptedState = adaptStateToCluster(level, rootPos, state);
			session.syncPreview(adaptedState, tickCounter);
		}
		return session;
	}

	public static List<NearbyDisplayInfo> findNearbyDisplays(ClientLevel level, Vec3 origin, double radius) {
		ArrayList<NearbyDisplayInfo> displays = new ArrayList<>();
		Set<BlockPos> seenRoots = new HashSet<>();
		double maxDistanceSqr = radius * radius;
		int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(origin.x - radius));
		int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(origin.x + radius));
		int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(origin.z - radius));
		int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(origin.z + radius));

		for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
			for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
				LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
				if (chunk == null) {
					continue;
				}

				for (net.minecraft.world.level.block.entity.BlockEntity blockEntity : chunk.getBlockEntities().values()) {
					if (!(blockEntity instanceof DisplayBlockEntity display)) {
						continue;
					}

					DisplayCluster cluster = display.getCluster();
					BlockPos rootPos = cluster.root();
					if (!seenRoots.add(rootPos) || rootPos.getCenter().distanceToSqr(origin) > maxDistanceSqr) {
						continue;
					}

					if (!(level.getBlockEntity(rootPos) instanceof DisplayBlockEntity rootDisplay)) {
						continue;
					}

					displays.add(new NearbyDisplayInfo(
							rootPos,
							cluster.widthBlocks(),
							cluster.heightBlocks(),
							rootDisplay.isPowered(),
							rootDisplay.getScreenState().adaptToAspect(cluster.widthBlocks(), cluster.heightBlocks())
					));
				}
			}
		}

		displays.sort(Comparator.comparingDouble(info -> info.rootPos.getCenter().distanceToSqr(origin)));
		return displays;
	}

	private static @Nullable DisplayBrowserSession getOrCreateSession(ClientLevel level, BlockPos rootPos) {
		if (!BrowserBootstrap.isReady()) {
			return null;
		}

		if (!(level.getBlockEntity(rootPos) instanceof DisplayBlockEntity display)) {
			DisplayKey staleKey = new DisplayKey(level.dimension(), rootPos);
			DisplayBrowserSession staleSession = SESSIONS.remove(staleKey);
			if (staleSession != null) {
				staleSession.close();
			}
			return null;
		}
		if (!display.isPowered()) {
			DisplayBrowserSession suspendedSession = SESSIONS.get(new DisplayKey(level.dimension(), rootPos));
			if (suspendedSession != null) {
				suspendedSession.suspend();
			}
			return null;
		}

		DisplayCluster cluster = display.getCluster();
		DisplayKey key = new DisplayKey(level.dimension(), cluster.root());
		DisplayBrowserSession session = SESSIONS.get(key);
		if (session == null) {
			session = moveExistingSession(level.dimension(), cluster, key);
			if (session == null) {
				session = new DisplayBrowserSession(key);
			}
			SESSIONS.put(key, session);
		}

		session.key = key;
		session.clusterBlocks = cluster.blocks();
		session.lastAccessTick = tickCounter;
		session.resume();
		session.syncAuthoritative(display.getScreenState().adaptToAspect(cluster.widthBlocks(), cluster.heightBlocks()), tickCounter);
		return session;
	}

	private static DisplayStateData adaptStateToCluster(ClientLevel level, BlockPos rootPos, DisplayStateData state) {
		if (!(level.getBlockEntity(rootPos) instanceof DisplayBlockEntity display)) {
			return state.sanitize();
		}

		DisplayCluster cluster = display.getCluster();
		return state.adaptToAspect(cluster.widthBlocks(), cluster.heightBlocks());
	}

	private static @Nullable DisplayBrowserSession moveExistingSession(ResourceKey<Level> dimension, DisplayCluster cluster, DisplayKey replacementKey) {
		Iterator<Map.Entry<DisplayKey, DisplayBrowserSession>> iterator = SESSIONS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<DisplayKey, DisplayBrowserSession> entry = iterator.next();
			DisplayKey existingKey = entry.getKey();
			DisplayBrowserSession session = entry.getValue();
			if (!existingKey.dimension.equals(dimension) || !session.overlaps(cluster.blocks())) {
				continue;
			}

			iterator.remove();
			session.key = replacementKey;
			session.clusterBlocks = cluster.blocks();
			return session;
		}

		return null;
	}

	public record NearbyDisplayInfo(BlockPos rootPos, int widthBlocks, int heightBlocks, boolean powered, DisplayStateData state) {
	}

	public static final class DisplayBrowserSession {
		private DisplayKey key;
		private final List<MCEFBrowser> browsers = new ArrayList<>();
		private final Map<MCEFBrowser, String> volumeSyncedUrls = new IdentityHashMap<>();
		private DisplayStateData state = DisplayStateData.DEFAULT;
		private long lastAccessTick;
		private Set<BlockPos> clusterBlocks = Set.of();
		private @Nullable DisplayStateData previewState;
		private long previewStateUntilTick;
		private boolean suspended;

		private DisplayBrowserSession(DisplayKey key) {
			this.key = key;
		}

		public DisplayStateData state() {
			return state;
		}

		public @Nullable MCEFBrowser activeBrowser() {
			if (browsers.isEmpty()) {
				return null;
			}

			int index = Math.max(0, Math.min(state.activeTab(), browsers.size() - 1));
			return browsers.get(index);
		}

		public void sync(DisplayStateData newState) {
			DisplayStateData sanitized = newState.sanitize();
			boolean stateChanged = !sanitized.equals(this.state);
			if (!stateChanged && browsers.size() == sanitized.tabs().size()) {
				return;
			}

			DisplayStateData previousState = this.state;
			this.state = sanitized;
			boolean resolutionChanged = previousState.resolutionWidth() != sanitized.resolutionWidth()
					|| previousState.resolutionHeight() != sanitized.resolutionHeight();
			boolean volumeChanged = Float.compare(previousState.volume(), sanitized.volume()) != 0;

			while (browsers.size() > sanitized.tabs().size()) {
				MCEFBrowser browser = browsers.removeLast();
				volumeSyncedUrls.remove(browser);
				BrowserRenderUtil.release(browser);
				browser.close();
			}

			for (int i = 0; i < sanitized.tabs().size(); i++) {
				String url = sanitized.tabs().get(i).currentUrl();
				MCEFBrowser browser;
				boolean created = false;
				boolean urlChanged = false;
				if (i >= browsers.size()) {
					browser = MCEFApi.getInstance().createBrowser(url, false);
					browsers.add(browser);
					created = true;
				} else {
					browser = browsers.get(i);
					String currentUrl = browser.getCefBrowser().getURL();
					if (stateChanged && !url.equals(currentUrl)) {
						browser.getCefBrowser().loadURL(url);
						urlChanged = true;
					}
				}

				if (created || resolutionChanged) {
					browser.resize(sanitized.resolutionWidth(), sanitized.resolutionHeight());
				}
				if (created) {
					browser.setFocus(false);
				}
				if (created || volumeChanged || urlChanged) {
					volumeSyncedUrls.remove(browser);
				}
				if (suspended) {
					suspendBrowser(browser);
				}
			}
		}

		public void syncPreview(DisplayStateData newState, long currentTick) {
			DisplayStateData sanitized = newState.sanitize();
			previewState = sanitized;
			previewStateUntilTick = currentTick + PREVIEW_SYNC_GRACE_TICKS;
			sync(sanitized);
		}

		public void syncAuthoritative(DisplayStateData newState, long currentTick) {
			DisplayStateData sanitized = newState.sanitize();
			if (previewState != null) {
				if (sanitized.equals(previewState)) {
					previewState = null;
				} else if (currentTick <= previewStateUntilTick) {
					return;
				} else {
					previewState = null;
				}
			}

			sync(sanitized);
		}

		public void close() {
			for (MCEFBrowser browser : browsers) {
				BrowserRenderUtil.release(browser);
				suspendBrowser(browser);
				browser.close();
			}
			browsers.clear();
			volumeSyncedUrls.clear();
			clusterBlocks = Set.of();
			suspended = false;
		}

		public boolean overlaps(Set<BlockPos> blocks) {
			if (clusterBlocks.isEmpty() || blocks.isEmpty()) {
				return false;
			}

			Set<BlockPos> smaller = clusterBlocks.size() <= blocks.size() ? clusterBlocks : blocks;
			Set<BlockPos> larger = smaller == clusterBlocks ? blocks : clusterBlocks;
			for (BlockPos pos : smaller) {
				if (larger.contains(pos)) {
					return true;
				}
			}
			return false;
		}

		public boolean containsBrowser(MCEFBrowser browser) {
			return browsers.contains(browser);
		}

		public void suspend() {
			if (suspended) {
				return;
			}

			suspended = true;
			for (MCEFBrowser browser : browsers) {
				suspendBrowser(browser);
			}
		}

		public void resume() {
			if (!suspended) {
				return;
			}

			suspended = false;
			for (MCEFBrowser browser : browsers) {
				applyVolume(browser, state.volume());
			}
		}

		public void refreshBrowserState() {
			if (suspended) {
				return;
			}

			for (MCEFBrowser browser : browsers) {
				String currentUrl = browser.getCefBrowser().getURL();
				if (browser.getCefBrowser().isLoading() || Objects.equals(volumeSyncedUrls.get(browser), currentUrl)) {
					continue;
				}

				applyVolume(browser, state.volume());
				volumeSyncedUrls.put(browser, currentUrl);
			}
		}

		public void applyInput(int eventType, int x, int y, int button, int keyCode, int scanCode, int modifiers, int codePoint, double scrollDelta) {
			MCEFBrowser browser = activeBrowser();
			if (browser == null) {
				return;
			}

			switch (eventType) {
				case ComputerpcNetworking.EVENT_MOUSE_MOVE -> browser.onMouseMoved(x, y);
				case ComputerpcNetworking.EVENT_MOUSE_PRESS -> browser.onMouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers)), false);
				case ComputerpcNetworking.EVENT_MOUSE_RELEASE -> browser.onMouseReleased(new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers)));
				case ComputerpcNetworking.EVENT_MOUSE_SCROLL -> browser.onMouseScrolled(x, y, scrollDelta);
				case ComputerpcNetworking.EVENT_KEY_PRESS -> browser.onKeyPressed(new KeyEvent(keyCode, scanCode, modifiers));
				case ComputerpcNetworking.EVENT_KEY_RELEASE -> browser.onKeyReleased(new KeyEvent(keyCode, scanCode, modifiers));
				case ComputerpcNetworking.EVENT_CHAR_TYPED -> browser.onCharTyped(new CharacterEvent(codePoint));
				default -> {
				}
			}
		}

		public String currentUrl() {
			MCEFBrowser browser = activeBrowser();
			String authoritativeUrl = state.activeTabData().currentUrl();
			if (browser == null) {
				return authoritativeUrl;
			}

			String currentUrl = browser.getCefBrowser().getURL();
			if (currentUrl == null || currentUrl.isBlank()) {
				return authoritativeUrl;
			}
			if (browser.getCefBrowser().isLoading()
					&& BrowserTabData.defaultUrl().equals(currentUrl)
					&& !BrowserTabData.defaultUrl().equals(authoritativeUrl)) {
				return authoritativeUrl;
			}
			return currentUrl;
		}

		private static void applyVolume(MCEFBrowser browser, float volume) {
			String normalizedVolume = Float.toString(Mth.clamp(volume, 0.0F, 1.0F));
			String script = """
					(() => {
					  const volume = %s;
					  const attach = (element) => {
					    if (!(element instanceof HTMLMediaElement) || element.dataset.computerpcVolumeHook === '1') {
					      return;
					    }
					    const apply = () => {
					      const stored = typeof window.__computerpcVolume === 'number' ? window.__computerpcVolume : volume;
					      try {
					        element.volume = stored;
					        element.muted = stored <= 0.0;
					        element.defaultMuted = stored <= 0.0;
					      } catch (ignored) {
					      }
					    };
					    element.addEventListener('loadedmetadata', apply, true);
					    element.addEventListener('canplay', apply, true);
					    element.addEventListener('play', apply, true);
					    element.dataset.computerpcVolumeHook = '1';
					  };
					  const scan = (root) => {
					    if (!(root instanceof Element || root instanceof Document)) {
					      return;
					    }
					    if (root instanceof Element && root.matches('video, audio')) {
					      attach(root);
					    }
					    if (typeof root.querySelectorAll === 'function') {
					      root.querySelectorAll('video, audio').forEach(attach);
					    }
					  };
					  window.__computerpcVolume = volume;
					  scan(document);
					  document.querySelectorAll('video, audio').forEach((element) => {
					    try {
					      element.volume = volume;
					      element.muted = volume <= 0.0;
					      element.defaultMuted = volume <= 0.0;
					    } catch (ignored) {
					    }
					  });
					  if (window.__computerpcVolumeObserver !== true) {
					    new MutationObserver((mutations) => {
					      for (const mutation of mutations) {
					        mutation.addedNodes.forEach(scan);
					      }
					      document.querySelectorAll('video, audio').forEach((element) => {
					        try {
					          element.volume = window.__computerpcVolume;
					          element.muted = window.__computerpcVolume <= 0.0;
					          element.defaultMuted = window.__computerpcVolume <= 0.0;
					        } catch (ignored) {
					        }
					      });
					    }).observe(document.documentElement || document.body, {childList: true, subtree: true});
					    window.__computerpcVolumeObserver = true;
					  }
					})();
					""".formatted(normalizedVolume);
			String url = browser.getCefBrowser().getURL();
			browser.getCefBrowser().executeJavaScript(script, url == null ? "about:blank" : url, 0);
		}

		private static void suspendBrowser(MCEFBrowser browser) {
			browser.getCefBrowser().stopLoad();
			browser.setFocus(false);
			String url = browser.getCefBrowser().getURL();
			browser.getCefBrowser().executeJavaScript("""
					(() => {
					  document.querySelectorAll('video, audio').forEach((element) => {
					    try {
					      element.pause();
					    } catch (ignored) {
					    }
					  });
					  try {
					    window.stop();
					  } catch (ignored) {
					  }
					})();
					""", url == null ? "about:blank" : url, 0);
		}
	}

	private record DisplayKey(ResourceKey<Level> dimension, BlockPos rootPos) {
	}
}
