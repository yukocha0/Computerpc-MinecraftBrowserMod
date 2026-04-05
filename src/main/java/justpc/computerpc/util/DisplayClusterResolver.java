package justpc.computerpc.util;

import justpc.computerpc.block.DisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class DisplayClusterResolver {
	private static final ThreadLocal<Map<Level, TickCache>> CACHES = ThreadLocal.withInitial(WeakHashMap::new);

	private DisplayClusterResolver() {
	}

	public static DisplayCluster resolve(Level level, BlockPos startPos) {
		BlockState state = level.getBlockState(startPos);
		if (!(state.getBlock() instanceof DisplayBlock)) {
			return resolveDummy(startPos, Direction.NORTH);
		}

		return resolve(level, startPos, state.getValue(DisplayBlock.FACING));
	}

	public static DisplayCluster resolve(Level level, BlockPos startPos, Direction facing) {
		TickCache cache = CACHES.get().computeIfAbsent(level, ignored -> new TickCache());
		cache.prepare(level.getGameTime());

		DisplayCluster cachedCluster = cache.clusters.get(startPos);
		if (cachedCluster != null && cachedCluster.facing() == facing) {
			return cachedCluster;
		}

		Set<BlockPos> visited = new HashSet<>();
		ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
		frontier.add(startPos.immutable());

		Direction horizontal = horizontalAxisFor(facing);
		while (!frontier.isEmpty()) {
			BlockPos current = frontier.removeFirst();
			if (!visited.add(current)) {
				continue;
			}

			BlockState state = level.getBlockState(current);
			if (!(state.getBlock() instanceof DisplayBlock) || state.getValue(DisplayBlock.FACING) != facing) {
				visited.remove(current);
				continue;
			}

			frontier.add(current.relative(horizontal));
			frontier.add(current.relative(horizontal.getOpposite()));
			frontier.add(current.above());
			frontier.add(current.below());
		}

		if (visited.isEmpty()) {
			return resolveDummy(startPos, facing);
		}

		int minU = Integer.MAX_VALUE;
		int maxU = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		BlockPos root = startPos;

		for (BlockPos pos : visited) {
			int projectedU = project(pos, horizontal);
			if (projectedU < minU) {
				minU = projectedU;
			}
			if (projectedU > maxU) {
				maxU = projectedU;
			}
			if (pos.getY() < minY) {
				minY = pos.getY();
			}
			if (pos.getY() > maxY) {
				maxY = pos.getY();
			}
		}

		for (BlockPos pos : visited) {
			if (project(pos, horizontal) == minU && pos.getY() == minY) {
				root = pos;
				break;
			}
		}

		int width = maxU - minU + 1;
		int height = maxY - minY + 1;
		boolean rectangular = visited.size() == width * height;
		DisplayCluster cluster = new DisplayCluster(root, facing, width, height, minU, maxU, minY, maxY, rectangular, Set.copyOf(visited));
		for (BlockPos clusterPos : cluster.blocks()) {
			cache.clusters.put(clusterPos, cluster);
		}
		return cluster;
	}

	public static DisplayCluster resolveDummy(BlockPos pos, Direction facing) {
		return new DisplayCluster(pos, facing, 1, 1, 0, 0, pos.getY(), pos.getY(), true, Set.of(pos));
	}

	public static int blockIndexX(DisplayCluster cluster, BlockPos pos) {
		return project(pos, horizontalAxisFor(cluster.facing())) - cluster.minU();
	}

	public static int blockIndexY(DisplayCluster cluster, BlockPos pos) {
		return pos.getY() - cluster.minY();
	}

	public static Direction horizontalAxisFor(Direction facing) {
		return switch (facing) {
			case NORTH -> Direction.WEST;
			case SOUTH -> Direction.EAST;
			case WEST -> Direction.SOUTH;
			case EAST -> Direction.NORTH;
			default -> Direction.EAST;
		};
	}

	private static int project(BlockPos pos, Direction axis) {
		return pos.getX() * axis.getStepX() + pos.getY() * axis.getStepY() + pos.getZ() * axis.getStepZ();
	}

	private static final class TickCache {
		private long gameTime = Long.MIN_VALUE;
		private final Map<BlockPos, DisplayCluster> clusters = new HashMap<>();

		private void prepare(long currentGameTime) {
			if (gameTime == currentGameTime) {
				return;
			}

			gameTime = currentGameTime;
			clusters.clear();
		}
	}
}
