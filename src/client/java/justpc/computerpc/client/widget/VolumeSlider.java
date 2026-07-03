package justpc.computerpc.client.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

public final class VolumeSlider extends AbstractSliderButton {
	private final DoubleConsumer onChange;

	public VolumeSlider(int x, int y, int width, int height, double initialValue, DoubleConsumer onChange) {
		super(x, y, width, height, Component.empty(), initialValue);
		this.onChange = onChange;
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.literal("Volume: " + Math.round(value * 100.0) + "%"));
	}

	@Override
	protected void applyValue() {
		onChange.accept(value);
	}
}
