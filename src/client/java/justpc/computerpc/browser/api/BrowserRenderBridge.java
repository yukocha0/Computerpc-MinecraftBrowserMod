package justpc.computerpc.browser.api;

import java.nio.ByteBuffer;

public interface BrowserRenderBridge {
	void executeOnRenderThread(Runnable task);

	ExternalTexture createTexture(String debugName, int width, int height);

	void uploadBgra(ExternalTexture texture, ByteBuffer pixels, int bytes, int width, int height);
}
