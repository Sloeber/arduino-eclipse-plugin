package io.sloeber.core;

import java.io.File;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.tools.TxtFile;

/**
 * This class exists solely for the purpose of having access to the
 * boardDescriptor API class and have some extra access
 *
 * @author jan
 *
 */
public class InternalBoardDescriptor extends BoardDescriptor {
	private ICConfigurationDescription mConfdesc = null;

	public InternalBoardDescriptor(ICConfigurationDescription confdesc) {
		super(confdesc);
		this.mConfdesc = confdesc;

	}

	public InternalBoardDescriptor(File boardsFile, String boardID, Map<String, String> options) {
		super(boardsFile, boardID, options);

	}

	public InternalBoardDescriptor(TxtFile txtFile, String boardID) {
		super(txtFile, boardID);

	}

	public InternalBoardDescriptor(BoardDescriptor sourceBoardDescriptor) {
		super(sourceBoardDescriptor);
	}

	public TxtFile getTxtFile() {
		return this.myTxtFile;
	}

	@Override
	public void saveConfiguration() {
		saveConfiguration(this.mConfdesc, null);
	}

}
