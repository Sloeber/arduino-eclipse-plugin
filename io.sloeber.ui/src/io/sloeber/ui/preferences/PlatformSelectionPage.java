
package io.sloeber.ui.preferences;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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
import io.sloeber.core.api.BoardsManager.PlatformTree.IndexFile;
import io.sloeber.core.api.BoardsManager.PlatformTree.Package;
import io.sloeber.core.api.BoardsManager.PlatformTree.Platform;
import io.sloeber.core.api.VersionNumber;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;

public class PlatformSelectionPage extends PreferencePage implements IWorkbenchPreferencePage {
    public PlatformSelectionPage() {
    }

    protected PlatformTree myPlatformTree = new BoardsManager.PlatformTree();
    protected Tree myGuiplatformTree;
    protected boolean myHideJson = MyPreferences.getHideJson();

    @Override
    public void init(IWorkbench workbench) {
	// not needed
    }

    @Override
    protected Control createContents(Composite parent) {
	Composite control = new Composite(parent, SWT.NONE);
	control.setLayout(new GridLayout());

	Button btnCheckButton = new Button(control, SWT.CHECK);
	btnCheckButton.setText("Hide 3th party json files"); //$NON-NLS-1$
	btnCheckButton.setSelection(this.myHideJson);
	btnCheckButton.addListener(SWT.Selection, new Listener() {

	    @Override
	    public void handleEvent(Event event) {

		PlatformSelectionPage.this.myHideJson = btnCheckButton.getSelection();
		MyPreferences.setHideJson(PlatformSelectionPage.this.myHideJson);
		updateTable();

	    }
	});

	Text desc = new Text(control, SWT.READ_ONLY);
	desc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
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
			setParentCheck(thechangeItem);

		    } else {
			VersionNumber versionNumber = (VersionNumber) thechangeItem.getData();
			Platform platform = (Platform) thechangeItem.getParentItem().getData();
			platform.setInstalled(versionNumber, thechangeItem.getChecked());
			setParentCheck(thechangeItem.getParentItem());
		    }
		}
	    }

	    private void setParentCheck(TreeItem parentItem) {
		if (parentItem != null) {
		    boolean isChecked = false;
		    for (TreeItem curItem : parentItem.getItems()) {
			isChecked = isChecked || curItem.getChecked();
		    }
		    if (isChecked != parentItem.getChecked()) {
			parentItem.setChecked(isChecked);
			parentItem.setGrayed(isChecked);
			setParentCheck(parentItem.getParentItem());
		    }
		}
	    }
	});
	updateTable();

	return control;
    }

    protected void updateTable() {
	if (this.myGuiplatformTree == null || this.myGuiplatformTree.isDisposed()) {
	    return;
	}

	this.myGuiplatformTree.removeAll();
	if (this.myHideJson) {
	    addPackages(this.myPlatformTree.getAllPackages(), null);
	} else {
	    Collection<IndexFile> indexFiles = this.myPlatformTree.getAllIndexFiles();
	    for (IndexFile curIndexFile : indexFiles) {
		TreeItem indexItem = new TreeItem(this.myGuiplatformTree, SWT.NONE);
		indexItem.setText(curIndexFile.getNiceName());
		addPackages(curIndexFile.getAllPackages(), indexItem);
	    }
	}
    }

    private void addPackages(Collection<Package> collection, TreeItem parent) {
	for (Package curPackage : collection) {
	    TreeItem packageItem;
	    if (parent == null) {
		packageItem = new TreeItem(this.myGuiplatformTree, SWT.NONE);
	    } else {
		packageItem = new TreeItem(parent, SWT.NONE);
	    }
	    packageItem.setText(curPackage.getName());
	    Collection<Platform> platforms = curPackage.getPlatforms();

	    for (Platform curPlatform : platforms) {

		TreeItem platformItem = new TreeItem(packageItem, SWT.NONE);
		platformItem.setData(curPlatform);
		platformItem.setText(curPlatform.getName() + " -" + curPlatform.getArchitecture() + "- (" //$NON-NLS-1$ //$NON-NLS-2$
			+ curPlatform.getBoards() + ')');
		for (VersionNumber curVersion : curPlatform.getVersions()) {
		    TreeItem versionItem = new TreeItem(platformItem, SWT.NONE);
		    versionItem.setData(curVersion);
		    versionItem.setText(curVersion.toString());
		    boolean isInstalled = curPlatform.isInstalled(curVersion);
		    versionItem.setChecked(isInstalled);
		    if (isInstalled) {
			platformItem.setChecked(true);
			platformItem.setGrayed(true);

			packageItem.setChecked(true);
			packageItem.setGrayed(true);
			if (parent != null) {
			    parent.setChecked(true);
			    parent.setGrayed(true);
			}
		    }
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
