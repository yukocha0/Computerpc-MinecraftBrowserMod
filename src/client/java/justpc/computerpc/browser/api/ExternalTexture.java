package justpc.computerpc.browser.api;

public interface ExternalTexture extends AutoCloseable {
	void bind();

	void release();

	void resize(int width, int height);

	int getWidth();

	int getHeight();

	long getNativeHandle();

	@Override
	default void close() {
		release();
	}
}
