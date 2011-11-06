/*******************************************************************************
 * 
 * Copyright (c) 2009, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: SectionAvrdude.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration;
import it.baeyens.avreclipse.core.targets.ITargetConfigurationTool;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration.ValidationResult;
import it.baeyens.avreclipse.core.targets.tools.AvrdudeTool;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


/**
 * @author Thomas Holland
 * @since
 * 
 */
public class SectionAvrdude extends AbstractTCSectionPart {

	private Text					fCommandText;

	private Label					fVersionLabel;

	private Combo					fVerbosityCombo;

	private final static String[]	VERBOSITY_NAMES	= new String[] { "normal", "verbose",
			"very verbose", "extremly verbose"		};

	private final static String[]	PART_ATTRS		= new String[] { AvrdudeTool.ATTR_CMD_NAME,
			AvrdudeTool.ATTR_USE_CONSOLE, AvrdudeTool.ATTR_VERBOSITY };
	private final static String[]	PART_DEPENDS	= new String[] {};

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getTitle()
	 */
	@Override
	protected String getTitle() {
		return "AVRDude Settings";
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getDescription()
	 */
	@Override
	protected String getDescription() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getPartAttributes
	 * ()
	 */
	@Override
	public String[] getPartAttributes() {
		return PART_ATTRS;
	}

	/*
	 * (non-Javadoc)
	 * @seeit.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#
	 * getDependentAttributes()
	 */
	@Override
	protected String[] getDependentAttributes() {
		return PART_DEPENDS;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.ui.editors.targets.AbstractTCSectionPart#createSectionContent(org.eclipse
	 * .swt.widgets.Composite, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	@Override
	protected void createSectionContent(Composite parent, FormToolkit toolkit) {
		// Call the subclass to have it create its user interface stuff.
		TableWrapLayout layout = new TableWrapLayout();
		layout.horizontalSpacing = 12;
		layout.numColumns = 3;
		parent.setLayout(layout);

		addCommandText(parent, toolkit);

		addVerbosityCombo(parent, toolkit);

		addAdvancedSection(parent, toolkit);
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTCSectionPart#refreshSectionContent()
	 */
	@Override
	protected void refreshSectionContent() {
		String command = getTargetConfiguration().getAttribute(AvrdudeTool.ATTR_CMD_NAME);
		fCommandText.setText(command);
		validateCommand();

		int verbosity = getTargetConfiguration().getIntegerAttribute(AvrdudeTool.ATTR_VERBOSITY);
		fVerbosityCombo.select(verbosity);

	}

	/**
	 * Add the port name settings controls to the parent.
	 * <p>
	 * This consists of a Label and a Text control. After creation the control is set to the current
	 * port name.
	 * </p>
	 * <p>
	 * The parent is expected to have a <code>TableWrapLayout</code> with 3 columns.
	 * </p>
	 * 
	 * @param parent
	 *            Composite to which the port name settings controls are added.
	 * @param toolkit
	 *            FormToolkit to use for the new controls.
	 */
	private void addCommandText(Composite parent, FormToolkit toolkit) {

		//
		// The Label
		//
		Label label = toolkit.createLabel(parent, "Command:");
		label.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));

		// 
		// The Text control
		//
		fCommandText = toolkit.createText(parent, "");
		fCommandText
				.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.MIDDLE));
		fCommandText
				.setToolTipText("The command to start avrdude.\nIf the avrdude executable is not on the system path,\nthen the full path to the executable must be provided.");

		fCommandText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String command = fCommandText.getText();
				getTargetConfiguration().setAttribute(AvrdudeTool.ATTR_CMD_NAME, command);
				getManagedForm().dirtyStateChanged();

