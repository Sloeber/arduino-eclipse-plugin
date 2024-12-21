package io.sloeber.ui.wizard.newsketch;

import static io.sloeber.ui.Activator.*;

import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;

/**
 * This class is the class related to the new arduino sketch
 *
 * @author Jan Baeyens
 *
 */
public class NewSketchWizard extends Wizard implements INewWizard, IExecutableExtension {
	private WizardNewProjectCreationPage mWizardPage = new WizardNewProjectCreationPage(Messages.ui_new_sketch_title);
	protected NewSketchWizardBoardPage mArduinoPage = new NewSketchWizardBoardPage(
			Messages.ui_new_sketch_arduino_information);
	protected NewSketchWizardCodeSelectionPage mNewArduinoSketchWizardCodeSelectionPage = new NewSketchWizardCodeSelectionPage(
			Messages.ui_new_sketch_sketch_template_location);
	private NewProjectSourceLocationPage mySourceLocationPage= new NewProjectSourceLocationPage("code location page"); //$NON-NLS-1$
	private IConfigurationElement mConfig;
	private IProject mProject;

	public NewSketchWizard() {
		super();
	}

	@Override
	/**
	 * adds pages to the wizard. We are using the standard project wizard of
	 * Eclipse
	 */
	public void addPages() {
		//
		// We assume everything is OK as it is tested in the handler
		// create each page and fill in the title and description
		// first page to fill in the project name
		//
		this.mWizardPage.setDescription(Messages.ui_new_sketch_title_help);
		this.mWizardPage.setTitle(Messages.ui_new_sketch_title); // $NON-NLS-1$
		//
		// settings for Arduino board etc
		//
		this.mArduinoPage.setTitle(Messages.ui_new_sketch_arduino_information_help);
		this.mArduinoPage.setDescription(Messages.ui_new_sketch_these_settings_cn_be_changed_later);
		//
		// settings for template file location
		//
		mNewArduinoSketchWizardCodeSelectionPage.setTitle(Messages.ui_new_sketch_sketch_template_folder);
		mNewArduinoSketchWizardCodeSelectionPage
				.setDescription(Messages.ui_new_sketch_error_folder_must_contain_sketch_cpp);
		mNewArduinoSketchWizardCodeSelectionPage.setSketchWizardPage(mArduinoPage);



		//
		// actually add the pages to the wizard
		//
		addPage(this.mWizardPage);
		addPage(this.mArduinoPage);
		addPage(this.mNewArduinoSketchWizardCodeSelectionPage);
		addPage(mySourceLocationPage);
	}

	@Override
	public boolean performFinish() {
		if (this.mProject != null) {
			return true;
		}
		BoardDescription boardDescription = this.mArduinoPage.getBoardDescriptor();
		CodeDescription codeDescription = this.mNewArduinoSketchWizardCodeSelectionPage.getCodeDescription();
		codeDescription.setCodeFolder(mySourceLocationPage.getSourceCodeLocation());
		CompileDescription compileDescription = new CompileDescription();
		URI locationURI = (!this.mWizardPage.useDefaults()) ? this.mWizardPage.getLocationURI() : null;
		compileDescription.setEnableParallelBuild(MyPreferences.getEnableParallelBuildForNewProjects());
		boardDescription.saveUserSelection();
		this.mProject = SloeberProject.createArduinoProject(this.mWizardPage.getProjectName(),
				locationURI, boardDescription, codeDescription, compileDescription, new NullProgressMonitor());

		if (this.mProject == null) {
			log(new Status(IStatus.ERROR, PLUGIN_ID,
					Messages.ui_new_sketch_error_failed_to_create_project));
			return false;
		}


		// so now we set Eclipse to the right perspective and switch to our just
		// created project

		BasicNewProjectResourceWizard.updatePerspective(this.mConfig);
		IWorkbenchWindow TheWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		BasicNewResourceWizard.selectAndReveal(this.mProject, TheWindow);

		return true;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// nothing to do here
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		this.mConfig = config;

	}

}
