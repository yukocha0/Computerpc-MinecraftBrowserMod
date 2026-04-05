package net.dimaskama.mcef.impl;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import org.lwjgl.glfw.GLFW;

public final class ExtraCursorTypes {
	public static final CursorType RESIZE_NWSE = CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NWSE_CURSOR, "resize_nwse", CursorTypes.ARROW);
	public static final CursorType RESIZE_NESW = CursorType.createStandardCursor(GLFW.GLFW_RESIZE_NESW_CURSOR, "resize_nesw", CursorTypes.ARROW);

	private ExtraCursorTypes() {
	}
}
