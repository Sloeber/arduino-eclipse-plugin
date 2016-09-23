
package io.sloeber.ui.preferences;

import java.util.Collection;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.BoardsManager.PlatformTree;
import io.sloeber.core.api.BoardsManager.PlatformTree.Package;
import io.sloeber.core.api.BoardsManager.PlatformTree.Platform;
import io.sloeber.core.api.VersionNumber;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;

public class PlatformSelectionPage extends PreferencePage implements IWorkbenchPreferencePage {

    protected PlatformTree myPlatformTree = new BoardsManager.PlatformTree();
    protected Tree myGuiplatformTree;

    @Override
    public void init(IWorkbench workbench) {
	// not needed
    }

    @Override
    protected Control createContents(Composite parent) {
	Composite control = new Composite(parent, SWT.NONE);
	control.setLayout(new GridLayout());

	Text desc = new Text(control, SWT.READ_ONLY);
	GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
	desc.setLayoutData(layoutData);
	desc.setBackground(parent.getBackground());
	desc.setText("remove or add checkboxes to update your configuration."); //$NON-NLS-1$

	this.myGuiplatformTree = new Tree(control, SWT.CHECK | SWT.BORDER);
	this.myGuiplatformTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	this.myGuiplatformTree.addListener(SWT.Selection, new Listener() {
	    @Override
	    public void handleEvent(Event event) {

		if (event.detail == SWT.CHECK) {
		    TreeItem thechangeItem = (TreeItem) event.item;
		    if (thechangeItem.getItemCount() > 0) {
			event.detail = SWT.NONE;
			event.type = SWT.None;
			event.doit = false;
			thechangeItem.setChecked(false);
		    }
		}
	    }
	});
	updateTable();

	return control;
    }

    private void updateTable() {
	if (this.myGuiplatformTree == null || this.myGuiplatformTree.isDisposed()) {
	    return;
	}

	this.myGuiplatformTree.removeAll();

	Set<Package> packages = this.myPlatformTree.getAllPackages();

	for (Package curPackage : packages) {
	    TreeItem packageItem = new TreeItem(this.myGuiplatformTree, SWT.NONE);
	    packageItem.setText(curPackage.getName());
	    Collection<Platform> platforms = curPackage.getPlatforms();

	    for (Platform curPlatform : platforms) {

		TreeItem platformItem = new TreeItem(packageItem, SWT.NONE);
		platformItem.setText(curPlatform.getName());
		for (VersionNumber curVersion : curPlatform.getVersions()) {
		    TreeItem versionItem = new TreeItem(platformItem, SWT.NONE);
		    versionItem.setData(curPlatform);
		    versionItem.setText(curVersion.toString());
		    versionItem.setChecked(curPlatform.isInstalled(curVersion));
		}
	    }
	}
    }

    protected IStatus updateInstallation(IProgressMonitor monitor) {
	MultiStatus status = new MultiStatus(Activator.getId(), 0, Messages.ui_installing_platforms, null);
	BoardsManager.setPlatformTree(this.myPlatformTree, monitor, status);
	return status;
    }

    @Override
    public boolean performOk() {
	new Job(Messages.ui_adopting_platforms) {

	    @Override
	    protected IStatus run(IProgressMonitor monitor) {

		return updateInstallation(monitor);

	    }
	}.schedule();

	return true;
    }

}
