package justpc.computerpc.browser.jcef;

import justpc.computerpc.browser.api.BrowserFrame;
import justpc.computerpc.browser.api.BrowserRenderBridge;
import justpc.computerpc.browser.api.BrowserTexture;
import justpc.computerpc.browser.api.DirtyRectangle;
import justpc.computerpc.browser.api.ExternalTexture;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

final class SoftwarePaintTextureSurface implements JcefBrowserSurface {
	private final String debugName;
	private final BrowserRenderBridge renderBridge;
	private final Object paintLock = new Object();
	private @Nullable ExternalTexture externalTexture;
	private @Nullable BrowserTexture browserTexture;
	private @Nullable BrowserFrame latestFrame;
	private @Nullable ByteBuffer pendingPaintBuffer;
	private int pendingPaintCapacity;
	private int pendingPaintBytes;
	private int pendingPaintWidth;
	private int pendingPaintHeight;
	private @Nullable ByteBuffer uploadPaintBuffer;
	private int uploadPaintCapacity;
	private boolean pendingPaintReady;
	private boolean paintUploadScheduled;

	SoftwarePaintTextureSurface(String debugName, BrowserRenderBridge renderBridge) {
		this.debugName = debugName;
		this.renderBridge = renderBridge;
	}

	@Override
	public @Nullable BrowserTexture texture() {
		return browserTexture;
	}

	@Override
	public @Nullable BrowserFrame latestFrame() {
		return latestFrame;
	}

	@Override
	public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
		if (popup || dirtyRects.length <= 0 || width <= 0 || height <= 0) {
			return;
		}

		int bytes = buffer.remaining();
		if (bytes <= 0) {
			return;
		}

		boolean scheduleUpload = false;
		synchronized (paintLock) {
			if (pendingPaintBuffer == null) {
				pendingPaintBuffer = MemoryUtil.memAlloc(bytes);
				pendingPaintCapacity = bytes;
			} else if (pendingPaintCapacity < bytes) {
				pendingPaintBuffer = MemoryUtil.memRealloc(pendingPaintBuffer, bytes);
				pendingPaintCapacity = bytes;
			}

			ByteBuffer target = pendingPaintBuffer.duplicate();
			target.clear();
			target.limit(bytes);
			MemoryUtil.memCopy(MemoryUtil.memAddress(buffer), MemoryUtil.memAddress(target), bytes);

			List<DirtyRectangle> dirtyRectangles = Arrays.stream(dirtyRects)
					.map(rect -> new DirtyRectangle(rect.x, rect.y, rect.width, rect.height))
					.toList();
			pendingPaintBytes = bytes;
			pendingPaintWidth = width;
			pendingPaintHeight = height;
			pendingDirtyRectangles = dirtyRectangles;
			pendingPaintReady = true;
			if (!paintUploadScheduled) {
				paintUploadScheduled = true;
				scheduleUpload = true;
			}
		}

		if (scheduleUpload) {
			renderBridge.executeOnRenderThread(this::flushPendingPaint);
		}
	}

	@Override
	public void close() {
		synchronized (paintLock) {
			if (pendingPaintBuffer != null) {
				MemoryUtil.memFree(pendingPaintBuffer);
				pendingPaintBuffer = null;
				pendingPaintCapacity = 0;
			}
			if (uploadPaintBuffer != null) {
				MemoryUtil.memFree(uploadPaintBuffer);
				uploadPaintBuffer = null;
				uploadPaintCapacity = 0;
			}
			pendingPaintBytes = 0;
			pendingPaintWidth = 0;
			pendingPaintHeight = 0;
			pendingPaintReady = false;
			paintUploadScheduled = false;
		}
		if (externalTexture != null) {
			externalTexture.release();
			externalTexture = null;
		}
		browserTexture = null;
		latestFrame = null;
	}

	private void flushPendingPaint() {
		ByteBuffer buffer;
		int bytes;
		int width;
		int height;
		List<DirtyRectangle> dirtyRectangles;
		synchronized (paintLock) {
			if (!pendingPaintReady || pendingPaintBuffer == null) {
				paintUploadScheduled = false;
				return;
			}

			ByteBuffer reusableBuffer = uploadPaintBuffer;
			int reusableCapacity = uploadPaintCapacity;
			uploadPaintBuffer = pendingPaintBuffer;
			uploadPaintCapacity = pendingPaintCapacity;
			pendingPaintBuffer = reusableBuffer;
			pendingPaintCapacity = reusableCapacity;

			buffer = uploadPaintBuffer;
			bytes = pendingPaintBytes;
			width = pendingPaintWidth;
			height = pendingPaintHeight;
			dirtyRectangles = pendingDirtyRectangles;
			pendingPaintReady = false;
		}

		uploadFrame(buffer, bytes, width, height, dirtyRectangles);

		boolean scheduleNext;
		synchronized (paintLock) {
			scheduleNext = pendingPaintReady;
			paintUploadScheduled = scheduleNext;
		}
		if (scheduleNext) {
			renderBridge.executeOnRenderThread(this::flushPendingPaint);
		}
	}

	private void uploadFrame(ByteBuffer buffer, int bytes, int width, int height, List<DirtyRectangle> dirtyRectangles) {
		if (buffer == null || bytes <= 0 || width <= 0 || height <= 0) {
			return;
		}
		if (externalTexture == null || externalTexture.getWidth() != width || externalTexture.getHeight() != height) {
			if (externalTexture != null) {
				externalTexture.release();
			}
			externalTexture = renderBridge.createTexture(debugName, width, height);
			browserTexture = () -> externalTexture;
		}

		ByteBuffer uploadView = buffer.duplicate();
		uploadView.clear();
		uploadView.limit(bytes);

		renderBridge.uploadBgra(externalTexture, uploadView, bytes, width, height);
		latestFrame = new BrowserFrame(externalTexture, width, height, dirtyRectangles);
	}

	private List<DirtyRectangle> pendingDirtyRectangles = List.of();

}
