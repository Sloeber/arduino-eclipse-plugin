package jUnit.boards;

@SuppressWarnings("nls")
public class leonardoBoard extends GenericArduinoAvrBoard {
	public leonardoBoard() {
		super("leonardo");
		this.myBoardDescriptor.setUploadPort("COM7");
	}

}
