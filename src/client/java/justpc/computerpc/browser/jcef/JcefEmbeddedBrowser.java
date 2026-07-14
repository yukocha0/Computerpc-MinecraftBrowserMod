package justpc.computerpc.browser.jcef;

import justpc.computerpc.browser.api.BrowserCursor;
import justpc.computerpc.browser.api.BrowserFrame;
import justpc.computerpc.browser.api.BrowserInput;
import justpc.computerpc.browser.api.BrowserInstance;
import justpc.computerpc.browser.api.BrowserRenderer;
import justpc.computerpc.browser.api.BrowserTexture;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefRequestContext;
import org.cef.browser.ComputerPcCefBrowser;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.ByteBuffer;

public final class JcefEmbeddedBrowser extends ComputerPcCefBrowser implements BrowserInstance, BrowserInput, BrowserRenderer {
	private final JcefBrowserSurface surface = new SoftwarePaintTextureSurface("ComputerPcBrowser");
	private int lastPressedMouseButton = MouseEvent.NOBUTTON;
	private boolean lastMouseEntered;
	private int lastMouseX = Integer.MIN_VALUE;
	private int lastMouseY = Integer.MIN_VALUE;
	private boolean lastMouseDragging;
	private int cursorType = Cursor.DEFAULT_CURSOR;

	public JcefEmbeddedBrowser(CefClient client, String url, boolean transparent, CefRequestContext context) {
		super(client, url, transparent, context, (CefBrowserSettings) null);
	}

	@Override
	public BrowserRenderer renderer() {
		return this;
	}

	@Override
	public BrowserInput input() {
		return this;
	}

	@Override
	public void resize(int width, int height) {
		if (browserRect.width == width && browserRect.height == height) {
			return;
		}

		browserRect.setBounds(0, 0, width, height);
		wasResized(width, height);
	}

	public void onMouseClicked(MouseButtonEvent event, boolean doubled) {
		int btn = toAwtMouseButton(event.button());
		lastPressedMouseButton = btn;
		sendMouseEvent(new MouseEvent(
				component,
				MouseEvent.MOUSE_PRESSED,
				System.currentTimeMillis(),
				toAwtInputModifiers(event.modifiers()),
				(int) event.x(),
				(int) event.y(),
				doubled ? 2 : 1,
				false,
				btn
		));
	}

	public void onMouseReleased(MouseButtonEvent event) {
		int btn = toAwtMouseButton(event.button());
		if (btn == lastPressedMouseButton) {
			lastPressedMouseButton = MouseEvent.NOBUTTON;
		}

		sendMouseEvent(new MouseEvent(
				component,
				MouseEvent.MOUSE_RELEASED,
				System.currentTimeMillis(),
				toAwtInputModifiers(event.modifiers()),
				(int) event.x(),
				(int) event.y(),
				1,
				false,
				btn
		));
	}

	public void onMouseScrolled(int x, int y, double amount) {
		sendMouseWheelEvent(new MouseWheelEvent(
				component,
				MouseWheelEvent.WHEEL_UNIT_SCROLL,
				System.currentTimeMillis(),
				0,
				x,
				y,
				0,
				false,
				MouseWheelEvent.WHEEL_UNIT_SCROLL,
				100,
				(int) Math.signum(amount)
		));
	}

	public void onMouseMoved(int x, int y) {
		boolean mouseEntered = browserRect.contains(x, y);
		boolean dragging = lastPressedMouseButton != MouseEvent.NOBUTTON;
		if (x == lastMouseX && y == lastMouseY && dragging == lastMouseDragging && mouseEntered == lastMouseEntered) {
			return;
		}

		lastMouseX = x;
		lastMouseY = y;
		lastMouseDragging = dragging;
		if (mouseEntered != lastMouseEntered) {
			lastMouseEntered = mouseEntered;
			sendMouseEvent(new MouseEvent(
					component,
					mouseEntered ? MouseEvent.MOUSE_ENTERED : MouseEvent.MOUSE_EXITED,
					System.currentTimeMillis(),
					0,
					x,
					y,
					0,
					false,
					MouseEvent.NOBUTTON
			));
		}

		sendMouseEvent(new MouseEvent(
				component,
				dragging ? MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED,
				System.currentTimeMillis(),
				0,
				x,
				y,
				0,
				false,
				dragging ? lastPressedMouseButton : MouseEvent.NOBUTTON
		));
	}

