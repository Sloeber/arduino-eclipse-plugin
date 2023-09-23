package io.sloeber.ui.project.properties;

import static io.sloeber.ui.Activator.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.cdt.core.parser.util.ArrayUtil;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.PasswordManager;
import io.sloeber.core.api.SerialManager;
import io.sloeber.ui.LabelCombo;
import io.sloeber.ui.Messages;

/**
 * The BoardSelectionPage class is used in the new wizard and the project
 * properties. This class controls the gui and the data underneath the gui. This
 * class allows to select the arduino board and the port name
 *
 * @author Jan Baeyens
 * @see ArduinoProperties ArduinoSettingsPage
 *
 */

public class BoardSelectionPage extends SloeberCpropertyTab {

	// GUI elements
	private LabelCombo myControlBoardsTxtFile = null;
	private LabelCombo mycontrolBoardName = null;
	private LabelCombo myControlUploadProtocol = null;
	private LabelCombo myControlUploadPort = null;
	private Map<String, LabelCombo> myBoardOptionCombos = new HashMap<>();
	private Composite myComposite = null;
	private ScrolledComposite myScrollComposite = null;
	private org.eclipse.swt.widgets.Button myPasswordButton = null;

	private TreeMap<String, File> myAllBoardsFiles = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private Map<String, String> myUsedOptionValues = new HashMap<>();
	private File myCurrentLabelComboBoardFile = null;
	private String myCurrentOptionBoardID = null;
	private Listener myCompleteListener = null;
	private boolean disableListeners = false;
	private BoardDescription myBoardDesc = new BoardDescription();

	public BoardDescription getBoardDescription() {
		return myBoardDesc;
	}

	private Listener myBoardFileModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			if (disableListeners) {
				return;
			}
			getBoardFromScreen();

			/*
			 * Change the list of available boards
			 */
			mycontrolBoardName.setItems(myBoardDesc.getCompatibleBoards());
			mycontrolBoardName.setText(myBoardDesc.getBoardName());

			/*
			 * Change the list of available upload protocols
			 */
			myControlUploadProtocol.setItems(myBoardDesc.getUploadProtocols());
			myControlUploadProtocol.setText(myBoardDesc.getProgrammer());

