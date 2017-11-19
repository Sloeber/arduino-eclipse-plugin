package io.sloeber.core.boards;

@SuppressWarnings("nls")
public class NodeMCUBoard extends GenericESP8266Board {
	public NodeMCUBoard() {
		super("nodemcu", null);
		this.myBoardDescriptor.setUploadPort("COM22");
	}

}