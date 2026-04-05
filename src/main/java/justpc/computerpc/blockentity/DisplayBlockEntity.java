package justpc.computerpc.blockentity;

import justpc.computerpc.block.DisplayBlock;
import justpc.computerpc.browser.DisplayStateData;
import justpc.computerpc.registry.ComputerpcBlockEntities;
import justpc.computerpc.util.DisplayCluster;
import justpc.computerpc.util.DisplayClusterResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.Consumer;

public final class DisplayBlockEntity extends BlockEntity {
	private DisplayStateData screenState = DisplayStateData.DEFAULT;

	public DisplayBlockEntity(BlockPos pos, BlockState blockState) {
		super(ComputerpcBlockEntities.DISPLAY, pos, blockState);
	}

	public DisplayStateData getScreenState() {
		return screenState;
	}

	public void setScreenState(DisplayStateData state) {
		setScreenStateInternal(state == null ? DisplayStateData.DEFAULT : state.sanitize());
	}

	public void pushNavigation(String url) {
		DisplayStateData updated = screenState.syncActiveUrl(url);
		if (!updated.equals(screenState)) {
			screenState = updated;
			notifyUpdate();
		}
	}

	public boolean isPowered() {
		return getBlockState().getValue(DisplayBlock.POWERED);
	}

	public void setPowered(boolean powered) {
		if (level == null || isPowered() == powered) {
			return;
		}

		level.setBlock(worldPosition, getBlockState().setValue(DisplayBlock.POWERED, powered), Block.UPDATE_ALL);
	}

	public void togglePower() {
		setPowered(!isPowered());
	}

	public void toggleClusterPower() {
		boolean targetPower = !isPowered();
		forEachInCluster(display -> display.setPowered(targetPower));
	}

	public void setClusterScreenState(DisplayStateData state) {
		DisplayStateData sanitized = state == null ? DisplayStateData.DEFAULT : state.sanitize();
		forEachInCluster(display -> display.setScreenStateInternal(sanitized));
	}

	public void pushClusterNavigation(String url) {
		forEachInCluster(display -> display.pushNavigation(url));
	}

	public void copyClusterStateFrom(DisplayBlockEntity source) {
		if (source == null || source == this) {
			return;
		}

		DisplayStateData sourceScreenState = source.getScreenState();
		boolean dataChanged = !screenState.equals(sourceScreenState);

		screenState = sourceScreenState;
		setPowered(source.isPowered());
		if (dataChanged) {
			notifyUpdate();
		}
	}

	public DisplayCluster getCluster() {
		if (level == null) {
			return DisplayClusterResolver.resolveDummy(worldPosition, getBlockState().getValue(DisplayBlock.FACING));
		}
		return DisplayClusterResolver.resolve(level, worldPosition, getBlockState().getValue(DisplayBlock.FACING));
	}

	public void notifyUpdate() {
		setChanged();
		if (level != null) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
		}
	}

	private void forEachInCluster(Consumer<DisplayBlockEntity> action) {
		if (level == null) {
			action.accept(this);
			return;
		}

		DisplayCluster cluster = getCluster();
		for (BlockPos clusterPos : cluster.blocks()) {
			if (level.getBlockEntity(clusterPos) instanceof DisplayBlockEntity display) {
				action.accept(display);
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("screen_state", DisplayStateData.CODEC, screenState);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		screenState = input.read("screen_state", DisplayStateData.CODEC).orElse(DisplayStateData.DEFAULT).sanitize();
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveWithoutMetadata(registries);
	}

	private void setScreenStateInternal(DisplayStateData sanitizedState) {
		if (screenState.equals(sanitizedState)) {
			return;
		}

		screenState = sanitizedState;
		notifyUpdate();
	}
}
