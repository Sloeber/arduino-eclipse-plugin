/*******************************************************************************
 * 
 * Copyright (c) 2008, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: AbstractTabAVRDudeBytes.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.propertypages;

import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.avrdude.AVRDudeSchedulingRule;
import it.baeyens.avreclipse.core.avrdude.BaseBytesProperties;
import it.baeyens.avreclipse.core.properties.AVRDudeProperties;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.core.toolinfo.fuses.ConversionResults;
import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;
import it.baeyens.avreclipse.ui.AVRUIPlugin;
import it.baeyens.avreclipse.ui.controls.FuseBytePreviewControl;
import it.baeyens.avreclipse.ui.dialogs.AVRDudeErrorDialogJob;
import it.baeyens.avreclipse.ui.dialogs.ByteValuesEditorDialog;
import it.baeyens.avreclipse.ui.dialogs.ProjectMCUMismatchDialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.progress.UIJob;


/**
 * The base AVRDude Tab page for Fuses and Lockbits.
 * <p>
 * The GUI for Fuse bytes and for Lockbits is the same and is handled in this class. The subclasses
 * just supply a few basic informations, but do not need to do any user interface handling.
 * </p>
 * <p>
 * Subclasses of this tab have three radio buttons:
 * <ul>
 * <li>Do not upload anything</li>
 * <li>Upload the byte values defined in a file</li>
 * <li>Upload some immediate byte values</li>
 * </ul>
 * Also a detailed preview of the selected bytes is shown.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public abstract class AbstractTabAVRDudeBytes extends AbstractAVRDudePropertyTab {

	private final static int		LABEL_GROUPNAME			= 0;
	private final static int		LABEL_NAME				= 1;

	// The GUI texts
	private final static String		GROUP_NAME				= "Upload {0}";
	private final static String		TEXT_NOUPLOAD			= "do not set {0}";
	private final static String		TEXT_FROMFILE			= "from {0} file";
	private final static String		TEXT_IMMEDIATE			= "direct hex value{0}";

	private final static String		WARN_BYTESINCOMPATIBLE	= "These hex values are for an {0} MCU.\n"
																	+ "This is not compatible with the {2} MCU setting [{1}].";
	private final static String		WARN_BUTTON_CONVERT		= "Convert";
	private final static String		WARN_FROMPROJECT		= "project";
	private final static String		WARN_FROMCONFIG			= "build configuration";

	// Warning image
	private static final Image		IMG_WARN				= PlatformUI
																	.getWorkbench()
																	.getSharedImages()
																	.getImage(
																			ISharedImages.IMG_OBJS_WARN_TSK);

	// ToolTip texts for the hex value actions
	private final static String		MENU_EDIT				= "Start editor";
	private final static String		MENU_READDEVICE			= "Load from MCU";
	private final static String		TEXT_LOADING			= "Loading from MCU...";
	private final static String		MENU_COPYFILE			= "Copy from file";
	private final static String		MENU_DEFAULTS			= "Set to default (if available)";
	private final static String		MENU_ALLONES			= "Set all bits to 1";
	private final static String		MENU_ALLZEROS			= "Set all bits to 0";
	private final static String		MENU_CLEARALL			= "Clear all bytes";

	// The GUI widgets
	private Button					fNoUploadButton;

	private Button					fUploadFileButton;
	private Text					fFileText;
	private Composite				fFileWarningCompo;
	private Label					fFileWarningLabel;
	private Button					fWorkplaceButton;
	private Button					fFilesystemButton;
	private Button					fVariableButton;

	private Button					fImmediateButton;
	private Composite				fBytesCompo;
	private ToolBar					fActionsToolBar;
	private Label					fLoadingLabel;

	private Composite[]				fByteCompos;
	private Text[]					fValueTexts;
	private Label[]					fFuseLabels;

	private Composite				fHexWarningCompo;
	private Label					fWarningLabel;
	private Button					fConvertButton;

	private FuseBytePreviewControl	fPreviewControl;

	/** List of all created Images to dispose them when this tab is disposed. */
	private final List<Image>		fImages					= new ArrayList<Image>(ActionItem
																	.values().length * 2);

	/** The Properties that this page works with */
	private AVRDudeProperties		fTargetProps;

	/** The BaseBytesProperties property object this page works with */
	protected BaseBytesProperties	fBytes;

	// The abstract hook methods for the subclasses

	/**
	 * Get an array of label strings.
	 * <p>
	 * Currently the returned array must contain two Strings. The first entry is used for the group
	 * label ("Upload {0}") and the second entry is used in multiple places like ("from {0} file").
	 * </p>
	 * 
	 * @return Array of <code>String</code>s with label strings.
	 */
	protected abstract String[] getLabels();

	/**
	 * Get the type of fuse memory this tab is for, {@link FuseType#FUSE} or
	 * {@link FuseType#LOCKBITS}.
	 * 
	 * @return
	 */
	protected abstract FuseType getType();

	/**
	 * Load the ByteValues from the target MCU with avrdude.
	 * 
	 * @param avrdudeprops
	 *            The current properties, including the ProgrammerConfig needed by avrdude.
	 * @return A <code>ByteValues</code> object with the bytes read from the MCU.
	 * @throws AVRDudeException
	 *             for any Exception thrown by avrdude
	 */
	protected abstract ByteValues getByteValues(AVRDudeProperties avrdudeprops,
			IProgressMonitor monitor) throws AVRDudeException;

	/**
	 * Get the Label text for the n-th byte.
	 * 
	 * @param index
	 *            0-5 for fuses, 0 for lockbits
	 * @return <code>String</code> with the name of the byte at the index.
	 */
	protected abstract String getByteEditorLabel(int index);

	/**
	 * Get an array with file extensions.
	 * <p>
	 * This list is used by the "from FileSystem" file dialog to show only files with the
	 * appropriate extension.
	 * 
	 * @return Array of <code>String</code>s with file extensions like ".fuses".
	 */
	protected abstract String[] getFileExtensions();

	/**
	 * Get the actual Byte properties this tab works with.
	 * 
	 * @param avrdudeprops
	 *            Source properties
	 * @return <code>FuseBytesProperties</code> or <code>LockbitBytesProperties</code> object
	 *         extracted from the given <code>AVRDudeProperties</code>
	 */
	protected abstract BaseBytesProperties getByteProps(AVRDudeProperties avrdudeprops);

	// The GUI stuff

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#dispose()
	 */
	@Override
	public void dispose() {
		// remove all allocated images
		for (Image image : fImages) {
			image.dispose();
		}
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.cdt.ui.newui.AbstractCPropertyTab#createControls(org.eclipse.swt.widgets.Composite
	 * )
	 */
	@Override
	public void createControls(Composite parent) {

		// init the arrays
		int maxbytes = getType().getMaxBytes();
		fByteCompos = new Composite[maxbytes];
		fValueTexts = new Text[maxbytes];
		fFuseLabels = new Label[maxbytes];

		parent.setLayout(new GridLayout(1, false));

		// Add the source selection group
		addSourceSelectionGroup(parent);

		// Add the detailed byte values preview
		fPreviewControl = new FuseBytePreviewControl(parent, SWT.BORDER);
		fPreviewControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

	}

	/**
	 * Add the main selection group.
	 * <p>
	 * This group has three sections (with radio buttons):
	 * <ul>
	 * <li>Do not write bytes</li>
	 * <li>Write bytes from a user selectable file</li>
	 * <li>Write the bytes given</li>
	 * </ul>
	 * </p>
	 * 
	 * @param parent
	 *            Parent <code>Composite</code>
	 */
	private void addSourceSelectionGroup(Composite parent) {

		// Group Setup
		Group group = new Group(parent, SWT.NONE);
		group.setText(MessageFormat.format(GROUP_NAME, getLabels()[LABEL_GROUPNAME]));
		group.setLayout(new GridLayout(4, false));
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

		addNoUploadSection(group);

		// addSeparator(group);

		addFromFileSection(group);

		// addSeparator(group);

		addImmediateSection(group);

		addWarningSection(group);
	}

	/**
	 * The "No upload" Section.
	 * 
	 * @param parent
	 *            Parent <code>Composite</code>
	 */
	private void addNoUploadSection(Composite parent) {

		fNoUploadButton = new Button(parent, SWT.RADIO);
		fNoUploadButton.setText(MessageFormat.format(TEXT_NOUPLOAD, getLabels()[LABEL_NAME]));
		fNoUploadButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false, 1, 1));
		fNoUploadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Set the properties
				fBytes.setWrite(false);

				// and disable the other controls
				enableFileGroup(false);
				enableByteGroup(false);

				updateFields();
				// If the warning was active it is now made invisible
				checkValid();
			}
		});

		// Dummy to fill up the next 3 columns of the gridlayout
		Label dummy = new Label(parent, SWT.NONE);
		dummy.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

	}

	/**
	 * The "Upload from file" Section.
	 * <p>
	 * Contains a Text control to enter a filename and three buttons to select the filename from the
	 * workplace, the filesystem or from a build variable.
	 * </p>
	 * 
	 * @param parent
	 *            Parent <code>Composite</code>
	 */
	private void addFromFileSection(Composite parent) {

		fUploadFileButton = new Button(parent, SWT.RADIO);
		fUploadFileButton.setText(MessageFormat.format(TEXT_FROMFILE, getLabels()[LABEL_NAME]));
		fUploadFileButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		fUploadFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fUploadFileButton.getSelection() == false) {
					// Button was deselected (another button has been selected
					// The other button will handle everything
					return;
				}
				fBytes.setWrite(true);
				fBytes.setUseFile(true);
				enableFileGroup(true);
				enableByteGroup(false);
				updateFields();
				updateAVRDudePreview(fTargetProps);
				fPreviewControl.setByteValues(fBytes.getByteValues());
				checkValid();
			}
		});

		fFileText = new Text(parent, SWT.BORDER);
		fFileText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 3, 1));
		fFileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String newpath = fFileText.getText();
				fBytes.setFileName(newpath);
				updateFields();
				checkValid();
			}
		});

		// The next line in the GUI consists of a Warning composite and three file dialog buttons,
		// all wrapped in one composite.
		Composite compo = new Composite(parent, SWT.NONE);
		compo.setBackgroundMode(SWT.INHERIT_FORCE);
		compo.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 4, 1));
		compo.setLayout(new GridLayout(4, false));

		// Warning composite
		fFileWarningCompo = new Composite(compo, SWT.NONE);
		fFileWarningCompo.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false, 1, 1));
		fFileWarningCompo.setLayout(new GridLayout(2, false));

		Label warnicon = new Label(fFileWarningCompo, SWT.LEFT);
		warnicon.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		warnicon.setImage(IMG_WARN);

		fFileWarningLabel = new Label(fFileWarningCompo, SWT.WRAP);
		fFileWarningLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		fFileWarningLabel.setText("");

		// The three buttons
		fWorkplaceButton = setupWorkplaceButton(compo, fFileText);
		fFilesystemButton = setupFilesystemButton(compo, fFileText, getFileExtensions());
		fVariableButton = setupVariableButton(compo, fFileText);

	}

	/**
	 * The "Upload from direct values" Section.
	 * <p>
	 * Contains controls to edit all bytes directly and two buttons to read the byte values from the
	 * programmer and to copy the values from the file.
	 * </p>
	 * 
	 * @param parent
	 *            Parent <code>Composite</code>
	 */
	private void addImmediateSection(Composite parent) {

		// add the radio button
		fImmediateButton = new Button(parent, SWT.RADIO);
		fImmediateButton.setText(MessageFormat.format(TEXT_IMMEDIATE,
				getType().getMaxBytes() > 1 ? "s" : ""));
		GridData buttonGD = new GridData(SWT.BEGINNING, SWT.TOP, false, false);
		// This is somewhat arbitrarily and looks good on my setup.
		// Your mileage may vary.
		buttonGD.verticalIndent = 8;
		fImmediateButton.setLayoutData(buttonGD);
		fImmediateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fImmediateButton.getSelection() == false) {
					// Button was deselected (another button has been selected
					// The other button will handle everything
					return;
				}
				fBytes.setWrite(true);
				fBytes.setUseFile(false);
				enableFileGroup(false);
				enableByteGroup(true);
				updateFields();

				// Check if the byte values are compatible and display a warning if required
				checkValid();
			}
		});

		// add the byte editor composites (wrapped in a composite)
		fBytesCompo = new Composite(parent, SWT.NONE);
		GridData bytesGD = new GridData(SWT.BEGINNING, SWT.TOP, false, false, 1, 1);

		// Make the size of the byte edit fields somewhat dependent on the font
		// size. I use 6 chars instead of the actual required 2, because 2 was
		// just to small.
		FontMetrics fm = getFontMetrics(parent);
		bytesGD.widthHint = Dialog.convertWidthInCharsToPixels(fm, 6) * getType().getMaxBytes();
		fBytesCompo.setLayoutData(bytesGD);
		fBytesCompo.setLayout(new FillLayout(SWT.HORIZONTAL));

		// Insert the byte editor compos
		for (int i = 0; i < getType().getMaxBytes(); i++) {
			makeByteEditComposite(fBytesCompo, i);
		}

		// Add the actions menu

		fActionsToolBar = createActionsToolbar(parent);
		GridData toolbarGD = new GridData(SWT.BEGINNING, SWT.TOP, false, false);
		fActionsToolBar.setLayoutData(toolbarGD);

		// and the loading label
		fLoadingLabel = new Label(parent, SWT.NONE);
		fLoadingLabel.setText(TEXT_LOADING);
		GridData loadingGD = new GridData(SWT.BEGINNING, SWT.TOP, true, false);
		fLoadingLabel.setLayoutData(loadingGD);
		fLoadingLabel.setVisible(false);

		// Adjust the Layout: try to get all components to line up on their baseline.
		// This is more or less a hack and probably only looks good on my system.
		// But I don't know SWT well enough to do this the right way.
		// Feel free to do something else!
		Point sizeButton = fImmediateButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point sizeToolBar = fActionsToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point sizeEditors = fByteCompos[0].getChildren()[0].computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point sizeLabel = fLoadingLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT);

		// Toolbar is the master. Align the tops of the other three components in this row according
		// to the height of the toolbar.
		int tbheight = sizeToolBar.y;

		buttonGD.verticalIndent = tbheight - sizeButton.y - 3;
		bytesGD.verticalIndent = tbheight - sizeEditors.y - 2;
		loadingGD.verticalIndent = tbheight - sizeLabel.y - 5;
	}

	/**
	 * Add the warning section, which consists of a composite that can be set visible or hidden as
	 * required.
	 * 
	 * @param parent
	 *            Parent <code>Composite</code>
	 */
	private void addWarningSection(Composite parent) {

		// The Warning Composite
		fHexWarningCompo = new Composite(parent, SWT.NONE);
		fHexWarningCompo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1));
		GridLayout gl = new GridLayout(3, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		fHexWarningCompo.setLayout(gl);

		Label warnicon = new Label(fHexWarningCompo, SWT.LEFT);
		warnicon.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		warnicon.setImage(IMG_WARN);

		fWarningLabel = new Label(fHexWarningCompo, SWT.LEFT | SWT.WRAP);
		fWarningLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		fWarningLabel.setText("");

		fConvertButton = new Button(fHexWarningCompo, SWT.PUSH);
		fConvertButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		fConvertButton.setText(WARN_BUTTON_CONVERT);
		fConvertButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				// Convert the Fuse bytes to the MCU of the project / build configuration
				String mcuid = fTargetProps.getParent().getMCUId();
				ByteValues newvalues = convertFusesTo(mcuid, fBytes.getByteValues());
				fBytes.setByteValues(newvalues);

				checkValid();
				updateFields();
			}
		});

		fHexWarningCompo.setVisible(false);

	}

	/**
	 * Create a ToolBar with the actions for the direct Hex entry line.
	 * <p>
	 * The contents of the ToolBar are actually defined by the {@link ActionItem} enumeration.
	 * </p>
	 * 
	 * @param parent
	 *            <code>Composite</code> to which the ToolBar is added.
	 * @return Reference to the ToolBar
	 */
	private ToolBar createActionsToolbar(Composite parent) {

		SelectionListener menuListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ActionItem item = (ActionItem) e.widget.getData();
				item.performAction(AbstractTabAVRDudeBytes.this);
				checkValid();
				updateFields();
			}
		};

		ToolBar toolbar = new ToolBar(parent, SWT.FLAT);

		for (ActionItem item : ActionItem.values()) {
			ToolItem ti = new ToolItem(toolbar, SWT.PUSH);
			ti.setToolTipText(item.tooltipText);
			ti.setData(item);
			ti.addSelectionListener(menuListener);
			Image image = item.image.createImage();
			Image disabledimage = item.disabledImage.createImage();
			ti.setImage(image);
			ti.setDisabledImage(disabledimage);
			fImages.add(image);
			fImages.add(disabledimage);
		}

		return toolbar;
	}

	/**
	 * Create an byte "Editor" composite.
	 * <p>
	 * The editor consists of a Text control to enter the byte value and a Label below it with the
	 * name of the byte.
	 * </p>
	 * <p>
	 * The Text control will only accept (up to) 2 hex digits (converted to uppercase). The value is
	 * stored as an <code>int</code> in the target properties, with <code>-1</code> representing an
	 * empty value.
	 * </p>
	 * <p>
	 * The Editor uses a <code>FillLayout</code> to pack both elements tightly. It is up to the
	 * caller to set the LayoutData for this composite
	 * </p>
	 * <p>
	 * This method saves a reference to the Text control and the Label control in the
	 * {@link #fValueTexts} respectively {@link #fFuseLabels} arrays for access to them outside of
	 * this method.
	 * </p>
	 * 
	 * @param parent
	 *            Parent <code>Composite</code>
	 * @param index
	 *            The byte index for the editor.
	 * 
	 * @return <code>Composite</code> with the editor.
	 */
	private Composite makeByteEditComposite(Composite parent, int index) {

		// TODO: maybe more elegant if coded as a separate class extending composite. That way we
		// could also get rid of the three ugly arrays with references to the hex editor components.

		FillLayout layout = new FillLayout(SWT.VERTICAL);

		Composite compo = new Composite(parent, SWT.NONE);
		compo.setLayout(layout);
		fByteCompos[index] = compo;

		// Add the Text control
		Text text = new Text(compo, SWT.BORDER | SWT.CENTER);
		text.setTextLimit(2);
		text.setSize(10, 20);
		text.setData(Integer.valueOf(index));

		// Add a modification listener to set the fuse byte
		text.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				// Get the byte number
				Text source = (Text) e.widget;
				int index = (Integer) source.getData();

				// Get the new value...
				int newvalue;
				if (source.getText().length() > 0) {
					newvalue = Integer.parseInt(source.getText(), 16);
				} else {
					// Text control is empty. Use the default
					newvalue = -1;
				}

				// ... and set the property (if the source text control is enabled)
				// The check is necessary because this event handler is
				// also called when the #setText(String) method of this Text control
				// is called, even when the control is disabled.
				// The check prevents unnecessary updates of the previews.
				if (source.isEnabled()) {
					fBytes.setValue(index, newvalue);
					updateAVRDudePreview(fTargetProps);
					fPreviewControl.setByteValues(fBytes.getByteValues());
				}
			}

		});

		// Add a verify listener to only accept hex digits and convert them to
		// upper case
		text.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				String text = event.text.toUpperCase();
				if (!text.matches("[0-9A-F]*")) {
					event.doit = false;
				}
				event.text = text;
			}
		});

		// Add a focus listener to select the complete text when the control gets the focus
		text.addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				Text source = (Text) e.widget;
				source.selectAll();
			}

		});

		// Store a reference to the Text control, so that the updateData()
		// method can update the content when required.
		fValueTexts[index] = text;

		// Add the label
		Label fuselabel = new Label(compo, SWT.CENTER);
		fuselabel.setText(Integer.toString(index));
		fuselabel.setSize(10, 0);
		fFuseLabels[index] = fuselabel;

		return compo;
	}

	/**
	 * Enable / Disable the file selector Controls
	 * 
	 * @param enabled
	 *            <code>true</code> to enable, <code>false</code> to disable.
	 */
	private void enableFileGroup(boolean enabled) {
		fFileText.setEnabled(enabled);
		fWorkplaceButton.setEnabled(enabled);
		fFilesystemButton.setEnabled(enabled);
		fVariableButton.setEnabled(enabled);
	}

	/**
	 * Enable / Disable the Byte Editor Controls
	 * <p>
	 * When enabling, only those editors are enabled that are actually valid for the current MCU.
	 * </p>
	 * 
	 * @param enabled
	 *            <code>true</code> to enable, <code>false</code> to disable.
	 */
	private void enableByteGroup(boolean enabled) {
		for (int i = 0; i < fByteCompos.length; i++) {
			setEnabled(fByteCompos[i], enabled);
		}
		fActionsToolBar.setEnabled(enabled);
	}

	/**
	 * Check if the MCU from the active ByteValues is compatible with the MCU from the project.
	 * <p>
	 * Three possible results:
	 * <ul>
	 * <li>The MCUs are the same: do nothing</li>
	 * <li>The MCUs are not the same but compatible: Silently convert the ByteValues to the new MCU</li>
	 * <li>The MCUs are not compatible: Show the warning and the convert button</li>
	 * </ul>
	 * </p>
	 */
	private void checkValid() {

		// clear all warnings
		fFileWarningCompo.setVisible(false);
		fHexWarningCompo.setVisible(false);

		String projectmcuid = fTargetProps.getParent().getMCUId();

		if (!fBytes.getWrite()) {
			// No write - no warning
			return;
		}

		if (fBytes.getUseFile()) {
			// Check if the file is valid.
			// If not the FileWarningCompo is activated
			ByteValues filebv = null;
			String message = null;
			try {
				filebv = fBytes.getByteValuesFromFile();
				if (filebv == null) {
					message = "Could not access the file";
				} else if (!filebv.isCompatibleWith(projectmcuid)) {
					String filemcu = AVRMCUidConverter.id2name(filebv.getMCUId());
					String projectmcu = AVRMCUidConverter.id2name(projectmcuid);
					message = MessageFormat.format(
							"File is for an {0} MCU,\nincompatible with {2} MCU [{1}]", filemcu,
							projectmcu, isPerConfig() ? WARN_FROMCONFIG : WARN_FROMPROJECT);
				}

			} catch (CoreException ce) {
				IStatus status = ce.getStatus();
				int code = status.getCode();
				switch (code) {
					case BaseBytesProperties.FILE_EMPTY_FILENAME:
						message = "Empty filename";
						break;
					case BaseBytesProperties.FILE_NOT_FOUND:
						message = "File not found";
						break;
					case BaseBytesProperties.FILE_MCU_PROPERTY_MISSING:
						message = "File has no 'MCU=xxx' property";
						break;
					case BaseBytesProperties.FILE_WRONG_TYPE:
						message = MessageFormat.format("File is not a {0} file", getType()
								.toString());
						break;
					case BaseBytesProperties.FILE_INVALID_FILENAME:
						message = "Invalid filename";
						break;
					default:
						message = "Internal error accessing the file.";
				}
			}

			if (message != null) {
				fFileWarningLabel.setText(message);
				fFileWarningCompo.pack();
				fFileWarningCompo.setVisible(true);
			}
			return;
		}

		// Immediate values are used

		String ourmcuid = fBytes.getMCUId();
		if (projectmcuid.equals(ourmcuid)) {
			// Identical MCUs - hide the warning and do nothing
			return;
		}

		if (fBytes.isCompatibleWith(projectmcuid)) {
			// Compatible - no warning
			// But convert the ByteValues anyway so everything is in sync.
			ByteValues newvalues = convertFusesTo(projectmcuid, fBytes.getByteValues());
			fBytes.setByteValues(newvalues);
			updateFields();
			return;
		}

		// The two MCUs are not compatible.
		// Update the warning composite with the MCU names and show it.
		String valuesmcu = AVRMCUidConverter.id2name(fBytes.getMCUId());
		String projectmcu = AVRMCUidConverter.id2name(projectmcuid);

		String message = MessageFormat.format(WARN_BYTESINCOMPATIBLE, valuesmcu, projectmcu,
				isPerConfig() ? WARN_FROMCONFIG : WARN_FROMPROJECT);

		fWarningLabel.setText(message);
		fConvertButton.setVisible(true);
		fHexWarningCompo.pack();
		fHexWarningCompo.setVisible(true);
	}

	/*
	 * (non-Javadoc)
	 * @see* it.baeyens.avreclipse.ui.propertypages.AbstractAVRPropertyTab#performApply(it.baeyens.avreclipse
	 * .core.preferences.AVRProjectProperties)
	 */
	@Override
	protected void performApply(AVRDudeProperties dstprops) {

		if (fTargetProps == null) {
			// updataData() has not been called and this tab has no (modified)
			// settings yet.
			return;
		}

		// Copy the currently selected values of this tab to the given, fresh
		// Properties.
		// The caller of this method will handle the actual saving

		// Copy the settings from the FuseBytesProperties sub-properties
		BaseBytesProperties src = getByteProps(fTargetProps);
		BaseBytesProperties dst = getByteProps(dstprops);

		dst.setWrite(src.getWrite());
		dst.setUseFile(src.getUseFile());
		dst.setFileName(src.getFileName());
		dst.setValues(src.getValuesFromImmediate());
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.propertypages.AbstractAVRPropertyTab#performDefaults(it.baeyens.avreclipse
	 * .core.preferences.AVRProjectProperties)
	 */
	@Override
	protected void performCopy(AVRDudeProperties srcprops) {

		// Copy the settings from the BaseBytesProperties sub-properties
		BaseBytesProperties src = getByteProps(srcprops);
		BaseBytesProperties dst = getByteProps(fTargetProps);

		dst.setWrite(src.getWrite());
		dst.setUseFile(src.getUseFile());
		dst.setFileName(src.getFileName());
		dst.setValues(src.getValuesFromImmediate());

		updateData(fTargetProps);
	}

	/*
	 * (non-Javadoc)
	 * @see* it.baeyens.avreclipse.ui.propertypages.AbstractAVRPropertyTab#updateData(it.baeyens.avreclipse
	 * .core.preferences.AVRProjectProperties)
	 */
	@Override
	protected void updateData(AVRDudeProperties props) {

		fTargetProps = props;
		fBytes = getByteProps(props);

		// Set the text for the filename
		fFileText.setText(fBytes.getFileName());

		// Check if the values are valid and show a warning (if required)
		checkValid();

		// Update the radio buttons

		// There are three possibilities:
		// a) No upload wanted: Write == false
		// b) Upload from file: Write == true && useFile == true
		// c) Upload from immediate: Write == true && useFile == false
		if (!fBytes.getWrite()) {
			// a) No upload wanted
			fNoUploadButton.setSelection(true);
			// fUploadFileButton.setSelection(false);
			fImmediateButton.setSelection(false);
			enableFileGroup(false);
			enableByteGroup(false);
			fPreviewControl.setByteValues(null);
		} else {
			// write bytes
			fNoUploadButton.setSelection(false);
			if (fBytes.getUseFile()) {
				// b) write bytes - use supplied file
				fUploadFileButton.setSelection(true);
				fImmediateButton.setSelection(false);
				enableFileGroup(true);
				enableByteGroup(false);
			} else {
				// c) write bytes - use immediate bytes
				fUploadFileButton.setSelection(false);
				fImmediateButton.setSelection(true);
				enableFileGroup(false);
				enableByteGroup(true);
			}
		}

		updateFields();

	}

	/**
	 * Update all fields showing fuse byte values.
	 * <p>
	 * These are:
	 * <ul>
	 * <li>The Fuse Byte Editor Text controls</li>
	 * <li>The Fuse Byte Values Preview control</li>
	 * <li>The AVRDude command line preview</li>
	 * </ul>
	 * This method should be called whenever any fuse byte value has changed.
	 * </p>
	 */
	private void updateFields() {

		// Update the Fuse Byte Editor Texts.
		int[] values = fBytes.getValuesFromImmediate();
		int count = getType().getMaxBytes();

		for (int i = 0; i < count; i++) {
			if (i < values.length) {
				String newvalue = "";
				int currvalue = values[i];
				if (0 <= currvalue && currvalue <= 255) {
					newvalue = "00" + Integer.toHexString(currvalue).toUpperCase();
					newvalue = newvalue.substring(newvalue.length() - 2);
				}
				fValueTexts[i].setText(newvalue);
				fFuseLabels[i].setText(getByteEditorLabel(i));
				fByteCompos[i].setVisible(true);
			} else {
				// byte value index > than max. supported by the current Fuse MCU.
				// hide the editor compo
				fByteCompos[i].setVisible(false);
				fValueTexts[i].setText("");
			}
		}

		// Update the Fuses Preview.
		// If the "no write" flag is set, the preview is cleared (set to null)
		if (fBytes.getWrite()) {
			fPreviewControl.setByteValues(fBytes.getByteValues());
		} else {
			fPreviewControl.setByteValues(null);
		}

		// Update the AVRDUDE preview
		updateAVRDudePreview(fTargetProps);
	}

	/**
	 * Convert a ByteValues object to a new MCU, color the fuse bytes preview according to the
	 * conversion results and print the results to the console.
	 * 
	 * @param targetmcu
	 *            The new MCU value
	 * @param sourcevalues
	 *            The <code>ByteValues</code> to convert
	 * @return A new <code>ByteValues</code> object valid for the targetmcu.
	 */
	private ByteValues convertFusesTo(final String targetmcu, final ByteValues sourcevalues) {

		sourcevalues.setMCUId(targetmcu, true);
		fPreviewControl.setByteValues(sourcevalues);

		ConversionResults results = sourcevalues.getConversionResults();
		if (results != null) {
			MessageConsole console = AVRPlugin.getDefault().getConsole("Fuse Byte Conversion");
			results.printToConsole(console);
		}
		return sourcevalues;
	}

	/**
	 * Load the Bytes from the currently attached MCU.
	 * <p>
	 * This method will start a new Job to load the values and return immediately.
	 * </p>
	 */
	private void readFuseBytesFromDevice() {
		// Disable the Actions Menu. It is re-enabled by the load job when it finishes.
		fActionsToolBar.setEnabled(false);
		fLoadingLabel.setVisible(true);

		// The Job that does the actual loading.
		Job readJob = new Job("Reading Fuse Bytes") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					monitor.beginTask("Starting AVRDude", 100);

					final ByteValues bytevalues = getByteValues(fTargetProps,
							new SubProgressMonitor(monitor, 95));

					// and update the user interface
					if (!fActionsToolBar.isDisposed()) {
						fActionsToolBar.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								// Check if the mcus match
								String projectmcu = fTargetProps.getParent().getMCUId();
								String newmcu = bytevalues.getMCUId();
								if (!bytevalues.isCompatibleWith(projectmcu)) {
									// No, they don't match. Ask the user what to do
									// "Convert to project MCU", "Accept anyway" or "Cancel"
									Dialog dialog = new ProjectMCUMismatchDialog(fActionsToolBar
											.getShell(), newmcu, projectmcu, getType(),
											isPerConfig());
									int choice = dialog.open();
									switch (choice) {
										case ProjectMCUMismatchDialog.CANCEL:
											return;
										case ProjectMCUMismatchDialog.ACCEPT:
											// Accept the values including their MCUId.
											// Set the properties accordingly
											fBytes.setByteValues(bytevalues);
											break;
										case ProjectMCUMismatchDialog.CONVERT:
											// Convert the values to the current project MCU
											ByteValues newvalues = convertFusesTo(projectmcu,
													bytevalues);
											fBytes.setByteValues(newvalues);
											break;
									}
								} else {
									// MCUs are compatible.
									fBytes.setByteValues(bytevalues);
								}
								updateData(fTargetProps);
							}
						});
					}
					monitor.worked(5);
				} catch (AVRDudeException ade) {
					// Show an Error message and exit
					if (!fActionsToolBar.isDisposed()) {
						UIJob messagejob = new AVRDudeErrorDialogJob(fActionsToolBar.getDisplay(),
								ade, fTargetProps.getProgrammerId());
						messagejob.setPriority(Job.INTERACTIVE);
						messagejob.schedule();
						try {
							messagejob.join(); // block until the dialog is closed.
						} catch (InterruptedException e) {
							// Don't care if the dialog is interrupted from outside.
						}
					}
				} catch (SWTException swte) {
					// The display has been disposed, so the user is not
					// interested in the results from this job
					return Status.CANCEL_STATUS;
				} finally {
					monitor.done();
					// Enable the Load from MCU Button
					if (!fActionsToolBar.isDisposed()) {
						fActionsToolBar.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								// Re-Enable the Button
								fActionsToolBar.setEnabled(true);
								fLoadingLabel.setVisible(false);
							}
						});
					}
				}

				return Status.OK_STATUS;
			}
		};

		// now set the Job properties and start it
		readJob.setRule(new AVRDudeSchedulingRule(fTargetProps.getProgrammer()));
		readJob.setPriority(Job.SHORT);
		readJob.setUser(true);
		readJob.schedule();
	}

	// The Images for the Actions ToolBar
	private static final ImageDescriptor	IMG_EN_EDIT			= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/e_edit_fuses.png");
	private static final ImageDescriptor	IMG_DIS_EDIT		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/d_edit_fuses.png");

	private static final ImageDescriptor	IMG_EN_LOADFILE		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/e_copy_fusefile.png");
	private static final ImageDescriptor	IMG_DIS_LOADFILE	= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/d_copy_fusefile.png");

	private static final ImageDescriptor	IMG_EN_READMCU		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/e_read_mcu.png");
	private static final ImageDescriptor	IMG_DIS_READMCU		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/d_read_mcu.png");

	private static final ImageDescriptor	IMG_EN_DEFAULT		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/e_copy_default.png");
	private static final ImageDescriptor	IMG_DIS_DEFAULT		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/d_copy_default.png");

	private static final ImageDescriptor	IMG_EN_ALLONES		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/e_0xff.png");
	private static final ImageDescriptor	IMG_DIS_ALLONES		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/d_0xff.png");

	private static final ImageDescriptor	IMG_EN_ALLZEROS		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/e_0x00.png");
	private static final ImageDescriptor	IMG_DIS_ALLZEROS	= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/d_0x00.png");

	private static final ImageDescriptor	IMG_EN_CLEAR		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/e_clear_bytes.png");
	private static final ImageDescriptor	IMG_DIS_CLEAR		= AVRUIPlugin
																		.getImageDescriptor("icons/objs16/d_clear_bytes.png");

	/**
	 * Enumeration of all Actions for the hex editor actions Toolbar.
	 */
	private enum ActionItem {
		// Edit Action
		EDIT(MENU_EDIT, IMG_EN_EDIT, IMG_DIS_EDIT) {
			@Override
			public void performAction(AbstractTabAVRDudeBytes tab) {
				ByteValuesEditorDialog dialog = new ByteValuesEditorDialog(tab.fActionsToolBar
						.getShell(), tab.fBytes.getByteValues());
				dialog.create();
				// dialog.getShell().setSize(100, 100);
				dialog.optimizeSize();
				int result = dialog.open();
				if (result == Dialog.OK) {
					ByteValues newvalues = dialog.getResult();
					tab.fBytes.setByteValues(newvalues);
				}

			}
		},

		// Copy from file Action
		COPY(MENU_COPYFILE, IMG_EN_LOADFILE, IMG_DIS_LOADFILE) {
			@Override
			public void performAction(AbstractTabAVRDudeBytes tab) {
				tab.fBytes.syncFromFile();
			}
		},
		// Read from MCU Action
		READ(MENU_READDEVICE, IMG_EN_READMCU, IMG_DIS_READMCU) {
			@Override
			public void performAction(AbstractTabAVRDudeBytes tab) {
				tab.readFuseBytesFromDevice();
			}
		},
		// Set to default values action
		DEFAULTS(MENU_DEFAULTS, IMG_EN_DEFAULT, IMG_DIS_DEFAULT) {
			@Override
			public void performAction(AbstractTabAVRDudeBytes tab) {
				tab.fBytes.setDefaultValues();
			}
		},
		// Set all bytes to 0xff
		ALL_1(MENU_ALLONES, IMG_EN_ALLONES, IMG_DIS_ALLONES) {
			@Override
			public void performAction(AbstractTabAVRDudeBytes tab) {
				setBytes(tab.fBytes, 0xff);
			}
		},
		// Set all bytes to 0x00
		ALL_0(MENU_ALLZEROS, IMG_EN_ALLZEROS, IMG_DIS_ALLZEROS) {
			@Override
			public void performAction(AbstractTabAVRDudeBytes tab) {
				setBytes(tab.fBytes, 0x00);
			}
		},
		// Set all bytes to -1
		CLEAR(MENU_CLEARALL, IMG_EN_CLEAR, IMG_DIS_CLEAR) {
			@Override
			public void performAction(AbstractTabAVRDudeBytes tab) {
				tab.fBytes.clearValues();
			}
		};

		public final String				tooltipText;
		public final ImageDescriptor	image;
		public final ImageDescriptor	disabledImage;

		private ActionItem(String text, ImageDescriptor image, ImageDescriptor disabledImage) {
			this.tooltipText = text;
			this.image = image;
			this.disabledImage = disabledImage;
		}

		/**
		 * Perform the action associated with this item.
		 * 
		 * @param tab
		 *            A reference to the parent object to get access to its internl values.
		 */
		public abstract void performAction(AbstractTabAVRDudeBytes tab);

		/**
		 * Convenience method to set all Bytes of a <code>ByteValues</code> object to the same
		 * value.
		 * 
		 * @param bytevalues
		 *            <code>ByteValues</code> object
		 * @param value
		 *            The new value for all bytes
		 */
		private static void setBytes(BaseBytesProperties bytevalues, int value) {
			int count = bytevalues.getValues().length;
			for (int i = 0; i < count; i++) {
				bytevalues.setValue(i, value);
			}
		}

	}

}
