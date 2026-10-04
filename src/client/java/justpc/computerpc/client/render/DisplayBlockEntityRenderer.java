package justpc.computerpc.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import justpc.computerpc.block.DisplayBlock;
import justpc.computerpc.blockentity.DisplayBlockEntity;
import justpc.computerpc.client.DisplayBrowserManager;
import justpc.computerpc.util.DisplayCluster;
import justpc.computerpc.util.DisplayClusterResolver;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DisplayBlockEntityRenderer implements BlockEntityRenderer<DisplayBlockEntity, DisplayBlockEntityRenderer.State> {
	private static final float SCREEN_OFFSET = 0.0025f;
	private static final int FULL_BRIGHT = 15728880;

	public DisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DisplayBlockEntity blockEntity, State state, float tickProgress, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
		state.reset();

		BlockState blockState = blockEntity.getBlockState();
		Direction facing = blockState.getValue(DisplayBlock.FACING);
		DisplayCluster cluster = blockEntity.getCluster();
		state.facing = facing;
		state.renderScreen = blockEntity.isPowered();
		state.light = FULL_BRIGHT;

		if (!state.renderScreen || !(blockEntity.getLevel() instanceof ClientLevel clientLevel)) {
			return;
		}

		DisplayBrowserManager.DisplayBrowserSession session = DisplayBrowserManager.getSession(clientLevel, cluster.root());
		if (session == null || session.activeBrowser() == null) {
			return;
		}

		Identifier texture = BrowserRenderUtil.syncWorldTexture(session.activeBrowser());
		if (texture == null) {
			return;
		}

		applyBlockSlice(state, cluster, blockEntity.getBlockPos());
		state.texture = texture;
	}

	@Override
	public void submit(State state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
		if (state.renderScreen && state.texture != null) {
			queue.submitCustomGeometry(matrices, RenderTypes.entityTranslucentEmissive(state.texture), (pose, consumer) -> emitScreenQuad(state, pose, consumer));
		}
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 128;
	}

	private static void emitScreenQuad(State state, PoseStack.Pose pose, VertexConsumer consumer) {
		switch (state.facing) {
			case NORTH -> {
				float z = -SCREEN_OFFSET;
				vertex(consumer, pose, 0.0f, 0.0f, z, state.uAtMinAxis, state.vAtMinY, state.light, 0.0f, 0.0f, -1.0f);
				vertex(consumer, pose, 1.0f, 0.0f, z, state.uAtMaxAxis, state.vAtMinY, state.light, 0.0f, 0.0f, -1.0f);
				vertex(consumer, pose, 1.0f, 1.0f, z, state.uAtMaxAxis, state.vAtMaxY, state.light, 0.0f, 0.0f, -1.0f);
				vertex(consumer, pose, 0.0f, 1.0f, z, state.uAtMinAxis, state.vAtMaxY, state.light, 0.0f, 0.0f, -1.0f);
			}
			case SOUTH -> {
				float z = 1.0f + SCREEN_OFFSET;
				vertex(consumer, pose, 1.0f, 0.0f, z, state.uAtMaxAxis, state.vAtMinY, state.light, 0.0f, 0.0f, 1.0f);
				vertex(consumer, pose, 0.0f, 0.0f, z, state.uAtMinAxis, state.vAtMinY, state.light, 0.0f, 0.0f, 1.0f);
				vertex(consumer, pose, 0.0f, 1.0f, z, state.uAtMinAxis, state.vAtMaxY, state.light, 0.0f, 0.0f, 1.0f);
				vertex(consumer, pose, 1.0f, 1.0f, z, state.uAtMaxAxis, state.vAtMaxY, state.light, 0.0f, 0.0f, 1.0f);
			}
			case WEST -> {
				float x = -SCREEN_OFFSET;
				vertex(consumer, pose, x, 0.0f, 1.0f, state.uAtMaxAxis, state.vAtMinY, state.light, -1.0f, 0.0f, 0.0f);
				vertex(consumer, pose, x, 0.0f, 0.0f, state.uAtMinAxis, state.vAtMinY, state.light, -1.0f, 0.0f, 0.0f);
				vertex(consumer, pose, x, 1.0f, 0.0f, state.uAtMinAxis, state.vAtMaxY, state.light, -1.0f, 0.0f, 0.0f);
				vertex(consumer, pose, x, 1.0f, 1.0f, state.uAtMaxAxis, state.vAtMaxY, state.light, -1.0f, 0.0f, 0.0f);
			}
			case EAST -> {
				float x = 1.0f + SCREEN_OFFSET;
				vertex(consumer, pose, x, 0.0f, 0.0f, state.uAtMinAxis, state.vAtMinY, state.light, 1.0f, 0.0f, 0.0f);
				vertex(consumer, pose, x, 0.0f, 1.0f, state.uAtMaxAxis, state.vAtMinY, state.light, 1.0f, 0.0f, 0.0f);
				vertex(consumer, pose, x, 1.0f, 1.0f, state.uAtMaxAxis, state.vAtMaxY, state.light, 1.0f, 0.0f, 0.0f);
				vertex(consumer, pose, x, 1.0f, 0.0f, state.uAtMinAxis, state.vAtMaxY, state.light, 1.0f, 0.0f, 0.0f);
			}
			default -> {
			}
		}
	}

	private static void applyBlockSlice(State state, DisplayCluster cluster, BlockPos pos) {
		int widthBlocks = Math.max(1, cluster.widthBlocks());
		int heightBlocks = Math.max(1, cluster.heightBlocks());
		int blockX = DisplayClusterResolver.blockIndexX(cluster, pos);
		int blockY = DisplayClusterResolver.blockIndexY(cluster, pos);

		float sliceStartU = blockX / (float) widthBlocks;
		float sliceEndU = (blockX + 1.0f) / widthBlocks;
		float sliceTopV = 1.0f - (blockY + 1.0f) / heightBlocks;
		float sliceBottomV = 1.0f - blockY / (float) heightBlocks;

		switch (state.facing) {
			case NORTH, EAST -> {
				state.uAtMinAxis = sliceEndU;
				state.uAtMaxAxis = sliceStartU;
			}
			case SOUTH, WEST -> {
				state.uAtMinAxis = sliceStartU;
				state.uAtMaxAxis = sliceEndU;
			}
			default -> {
				state.uAtMinAxis = sliceStartU;
				state.uAtMaxAxis = sliceEndU;
			}
		}

		state.vAtMinY = sliceBottomV;
		state.vAtMaxY = sliceTopV;
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light, float normalX, float normalY, float normalZ) {
		consumer.addVertex(pose, x, y, z)
				.setColor(1.0f, 1.0f, 1.0f, 1.0f)
				.setUv(u, v)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(light)
				.setNormal(pose, normalX, normalY, normalZ);
	}

	public static final class State extends BlockEntityRenderState {
		private Direction facing = Direction.NORTH;
		private boolean renderScreen;
		private int light = FULL_BRIGHT;
		private float uAtMinAxis;
		private float uAtMaxAxis = 1.0f;
		private float vAtMinY = 1.0f;
		private float vAtMaxY;
		private @Nullable Identifier texture;

		private void reset() {
			facing = Direction.NORTH;
			renderScreen = false;
			light = FULL_BRIGHT;
			uAtMinAxis = 0.0f;
			uAtMaxAxis = 1.0f;
			vAtMinY = 1.0f;
			vAtMaxY = 0.0f;
			texture = null;
		}
	}
}