	public void onKeyPressed(KeyEvent event) {
		int key = toAwtKeyCode(event.key());
		sendKeyEvent(new java.awt.event.KeyEvent(
				component,
				java.awt.event.KeyEvent.KEY_PRESSED,
				System.currentTimeMillis(),
				toAwtInputModifiers(event.modifiers()),
				key,
				(char) key
		));
	}

	public void onKeyReleased(KeyEvent event) {
		int key = toAwtKeyCode(event.key());
		sendKeyEvent(new java.awt.event.KeyEvent(
				component,
				java.awt.event.KeyEvent.KEY_RELEASED,
				System.currentTimeMillis(),
				toAwtInputModifiers(event.modifiers()),
				key,
				(char) key
		));
	}

	public void onCharTyped(CharacterEvent event) {
		sendKeyEvent(new java.awt.event.KeyEvent(
				component,
				java.awt.event.KeyEvent.KEY_TYPED,
				System.currentTimeMillis(),
				0,
				java.awt.event.KeyEvent.VK_UNDEFINED,
				(char) event.codepoint()
		));
	}

	@Override
	public @Nullable BrowserTexture texture() {
		return surface.texture();
	}

	@Override
	public @Nullable BrowserFrame latestFrame() {
		return surface.latestFrame();
	}

	public CursorType getCursorType() {
		return switch (cursor()) {
			case CROSSHAIR -> CursorTypes.CROSSHAIR;
			case IBEAM -> CursorTypes.IBEAM;
			case RESIZE_NESW -> BrowserCursorTypes.RESIZE_NESW;
			case RESIZE_NWSE -> BrowserCursorTypes.RESIZE_NWSE;
			case RESIZE_NS -> CursorTypes.RESIZE_NS;
			case RESIZE_EW -> CursorTypes.RESIZE_EW;
			case POINTING_HAND -> CursorTypes.POINTING_HAND;
			case RESIZE_ALL -> CursorTypes.RESIZE_ALL;
			default -> CursorTypes.ARROW;
		};
	}

	@Override
	public BrowserCursor cursor() {
		return switch (cursorType) {
			case Cursor.CROSSHAIR_CURSOR -> BrowserCursor.CROSSHAIR;
			case Cursor.TEXT_CURSOR -> BrowserCursor.IBEAM;
			case Cursor.SW_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR -> BrowserCursor.RESIZE_NESW;
			case Cursor.SE_RESIZE_CURSOR, Cursor.NW_RESIZE_CURSOR -> BrowserCursor.RESIZE_NWSE;
			case Cursor.N_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR -> BrowserCursor.RESIZE_NS;
			case Cursor.W_RESIZE_CURSOR, Cursor.E_RESIZE_CURSOR -> BrowserCursor.RESIZE_EW;
			case Cursor.HAND_CURSOR -> BrowserCursor.POINTING_HAND;
			case Cursor.MOVE_CURSOR -> BrowserCursor.RESIZE_ALL;
			default -> BrowserCursor.ARROW;
		};
	}

	@Override
	public void close() {
		surface.close();
		close(true);
	}

