package jUnit.boards;

import io.sloeber.core.api.BoardDescriptor;

public abstract class IBoard {
	protected BoardDescriptor myBoardDescriptor = null;

	public BoardDescriptor getBoardDescriptor() {
		return this.myBoardDescriptor;
	}

	public abstract boolean isExampleOk(String inoName, String libName);
}