			setTheLabelCombos();
			genericListenerEnd();
		}

	};

	protected Listener myBoardModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			if (disableListeners) {
				return;
			}
			setTheLabelCombos();
			genericListenerEnd();
		}
	};
	protected Listener myChangeListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			if (disableListeners) {
				return;
			}
			genericListenerEnd();
		}
	};

	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);
		draw(usercomp);

	}

	private void createLine() {
		Label line = new Label(myComposite, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		line.setLayoutData(gridData);
	}

	public void draw(Composite parent) {
		parent.setLayout(new FillLayout());

		myScrollComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
		myScrollComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		myScrollComposite.setExpandHorizontal(true);
		myScrollComposite.setExpandVertical(true);

		myComposite = new Composite(myScrollComposite, SWT.NONE);
		GridLayout theGridLayout = new GridLayout();
		theGridLayout.numColumns = 3;
		myComposite.setLayout(theGridLayout);
		myComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		myScrollComposite.setContent(myComposite);

		File[] allBoardsFileNames = BoardsManager.getAllBoardsFiles();
		for (File curBoardFile : allBoardsFileNames) {
			myAllBoardsFiles.put(tidyUpLength(curBoardFile.toString()), curBoardFile);
		}
		if (myAllBoardsFiles.isEmpty()) {
			log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.error_no_platform_files_found, null));
		}

		myControlBoardsTxtFile = new LabelCombo(myComposite, Messages.BoardSelectionPage_platform_folder, 2, true);
		myControlBoardsTxtFile.setItems(myAllBoardsFiles.keySet().toArray(new String[0]));
		createLine();

		mycontrolBoardName = new LabelCombo(myComposite, Messages.BoardSelectionPage_board, 2, true);
		mycontrolBoardName.setItems(myAllBoardsFiles.keySet().toArray(new String[0]));

		myControlUploadProtocol = new LabelCombo(myComposite, Messages.BoardSelectionPage_upload_protocol, 2, true);
		myControlUploadProtocol.setItems(myAllBoardsFiles.keySet().toArray(new String[0]));

		myControlUploadPort = new LabelCombo(myComposite, Messages.ui_port, 1, false);

		myControlUploadPort.setItems(ArrayUtil.addAll(SerialManager.listNetworkPorts(), SerialManager.listComPorts()));
		myPasswordButton = new org.eclipse.swt.widgets.Button(myComposite, SWT.PUSH | SWT.CENTER);
		myPasswordButton.setText(Messages.set_or_remove_password);
		myPasswordButton.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
					String host = myControlUploadPort.getText().split(" ")[0]; //$NON-NLS-1$
					if (host.equals(myControlUploadPort.getText())) {
						log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.port_is_not_a_computer_name));
					} else {
						PasswordManager passwordManager = new PasswordManager();
						PasswordDialog dialog = new PasswordDialog(myComposite.getShell());
						passwordManager.setHost(host);
						dialog.setPasswordManager(passwordManager);
						dialog.open();
					}
					break;
				default:
					break;
				}
			}
		});
		createLine();

		updateScreen(false);

		mycontrolBoardName.addListener(SWT.Modify, myBoardModifyListener);
		myControlBoardsTxtFile.addListener(SWT.Modify, myBoardFileModifyListener);
		myControlUploadProtocol.addListener(myChangeListener);
		myControlUploadPort.addListener(myChangeListener);
		genericListenerEnd();

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

	/*
	 * This method should be called after each listener. It saves the current
	 * settings validates the page for completeness reports back to the listener
	 */
	private void genericListenerEnd() {

		getBoardFromScreen();
		boolean ret = true;
		int selectedBoardFile = myControlBoardsTxtFile.getSelectionIndex();
		if (selectedBoardFile == -1)
			ret = false;

		for (LabelCombo curLabelCombo : myBoardOptionCombos.values()) {
			ret = curLabelCombo.isValid() && ret;
		}

		ret = !mycontrolBoardName.getText().isEmpty() && ret;
		if (myCompleteListener != null) {
			Event event = new Event();
			event.doit = ret;
			myCompleteListener.handleEvent(event);
		}
	}

	@Override
	public boolean canBeVisible() {
		return true;
	}

	/**
	 * Get the options from screen you need the boarddescriptor to convert the menu
	 * item name to menu item id
	 * 
	 * @param boardDesc
	 * @return a map containing menuid, menuitemid mapping with what is shown on
	 *         screen
	 */
	private Map<String, String> getOptions() {
		if (myBoardOptionCombos == null) {
			return null;
		}
		Map<String, String> options = new HashMap<>();
		for (Entry<String, LabelCombo> curOption : myBoardOptionCombos.entrySet()) {
			String MenuID = curOption.getKey();
			String menuItemName = curOption.getValue().getText();
			String menuItemID = myBoardDesc.getMenuItemIDFromMenuItemName(menuItemName, MenuID);
			options.put(MenuID, menuItemID);
		}
		return options;
	}

	@Override
	protected void updateScreen(boolean updateData) {
		if (updateData) {
			myBoardDesc = mySloeberCfg.getBoardDescription();
		}
		disableListeners = true;

		myControlBoardsTxtFile.setText(tidyUpLength(myBoardDesc.getReferencingBoardsFile().toString()));
		mycontrolBoardName.setItems(myBoardDesc.getCompatibleBoards());
		mycontrolBoardName.setText(myBoardDesc.getBoardName());

		String CurrentUploadProtocol = myControlUploadProtocol.getText();
		myControlUploadProtocol.setItems(myBoardDesc.getUploadProtocols());
		myControlUploadProtocol.setText(CurrentUploadProtocol);
		if (myControlUploadProtocol.getText().trim().isEmpty()) {
			myControlUploadProtocol.setText(myBoardDesc.getProgrammer());
		}

		myControlUploadPort.setText(myBoardDesc.getUploadPort());
		setTheLabelCombos();
		disableListeners = false;
	}

	private void getBoardFromScreen() {
		String selectedText = myControlBoardsTxtFile.getText().trim();
		myBoardDesc.setreferencingBoardsFile(myAllBoardsFiles.get(selectedText));
		myBoardDesc.setUploadPort(myControlUploadPort.getText());
		myBoardDesc.setProgrammer(myControlUploadProtocol.getText());
		myBoardDesc.setBoardName(mycontrolBoardName.getText());
		myBoardDesc.setOptions(getOptions());
	}

	private void setTheLabelCombos() {

		saveUsedOptionValues();

		File onScreenComboBoardFile = myBoardDesc.getReferencingBoardsFile();
		String onScreenBoardID = myBoardDesc.getBoardID();
		boolean boardsFileChanged = !onScreenComboBoardFile.equals(myCurrentLabelComboBoardFile);
		boolean boardIDChanged = !onScreenBoardID.equals(myCurrentOptionBoardID);
		if (boardsFileChanged || boardIDChanged) {
			myCurrentLabelComboBoardFile = onScreenComboBoardFile;
			myCurrentOptionBoardID = onScreenBoardID;

			saveUsedOptionValues();
			for (LabelCombo labelCombo : myBoardOptionCombos.values()) {
				labelCombo.dispose();
			}
			myBoardOptionCombos.clear();

			Map<String, String> menus = myBoardDesc.getAllMenus();
			Map<String, String> boardOptions = myBoardDesc.getOptions();

			for (Map.Entry<String, String> curMenu : menus.entrySet()) {
				String menuName = curMenu.getValue();
				String menuID = curMenu.getKey();
				String[] menuItemNames = myBoardDesc.getMenuItemNamesFromMenuID(menuID);
				if (menuItemNames.length > 0) {
					LabelCombo newLabelCombo = new LabelCombo(myComposite, menuName, 2, true);
					myBoardOptionCombos.put(menuID, newLabelCombo);

					newLabelCombo.setItems(myBoardDesc.getMenuItemNamesFromMenuID(menuID));
					newLabelCombo.setLabel(menuName);
					String optionValue = boardOptions.get(menuID);
					if (optionValue != null) {
						// convert the ID to a name
						optionValue = myBoardDesc.getMenuItemNamedFromMenuItemID(optionValue, menuID);
					} else {
						// use last used name for this menu ID
						optionValue = myUsedOptionValues.get(menuID);
					}
					if (optionValue != null) {
						newLabelCombo.setText(optionValue);
					}
					newLabelCombo.addListener(myChangeListener);
				}
			}

			myComposite.layout(true);
			myComposite.pack();
			myScrollComposite.setContent(myComposite);
			Point point = myComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			myScrollComposite.setMinSize(point);
		} else {
			Map<String, String> boardOptions = myBoardDesc.getOptions();
			for (Entry<String, LabelCombo> curOptionCombo : myBoardOptionCombos.entrySet()) {
				String curMenuID = curOptionCombo.getKey();
				LabelCombo curLabelCombo = curOptionCombo.getValue();
				String optionValue = boardOptions.get(curMenuID);
				if (optionValue != null) {
					// convert the ID to a name
					optionValue = myBoardDesc.getMenuItemNamedFromMenuItemID(optionValue, curMenuID);
				} else {
					// use last used name for this menu ID
					optionValue = myUsedOptionValues.get(curMenuID);
				}
				if (optionValue != null) {
					curLabelCombo.setText(optionValue);
				}
			}
		}

	}

	public void addListener(Listener completeListener) {
		myCompleteListener = completeListener;
	}

	/**
	 * save the options as they are selected so the user does not have to reenter
	 * them next time they come to the old selection
	 * 
	 * This does not work across opening of the dialog
	 */
	private void saveUsedOptionValues() {
		for (Entry<String, LabelCombo> curEntry : myBoardOptionCombos.entrySet()) {
			String key = curEntry.getKey();
			LabelCombo labelCombo = curEntry.getValue();
			String value = labelCombo.getText();
			if (!value.isEmpty()) {
				myUsedOptionValues.put(key, value);
			}
		}
	}

	@Override
	protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void performDefaults() {
		// TODO Auto-generated method stub

	}

}