	@Override
	public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
		surface.onPaint(browser, popup, dirtyRects, buffer, width, height);
		super.onPaint(browser, popup, dirtyRects, buffer, width, height);
	}

	@Override
	public void setVisible(boolean visible) {
		setWindowVisibility(visible);
	}

	@Override
	public void navigate(String url) {
		loadURL(url);
	}

	@Override
	public void stop() {
		stopLoad();
	}

	@Override
	public void executeJavaScript(String script) {
		String url = getURL();
		executeJavaScript(script, url == null ? "about:blank" : url, 0);
	}

	@Override
	public String currentUrl() {
		String url = getURL();
		return url == null ? "" : url;
	}

	@Override
	public void mousePressed(int x, int y, int button, int modifiers, boolean doubled) {
		onMouseClicked(new MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(button, modifiers)), doubled);
	}

	@Override
	public void mouseReleased(int x, int y, int button, int modifiers) {
		onMouseReleased(new MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(button, modifiers)));
	}

	@Override
	public void mouseScrolled(int x, int y, double amount) {
		onMouseScrolled(x, y, amount);
	}

	@Override
	public void mouseMoved(int x, int y) {
		onMouseMoved(x, y);
	}

	@Override
	public void keyPressed(int keyCode, int modifiers) {
		onKeyPressed(new KeyEvent(keyCode, 0, modifiers));
	}

	@Override
	public void keyReleased(int keyCode, int modifiers) {
		onKeyReleased(new KeyEvent(keyCode, 0, modifiers));
	}

	@Override
	public void charTyped(int codePoint) {
		onCharTyped(new CharacterEvent(codePoint));
	}

	@Override
	public boolean onCursorChange(CefBrowser browser, int cursorType) {
		this.cursorType = cursorType;
		return super.onCursorChange(browser, cursorType);
	}

	private static int toAwtInputModifiers(int modifiers) {
		int awtModifiers = 0;
		if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
			awtModifiers |= InputEvent.SHIFT_DOWN_MASK;
		}
		if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
			awtModifiers |= InputEvent.CTRL_DOWN_MASK;
		}
		if ((modifiers & GLFW.GLFW_MOD_ALT) != 0) {
			awtModifiers |= InputEvent.ALT_DOWN_MASK;
		}
		if ((modifiers & GLFW.GLFW_MOD_SUPER) != 0) {
			awtModifiers |= InputEvent.META_DOWN_MASK;
		}
		return awtModifiers;
	}

	private static int toAwtMouseButton(int button) {
		return switch (button) {
			case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> MouseEvent.BUTTON3;
			case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> MouseEvent.BUTTON2;
			default -> MouseEvent.BUTTON1;
		};
	}

	private static int toAwtKeyCode(int glfwKey) {
		return switch (glfwKey) {
			case GLFW.GLFW_KEY_SPACE -> java.awt.event.KeyEvent.VK_SPACE;
			case GLFW.GLFW_KEY_APOSTROPHE -> java.awt.event.KeyEvent.VK_QUOTE;
			case GLFW.GLFW_KEY_COMMA -> java.awt.event.KeyEvent.VK_COMMA;
			case GLFW.GLFW_KEY_MINUS -> java.awt.event.KeyEvent.VK_MINUS;
			case GLFW.GLFW_KEY_PERIOD -> java.awt.event.KeyEvent.VK_PERIOD;
			case GLFW.GLFW_KEY_SLASH -> java.awt.event.KeyEvent.VK_SLASH;
			case GLFW.GLFW_KEY_0 -> java.awt.event.KeyEvent.VK_0;
			case GLFW.GLFW_KEY_1 -> java.awt.event.KeyEvent.VK_1;
			case GLFW.GLFW_KEY_2 -> java.awt.event.KeyEvent.VK_2;
			case GLFW.GLFW_KEY_3 -> java.awt.event.KeyEvent.VK_3;
			case GLFW.GLFW_KEY_4 -> java.awt.event.KeyEvent.VK_4;
			case GLFW.GLFW_KEY_5 -> java.awt.event.KeyEvent.VK_5;
			case GLFW.GLFW_KEY_6 -> java.awt.event.KeyEvent.VK_6;
			case GLFW.GLFW_KEY_7 -> java.awt.event.KeyEvent.VK_7;
			case GLFW.GLFW_KEY_8 -> java.awt.event.KeyEvent.VK_8;
			case GLFW.GLFW_KEY_9 -> java.awt.event.KeyEvent.VK_9;
			case GLFW.GLFW_KEY_A -> java.awt.event.KeyEvent.VK_A;
			case GLFW.GLFW_KEY_B -> java.awt.event.KeyEvent.VK_B;
			case GLFW.GLFW_KEY_C -> java.awt.event.KeyEvent.VK_C;
			case GLFW.GLFW_KEY_D -> java.awt.event.KeyEvent.VK_D;
			case GLFW.GLFW_KEY_E -> java.awt.event.KeyEvent.VK_E;
			case GLFW.GLFW_KEY_F -> java.awt.event.KeyEvent.VK_F;
			case GLFW.GLFW_KEY_G -> java.awt.event.KeyEvent.VK_G;
			case GLFW.GLFW_KEY_H -> java.awt.event.KeyEvent.VK_H;
			case GLFW.GLFW_KEY_I -> java.awt.event.KeyEvent.VK_I;
			case GLFW.GLFW_KEY_J -> java.awt.event.KeyEvent.VK_J;
			case GLFW.GLFW_KEY_K -> java.awt.event.KeyEvent.VK_K;
			case GLFW.GLFW_KEY_L -> java.awt.event.KeyEvent.VK_L;
			case GLFW.GLFW_KEY_M -> java.awt.event.KeyEvent.VK_M;
			case GLFW.GLFW_KEY_N -> java.awt.event.KeyEvent.VK_N;
			case GLFW.GLFW_KEY_O -> java.awt.event.KeyEvent.VK_O;
			case GLFW.GLFW_KEY_P -> java.awt.event.KeyEvent.VK_P;
			case GLFW.GLFW_KEY_Q -> java.awt.event.KeyEvent.VK_Q;
			case GLFW.GLFW_KEY_R -> java.awt.event.KeyEvent.VK_R;
			case GLFW.GLFW_KEY_S -> java.awt.event.KeyEvent.VK_S;
			case GLFW.GLFW_KEY_T -> java.awt.event.KeyEvent.VK_T;
			case GLFW.GLFW_KEY_U -> java.awt.event.KeyEvent.VK_U;
			case GLFW.GLFW_KEY_V -> java.awt.event.KeyEvent.VK_V;
			case GLFW.GLFW_KEY_W -> java.awt.event.KeyEvent.VK_W;
			case GLFW.GLFW_KEY_X -> java.awt.event.KeyEvent.VK_X;
			case GLFW.GLFW_KEY_Y -> java.awt.event.KeyEvent.VK_Y;
			case GLFW.GLFW_KEY_Z -> java.awt.event.KeyEvent.VK_Z;
			case GLFW.GLFW_KEY_ESCAPE -> java.awt.event.KeyEvent.VK_ESCAPE;
			case GLFW.GLFW_KEY_ENTER -> java.awt.event.KeyEvent.VK_ENTER;
			case GLFW.GLFW_KEY_TAB -> java.awt.event.KeyEvent.VK_TAB;
			case GLFW.GLFW_KEY_BACKSPACE -> java.awt.event.KeyEvent.VK_BACK_SPACE;
			case GLFW.GLFW_KEY_INSERT -> java.awt.event.KeyEvent.VK_INSERT;
			case GLFW.GLFW_KEY_DELETE -> java.awt.event.KeyEvent.VK_DELETE;
			case GLFW.GLFW_KEY_RIGHT -> java.awt.event.KeyEvent.VK_RIGHT;
			case GLFW.GLFW_KEY_LEFT -> java.awt.event.KeyEvent.VK_LEFT;
			case GLFW.GLFW_KEY_DOWN -> java.awt.event.KeyEvent.VK_DOWN;
			case GLFW.GLFW_KEY_UP -> java.awt.event.KeyEvent.VK_UP;
			case GLFW.GLFW_KEY_PAGE_UP -> java.awt.event.KeyEvent.VK_PAGE_UP;
			case GLFW.GLFW_KEY_PAGE_DOWN -> java.awt.event.KeyEvent.VK_PAGE_DOWN;
			case GLFW.GLFW_KEY_HOME -> java.awt.event.KeyEvent.VK_HOME;
			case GLFW.GLFW_KEY_END -> java.awt.event.KeyEvent.VK_END;
			case GLFW.GLFW_KEY_CAPS_LOCK -> java.awt.event.KeyEvent.VK_CAPS_LOCK;
			case GLFW.GLFW_KEY_SCROLL_LOCK -> java.awt.event.KeyEvent.VK_SCROLL_LOCK;
			case GLFW.GLFW_KEY_NUM_LOCK -> java.awt.event.KeyEvent.VK_NUM_LOCK;
			case GLFW.GLFW_KEY_PRINT_SCREEN -> java.awt.event.KeyEvent.VK_PRINTSCREEN;
			case GLFW.GLFW_KEY_PAUSE -> java.awt.event.KeyEvent.VK_PAUSE;
			case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> java.awt.event.KeyEvent.VK_SHIFT;
			case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> java.awt.event.KeyEvent.VK_CONTROL;
			case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> java.awt.event.KeyEvent.VK_ALT;
			case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER -> java.awt.event.KeyEvent.VK_META;
			case GLFW.GLFW_KEY_F1 -> java.awt.event.KeyEvent.VK_F1;
			case GLFW.GLFW_KEY_F2 -> java.awt.event.KeyEvent.VK_F2;
			case GLFW.GLFW_KEY_F3 -> java.awt.event.KeyEvent.VK_F3;
			case GLFW.GLFW_KEY_F4 -> java.awt.event.KeyEvent.VK_F4;
			case GLFW.GLFW_KEY_F5 -> java.awt.event.KeyEvent.VK_F5;
			case GLFW.GLFW_KEY_F6 -> java.awt.event.KeyEvent.VK_F6;
			case GLFW.GLFW_KEY_F7 -> java.awt.event.KeyEvent.VK_F7;
			case GLFW.GLFW_KEY_F8 -> java.awt.event.KeyEvent.VK_F8;
			case GLFW.GLFW_KEY_F9 -> java.awt.event.KeyEvent.VK_F9;
			case GLFW.GLFW_KEY_F10 -> java.awt.event.KeyEvent.VK_F10;
			case GLFW.GLFW_KEY_F11 -> java.awt.event.KeyEvent.VK_F11;
			case GLFW.GLFW_KEY_F12 -> java.awt.event.KeyEvent.VK_F12;
			case GLFW.GLFW_KEY_KP_0 -> java.awt.event.KeyEvent.VK_NUMPAD0;
			case GLFW.GLFW_KEY_KP_1 -> java.awt.event.KeyEvent.VK_NUMPAD1;
			case GLFW.GLFW_KEY_KP_2 -> java.awt.event.KeyEvent.VK_NUMPAD2;
			case GLFW.GLFW_KEY_KP_3 -> java.awt.event.KeyEvent.VK_NUMPAD3;
			case GLFW.GLFW_KEY_KP_4 -> java.awt.event.KeyEvent.VK_NUMPAD4;
			case GLFW.GLFW_KEY_KP_5 -> java.awt.event.KeyEvent.VK_NUMPAD5;
			case GLFW.GLFW_KEY_KP_6 -> java.awt.event.KeyEvent.VK_NUMPAD6;
			case GLFW.GLFW_KEY_KP_7 -> java.awt.event.KeyEvent.VK_NUMPAD7;
			case GLFW.GLFW_KEY_KP_8 -> java.awt.event.KeyEvent.VK_NUMPAD8;
			case GLFW.GLFW_KEY_KP_9 -> java.awt.event.KeyEvent.VK_NUMPAD9;
			case GLFW.GLFW_KEY_KP_DECIMAL -> java.awt.event.KeyEvent.VK_DECIMAL;
			case GLFW.GLFW_KEY_KP_DIVIDE -> java.awt.event.KeyEvent.VK_DIVIDE;
			case GLFW.GLFW_KEY_KP_MULTIPLY -> java.awt.event.KeyEvent.VK_MULTIPLY;
			case GLFW.GLFW_KEY_KP_SUBTRACT -> java.awt.event.KeyEvent.VK_SUBTRACT;
			case GLFW.GLFW_KEY_KP_ADD -> java.awt.event.KeyEvent.VK_ADD;
			case GLFW.GLFW_KEY_KP_ENTER -> java.awt.event.KeyEvent.VK_ENTER;
			case GLFW.GLFW_KEY_KP_EQUAL -> java.awt.event.KeyEvent.VK_EQUALS;
			case GLFW.GLFW_KEY_SEMICOLON -> java.awt.event.KeyEvent.VK_SEMICOLON;
			case GLFW.GLFW_KEY_EQUAL -> java.awt.event.KeyEvent.VK_EQUALS;
			case GLFW.GLFW_KEY_LEFT_BRACKET -> java.awt.event.KeyEvent.VK_OPEN_BRACKET;
			case GLFW.GLFW_KEY_RIGHT_BRACKET -> java.awt.event.KeyEvent.VK_CLOSE_BRACKET;
			case GLFW.GLFW_KEY_BACKSLASH -> java.awt.event.KeyEvent.VK_BACK_SLASH;
			case GLFW.GLFW_KEY_GRAVE_ACCENT -> java.awt.event.KeyEvent.VK_BACK_QUOTE;
			default -> java.awt.event.KeyEvent.VK_UNDEFINED;
		};
	}
}
