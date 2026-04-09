package justpc.computerpc.client.screen;

import com.cinemamod.mcef.MCEFBrowser;
import justpc.computerpc.client.BrowserBootstrap;
import justpc.computerpc.client.DisplayBrowserManager;
import justpc.computerpc.client.render.BrowserRenderUtil;
import justpc.computerpc.network.ComputerpcNetworking;
import justpc.computerpc.network.ComputerpcPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class DisplayControlScreen extends Screen {
	private final net.minecraft.core.BlockPos rootPos;
	private DisplayBrowserManager.DisplayBrowserSession session;
	private String lastKnownUrl = "";

	public DisplayControlScreen(net.minecraft.core.BlockPos rootPos) {
		super(Component.literal("Display Control"));
		this.rootPos = rootPos;
	}

	@Override
	protected void init() {
		if (minecraft.level != null) {
			session = DisplayBrowserManager.getSession(minecraft.level, rootPos);
			if (session != null) {
				lastKnownUrl = session.currentUrl();
				if (session.activeBrowser() != null) {
					session.activeBrowser().setFocus(true);
				}
			}
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (minecraft.level != null) {
			session = DisplayBrowserManager.getSession(minecraft.level, rootPos);
			if (session != null && session.activeBrowser() != null) {
				session.activeBrowser().setFocus(true);
			}
		}
		syncNavigation();
	}

	@Override
	public void onClose() {
		if (session != null && session.activeBrowser() != null) {
			session.activeBrowser().setFocus(false);
		}
		super.onClose();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, width, height, 0xFF050505);
		if (session == null || !BrowserBootstrap.isReady()) {
			graphics.centeredText(font, BrowserBootstrap.getStatus(), width / 2, height / 2 - 6, 0xD0D0D0);
			graphics.centeredText(font, Component.literal("Press Esc to exit"), width / 2, height / 2 + 10, 0x909090);
			super.extractRenderState(graphics, mouseX, mouseY, partialTick);
			return;
		}

		MCEFBrowser browser = session.activeBrowser();
		if (browser != null) {
			BrowserRenderUtil.drawGuiTexture(graphics, browser, 0, 0, width, height);
		}

		graphics.text(font, Component.literal("Esc exits remote control"), 10, 10, 0xFFFFFF, true);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
			onClose();
			return true;
		}

		if (session != null) {
			session.applyInput(ComputerpcNetworking.EVENT_KEY_PRESS, 0, 0, 0, event.key(), (int) event.scancode(), event.modifiers(), 0, 0);
			ClientPlayNetworking.send(new ComputerpcPayloads.BrowserInputC2S(rootPos, ComputerpcNetworking.EVENT_KEY_PRESS, 0, 0, 0, event.key(), (int) event.scancode(), event.modifiers(), 0, 0));
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean keyReleased(net.minecraft.client.input.KeyEvent event) {
		if (session != null) {
			session.applyInput(ComputerpcNetworking.EVENT_KEY_RELEASE, 0, 0, 0, event.key(), (int) event.scancode(), event.modifiers(), 0, 0);
			ClientPlayNetworking.send(new ComputerpcPayloads.BrowserInputC2S(rootPos, ComputerpcNetworking.EVENT_KEY_RELEASE, 0, 0, 0, event.key(), (int) event.scancode(), event.modifiers(), 0, 0));
			return true;
		}

		return super.keyReleased(event);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
		if (session != null) {
			session.applyInput(ComputerpcNetworking.EVENT_CHAR_TYPED, 0, 0, 0, 0, 0, 0, event.codepoint(), 0);
			ClientPlayNetworking.send(new ComputerpcPayloads.BrowserInputC2S(rootPos, ComputerpcNetworking.EVENT_CHAR_TYPED, 0, 0, 0, 0, 0, 0, event.codepoint(), 0));
			return true;
		}

		return super.charTyped(event);
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean primary) {
		if (session != null) {
			int x = browserX(event.x());
			int y = browserY(event.y());
			session.applyInput(ComputerpcNetworking.EVENT_MOUSE_PRESS, x, y, event.button(), 0, 0, 0, 0, 0);
			ClientPlayNetworking.send(new ComputerpcPayloads.BrowserInputC2S(rootPos, ComputerpcNetworking.EVENT_MOUSE_PRESS, x, y, event.button(), 0, 0, 0, 0, 0));
			return true;
		}

		return super.mouseClicked(event, primary);
	}

	@Override
	public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
		if (session != null) {
			int x = browserX(event.x());
			int y = browserY(event.y());
			session.applyInput(ComputerpcNetworking.EVENT_MOUSE_RELEASE, x, y, event.button(), 0, 0, 0, 0, 0);
			ClientPlayNetworking.send(new ComputerpcPayloads.BrowserInputC2S(rootPos, ComputerpcNetworking.EVENT_MOUSE_RELEASE, x, y, event.button(), 0, 0, 0, 0, 0));
			return true;
		}

		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
		if (session != null) {
			int x = browserX(event.x());
			int y = browserY(event.y());
			session.applyInput(ComputerpcNetworking.EVENT_MOUSE_MOVE, x, y, event.button(), 0, 0, 0, 0, 0);
			ClientPlayNetworking.send(new ComputerpcPayloads.BrowserInputC2S(rootPos, ComputerpcNetworking.EVENT_MOUSE_MOVE, x, y, event.button(), 0, 0, 0, 0, 0));
			return true;
		}

		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (session != null) {
			int x = browserX(mouseX);
			int y = browserY(mouseY);
			session.applyInput(ComputerpcNetworking.EVENT_MOUSE_MOVE, x, y, 0, 0, 0, 0, 0, 0);
			ClientPlayNetworking.send(new ComputerpcPayloads.BrowserInputC2S(rootPos, ComputerpcNetworking.EVENT_MOUSE_MOVE, x, y, 0, 0, 0, 0, 0, 0));
		}

		super.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (session != null) {
			int x = browserX(mouseX);
			int y = browserY(mouseY);
			session.applyInput(ComputerpcNetworking.EVENT_MOUSE_SCROLL, x, y, 0, 0, 0, 0, 0, verticalAmount);
			ClientPlayNetworking.send(new ComputerpcPayloads.BrowserInputC2S(rootPos, ComputerpcNetworking.EVENT_MOUSE_SCROLL, x, y, 0, 0, 0, 0, 0, verticalAmount));
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	private void syncNavigation() {
		if (session == null) {
			return;
		}
		MCEFBrowser browser = session.activeBrowser();
		if (browser == null) {
			return;
		}

		String currentUrl = session.currentUrl();
		if (!currentUrl.equals(lastKnownUrl)) {
			lastKnownUrl = currentUrl;
			ClientPlayNetworking.send(new ComputerpcPayloads.BrowserNavigateC2S(rootPos, currentUrl));
		}
	}

	private int browserX(double mouseX) {
		return Mth.clamp((int) (mouseX / (double) Math.max(1, width) * session.state().resolutionWidth()), 0, Math.max(0, session.state().resolutionWidth() - 1));
	}

	private int browserY(double mouseY) {
		return Mth.clamp((int) (mouseY / (double) Math.max(1, height) * session.state().resolutionHeight()), 0, Math.max(0, session.state().resolutionHeight() - 1));
	}
}
