package io.sloeber.autoBuild.integrations;

import static io.sloeber.autoBuild.ui.internal.Messages.*;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tools.templates.core.IGenerator;
import org.eclipse.tools.templates.ui.TemplateWizard;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.osgi.framework.FrameworkUtil;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.integration.AutoBuildProjectGenerator;
import io.sloeber.autoBuild.ui.internal.Messages;
import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.schema.api.IProjectType;

public class NewProjectWizard extends TemplateWizard {
    private WizardNewProjectCreationPage myMainPage;
    private NewProjectNaturePage myNaturePage;
    private NewProjectBuildToolsPage myBuildToolsPage;
    private NewProjectProjectTypePage myProjectTypePage;
    private NewProjectSourceLocationPage mySourceCodePage;

    @Override
    public boolean performFinish() {
        try {
            String projectName = myMainPage.getProjectName();
            IProjectType projectType = myProjectTypePage.getProjectType();
            String natureID = myNaturePage.getNatureID();
            ICodeProvider codeProvider = null;
            IBuildTools buildTools= myBuildToolsPage.getBuildTools();
            String codeRootFolder=mySourceCodePage.getSourceCodeLocation();
            getContainer().run(true, true, new WorkspaceModifyOperation() {
                @Override
                protected void execute(IProgressMonitor monitor)
                        throws CoreException, InvocationTargetException, InterruptedException {
                    SubMonitor sub = SubMonitor.convert(monitor, Messages.TemplateWizard_Generating, 1);

                    AutoBuildProject.createProject(projectName, projectType, natureID,
                    		codeRootFolder,codeProvider, buildTools, false, sub);
                    //                    generator.generate(model, sub);
                    //                    getWorkbench().getDisplay().asyncExec(new Runnable() {
                    //                        @Override
                    //                        public void run() {
                    //                            postProcess(generator);
                    //                        }
                    //                    });
                    sub.done();
                }

                @Override
                public ISchedulingRule getRule() {
                    return ResourcesPlugin.getWorkspace().getRoot();
                }
            });
        } catch (Exception e) {
            handle(e);
            return false;
        }
        return true;
    }

    @Override
    public void setContainer(IWizardContainer wizardContainer) {
        super.setContainer(wizardContainer);
        setWindowTitle(NewAutoMakeProjectWizard_WindowTitle);
    }

    @Override
    public void addPages() {
        myMainPage = new WizardNewProjectCreationPage("basicNewProjectPage") { //$NON-NLS-1$
            @Override
            public void createControl(Composite parent) {
                super.createControl(parent);
                createWorkingSetGroup((Composite) getControl(), getSelection(),
                        new String[] { "org.eclipse.ui.resourceWorkingSetPage" }); //$NON-NLS-1$
                Dialog.applyDialogFont(getControl());
            }
        };
        myMainPage.setTitle(NewAutoMakeProjectWizard_PageTitle);
        myMainPage.setDescription(NewAutoMakeProjectWizard_Description);
        myNaturePage=new NewProjectNaturePage("Select Nature Page");
        myBuildToolsPage=new NewProjectBuildToolsPage ("Build tools Page");
        myProjectTypePage=new NewProjectProjectTypePage("Select project type page");
         mySourceCodePage =new NewProjectSourceLocationPage("soyrce code page");
        
        addPage(myMainPage);
        addPage(myNaturePage);
        addPage(myBuildToolsPage);
        addPage(myProjectTypePage);
        addPage(mySourceCodePage);
        
    }

    @Override
    protected IGenerator getGenerator() {
        AutoBuildProjectGenerator generator = new AutoBuildProjectGenerator();
        generator.setProjectName(myMainPage.getProjectName());
        if (!myMainPage.useDefaults()) {
            generator.setLocationURI(myMainPage.getLocationURI());
        }
        return generator;
    }

    private void handle(Throwable target) {
        String message = Messages.TemplateWizard_CannotBeCreated;
        log(message, target);
        IStatus status;
        if (target instanceof CoreException) {
            status = ((CoreException) target).getStatus();
        } else {
            status = new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
                    Messages.TemplateWizard_InternalError, target);
        }
        ErrorDialog.openError(getShell(), Messages.TemplateWizard_ErrorCreating, message, status);
    }

    private void log(String message, Throwable e) {
        ILog log = Platform.getLog(getClass());
        log.log(new Status(IStatus.ERROR, log.getBundle().getSymbolicName(), message, e));
    }
}
