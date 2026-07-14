package justpc.computerpc.browser.api;

public interface BrowserInput {
	void mousePressed(int x, int y, int button, int modifiers, boolean doubled);

	void mouseReleased(int x, int y, int button, int modifiers);

	void mouseScrolled(int x, int y, double amount);

	void mouseMoved(int x, int y);

	void keyPressed(int keyCode, int modifiers);

	void keyReleased(int keyCode, int modifiers);

	void charTyped(int codePoint);
}
