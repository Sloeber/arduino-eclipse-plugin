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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.Defaults;
import io.sloeber.core.api.PasswordManager;
import io.sloeber.core.api.SerialManager;
import io.sloeber.core.common.Const;
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
public class BoardSelectionPage extends AbstractCPropertyTab {
	private static final String TRUE = "TRUE"; //$NON-NLS-1$

	private static final String FALSE = "FALSE"; //$NON-NLS-1$

	// global stuff to allow to communicate outside this class
	public Text mFeedbackControl;

	// GUI elements
	protected Combo mControlBoardsTxtFile;
	protected Combo mcontrolBoardName;
	protected Combo mControlUploadProtocol;
	protected LabelCombo mControlUploadPort;
	protected LabelCombo[] mBoardOptionCombos = null;
	private final int ncol = 3;
	protected Listener mBoardSelectionChangedListener = null;
	protected BoardDescriptor myBoardID = null;

	/**
	 * Get the configuration we are currently working in. The configuration is
	 * null if we are in the create sketch wizard.
	 *
	 * @return the configuration to save info into
	 */
	public ICConfigurationDescription getConfdesc() {
		if (this.page != null) {
			return getResDesc().getConfiguration();
		}
		return null;
	}

	private Listener boardFileModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {

			File boardFile = getSelectedBoardsFile();
			BoardSelectionPage.this.myBoardID.setreferencingBoardsFile(boardFile);

			/*
			 * Change the list of available boards
			 */
			String CurrentBoard = getBoardName();
			BoardSelectionPage.this.mcontrolBoardName.removeAll();
			BoardSelectionPage.this.mcontrolBoardName.setItems(BoardSelectionPage.this.myBoardID.getCompatibleBoards());
			BoardSelectionPage.this.mcontrolBoardName.setText(CurrentBoard);

			/*
			 * Change the list of available upload protocols
			 */
			String CurrentUploadProtocol = getUpLoadProtocol();
			BoardSelectionPage.this.mControlUploadProtocol.removeAll();
			BoardSelectionPage.this.mControlUploadProtocol
					.setItems(BoardSelectionPage.this.myBoardID.getUploadProtocols());
			BoardSelectionPage.this.mControlUploadProtocol.setText(CurrentUploadProtocol);

			if (BoardSelectionPage.this.mControlUploadProtocol.getText().isEmpty()) {
				BoardSelectionPage.this.myBoardID.setUploadProtocol(Defaults.getDefaultUploadProtocol());
				BoardSelectionPage.this.mControlUploadProtocol.setText(Defaults.getDefaultUploadProtocol());
			}

			BoardSelectionPage.this.boardModifyListener.handleEvent(null);
		}

	};

	protected Listener boardModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {

			BoardSelectionPage.this.myBoardID.setBoardName(getBoardName());

			for (LabelCombo curLabelCombo : BoardSelectionPage.this.mBoardOptionCombos) {
				curLabelCombo
						.setItems(BoardSelectionPage.this.myBoardID.getMenuItemNamesFromMenuID(curLabelCombo.getID()));
				curLabelCombo.setLabel(BoardSelectionPage.this.myBoardID.getMenuNameFromMenuID(curLabelCombo.getID()));
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
		this.mBoardSelectionChangedListener = BoardSelectionChangedListener;
	}

	private static void createLabel(Composite parent, int ncol, String t) {
		Label line = new Label(parent, SWT.HORIZONTAL | SWT.BOLD);
		line.setText(t);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	}

	private static void createLine(Composite parent, int ncol) {
		Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	}

	public void draw(Composite composite) {
		// create the desired layout for this wizard page
		if (this.myBoardID == null) {
			this.myBoardID = BoardDescriptor.makeBoardDescriptor(getConfdesc());
		}
		ICConfigurationDescription confdesc = getConfdesc();

		String[] allBoardsFileNames = PackageManager.getAllBoardsFiles();
		for (String curBoardFile : allBoardsFileNames) {
			this.mAllBoardsFiles.put(tidyUpLength(curBoardFile), curBoardFile);
		}

		Slider slider = new Slider(composite, SWT.VERTICAL);
		slider.setMinimum(0);
		slider.setMaximum(100);
		
		this.mComposite = slider;

		GridLayout theGridLayout = new GridLayout();
		theGridLayout.numColumns = this.ncol;
		composite.setLayout(theGridLayout);

		GridData theGriddata;

		if (this.mAllBoardsFiles.isEmpty()) {

			Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.error_no_platform_files_found, null));
		}

		// create a combo to select the boards
		createLabel(composite, this.ncol, "The platform you want to use"); //$NON-NLS-1$
		new Label(composite, SWT.NONE).setText("Platform folder:"); //$NON-NLS-1$

		this.mControlBoardsTxtFile = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		theGriddata.horizontalSpan = (this.ncol - 1);
		this.mControlBoardsTxtFile.setLayoutData(theGriddata);
		this.mControlBoardsTxtFile.setEnabled(false);
		this.mControlBoardsTxtFile.setItems(this.mAllBoardsFiles.keySet().toArray(new String[0]));

		createLine(composite, this.ncol);
		// -------

		// ------
		createLabel(composite, this.ncol, "Your Arduino board specifications"); //$NON-NLS-1$

		new Label(composite, SWT.NONE).setText("Board:"); //$NON-NLS-1$
		this.mcontrolBoardName = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		theGriddata.horizontalSpan = (this.ncol - 1);
		this.mcontrolBoardName.setLayoutData(theGriddata);
		this.mcontrolBoardName.setEnabled(false);

		// ------
		new Label(composite, SWT.NONE).setText("Upload Protocol:"); //$NON-NLS-1$
		this.mControlUploadProtocol = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		theGriddata.horizontalSpan = (this.ncol - 1);
		this.mControlUploadProtocol.setLayoutData(theGriddata);
		this.mControlUploadProtocol.setEnabled(false);

		// ----
		this.mControlUploadPort = new LabelCombo(composite, Messages.ui_port, null, this.ncol - 2, false);

		this.mControlUploadPort
				.setItems(ArrayUtil.addAll(SerialManager.listNetworkPorts(), SerialManager.listComPorts()));
		this.mPwdButton = new org.eclipse.swt.widgets.Button(composite, SWT.PUSH | SWT.CENTER);
		this.mPwdButton.setText(Messages.Set_or_Remove_password);
		this.mPwdButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
					String host = getUpLoadPort().split(Const.SPACE)[0];
					if (host.equals(getUpLoadPort())) {
						Activator.log(
								new Status(IStatus.ERROR, Activator.getId(), Messages.port_is_not_a_computer_name));
					} else {
						PasswordManager passwordManager = new PasswordManager();
						PasswordDialog dialog = new PasswordDialog(composite.getShell());
						passwordManager.setHost(host);
						dialog.setPasswordManager(passwordManager);
						dialog.open();
					}
					break;
				}
			}
		});
		createLine(composite, this.ncol);

		TreeMap<String, String> menus = PackageManager.getAllmenus();

		this.mBoardOptionCombos = new LabelCombo[menus.size()];
		int index = 0;
		for (Map.Entry<String, String> curMenu : menus.entrySet()) {
			this.mBoardOptionCombos[index] = new LabelCombo(composite, curMenu.getValue(), curMenu.getKey(),
					this.ncol - 1, true);
			this.mBoardOptionCombos[index++].addListener(this.labelComboListener);

		}

		// Create the control to alert parents of changes
		this.mFeedbackControl = new Text(composite, SWT.None);
		this.mFeedbackControl.setVisible(false);
		this.mFeedbackControl.setEnabled(false);
		theGriddata = new GridData();
		theGriddata.horizontalSpan = 0;
		this.mFeedbackControl.setLayoutData(theGriddata);
		// End of special controls

		setValues(confdesc);

		this.mcontrolBoardName.addListener(SWT.Modify, this.boardModifyListener);
		this.mControlBoardsTxtFile.addListener(SWT.Modify, this.boardFileModifyListener);

		enableControls();
		Dialog.applyDialogFont(composite);
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
		int selectedBoardFile = this.mControlBoardsTxtFile.getSelectionIndex();
		if (selectedBoardFile == -1)
			return false;

		for (LabelCombo curLabelCombo : this.mBoardOptionCombos) {
			MenuOpionsValidAndComplete = MenuOpionsValidAndComplete && curLabelCombo.isValid();
		}

		ret = !getBoardName().isEmpty() && MenuOpionsValidAndComplete;
		if (!this.mFeedbackControl.getText().equals(ret ? TRUE : FALSE)) {
			this.mFeedbackControl.setText(ret ? TRUE : FALSE);
		}
		if (ret) {
			if (this.mBoardSelectionChangedListener != null) {
				this.mBoardSelectionChangedListener.handleEvent(new Event());
			}
		}

		return ret;
	}

	protected void enableControls() {
		this.mcontrolBoardName.setEnabled(true);
		this.mControlUploadPort.setEnabled(true);
		this.mControlUploadProtocol.setEnabled(true);
		this.mControlBoardsTxtFile.setEnabled(true);
		for (LabelCombo curLabelCombo : this.mBoardOptionCombos) {
			curLabelCombo.setVisible(true);
		}

		this.mComposite.layout(true, true);
		this.mComposite.requestLayout();
		// this.mComposite.getShell().pack();
		// this.mComposite.getShell().redraw();

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
		this.myBoardID.saveConfiguration();
		this.myBoardID = BoardDescriptor.makeBoardDescriptor(cfg.getConfiguration());
		setValues(cfg.getConfiguration());
	}

	@Override
	protected void updateButtons() {
		// nothing to do here

	}

	private void setValues(ICConfigurationDescription confdesc) {

		this.mControlBoardsTxtFile.setText(tidyUpLength(this.myBoardID.getReferencingBoardsFile().toString()));
		this.mcontrolBoardName.setItems(this.myBoardID.getCompatibleBoards());
		this.mcontrolBoardName.setText(this.myBoardID.getBoardName());

		String CurrentUploadProtocol = getUpLoadProtocol();
		BoardSelectionPage.this.mControlUploadProtocol.removeAll();
		BoardSelectionPage.this.mControlUploadProtocol.setItems(this.myBoardID.getUploadProtocols());
		BoardSelectionPage.this.mControlUploadProtocol.setText(CurrentUploadProtocol);
		if (getUpLoadProtocol().isEmpty()) {
			this.mControlUploadProtocol.setText(this.myBoardID.getProgrammer());
			if (this.mControlUploadProtocol.getText().isEmpty()) {
				this.mControlUploadProtocol.setText(Defaults.getDefaultUploadProtocol());
			}
		}

		this.mControlUploadPort.setValue(this.myBoardID.getUploadPort());

		// set the options in the combo boxes before setting the value
		Map<String, String> options = this.myBoardID.getOptions();

		for (LabelCombo curLabelCombo : this.mBoardOptionCombos) {
			curLabelCombo.setItems(this.myBoardID.getMenuItemNamesFromMenuID(curLabelCombo.getID()));
			if (options != null) {
				String value = options.get(curLabelCombo.getID());
				if (value != null) {
					try {
						curLabelCombo
								.setValue(this.myBoardID.getMenuItemNamedFromMenuItemID(value, curLabelCombo.getID()));
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
		this.myBoardID.setreferencingBoardsFile(getSelectedBoardsFile());
		this.myBoardID.setUploadPort(getUpLoadPort());
		this.myBoardID.setUploadProtocol(getUpLoadProtocol());
		this.myBoardID.setBoardName(getBoardName());
		this.myBoardID.setOptions(getOptions());
		ICConfigurationDescription confdesc = getConfdesc();

		try {
			this.myBoardID.save(confdesc);

		} catch (Exception e) {
			Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.error_adding_arduino_code, e));
		}
	}

	private class ArduinoSelectionPageListener implements Listener {
		private AbstractPage myPage;

		ArduinoSelectionPageListener(AbstractPage page) {
			this.myPage = page;
		}

		@Override
		public void handleEvent(Event event) {
			this.myPage.setValid(isPageComplete());
		}
	}

	@Override
	public void handleTabEvent(int kind, Object data) {
		if (kind == 222) {
			this.mFeedbackControl.addListener(SWT.Modify, new ArduinoSelectionPageListener((AbstractPage) data));
		}
		super.handleTabEvent(kind, data);
	}

	protected File getSelectedBoardsFile() {
		if (this.mControlBoardsTxtFile == null) {
			return null;
		}
		String selectedText = this.mControlBoardsTxtFile.getText().trim();
		String longText = this.mAllBoardsFiles.get(selectedText);
		if (longText == null) {
			return null;// this should not happen
		}
		return new File(longText);
	}

	public String getUpLoadPort() {
		if (this.mControlUploadPort == null) {
			return new String();
		}
		return this.mControlUploadPort.getValue();
	}

	protected String getBoardName() {
		if (this.mcontrolBoardName == null) {
			return null;
		}
		return this.mcontrolBoardName.getText().trim();
	}

	protected String getUpLoadProtocol() {
		if (this.mControlUploadProtocol == null) {
			return Defaults.getDefaultUploadProtocol();
		}
		return this.mControlUploadProtocol.getText().trim();
	}

	private Map<String, String> getOptions() {
		if (this.mBoardOptionCombos == null) {
			return null;
		}
		Map<String, String> options = new HashMap<>();
		for (LabelCombo curLabelCombo : BoardSelectionPage.this.mBoardOptionCombos) {
			if (curLabelCombo.isVisible()) {
				options.put(curLabelCombo.getID(),
						this.myBoardID.getMenuItemIDFromMenuItemName(curLabelCombo.getValue(), curLabelCombo.getID()));
			}
		}
		return options;
	}

	public BoardDescriptor getBoardID() {
		if (this.myBoardID == null) {
			this.myBoardID = BoardDescriptor.makeBoardDescriptor(getConfdesc());
		}
		if (this.mBoardOptionCombos != null) {// only update the values if the
			// page has been drawn
			this.myBoardID.setreferencingBoardsFile(getSelectedBoardsFile());
			this.myBoardID.setBoardName(getBoardName());
			this.myBoardID.setOptions(getOptions());
			this.myBoardID.setUploadPort(getUpLoadPort());
			this.myBoardID.setUploadProtocol(getUpLoadProtocol());
		}
		return this.myBoardID;
	}

}