				validateCommand();
			}
		});

		// 
		// Browse file system button
		//
		Button filesystembutton = createFilesystemButton(toolkit, parent, fCommandText,
				new String[] { "", "exe" });
		filesystembutton.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));

		// Placeholder to "eat" the column
		Label dummy = toolkit.createLabel(parent, "");
		dummy.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.TOP));

		//
		// The Label for the discovered version
		//
		fVersionLabel = toolkit.createLabel(parent, "avrdude version");
		fVersionLabel.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP,
				1, 2));

	}

	/**
	 * Add the verbosity settings controls to the parent.
	 * <p>
	 * This consists of a Label and a Combo control. The combo has 5 levels from 'very quiet' to
	 * 'very verbose'. After creation the control is set to the current verbosity level.
	 * </p>
	 * <p>
	 * The parent is expected to have a <code>GridLayout</code> with 2 columns.
	 * </p>
	 * 
	 * @param parent
	 *            Composite to which the port name settings controls are added.
	 * @param toolkit
	 *            FormToolkit to use for the new controls.
	 */
	private void addVerbosityCombo(Composite parent, FormToolkit toolkit) {
		//
		// The Label
		//
		Label label = toolkit.createLabel(parent, "Verbosity level:");
		label.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE));

		// 
		// The Text control
		//
		fVerbosityCombo = new Combo(parent, SWT.READ_ONLY);
		toolkit.adapt(fVerbosityCombo);
		fVerbosityCombo.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.MIDDLE,
				1, 2));

		fVerbosityCombo.setItems(VERBOSITY_NAMES);
		fVerbosityCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				int newindex = fVerbosityCombo.getSelectionIndex();
				getTargetConfiguration().setIntegerAttribute(AvrdudeTool.ATTR_VERBOSITY, newindex);
				getManagedForm().dirtyStateChanged();
			}
		});

	}

	/**
	 * @param parent
	 * @param toolkit
	 */
	private void addAdvancedSection(Composite parent, FormToolkit toolkit) {
		Section section = toolkit.createSection(parent, Section.SHORT_TITLE_BAR | Section.TWISTIE
				| Section.COMPACT);
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP, 1, 3));
		section.setText("Advanced AVRDude settings");

	}

	/**
	 * Open a FileSystem Dialog and return the selected file as a <code>String</code>.
	 * 
	 * @param shell
	 *            Shell in which to open the Dialog
	 * @param text
	 *            Root file name
	 * @param exts
	 *            <code>String[]</code> with all valid file extensions. Files with other extensions
	 *            will be filtered.
	 * @return <code>String</code> with the selected filename or <cod>null</code> if the user has
	 *         canceled or an error occurred.
	 */
	public static String getFileSystemFileDialog(Shell shell, String text, String[] exts) {

		FileDialog dialog = new FileDialog(shell);
		dialog.setFilterPath(text);
		dialog.setFilterExtensions(exts);
		dialog.setText("");
		return dialog.open();
	}

	/**
	 * Create and return a "Filesystem" browse Button.
	 * <p>
	 * Clicking the Button will open a file selector Dialog and the result is copied to the supplied
	 * <code>Text</code> Control.
	 * </p>
	 * 
	 * @param toolkit
	 *            The form toolkit to use for creating the button.
	 * @param parent
	 *            Parent <code>Composite</code>, which needs to have <code>GridLayout</code>
	 * @param text
	 *            Target <code>Text</code> Control
	 * @param exts
	 *            <code>String[]</code> with all valid file extensions. Files with other extensions
	 *            will be filtered.
	 * @return <code>Button</code> Control with the created Button.
	 */
	protected Button createFilesystemButton(FormToolkit toolkit, Composite parent, final Text text,
			String[] exts) {
		Button button = toolkit.createButton(parent, "Browse...", SWT.PUSH);
		button.setData(exts);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String[] exts = (String[]) event.widget.getData();
				String lastlocation = text.getText();
				IPath lastlocpath = new Path(lastlocation);
				String location = getFileSystemFileDialog(text.getShell(),
						lastlocpath.isAbsolute() ? lastlocation : Path.ROOT.toOSString(), exts);
				if (location != null) {
					text.setText(location);
				}
			}
		});
		return button;
	}

	/**
	 * Create and return a "Search" Button.
	 * <p>
	 * Clicking the Button will search the file system for avrdude and the result is copied to the
	 * supplied <code>Text</code> Control.
	 * </p>
	 * 
	 * @param toolkit
	 *            The form toolkit to use for creating the button.
	 * @param parent
	 *            Parent <code>Composite</code>, which needs to have <code>GridLayout</code>
	 * @param text
	 *            Target <code>Text</code> Control
	 * @return <code>Button</code> Control with the created Button.
	 */
	protected Button createSearchButton(FormToolkit toolkit, Composite parent, final Text text) {
		Button button = toolkit.createButton(parent, "Search", SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// TODO: Implement search for avrdude
			}
		});
		return button;
	}

	/**
	 * Check if the given command name is valid by fetching its version.
	 */
	private void validateCommand() {

		validate(AvrdudeTool.ATTR_CMD_NAME, fCommandText, fCVL);
	}

	private final CommandValidationListener	fCVL	= new SectionAvrdude.CommandValidationListener();

	private class CommandValidationListener implements IValidationListener {
		@Override
		public void result(ValidationResult result) {
			switch (result.result) {
				case OK:
					try {
						ITargetConfiguration tc = getTargetConfiguration();
						ITargetConfigurationTool tool = tc.getProgrammerTool();
						String version = tool.getVersion();
						fVersionLabel.setText(" Version: " + version);
					} catch (AVRDudeException e) {
						// This should not happen because the command has been validated 'OK'
						fVersionLabel.setText("");
					}
					break;
				default:
					fVersionLabel.setText("");
			}
		}
	}
}
