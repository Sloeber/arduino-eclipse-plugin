package io.sloeber.autoBuild.integrations;

import static io.sloeber.autoBuild.ui.internal.Messages.*;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.cdt.core.CCProjectNature;
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
import io.sloeber.autoBuild.api.IToolProvider;
import io.sloeber.autoBuild.api.IToolProviderManager;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.integration.AutoBuildProjectGenerator;
import io.sloeber.autoBuild.ui.internal.Messages;

public class NewAutoBuildProjectWizard extends TemplateWizard {
    private WizardNewProjectCreationPage mainPage;

    @Override
    public boolean performFinish() {
        try {
            String projectName = mainPage.getProjectName();
            String extensionPointID = AutoBuildManager.supportedExtensionPointIDs()[0];
            String extensionID = AutoBuildManager.getSupportedExtensionIDs(extensionPointID)[0];
            String projectTypeID = AutoBuildManager.getProjectIDs(extensionPointID, extensionID).keySet()
                    .toArray(new String[10])[0];
            String natureID = CCProjectNature.CC_NATURE_ID;
            ICodeProvider codeProvider = null;
            IToolProvider toolProvider = IToolProviderManager.getDefault().getAnyToolProvider();
            getContainer().run(true, true, new WorkspaceModifyOperation() {
                @Override
                protected void execute(IProgressMonitor monitor)
                        throws CoreException, InvocationTargetException, InterruptedException {
                    SubMonitor sub = SubMonitor.convert(monitor, Messages.TemplateWizard_Generating, 1);

                    AutoBuildProject.createProject(projectName, extensionPointID, extensionID, projectTypeID, natureID,
                            codeProvider, toolProvider, false, sub);
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
        // TODO Auto-generated method stub
        //return super.performFinish();
    }

    @Override
    public void setContainer(IWizardContainer wizardContainer) {
        super.setContainer(wizardContainer);
        setWindowTitle(NewAutoMakeProjectWizard_WindowTitle);
    }

    @Override
    public void addPages() {
        mainPage = new WizardNewProjectCreationPage("basicNewProjectPage") { //$NON-NLS-1$
            @Override
            public void createControl(Composite parent) {
                super.createControl(parent);
                createWorkingSetGroup((Composite) getControl(), getSelection(),
                        new String[] { "org.eclipse.ui.resourceWorkingSetPage" }); //$NON-NLS-1$
                Dialog.applyDialogFont(getControl());
            }
        };
        mainPage.setTitle(NewAutoMakeProjectWizard_PageTitle);
        mainPage.setDescription(NewAutoMakeProjectWizard_Description);
        this.addPage(mainPage);
    }

    @Override
    protected IGenerator getGenerator() {
        AutoBuildProjectGenerator generator = new AutoBuildProjectGenerator();
        generator.setProjectName(mainPage.getProjectName());
        if (!mainPage.useDefaults()) {
            generator.setLocationURI(mainPage.getLocationURI());
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
