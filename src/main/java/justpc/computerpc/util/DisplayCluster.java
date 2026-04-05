package justpc.computerpc.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Set;

public record DisplayCluster(
		BlockPos root,
		Direction facing,
		int widthBlocks,
		int heightBlocks,
		int minU,
		int maxU,
		int minY,
		int maxY,
		boolean rectangular,
		Set<BlockPos> blocks
) {
	public boolean isRoot(BlockPos pos) {
		return root.equals(pos);
	}
}
