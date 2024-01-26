/*******************************************************************************
 * Copyright (c) 2016 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.sloeber.autoBuild.integrations;

import java.net.URI;

import org.eclipse.cdt.internal.ui.wizards.ICDTCommonProjectWizard;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.tools.templates.core.IGenerator;
import org.eclipse.tools.templates.ui.TemplateWizard;
import org.eclipse.ui.IWorkbench;

public class CDTProjectWizard2 extends TemplateWizard implements IGenerator, ICDTCommonProjectWizard {

    private final CDTCommonProjectWizard cdtWizard;

    public CDTProjectWizard2() {
        this.cdtWizard = new CCProjectWizard();
    }

    protected CDTProjectWizard2(CDTCommonProjectWizard cdtWizard) {
        this.cdtWizard = cdtWizard;
    }

    @Override
    public void addPages() {
        cdtWizard.addPages();

        for (IWizardPage page : cdtWizard.getPages()) {
            addPage(page);
        }
    }

    @Override
    public void init(IWorkbench theWorkbench, IStructuredSelection currentSelection) {
        super.init(theWorkbench, currentSelection);
        cdtWizard.init(theWorkbench, currentSelection);
    }

    @Override
    public void setContainer(IWizardContainer wizardContainer) {
        super.setContainer(wizardContainer);
        cdtWizard.setContainer(wizardContainer);
    }

    @Override
    public boolean canFinish() {
        return cdtWizard.canFinish();
    }

    @Override
    public boolean performFinish() {
        return cdtWizard.performFinish();
    }

    @Override
    public boolean performCancel() {
        return cdtWizard.performCancel();
    }

    @Override
    protected IGenerator getGenerator() {
        return this;
    }

    @Override
    public void generate(IProgressMonitor monitor) throws CoreException {
        // Nothing to do for now, the performFinish already did it
    }

    @Override
    public IProject createIProject(String name, URI location) throws CoreException {
        return cdtWizard.createIProject(name, location);
    }

    @Override
    public IProject createIProject(String name, URI location, IProgressMonitor monitor) throws CoreException {
        return createIProject(name, location, monitor);
    }

    @Override
    public String[] getContentTypeIDs() {
        return cdtWizard.getContentTypeIDs();
    }

    @Override
    public String[] getExtensions() {
        return cdtWizard.getExtensions();
    }

    @Override
    public String[] getLanguageIDs() {
        return cdtWizard.getLanguageIDs();
    }

    @Override
    public IProject getLastProject() {
        return cdtWizard.getLastProject();
    }

    @Override
    public String[] getNatures() {
        return cdtWizard.getNatures();
    }

    @Override
    public IProject getProject(boolean defaults) {
        return cdtWizard.getProject(defaults);
    }

    @Override
    public IProject getProject(boolean defaults, boolean onFinish) {
        return cdtWizard.getProject(defaults, onFinish);
    }

    @Override
    public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
            throws CoreException {
        setInitializationData(config, propertyName, data);
    }

}
