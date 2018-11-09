package io.sloeber.ui.project.properties;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.cdt.core.parser.util.ArrayUtil;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.AbstractPage;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.Defaults;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.PasswordManager;
import io.sloeber.core.api.SerialManager;
import io.sloeber.ui.Activator;
import io.sloeber.ui.LabelCombo;
import io.sloeber.ui.Messages;

/**
 * The ArduinoSelectionPage class is used in the new wizard and the project
 * properties. This class controls the gui and the data underneath the gui. This
 * class allows to select the arduino board and the port name
 *
 * @author Jan Baeyens
 * @see ArduinoProperties ArduinoSettingsPage
 *
 */
@SuppressWarnings({"unused"})
public class BoardSelectionPage extends AbstractCPropertyTab {
	private static final String TRUE = "TRUE"; //$NON-NLS-1$

	private static final String FALSE = "FALSE"; //$NON-NLS-1$

	// global stuff to allow to communicate outside this class
	public Text mFeedbackControl;

	// GUI elements
	protected LabelCombo mControlBoardsTxtFile;
	protected LabelCombo mcontrolBoardName;
	protected LabelCombo mControlUploadProtocol;
	protected LabelCombo mControlUploadPort;
	protected LabelCombo[] mBoardOptionCombos = null;
	protected Listener mBoardSelectionChangedListener = null;
	protected BoardDescriptor myBoardID = null;

	/**
	 * Get the configuration we are currently working in. The configuration is
	 * null if we are in the create sketch wizard.
	 *
	 * @return the configuration to save info into
	 */
	public ICConfigurationDescription getConfdesc() {
		if (page != null) {
			return getResDesc().getConfiguration();
		}
		return null;
	}

