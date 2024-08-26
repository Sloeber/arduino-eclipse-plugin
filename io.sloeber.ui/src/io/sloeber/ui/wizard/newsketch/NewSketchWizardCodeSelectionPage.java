package io.sloeber.ui.wizard.newsketch;

import java.io.File;
import java.util.Set;

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

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.arduinoFramework.api.IExample;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CodeDescription.CodeTypes;
import io.sloeber.ui.LabelCombo;
import io.sloeber.ui.Messages;

public class NewSketchWizardCodeSelectionPage extends WizardPage {

//	final Shell shell = new Shell();
	private Composite myParentComposite = null;
	protected LabelCombo myCodeSourceOptionsCombo;
	protected DirectoryFieldEditor myTemplateFolderEditor;
	protected SampleSelector myExampleEditor = null;
	protected Button myCheckBoxUseCurrentLinkSample;
	private BoardDescription myCurrentBoardDesc = null;
	private CodeDescription myCodedescriptor = null;
	private NewSketchWizardBoardPage myArduinoPage;

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
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
		myParentComposite = composite;

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
		myCodeSourceOptionsCombo = new LabelCombo(composite, Messages.ui_new_sketch_selecy_code, 4, true);
		myCodeSourceOptionsCombo.addListener(comboListener);

		myCodeSourceOptionsCombo.setItems(getCodeTypeDescriptions());

		myTemplateFolderEditor = new DirectoryFieldEditor("temp1", Messages.ui_new_sketch_custom_template_location, //$NON-NLS-1$
				composite);

		myExampleEditor = new SampleSelector(composite, SWT.FILL, Messages.ui_new_sketch_select_example_code, 4);
		myExampleEditor.addchangeListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				validatePage();

			}

		});

		myTemplateFolderEditor.getTextControl(composite).addListener(SWT.Modify, comboListener);

		myCheckBoxUseCurrentLinkSample = new Button(composite, SWT.CHECK);
		myCheckBoxUseCurrentLinkSample.setText(Messages.ui_new_sketch_link_to_sample_code);

		//
		// End of special controls
		//

		restoreAllSelections();// load the default settings
		SetControls();// set the controls according to the setting

		validatePage();// validate the page
		setControl(composite);

	}

	/**
	 * @name SetControls() Enables or disables the controls based on the Checkbox
	 *       settings
	 */
	protected void SetControls() {
		switch (getCodeType()) {
		case None:
			myTemplateFolderEditor.setEnabled(false, myParentComposite);
			myExampleEditor.setEnabled(false);
			myCheckBoxUseCurrentLinkSample.setEnabled(false);
			break;
		case defaultIno:
			myTemplateFolderEditor.setEnabled(false, myParentComposite);
			myExampleEditor.setEnabled(false);
			myCheckBoxUseCurrentLinkSample.setEnabled(false);
			break;
		case defaultCPP:
			myTemplateFolderEditor.setEnabled(false, myParentComposite);
			myExampleEditor.setEnabled(false);
			myCheckBoxUseCurrentLinkSample.setEnabled(false);
			break;
		case CustomTemplate:
			myTemplateFolderEditor.setEnabled(true, myParentComposite);
			myExampleEditor.setEnabled(false);
			myCheckBoxUseCurrentLinkSample.setEnabled(false);
			break;
		case sample:
			myTemplateFolderEditor.setEnabled(false, myParentComposite);
			myExampleEditor.setEnabled(true);
			myCheckBoxUseCurrentLinkSample.setEnabled(true);
			break;
		default:
			break;
		}
	}

	/**
	 * @name validatePage() Check if the user has provided all the info to create
	 *       the project. If so enable the finish button.
	 */
	protected void validatePage() {

		switch (getCodeType()) {
		case None:
		case defaultIno:
		case defaultCPP:
			setPageComplete(true);// default and no file always works
			break;
		case CustomTemplate:
			IPath templateFolder = new Path(myTemplateFolderEditor.getStringValue());
			File cppFile = templateFolder.append(CodeDescription.DEFAULT_SKETCH_CPP).toFile();
			File headerFile = templateFolder.append(CodeDescription.DEFAULT_SKETCH_H).toFile();
			File inoFile = templateFolder.append(CodeDescription.DEFAULT_SKETCH_INO).toFile();
			boolean existFile = inoFile.isFile() || (cppFile.isFile() && headerFile.isFile());
			setPageComplete(existFile);
			break;
		case sample:
			BoardDescription mySelectedBoardDesc = myArduinoPage.getBoardDescriptor();
			if (!mySelectedBoardDesc.equals(myCurrentBoardDesc)) {
				myCurrentBoardDesc = new BoardDescription(mySelectedBoardDesc);
				myCodedescriptor=null;
				myExampleEditor.AddAllExamples(myCurrentBoardDesc, getCodeDescr().getExamples());

			}
			setPageComplete(myExampleEditor.isSampleSelected());
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
		myTemplateFolderEditor.setStringValue(getCodeDescr().getTemPlateFoldername().toString());
		myCodeSourceOptionsCombo.select(getCodeDescr().getCodeType().ordinal());
	}

	public CodeDescription getCodeDescription() {

		switch (getCodeType()) {
		case None:
			return CodeDescription.createNone();
		case defaultIno:
			return CodeDescription.createDefaultIno();
		case defaultCPP:
			return CodeDescription.createDefaultCPP();
		case CustomTemplate:
			return CodeDescription.createCustomTemplate(new Path(myTemplateFolderEditor.getStringValue()));
		case sample:
			Set<IExample> sampleFolders = myExampleEditor.GetSampleFolders();
			boolean link = myCheckBoxUseCurrentLinkSample.getSelection();
			return CodeDescription.createExample(link, sampleFolders);
		default:
			break;
		}
		// make sure this never happens
		return null;
	}

	public static String getCodeTypeDescription(CodeTypes codeType) {
		switch (codeType) {
		case None:
			return Messages.ui_new_sketch_none;
		case defaultIno:
			return Messages.ui_new_sketch_default_ino;
		case defaultCPP:
			return Messages.ui_new_sketch_default_cpp;
		case CustomTemplate:
			return Messages.ui_new_sketch_custom_template;
		case sample:
			return Messages.ui_new_sketch_sample_sketch;
		default:
			break;
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

	private CodeTypes getCodeType() {
		if (myCodeSourceOptionsCombo == null) {
			return CodeTypes.None;
		}
		return CodeTypes.values()[Math.max(0, myCodeSourceOptionsCombo.getSelectionIndex())];
	}

	public void setSketchWizardPage(NewSketchWizardBoardPage arduinoPage) {
		myArduinoPage = arduinoPage;

	}

	private CodeDescription getCodeDescr() {
		if(myCodedescriptor==null) {
			myCodedescriptor=CodeDescription.createLastUsed(myArduinoPage.getBoardDescriptor());
		}
		return myCodedescriptor;
	}

}
