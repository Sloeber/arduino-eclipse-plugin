package io.sloeber.autoBuild.integrations;

import static io.sloeber.autoBuild.ui.internal.Messages.*;

import org.eclipse.cdt.cmake.core.CMakeProjectGenerator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tools.templates.core.IGenerator;
import org.eclipse.tools.templates.ui.TemplateWizard;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

public class NewAutoBuildProjectWizard extends TemplateWizard {
    private WizardNewProjectCreationPage mainPage;

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
        CMakeProjectGenerator generator = new CMakeProjectGenerator("templates/simple/manifest.xml"); //$NON-NLS-1$
        generator.setProjectName(mainPage.getProjectName());
        if (!mainPage.useDefaults()) {
            generator.setLocationURI(mainPage.getLocationURI());
        }
        return generator;
    }

}
