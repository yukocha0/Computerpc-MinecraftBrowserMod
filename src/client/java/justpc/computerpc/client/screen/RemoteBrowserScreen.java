package justpc.computerpc.client.screen;

import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.serialization.DataResult;
import justpc.computerpc.browser.BrowserTabData;
import justpc.computerpc.browser.DisplayStateData;
import justpc.computerpc.client.BrowserBootstrap;
import justpc.computerpc.client.DisplayBrowserManager;
import justpc.computerpc.client.render.BrowserRenderUtil;
import justpc.computerpc.client.widget.VolumeSlider;
import justpc.computerpc.network.ComputerpcNetworking;
import justpc.computerpc.network.ComputerpcPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class RemoteBrowserScreen extends Screen {
	private static final int TAB_ROW_Y = 4;
	private static final int TOOLBAR_Y = 26;
	private static final int FOOTER_HEIGHT = 38;
	private static final int BUTTON_HEIGHT = 18;
	private static final int TAB_CELL_WIDTH = 92;
	private static final int TAB_LABEL_WIDTH = 76;
	private static final int LOADING_BAR_HEIGHT = 2;
	private static final int LOADING_BAR_COLOR = 0xFF3BA8FF;
	private static final int[] RESOLUTION_PRESET_HEIGHTS = new int[] {144, 240, 360, 480, 720, 1080};

	private List<DisplayBrowserManager.NearbyDisplayInfo> nearbyDisplays = List.of();
	private int selectedDisplayIndex;
	private int tabScrollOffset;
	private DisplayStateData workingState = DisplayStateData.DEFAULT;
	private @Nullable EditBox urlBox;
	private @Nullable VolumeSlider volumeSlider;
	private int ticksSinceRefresh;
	private boolean browserFocused;
	private boolean rebuildRequested;
	private int viewportX;
	private int viewportY;
	private int viewportWidth;
	private int viewportHeight;
	private int footerControlsX = Integer.MAX_VALUE;

	public RemoteBrowserScreen() {
		super(Component.literal("Remote Browser"));
	}

	@Override
	protected void init() {
		refreshNearbyDisplays();
		buildWidgets();
		updateBrowserFocus();
	}

	@Override
	public void tick() {
		super.tick();

		ticksSinceRefresh++;
		if (ticksSinceRefresh >= 20) {
			if (refreshNearbyDisplays()) {
				requestRebuild();
			}
			ticksSinceRefresh = 0;
		}

		if (minecraft.level == null || selectedDisplay() == null) {
			if (browserFocused) {
				browserFocused = false;
				updateBrowserFocus();
			}
			if (rebuildRequested) {
				buildWidgets();
			}
			return;
		}

		DisplayBrowserManager.NearbyDisplayInfo selected = selectedDisplay();
		DisplayBrowserManager.DisplayBrowserSession session = activeSession();
		if (session != null) {
			String currentUrl = session.currentUrl();
			if (!currentUrl.equals(workingState.activeTabData().currentUrl())) {
				workingState = workingState.syncActiveUrl(currentUrl);
				ClientPlayNetworking.send(new ComputerpcPayloads.BrowserNavigateC2S(selected.rootPos(), currentUrl));
				requestRebuild();
			}

			MCEFBrowser browser = session.activeBrowser();
			if (browser != null) {
				browser.setFocus(browserFocused && urlBox != null && !urlBox.isFocused());
			}
		}

		if (urlBox != null && !urlBox.isFocused()) {
			String currentUrl = workingState.activeTabData().currentUrl();
			if (!currentUrl.equals(urlBox.getValue())) {
				urlBox.setValue(currentUrl);
			}
		}

		if (rebuildRequested) {
			buildWidgets();
		}
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}

		if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0 && event.key() == GLFW.GLFW_KEY_L && urlBox != null) {
			browserFocused = false;
			urlBox.setFocused(true);
			updateBrowserFocus();
			return true;
		}

		if (urlBox != null && urlBox.isFocused()) {
			if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
				navigateToUrl(urlBox.getValue());
				return true;
			}
			return urlBox.keyPressed(event);
		}

		if (browserFocused && sendBrowserInput(ComputerpcNetworking.EVENT_KEY_PRESS, 0, 0, 0, event.key(), (int) event.scancode(), event.modifiers(), 0, 0.0)) {
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
		if (browserFocused && sendBrowserInput(ComputerpcNetworking.EVENT_KEY_RELEASE, 0, 0, 0, event.key(), (int) event.scancode(), event.modifiers(), 0, 0.0)) {
			return true;
		}

		return super.keyReleased(event);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
		if (urlBox != null && urlBox.isFocused()) {
			return urlBox.charTyped(event);
		}

		if (browserFocused && sendBrowserInput(ComputerpcNetworking.EVENT_CHAR_TYPED, 0, 0, 0, 0, 0, 0, event.codepoint(), 0.0)) {
			return true;
		}

		return super.charTyped(event);
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean primary) {
		if (super.mouseClicked(event, primary)) {
			browserFocused = false;
			updateBrowserFocus();
			return true;
		}

		if (!insideViewport(event.x(), event.y())) {
			browserFocused = false;
			updateBrowserFocus();
			return false;
		}

		if (urlBox != null) {
			urlBox.setFocused(false);
		}

		browserFocused = true;
		updateBrowserFocus();
		return sendBrowserInput(ComputerpcNetworking.EVENT_MOUSE_PRESS, browserX(event.x()), browserY(event.y()), event.button(), 0, 0, 0, 0, 0.0);
	}

	@Override
	public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
		if (browserFocused) {
			return sendBrowserInput(ComputerpcNetworking.EVENT_MOUSE_RELEASE, browserX(event.x()), browserY(event.y()), event.button(), 0, 0, 0, 0, 0.0);
		}

		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
		if (browserFocused) {
			return sendBrowserInput(ComputerpcNetworking.EVENT_MOUSE_MOVE, browserX(event.x()), browserY(event.y()), event.button(), 0, 0, 0, 0, 0.0);
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (browserFocused) {
			sendBrowserInput(ComputerpcNetworking.EVENT_MOUSE_MOVE, browserX(mouseX), browserY(mouseY), 0, 0, 0, 0, 0, 0.0);
		}

		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (insideViewport(mouseX, mouseY)) {
			browserFocused = true;
			updateBrowserFocus();
			return sendBrowserInput(ComputerpcNetworking.EVENT_MOUSE_SCROLL, browserX(mouseX), browserY(mouseY), 0, 0, 0, 0, 0, verticalAmount);
		}

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	@Override
	public void onClose() {
		browserFocused = false;
		updateBrowserFocus();
		super.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		layoutViewport();
		BrowserRenderUtil.AspectBox browserArea = browserArea();

		graphics.fill(0, 0, width, height, 0xFF0A0D12);
		graphics.fill(0, 0, width, 22, 0xFF151A24);
		graphics.fill(0, 22, width, 46, 0xFF121720);
		graphics.fill(0, height - FOOTER_HEIGHT, width, height, 0xFF151A24);

		graphics.fill(viewportX - 2, viewportY - 2, viewportX + viewportWidth + 2, viewportY + viewportHeight + 2, browserFocused ? 0xFF4D9FE8 : 0xFF273142);
		graphics.fill(viewportX, viewportY, viewportX + viewportWidth, viewportY + viewportHeight, 0xFF05070A);
		graphics.fill(browserArea.x() - 1, browserArea.y() - 1, browserArea.maxX() + 1, browserArea.maxY() + 1, 0xFF1E2632);

		DisplayBrowserManager.NearbyDisplayInfo selected = selectedDisplay();
		DisplayBrowserManager.DisplayBrowserSession session = activeSession();
		MCEFBrowser browser = session == null ? null : session.activeBrowser();

		if (selected == null) {
			graphics.centeredText(font, Component.literal("No nearby display screens were found."), width / 2, height / 2 - 14, 0xFFE3E8EF);
			graphics.centeredText(font, Component.literal("Place and power displays, then press Scan."), width / 2, height / 2 + 4, 0xFF9AA7B8);
		} else if (browser != null && browser.isTextureReady()) {
			BrowserRenderUtil.drawGuiTexture(graphics, browser, browserArea.x(), browserArea.y(), browserArea.width(), browserArea.height());
		} else {
			String status = selected.powered() ? BrowserBootstrap.getStatus() : "Display is powered off";
			graphics.centeredText(font, Component.literal(status), width / 2, viewportY + viewportHeight / 2 - 12, 0xFFD9E3F0);
			graphics.centeredText(font, Component.literal("The selected display will mirror this browser when Chromium is ready."), width / 2, viewportY + viewportHeight / 2 + 4, 0xFF8E9CAF);
		}

		graphics.text(font, Component.literal("REMOTE"), 8, height - 31, 0xFFF1F5FA, false);
		graphics.text(font, Component.literal(browserFocused ? "Page Input" : "UI Input"), 8, height - 18, 0xFF90A0B3, false);
		if (selected != null && footerControlsX > 210) {
			graphics.text(font, Component.literal(displaySummary()), 72, height - 18, 0xFFB9C6D8, false);
		}

		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		renderLoadingBar(graphics, browser);
	}

	private void buildWidgets() {
		boolean preserveUrlFocus = urlBox != null && urlBox.isFocused();
		String draftUrl = urlBox != null ? urlBox.getValue() : workingState.activeTabData().currentUrl();

		rebuildRequested = false;
		clearWidgets();
		layoutViewport();
		footerControlsX = width - 8;
		urlBox = null;
		volumeSlider = null;

		addRenderableWidget(Button.builder(Component.literal("X"), button -> onClose())
				.bounds(width - 24, TAB_ROW_Y, 18, BUTTON_HEIGHT)
				.build());

		if (selectedDisplay() == null) {
			addRenderableWidget(Button.builder(Component.literal("Scan"), button -> {
				if (refreshNearbyDisplays()) {
					requestRebuild();
				}
			}).bounds(width - 50, TOOLBAR_Y, 44, BUTTON_HEIGHT).build());
			return;
		}

		addRenderableWidget(Button.builder(Component.literal("+"), button -> {
			workingState = workingState.withAddedTab(BrowserTabData.defaultUrl());
			browserFocused = false;
			pushWorkingState(true);
		}).bounds(6, TAB_ROW_Y, 18, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal("<<"), button -> {
			tabScrollOffset = Math.max(0, tabScrollOffset - 1);
			requestRebuild();
		}).bounds(28, TAB_ROW_Y, 24, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal(">>"), button -> {
			tabScrollOffset++;
			requestRebuild();
		}).bounds(56, TAB_ROW_Y, 24, BUTTON_HEIGHT).build());

		int tabAreaStart = 84;
		int tabAreaWidth = Math.max(TAB_CELL_WIDTH, width - tabAreaStart - 30);
		int visibleTabs = Math.max(1, tabAreaWidth / TAB_CELL_WIDTH);
		int maxTabOffset = Math.max(0, workingState.tabs().size() - visibleTabs);
		tabScrollOffset = Mth.clamp(tabScrollOffset, 0, maxTabOffset);

		int tabX = tabAreaStart;
		for (int i = tabScrollOffset; i < workingState.tabs().size() && tabX + TAB_CELL_WIDTH <= width - 26; i++) {
			int index = i;
			BrowserTabData tab = workingState.tabs().get(i);
			Component label = Component.literal((workingState.activeTab() == index ? "* " : "") + ellipsize(tab.title(), 11));
			addRenderableWidget(Button.builder(label, button -> {
				workingState = workingState.withActiveTab(index);
				browserFocused = false;
				pushWorkingState(true);
			}).bounds(tabX, TAB_ROW_Y, TAB_LABEL_WIDTH, BUTTON_HEIGHT).build());
			addRenderableWidget(Button.builder(Component.literal("x"), button -> {
				workingState = workingState.withRemovedTab(index);
				browserFocused = false;
				pushWorkingState(true);
			}).bounds(tabX + TAB_LABEL_WIDTH, TAB_ROW_Y, 14, BUTTON_HEIGHT).build());
			tabX += TAB_CELL_WIDTH;
		}

		addRenderableWidget(Button.builder(Component.literal("<"), button -> navigateBack())
				.bounds(6, TOOLBAR_Y, 20, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal(">"), button -> navigateForward())
				.bounds(30, TOOLBAR_Y, 20, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("H"), button -> navigateToUrl(BrowserTabData.defaultUrl()))
				.bounds(54, TOOLBAR_Y, 20, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("R"), button -> reloadBrowser())
				.bounds(78, TOOLBAR_Y, 20, BUTTON_HEIGHT)
				.build());

		int scanWidth = 44;
		int goWidth = 34;
		int scanX = width - 6 - scanWidth;
		int goX = scanX - 4 - goWidth;
		int urlX = 102;
		int urlWidth = Math.max(100, goX - urlX - 4);

		EditBox newUrlBox = new EditBox(font, urlX, TOOLBAR_Y, urlWidth, BUTTON_HEIGHT, Component.literal("URL"));
		newUrlBox.setMaxLength(2048);
		newUrlBox.setValue(draftUrl);
		newUrlBox.setFocused(preserveUrlFocus);
		urlBox = addRenderableWidget(newUrlBox);

		addRenderableWidget(Button.builder(Component.literal("Go"), button -> navigateToUrl(urlBox == null ? workingState.activeTabData().currentUrl() : urlBox.getValue()))
				.bounds(goX, TOOLBAR_Y, goWidth, BUTTON_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(Component.literal("Scan"), button -> {
			if (refreshNearbyDisplays()) {
				requestRebuild();
			}
		}).bounds(scanX, TOOLBAR_Y, scanWidth, BUTTON_HEIGHT).build());

		int footerY = height - 25;
		int resolutionWidth = Mth.clamp(width / 7, 82, 106);
		int screenLabelWidth = Mth.clamp(width / 7, 92, 124);
		int sliderWidth = Mth.clamp(width / 5, 96, 144);

		int resolutionX = width - 8 - resolutionWidth;
		int screenNextX = resolutionX - 4 - 18;
		int screenLabelX = screenNextX - screenLabelWidth;
		int screenPrevX = screenLabelX - 18;
		int sliderX = screenPrevX - 4 - sliderWidth;

		if (sliderX < 8) {
			sliderWidth = Math.max(80, sliderWidth - (8 - sliderX));
			sliderX = screenPrevX - 4 - sliderWidth;
		}
		if (sliderX < 8) {
			screenLabelWidth = Math.max(80, screenLabelWidth - (8 - sliderX));
			screenLabelX = screenNextX - screenLabelWidth;
			screenPrevX = screenLabelX - 18;
			sliderX = screenPrevX - 4 - sliderWidth;
		}
		if (sliderX < 8) {
			resolutionWidth = Math.max(74, resolutionWidth - (8 - sliderX));
			resolutionX = width - 8 - resolutionWidth;
			screenNextX = resolutionX - 4 - 18;
			screenLabelX = screenNextX - screenLabelWidth;
			screenPrevX = screenLabelX - 18;
			sliderX = screenPrevX - 4 - sliderWidth;
		}

		footerControlsX = Math.max(8, sliderX);

		volumeSlider = addRenderableWidget(new VolumeSlider(footerControlsX, footerY, sliderWidth, 18, workingState.volume(), value -> {
			workingState = workingState.withVolume((float) value);
			pushWorkingState(false);
		}));

		addRenderableWidget(Button.builder(Component.literal("<"), button -> cycleDisplay(-1))
				.bounds(screenPrevX, footerY, 18, 18)
				.build());
		addRenderableWidget(Button.builder(Component.literal(selectedScreenLabel()), button -> requestRebuild())
				.bounds(screenLabelX, footerY, screenLabelWidth, 18)
				.build());
		addRenderableWidget(Button.builder(Component.literal(">"), button -> cycleDisplay(1))
				.bounds(screenNextX, footerY, 18, 18)
				.build());

		addRenderableWidget(Button.builder(Component.literal(currentResolutionLabel()), button -> {
			workingState = nextResolution(workingState);
			pushWorkingState(true);
		}).bounds(resolutionX, footerY, resolutionWidth, 18).build());
	}

	private boolean refreshNearbyDisplays() {
		if (minecraft.player == null || minecraft.level == null) {
			boolean changed = !nearbyDisplays.isEmpty() || selectedDisplayIndex != 0 || !workingState.equals(DisplayStateData.DEFAULT);
			nearbyDisplays = List.of();
			selectedDisplayIndex = 0;
			tabScrollOffset = 0;
			workingState = DisplayStateData.DEFAULT;
			return changed;
		}

		@Nullable DisplayBrowserManager.NearbyDisplayInfo previousSelection = selectedDisplay();
		int previousCount = nearbyDisplays.size();
		nearbyDisplays = DisplayBrowserManager.findNearbyDisplays(minecraft.level, minecraft.player.position(), 50.0);
		if (nearbyDisplays.isEmpty()) {
			selectedDisplayIndex = 0;
			tabScrollOffset = 0;
			workingState = DisplayStateData.DEFAULT;
			return previousCount > 0 || previousSelection != null;
		}

		int newIndex = Math.max(0, Math.min(selectedDisplayIndex, nearbyDisplays.size() - 1));
		if (previousSelection != null) {
			for (int i = 0; i < nearbyDisplays.size(); i++) {
				if (nearbyDisplays.get(i).rootPos().equals(previousSelection.rootPos())) {
					newIndex = i;
					break;
				}
			}
		}

		DisplayBrowserManager.NearbyDisplayInfo currentSelection = nearbyDisplays.get(newIndex);
		boolean selectionChanged = previousSelection == null || !currentSelection.rootPos().equals(previousSelection.rootPos());
		boolean geometryChanged = previousSelection == null
				|| previousSelection.widthBlocks() != currentSelection.widthBlocks()
				|| previousSelection.heightBlocks() != currentSelection.heightBlocks();
		selectedDisplayIndex = newIndex;
		if (selectionChanged || geometryChanged) {
			workingState = currentSelection.state();
			tabScrollOffset = 0;
		}

		return selectionChanged || geometryChanged || previousCount != nearbyDisplays.size();
	}

	private void navigateToUrl(String input) {
		workingState = workingState.navigateActive(input);
		browserFocused = false;
		pushWorkingState(true);
	}

	private void navigateBack() {
		DisplayBrowserManager.DisplayBrowserSession session = activeSession();
		if (session != null && session.activeBrowser() != null) {
			session.activeBrowser().goBack();
			requestRebuild();
			return;
		}

		workingState = workingState.goBack();
		pushWorkingState(true);
	}

	private void navigateForward() {
		DisplayBrowserManager.DisplayBrowserSession session = activeSession();
		if (session != null && session.activeBrowser() != null) {
			session.activeBrowser().goForward();
			requestRebuild();
			return;
		}

		workingState = workingState.goForward();
		pushWorkingState(true);
	}

	private void reloadBrowser() {
		DisplayBrowserManager.DisplayBrowserSession session = activeSession();
		if (session != null && session.activeBrowser() != null) {
			session.activeBrowser().reload();
		}
	}

	private void cycleDisplay(int direction) {
		if (nearbyDisplays.isEmpty()) {
			return;
		}

		selectedDisplayIndex = Math.floorMod(selectedDisplayIndex + direction, nearbyDisplays.size());
		workingState = nearbyDisplays.get(selectedDisplayIndex).state();
		browserFocused = false;
		requestRebuild();
	}

	private void pushWorkingState(boolean rebuildUi) {
		if (selectedDisplay() == null || minecraft.level == null) {
			return;
		}

		workingState = adaptResolutionToSelectedDisplay(workingState);
		DisplayBrowserManager.previewState(minecraft.level, selectedDisplay().rootPos(), workingState);
		DataResult<net.minecraft.nbt.Tag> encoded = DisplayStateData.CODEC.encodeStart(NbtOps.INSTANCE, workingState);
		encoded.result().ifPresent(tag -> ClientPlayNetworking.send(new ComputerpcPayloads.DisplayConfigC2S(
				selectedDisplay().rootPos(),
				(CompoundTag) tag
		)));

		if (rebuildUi) {
			requestRebuild();
		}
	}

	private void requestRebuild() {
		rebuildRequested = true;
	}

	private String displaySummary() {
		DisplayBrowserManager.NearbyDisplayInfo selected = selectedDisplay();
		if (selected == null) {
			return "No screen selected";
		}

		return "Screen " + (selectedDisplayIndex + 1) + "/" + nearbyDisplays.size()
				+ " | " + selected.widthBlocks() + "x" + selected.heightBlocks() + " blocks"
				+ " | " + (selected.powered() ? "On" : "Off");
	}

	private String selectedScreenLabel() {
		DisplayBrowserManager.NearbyDisplayInfo selected = selectedDisplay();
		if (selected == null) {
			return "No Screen";
		}

		String label = "S" + (selectedDisplayIndex + 1) + "/" + nearbyDisplays.size() + " " + selected.widthBlocks() + "x" + selected.heightBlocks();
		if (!selected.powered()) {
			label += " off";
		}
		return ellipsize(label, 12);
	}

	private String currentResolutionLabel() {
		return workingState.resolutionWidth() + "x" + workingState.resolutionHeight();
	}

	private DisplayStateData nextResolution(DisplayStateData state) {
		DisplayBrowserManager.NearbyDisplayInfo selected = selectedDisplay();
		if (selected == null) {
			return state;
		}

		int currentIndex = 0;
		for (int i = 0; i < RESOLUTION_PRESET_HEIGHTS.length; i++) {
			if (RESOLUTION_PRESET_HEIGHTS[i] <= state.resolutionHeight()) {
				currentIndex = i;
			}
		}

		int nextHeight = RESOLUTION_PRESET_HEIGHTS[(currentIndex + 1) % RESOLUTION_PRESET_HEIGHTS.length];
		int nextWidth = DisplayStateData.adaptedWidth(nextHeight, selected.widthBlocks(), selected.heightBlocks());
		return state.withResolution(nextWidth, nextHeight);
	}

	private @Nullable DisplayBrowserManager.NearbyDisplayInfo selectedDisplay() {
		if (nearbyDisplays.isEmpty()) {
			return null;
		}
		return nearbyDisplays.get(Mth.clamp(selectedDisplayIndex, 0, nearbyDisplays.size() - 1));
	}

	private @Nullable DisplayBrowserManager.DisplayBrowserSession activeSession() {
		if (minecraft.level == null || selectedDisplay() == null) {
			return null;
		}

		return DisplayBrowserManager.previewState(minecraft.level, selectedDisplay().rootPos(), workingState);
	}

	private boolean sendBrowserInput(int eventType, int x, int y, int button, int keyCode, int scanCode, int modifiers, int codePoint, double scrollDelta) {
		DisplayBrowserManager.NearbyDisplayInfo selected = selectedDisplay();
		DisplayBrowserManager.DisplayBrowserSession session = activeSession();
		if (selected == null || session == null) {
			return false;
		}

		session.applyInput(eventType, x, y, button, keyCode, scanCode, modifiers, codePoint, scrollDelta);
		ClientPlayNetworking.send(new ComputerpcPayloads.BrowserInputC2S(
				selected.rootPos(),
				eventType,
				x,
				y,
				button,
				keyCode,
				scanCode,
				modifiers,
				codePoint,
				scrollDelta
		));
		return true;
	}

	private void updateBrowserFocus() {
		DisplayBrowserManager.DisplayBrowserSession session = activeSession();
		if (session != null && session.activeBrowser() != null) {
			session.activeBrowser().setFocus(browserFocused && urlBox != null && !urlBox.isFocused());
		}
	}

	private void layoutViewport() {
		viewportX = 8;
		viewportY = 50;
		viewportWidth = Math.max(120, width - 16);
		viewportHeight = Math.max(90, height - viewportY - FOOTER_HEIGHT - 8);
	}

	private boolean insideViewport(double mouseX, double mouseY) {
		BrowserRenderUtil.AspectBox browserArea = browserArea();
		return mouseX >= browserArea.x() && mouseX < browserArea.maxX() && mouseY >= browserArea.y() && mouseY < browserArea.maxY();
	}

	private int browserX(double mouseX) {
		BrowserRenderUtil.AspectBox browserArea = browserArea();
		double normalized = (mouseX - browserArea.x()) / (double) Math.max(1, browserArea.width());
		return Mth.clamp((int) (normalized * workingState.resolutionWidth()), 0, Math.max(0, workingState.resolutionWidth() - 1));
	}

	private int browserY(double mouseY) {
		BrowserRenderUtil.AspectBox browserArea = browserArea();
		double normalized = (mouseY - browserArea.y()) / (double) Math.max(1, browserArea.height());
		return Mth.clamp((int) (normalized * workingState.resolutionHeight()), 0, Math.max(0, workingState.resolutionHeight() - 1));
	}

	private BrowserRenderUtil.AspectBox browserArea() {
		DisplayStateData browserState = adaptResolutionToSelectedDisplay(workingState);
		layoutViewport();
		return BrowserRenderUtil.fitInside(
				viewportX,
				viewportY,
				viewportWidth,
				viewportHeight,
				browserState.resolutionWidth(),
				browserState.resolutionHeight()
		);
	}

	private DisplayStateData adaptResolutionToSelectedDisplay(DisplayStateData state) {
		DisplayBrowserManager.NearbyDisplayInfo selected = selectedDisplay();
		if (selected == null) {
			return state.sanitize();
		}

		return state.adaptToAspect(selected.widthBlocks(), selected.heightBlocks());
	}

	private void renderLoadingBar(GuiGraphicsExtractor graphics, @Nullable MCEFBrowser browser) {
		if (browser == null || urlBox == null || !browser.isLoading()) {
			return;
		}

		int x = urlBox.getX();
		int y = urlBox.getY() - 2;
		int w = urlBox.getWidth();

		graphics.fill(x, y, x + w, y + LOADING_BAR_HEIGHT, 0x55000000);

		int segmentWidth = Math.max(20, w / 4);
		int travelRange = w + segmentWidth;
		int animatedOffset = (int) ((Util.getMillis() / 6L) % travelRange) - segmentWidth;

		int segmentStart = Math.max(x, x + animatedOffset);
		int segmentEnd = Math.min(x + w, x + animatedOffset + segmentWidth);
		if (segmentEnd > segmentStart) {
			graphics.fill(segmentStart, y, segmentEnd, y + LOADING_BAR_HEIGHT, LOADING_BAR_COLOR);
		}
	}

	private static String ellipsize(String value, int maxLength) {
		if (value == null || value.isBlank()) {
			return "Tab";
		}
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, Math.max(1, maxLength - 3)) + "...";
	}

}
