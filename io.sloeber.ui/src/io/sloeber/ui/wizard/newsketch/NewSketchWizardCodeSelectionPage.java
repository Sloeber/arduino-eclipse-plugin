package io.sloeber.ui.wizard.newsketch;

import java.io.File;
import java.util.ArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CodeDescriptor.CodeTypes;
import io.sloeber.ui.LabelCombo;
import io.sloeber.ui.Messages;

public class NewSketchWizardCodeSelectionPage extends WizardPage {

	final Shell shell = new Shell();
	Composite mParentComposite = null;

	protected LabelCombo mCodeSourceOptionsCombo; // ComboBox Containing all the
	// sketch creation options

	protected DirectoryFieldEditor mTemplateFolderEditor;
	protected SampleSelector mExampleEditor = null;
	protected Button mCheckBoxUseCurrentLinkSample;
	private BoardDescriptor myBoardDescriptor = null;
	private CodeDescriptor myCodedescriptor = CodeDescriptor.createLastUsed();

	public void setBoardDescriptor(BoardDescriptor boardDescriptor) {
		if (this.myBoardDescriptor == null) {
			this.myBoardDescriptor = boardDescriptor;
			boardDescriptor.addChangeListener(new ChangeListener() {

				@Override
				public void stateChanged(ChangeEvent e) {
					handleBoarDescriptorChange();
				}
			});
		}
		handleBoarDescriptorChange();
	}

	public void handleBoarDescriptorChange() {

		if (this.mExampleEditor != null) {
			if (!this.myBoardDescriptor.getBoardsFile().isEmpty()) {
				this.mExampleEditor.AddAllExamples(this.myBoardDescriptor, this.myCodedescriptor.getLastUsedExamples());
			}
		}

		validatePage();
	}

	public NewSketchWizardCodeSelectionPage(String pageName) {
		super(pageName);
		setPageComplete(true);
	}

	public NewSketchWizardCodeSelectionPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setPageComplete(true);
	}

	@Override
	public void createControl(Composite parent) {

		Composite composite = new Composite(parent, SWT.NULL);
		this.mParentComposite = composite;

		GridLayout theGridLayout = new GridLayout();
		theGridLayout.numColumns = 4;
		composite.setLayout(theGridLayout);

		Listener comboListener = new Listener() {

			@Override
			public void handleEvent(Event event) {
				SetControls();
				validatePage();

			}
		};
		this.mCodeSourceOptionsCombo = new LabelCombo(composite, Messages.ui_new_sketch_selecy_code, null, 4, true);
		this.mCodeSourceOptionsCombo.addListener(comboListener);

		this.mCodeSourceOptionsCombo.setItems(getCodeTypeDescriptions());

		this.mTemplateFolderEditor = new DirectoryFieldEditor("temp1", Messages.ui_new_sketch_custom_template_location, //$NON-NLS-1$
				composite);

		this.mExampleEditor = new SampleSelector(composite, SWT.FILL, Messages.ui_new_sketch_select_example_code, 4);
		this.mExampleEditor.addchangeListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				validatePage();

			}

		});

		this.mTemplateFolderEditor.getTextControl(composite).addListener(SWT.Modify, comboListener);

		this.mCheckBoxUseCurrentLinkSample = new Button(composite, SWT.CHECK);
		this.mCheckBoxUseCurrentLinkSample.setText(Messages.ui_new_sketch_link_to_sample_code);

		//
		// End of special controls
		//

		restoreAllSelections();// load the default settings
		SetControls();// set the controls according to the setting

		validatePage();// validate the page
		handleBoarDescriptorChange();

		setControl(composite);

	}

	/**
	 * @name SetControls() Enables or disables the controls based on the
	 *       Checkbox settings
	 */
	protected void SetControls() {
		switch (CodeTypes.values()[this.mCodeSourceOptionsCombo.mCombo.getSelectionIndex()]) {
		case defaultIno:
			this.mTemplateFolderEditor.setEnabled(false, this.mParentComposite);
			this.mExampleEditor.setEnabled(false);
			this.mCheckBoxUseCurrentLinkSample.setEnabled(false);
			break;
		case defaultCPP:
			this.mTemplateFolderEditor.setEnabled(false, this.mParentComposite);
			this.mExampleEditor.setEnabled(false);
			this.mCheckBoxUseCurrentLinkSample.setEnabled(false);
			break;
		case CustomTemplate:
			this.mTemplateFolderEditor.setEnabled(true, this.mParentComposite);
			this.mExampleEditor.setEnabled(false);
			this.mCheckBoxUseCurrentLinkSample.setEnabled(false);
			break;
		case sample:
			this.mTemplateFolderEditor.setEnabled(false, this.mParentComposite);
			this.mExampleEditor.setEnabled(true);
			this.mCheckBoxUseCurrentLinkSample.setEnabled(true);
			break;
		default:
			break;
		}
	}

	/**
	 * @name validatePage() Check if the user has provided all the info to
	 *       create the project. If so enable the finish button.
	 */
	protected void validatePage() {
		if (this.mCodeSourceOptionsCombo == null) {
			return;
		}
		switch (CodeTypes.values()[this.mCodeSourceOptionsCombo.mCombo.getSelectionIndex()]) {
		case defaultIno:
		case defaultCPP:
			setPageComplete(true);// default always works
			break;
		case CustomTemplate:
			IPath templateFolder = new Path(this.mTemplateFolderEditor.getStringValue());
			File cppFile = templateFolder.append("sketch.cpp").toFile(); //$NON-NLS-1$
			File headerFile = templateFolder.append("sketch.h").toFile(); //$NON-NLS-1$
			File inoFile = templateFolder.append("sketch.ino").toFile(); //$NON-NLS-1$
			boolean existFile = inoFile.isFile() || (cppFile.isFile() && headerFile.isFile());
			setPageComplete(existFile);
			break;
		case sample:
			setPageComplete(this.mExampleEditor.isSampleSelected());
			break;
		default:
			setPageComplete(false);
			break;
		}
	}

	/**
	 * @name restoreAllSelections() Restore all necessary variables into the
	 *       respective controls
	 */
	private void restoreAllSelections() {
		//
		// get the settings for the Use Default checkbox and foldername from the
		// environment settings
		// settings are saved when the files are created and the use this as
		// default flag is set
		//
		this.mTemplateFolderEditor.setStringValue(this.myCodedescriptor.getTemPlateFoldername().toString());
		this.mCodeSourceOptionsCombo.mCombo.select(this.myCodedescriptor.getCodeType().ordinal());
	}

	public CodeDescriptor getCodeDescription() {

		switch (CodeTypes.values()[this.mCodeSourceOptionsCombo.mCombo.getSelectionIndex()]) {
		case defaultIno:
			return CodeDescriptor.createDefaultIno();
		case defaultCPP:
			return CodeDescriptor.createDefaultCPP();
		case CustomTemplate:
			return CodeDescriptor.createCustomTemplate(new Path(this.mTemplateFolderEditor.getStringValue()));
		case sample:
			ArrayList<Path> sampleFolders = this.mExampleEditor.GetSampleFolders();
			boolean link = this.mCheckBoxUseCurrentLinkSample.getSelection();
			return CodeDescriptor.createSample(link, sampleFolders);
		}
		// make sure this never happens
		return null;
	}

	public static String getCodeTypeDescription(CodeTypes codeType) {
		switch (codeType) {
		case defaultIno:
			return Messages.ui_new_sketch_default_ino;
		case defaultCPP:
			return Messages.ui_new_sketch_default_cpp;
		case CustomTemplate:
			return Messages.ui_new_sketch_custom_template;
		case sample:
			return Messages.ui_new_sketch_sample_sketch;
		}
		return null;
	}

	public static String[] getCodeTypeDescriptions() {
		String[] ret = new String[CodeTypes.values().length];
		for (CodeTypes codeType : CodeTypes.values()) {
			ret[codeType.ordinal()] = getCodeTypeDescription(codeType);
		}
		return ret;
	}

}
