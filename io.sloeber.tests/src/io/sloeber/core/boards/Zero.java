package io.sloeber.core.boards;

@SuppressWarnings("nls")
public class Zero extends GenericArduinoSamdBoard {
	@Override
	public String getName() {
		return "zero";
	}

	public Zero() {
		super("arduino_zero_edbg");
		this.myBoardDescriptor.setUploadPort("COM14");
	}
}
