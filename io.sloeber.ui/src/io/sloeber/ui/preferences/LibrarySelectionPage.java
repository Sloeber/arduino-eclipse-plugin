package io.sloeber.ui.preferences;

import static io.sloeber.ui.Activator.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.arduinoFramework.api.LibraryManager;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.ArduinoLibraryTree;
import io.sloeber.ui.helpers.ArduinoLibraryTree.Library;

public class LibrarySelectionPage extends PreferencePage implements IWorkbenchPreferencePage {

	private boolean isJobRunning = false;
	protected TreeViewer viewer;
	protected TreeEditor editor;
	protected ArduinoLibraryTree libs = new ArduinoLibraryTree();
	final static String emptyString = ""; //$NON-NLS-1$

	@Override
	public void init(IWorkbench workbench) {
		// nothing needed here
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout());

		Text desc = new Text(control, SWT.READ_ONLY);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		desc.setLayoutData(layoutData);
		desc.setBackground(parent.getBackground());
		desc.setText(Messages.library_preference_page_add_remove);
		libs.createTree(control);

		return control;
	}

	@Override
	public boolean performOk() {
		if (this.isJobRunning == true) {
			MessageDialog.openInformation(getShell(), "Library Manager", //$NON-NLS-1$
					"Library Manager is busy. Please wait some time..."); //$NON-NLS-1$
			return false;
		}
		this.isJobRunning = true;
		Job job = new Job(Messages.ui_Adopting_arduino_libraries) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				MultiStatus status = new MultiStatus(PLUGIN_ID, 0, Messages.ui_installing_arduino_libraries, null);
				Set<IArduinoLibraryVersion> toRemoveLibs = new HashSet<>();
				Set<IArduinoLibraryVersion> toInstallLibs = new HashSet<>();
				for (Library library : libs.getLibs()) {
						IArduinoLibraryVersion installedVersion = library.getInstalledVersion();
						IArduinoLibraryVersion toInstalVersion = library.getVersion();
						if ((installedVersion != null) && (installedVersion.compareTo(toInstalVersion) != 0)) {
							toRemoveLibs.add(installedVersion);
						}
						if ((toInstalVersion != null) && (toInstalVersion.compareTo(installedVersion) != 0)) {
							toInstallLibs.add(toInstalVersion);
						}
				}
				return LibraryManager.updateLibraries(toRemoveLibs, toInstallLibs, monitor, status);
			}
		};
		job.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				LibrarySelectionPage.this.isJobRunning = false;
			}

		});
		job.setUser(true);
		job.schedule();
		return true;
	}

}
