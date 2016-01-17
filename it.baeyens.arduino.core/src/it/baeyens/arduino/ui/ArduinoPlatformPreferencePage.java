
package it.baeyens.arduino.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

import it.baeyens.arduino.managers.ArduinoManager;
import it.baeyens.arduino.managers.ArduinoPackage;
import it.baeyens.arduino.managers.ArduinoPlatform;

public class ArduinoPlatformPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    protected Tree platformTree;
    protected List<ArduinoPlatform> removePlatforms = null;
    protected List<ArduinoPlatform> addPlatforms = null;

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
	layoutData.widthHint = 500;
	desc.setLayoutData(layoutData);
	desc.setBackground(parent.getBackground());
	desc.setText("remove or add checkboxes to update your configuration."); //$NON-NLS-1$

	this.platformTree = new Tree(control, SWT.CHECK | SWT.BORDER);
	this.platformTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	this.platformTree.addListener(SWT.Selection, new Listener() {
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
	if (this.platformTree == null || this.platformTree.isDisposed()) {
	    return;
	}

	this.platformTree.removeAll();

	List<ArduinoPackage> packages = ArduinoManager.getPackages();
	Collections.sort(packages, new Comparator<ArduinoPackage>() {
	    @Override
	    public int compare(ArduinoPackage arg0, ArduinoPackage arg1) {
		return arg0.getName().compareTo(arg1.getName());
	    }
	});

	for (ArduinoPackage curPackage : packages) {
	    TreeItem packageItem = new TreeItem(this.platformTree, SWT.NONE);
	    packageItem.setText(curPackage.getName());
	    List<ArduinoPlatform> platforms = curPackage.getPlatforms();
	    Collections.sort(platforms, new Comparator<ArduinoPlatform>() {
		@Override
		public int compare(ArduinoPlatform arg0, ArduinoPlatform arg1) {
		    String field0 = arg0.getName() + '-' + arg0.getVersion();
		    String field1 = arg1.getName() + '-' + arg1.getVersion();
		    return field0.compareTo(field1);
		}
	    });
	    String prefPlatformName = null;
	    TreeItem platformItem = null;
	    for (ArduinoPlatform curPlatform : platforms) {

		if (!curPlatform.getName().equals(prefPlatformName)) {
		    platformItem = new TreeItem(packageItem, SWT.NONE);
		    platformItem.setText(curPlatform.getName());
		    prefPlatformName = curPlatform.getName();
		}
		TreeItem versionItem = new TreeItem(platformItem, SWT.NONE);
		versionItem.setData(curPlatform);
		versionItem.setText(curPlatform.getVersion());
		versionItem.setChecked(curPlatform.isInstalled());
	    }
	}
    }

    protected IStatus updateInstallation(IProgressMonitor monitor) {
	MultiStatus status = new MultiStatus(Activator.getId(), 0, Messages.ui_installing_platforms, null);

	for (ArduinoPlatform curPlatform : this.removePlatforms) {
	    status.add(curPlatform.remove(monitor));
	}
	for (ArduinoPlatform curPlatform : this.addPlatforms) {
	    status.add(curPlatform.install(monitor));
	}

	return status;
    }

    private void markInstallationChanges() {
	this.removePlatforms = new ArrayList<>();
	this.addPlatforms = new ArrayList<>();
	TreeItem packageItems[] = this.platformTree.getItems();
	for (TreeItem curPackageItem : packageItems) {
	    TreeItem PlatformItems[] = curPackageItem.getItems();
	    for (TreeItem curPlatformItem : PlatformItems) {
		TreeItem VersionItems[] = curPlatformItem.getItems();
		for (TreeItem curVersionItem : VersionItems) {
		    ArduinoPlatform curPlatform = (ArduinoPlatform) curVersionItem.getData();
		    if (curPlatform.isInstalled() && !curVersionItem.getChecked()) {
			this.removePlatforms.add(curPlatform);
		    } else if (!curPlatform.isInstalled() && curVersionItem.getChecked()) {
			this.addPlatforms.add(curPlatform);
		    }

		}
	    }
	}
    }

    @Override
    public boolean performOk() {
	markInstallationChanges();
	new Job(Messages.ui_adopting_platforms) {

	    @Override
	    protected IStatus run(IProgressMonitor monitor) {

		return updateInstallation(monitor);

	    }
	}.schedule();

	return true;
    }

}