	private Listener boardFileModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {

			File boardFile = getSelectedBoardsFile();
			myBoardID.setreferencingBoardsFile(boardFile);

			/*
			 * Change the list of available boards
			 */
			String CurrentBoard = getBoardName();
			mcontrolBoardName.setItems(myBoardID.getCompatibleBoards());
			mcontrolBoardName.setText(CurrentBoard);

			/*
			 * Change the list of available upload protocols
			 */
			String CurrentUploadProtocol = getUpLoadProtocol();
			mControlUploadProtocol
					.setItems(myBoardID.getUploadProtocols());
			mControlUploadProtocol.setText(CurrentUploadProtocol);

			if (mControlUploadProtocol.getText().isEmpty()) {
				myBoardID.setUploadProtocol(Defaults.getDefaultUploadProtocol());
				mControlUploadProtocol.setText(Defaults.getDefaultUploadProtocol());
			}

			boardModifyListener.handleEvent(null);
		}

	};

	protected Listener boardModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {

			myBoardID.setBoardName(getBoardName());

			for (LabelCombo curLabelCombo : mBoardOptionCombos) {
				curLabelCombo.setItems(myBoardID.getMenuItemNamesFromMenuID(curLabelCombo.getID()));
				curLabelCombo.setLabel(myBoardID.getMenuNameFromMenuID(curLabelCombo.getID()));
			}

			isPageComplete();
			enableControls();
		}
	};
	protected Listener labelComboListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			isPageComplete();
		}
	};

	private Composite mComposite;

	private TreeMap<String, String> mAllBoardsFiles = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

	private org.eclipse.swt.widgets.Button mPwdButton;

	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);
		draw(parent);

	}

	public void setListener(Listener BoardSelectionChangedListener) {
		mBoardSelectionChangedListener = BoardSelectionChangedListener;
	}

	private  void createLabel(int ncol, String t) {
		Label line = new Label(mComposite, SWT.HORIZONTAL | SWT.BOLD);
		line.setText(t);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	}

	private  void createLine() {
		Label line = new Label(mComposite, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		line.setLayoutData(gridData);
	}

	public void draw(Composite parent) {
		Composite inComp = parent;
		if (usercomp != null) {
			inComp = usercomp;
		}
		mComposite =inComp;		
		if (myBoardID == null) {
			myBoardID = BoardDescriptor.makeBoardDescriptor(getConfdesc());
			if (myBoardID.getActualCoreCodePath() == null) {
				Activator.log(new Status(IStatus.ERROR, Activator.getId(),
						Messages.BoardSelectionPage_failed_to_find_platform.replace(Messages.PLATFORM ,myBoardID.getReferencingPlatformFile().toString())));  
			}
		}
		

		String[] allBoardsFileNames = PackageManager.getAllBoardsFiles();
		for (String curBoardFile : allBoardsFileNames) {
			mAllBoardsFiles.put(tidyUpLength(curBoardFile), curBoardFile);
		}
		if (mAllBoardsFiles.isEmpty()) {
			Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.error_no_platform_files_found, null));
		}
		

		GridLayout theGridLayout = new GridLayout();
		theGridLayout.numColumns = 3;
		mComposite.setLayout(theGridLayout);


		//createLabel(mComposite, 3, Messages.BoardSelectionPage_platform_you_want_to_use); 
		mControlBoardsTxtFile=new LabelCombo(mComposite, Messages.BoardSelectionPage_platform_folder, null, 2, true);
		mControlBoardsTxtFile.setItems(mAllBoardsFiles.keySet().toArray(new String[0]));
		createLine();

		//createLabel(mComposite, 3, Messages.BoardSelectionPage_arduino_board_specification); 
		mcontrolBoardName=new LabelCombo(mComposite, Messages.BoardSelectionPage_board, null, 2, true);
		mcontrolBoardName.setItems(mAllBoardsFiles.keySet().toArray(new String[0]));

		mControlUploadProtocol=new LabelCombo(mComposite, Messages.BoardSelectionPage_upload_protocol, null, 2, true);
		mControlUploadProtocol.setItems(mAllBoardsFiles.keySet().toArray(new String[0]));

		// ----
		mControlUploadPort = new LabelCombo(mComposite, Messages.ui_port, null, 1, false);

		mControlUploadPort
				.setItems(ArrayUtil.addAll(SerialManager.listNetworkPorts(), SerialManager.listComPorts()));
		mPwdButton = new org.eclipse.swt.widgets.Button(mComposite, SWT.PUSH | SWT.CENTER);
		mPwdButton.setText(Messages.set_or_remove_password);
		mPwdButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				switch (e.type) {
					case SWT.Selection :
						String host = getUpLoadPort().split(" ")[0]; //$NON-NLS-1$
						if (host.equals(getUpLoadPort())) {
						Activator.log(
								new Status(IStatus.ERROR, Activator.getId(), Messages.port_is_not_a_computer_name));
						} else {
							PasswordManager passwordManager = new PasswordManager();
						PasswordDialog dialog = new PasswordDialog(mComposite.getShell());
							passwordManager.setHost(host);
							dialog.setPasswordManager(passwordManager);
							dialog.open();
						}
						break;
				}
			}
		});
		createLine();

		TreeMap<String, String> menus = PackageManager.getAllmenus();

		mBoardOptionCombos = new LabelCombo[menus.size()];
		int index = 0;
		for (Map.Entry<String, String> curMenu : menus.entrySet()) {
			mBoardOptionCombos[index] = new LabelCombo(mComposite, curMenu.getValue(), curMenu.getKey(),
					2, true);
			mBoardOptionCombos[index++].addListener(labelComboListener);

		}

		// Create the control to alert parents of changes
		mFeedbackControl = new Text(mComposite, SWT.None);
		mFeedbackControl.setVisible(false);
		mFeedbackControl.setEnabled(false);
		GridData theGriddata;
		theGriddata = new GridData();
		theGriddata.horizontalSpan = 0;
		mFeedbackControl.setLayoutData(theGriddata);
		// End of special controls


		setValues();

		mcontrolBoardName.addListener(SWT.Modify, boardModifyListener);
		mControlBoardsTxtFile.addListener(SWT.Modify, boardFileModifyListener);

		enableControls();
		Dialog.applyDialogFont(mComposite);
	}

	private static String tidyUpLength(String longName) {
		IPath longPath = new Path(longName).removeLastSegments(1);
		IPath tidyPath = longPath;
		int segments = longPath.segmentCount();
		if (segments > 7) {
			tidyPath = longPath.removeLastSegments(segments - 2);
			tidyPath = tidyPath.append("..."); //$NON-NLS-1$
			tidyPath = tidyPath.append(longPath.removeFirstSegments(segments - 4));
		}
		return tidyPath.toString();
	}

	public boolean isPageComplete() {

		boolean MenuOpionsValidAndComplete = true;
		boolean ret = true;
		int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();
		if (selectedBoardFile == -1)
			return false;

		for (LabelCombo curLabelCombo : mBoardOptionCombos) {
			MenuOpionsValidAndComplete = MenuOpionsValidAndComplete && curLabelCombo.isValid();
		}

		ret = !getBoardName().isEmpty() && MenuOpionsValidAndComplete;
		if (!mFeedbackControl.getText().equals(ret ? TRUE : FALSE)) {
			mFeedbackControl.setText(ret ? TRUE : FALSE);
		}
		if (ret) {
			if (mBoardSelectionChangedListener != null) {
				mBoardSelectionChangedListener.handleEvent(new Event());
			}
		}

		return ret;
	}

	protected void enableControls() {
		mComposite.setEnabled(false);
		mComposite.setVisible(false);
		for (LabelCombo curLabelCombo : mBoardOptionCombos) {
			curLabelCombo.setVisible(true);
		}

		Display display=mComposite.getDisplay();
		mComposite.setBackground(display.getSystemColor(SWT.COLOR_BLUE));
		mComposite.pack();
		mComposite.layout(true, true);
		mComposite.requestLayout();
		
		mComposite.setEnabled(true);
		mComposite.setVisible(true);

		mComposite.redraw();
		

		// mComposite.getShell().pack();
		// mComposite.getShell().redraw();

	}

	@Override
	public boolean canBeVisible() {
		return true;
	}

	@Override
	protected void performDefaults() {
		// nothing to do here

	}

	@Override
	protected void updateData(ICResourceDescription cfg) {
		myBoardID.saveConfiguration();
		myBoardID = BoardDescriptor.makeBoardDescriptor(cfg.getConfiguration());
		setValues();
	}

	@Override
	protected void updateButtons() {
		// nothing to do here

	}

	private void setValues() {

		mControlBoardsTxtFile.setText(tidyUpLength(myBoardID.getReferencingBoardsFile().toString()));
		mcontrolBoardName.setItems(myBoardID.getCompatibleBoards());
		mcontrolBoardName.setText(myBoardID.getBoardName());

		String CurrentUploadProtocol = getUpLoadProtocol();
		mControlUploadProtocol.setItems(myBoardID.getUploadProtocols());
		mControlUploadProtocol.setText(CurrentUploadProtocol);
		if (getUpLoadProtocol().isEmpty()) {
			mControlUploadProtocol.setText(myBoardID.getProgrammer());
			if (mControlUploadProtocol.getText().isEmpty()) {
				mControlUploadProtocol.setText(Defaults.getDefaultUploadProtocol());
			}
		}

		mControlUploadPort.setValue(myBoardID.getUploadPort());

		// set the options in the combo boxes before setting the value
		Map<String, String> options = myBoardID.getOptions();

		for (LabelCombo curLabelCombo : mBoardOptionCombos) {
			curLabelCombo.setItems(myBoardID.getMenuItemNamesFromMenuID(curLabelCombo.getID()));
			if (options != null) {
				String value = options.get(curLabelCombo.getID());
				if (value != null) {
					try {
						curLabelCombo
								.setValue(myBoardID.getMenuItemNamedFromMenuItemID(value, curLabelCombo.getID()));
					} catch (Exception e) {
						// When this fails no default value will be set
						// so nothing to do here
					}
				}
			}
		}
	}

	@Override
	protected void performOK() {
		doOK();
		super.performOK();
	}

	@Override
	protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
		doOK();
	}

	private void doOK() {
		myBoardID.setreferencingBoardsFile(getSelectedBoardsFile());
		myBoardID.setUploadPort(getUpLoadPort());
		myBoardID.setUploadProtocol(getUpLoadProtocol());
		myBoardID.setBoardName(getBoardName());
		myBoardID.setOptions(getOptions());
		ICConfigurationDescription confdesc = getConfdesc();

		try {
			myBoardID.save(confdesc);

		} catch (Exception e) {
			Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.error_adding_arduino_code, e));
		}
	}

	private class ArduinoSelectionPageListener implements Listener {
		private AbstractPage myPage;

		ArduinoSelectionPageListener(AbstractPage page) {
			myPage = page;
		}

		@Override
		public void handleEvent(Event event) {
			myPage.setValid(isPageComplete());
		}
	}

	@Override
	public void handleTabEvent(int kind, Object data) {
		if (kind == 222) {
			mFeedbackControl.addListener(SWT.Modify, new ArduinoSelectionPageListener((AbstractPage) data));
		}
		super.handleTabEvent(kind, data);
	}

	protected File getSelectedBoardsFile() {
		if (mControlBoardsTxtFile == null) {
			return null;
		}
		String selectedText = mControlBoardsTxtFile.getText().trim();
		String longText = mAllBoardsFiles.get(selectedText);
		if (longText == null) {
			return null;// this should not happen
		}
		return new File(longText);
	}

	public String getUpLoadPort() {
		if (mControlUploadPort == null) {
			return new String();
		}
		return mControlUploadPort.getValue();
	}

	protected String getBoardName() {
		if (mcontrolBoardName == null) {
			return null;
		}
		return mcontrolBoardName.getText().trim();
	}

	protected String getUpLoadProtocol() {
		if (mControlUploadProtocol == null) {
			return Defaults.getDefaultUploadProtocol();
		}
		return mControlUploadProtocol.getText().trim();
	}

	private Map<String, String> getOptions() {
		if (mBoardOptionCombos == null) {
			return null;
		}
		Map<String, String> options = new HashMap<>();
		for (LabelCombo curLabelCombo : mBoardOptionCombos) {
			if (curLabelCombo.isVisible()) {
				options.put(curLabelCombo.getID(),
						myBoardID.getMenuItemIDFromMenuItemName(curLabelCombo.getValue(), curLabelCombo.getID()));
			}
		}
		return options;
	}

	public BoardDescriptor getBoardID() {
		if (myBoardID == null) {
			myBoardID = BoardDescriptor.makeBoardDescriptor(getConfdesc());
		}
		if (mBoardOptionCombos != null) {// only update the values if the
			// page has been drawn
			myBoardID.setreferencingBoardsFile(getSelectedBoardsFile());
			myBoardID.setBoardName(getBoardName());
			myBoardID.setOptions(getOptions());
			myBoardID.setUploadPort(getUpLoadPort());
			myBoardID.setUploadProtocol(getUpLoadProtocol());
		}
		return myBoardID;
	}

}
