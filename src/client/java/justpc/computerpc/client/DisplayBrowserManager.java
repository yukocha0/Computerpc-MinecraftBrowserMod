package justpc.computerpc.client;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import justpc.computerpc.blockentity.DisplayBlockEntity;
import justpc.computerpc.browser.DisplayStateData;
import justpc.computerpc.network.ComputerpcNetworking;
import justpc.computerpc.network.ComputerpcPayloads;
import justpc.computerpc.util.DisplayCluster;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLoadHandlerAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DisplayBrowserManager {
	private static final long PREVIEW_SYNC_GRACE_TICKS = 40L;
	private static final Map<DisplayKey, DisplayBrowserSession> SESSIONS = new HashMap<>();
	private static boolean loadHandlerRegistered;
	private static long tickCounter;

	private DisplayBrowserManager() {
	}

	public static void tick(Minecraft client) {
		tickCounter++;
		if (client.level == null) {
			closeAll();
			return;
		}

		ensureLoadHandlerRegistered();
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
			}
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
		BlockPos center = BlockPos.containing(origin);
		int range = Mth.ceil(radius);
		double maxDistanceSqr = radius * radius;

		for (int x = center.getX() - range; x <= center.getX() + range; x++) {
			for (int y = center.getY() - range; y <= center.getY() + range; y++) {
				for (int z = center.getZ() - range; z <= center.getZ() + range; z++) {
					BlockPos currentPos = new BlockPos(x, y, z);
					if (!(level.getBlockEntity(currentPos) instanceof DisplayBlockEntity display)) {
						continue;
					}

					DisplayCluster cluster = display.getCluster();
					BlockPos rootPos = cluster.root();
					if (!seenRoots.add(rootPos)) {
						continue;
					}

					if (rootPos.getCenter().distanceToSqr(origin) > maxDistanceSqr) {
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

		ensureLoadHandlerRegistered();
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

	private static void ensureLoadHandlerRegistered() {
		if (loadHandlerRegistered || !BrowserBootstrap.isReady()) {
			return;
		}

		MCEF.getClient().addLoadHandler(new CefLoadHandlerAdapter() {
			@Override
			public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
				if (!frame.isMain() || !(browser instanceof MCEFBrowser mcefBrowser)) {
					return;
				}

				Minecraft.getInstance().submit(() -> {
					DisplayBrowserSession session = findSession(mcefBrowser);
					if (session != null) {
						session.handleLoadEnd(mcefBrowser);
					}
				});
			}
		});
		loadHandlerRegistered = true;
	}

	private static @Nullable DisplayBrowserSession findSession(MCEFBrowser browser) {
		for (DisplayBrowserSession session : SESSIONS.values()) {
			if (session.containsBrowser(browser)) {
				return session;
			}
		}
		return null;
	}

	public record NearbyDisplayInfo(BlockPos rootPos, int widthBlocks, int heightBlocks, boolean powered, DisplayStateData state) {
	}

	public static final class DisplayBrowserSession {
		private DisplayKey key;
		private final List<MCEFBrowser> browsers = new ArrayList<>();
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
				browser.close();
			}

			for (int i = 0; i < sanitized.tabs().size(); i++) {
				String url = sanitized.tabs().get(i).currentUrl();
				MCEFBrowser browser;
				boolean created = false;
				boolean urlChanged = false;
				if (i >= browsers.size()) {
					browser = MCEF.createBrowser(url, false);
					browser.useBrowserControls(false);
					browsers.add(browser);
					created = true;
				} else {
					browser = browsers.get(i);
					if (stateChanged && !url.equals(browser.getURL())) {
						browser.loadURL(url);
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
					applyVolume(browser, sanitized.volume());
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
				suspendBrowser(browser);
				browser.close();
			}
			browsers.clear();
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

		public void handleLoadEnd(MCEFBrowser browser) {
			applyVolume(browser, state.volume());
			if (suspended) {
				suspendBrowser(browser);
			}
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

		public void applyInput(int eventType, int x, int y, int button, int keyCode, int scanCode, int modifiers, int codePoint, double scrollDelta) {
			MCEFBrowser browser = activeBrowser();
			if (browser == null) {
				return;
			}

			switch (eventType) {
				case ComputerpcNetworking.EVENT_MOUSE_MOVE -> browser.sendMouseMove(x, y);
				case ComputerpcNetworking.EVENT_MOUSE_PRESS -> browser.sendMousePress(x, y, button);
				case ComputerpcNetworking.EVENT_MOUSE_RELEASE -> browser.sendMouseRelease(x, y, button);
				case ComputerpcNetworking.EVENT_MOUSE_SCROLL -> browser.sendMouseWheel(x, y, scrollDelta, modifiers);
				case ComputerpcNetworking.EVENT_KEY_PRESS -> browser.sendKeyPress(keyCode, scanCode, modifiers);
				case ComputerpcNetworking.EVENT_KEY_RELEASE -> browser.sendKeyRelease(keyCode, scanCode, modifiers);
				case ComputerpcNetworking.EVENT_CHAR_TYPED -> browser.sendKeyTyped((char) codePoint, modifiers);
				default -> {
				}
			}
		}

		public String currentUrl() {
			MCEFBrowser browser = activeBrowser();
			return browser != null ? browser.getURL() : state.activeTabData().currentUrl();
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
			browser.executeJavaScript(script, browser.getURL(), 0);
		}

		private static void suspendBrowser(MCEFBrowser browser) {
			browser.stopLoad();
			browser.setFocus(false);
			browser.executeJavaScript("""
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
					""", browser.getURL(), 0);
		}
	}

	private record DisplayKey(ResourceKey<Level> dimension, BlockPos rootPos) {
	}
}
