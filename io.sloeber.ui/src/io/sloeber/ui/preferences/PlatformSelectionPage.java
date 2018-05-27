
package io.sloeber.ui.preferences;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.PackageManager.PlatformTree;
import io.sloeber.core.api.PackageManager.PlatformTree.IndexFile;
import io.sloeber.core.api.PackageManager.PlatformTree.InstallableVersion;
import io.sloeber.core.api.PackageManager.PlatformTree.Package;
import io.sloeber.core.api.PackageManager.PlatformTree.Platform;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;

public class PlatformSelectionPage extends PreferencePage implements IWorkbenchPreferencePage {
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	public PlatformSelectionPage() {
	}

	protected PlatformTree myPlatformTree = new PackageManager.PlatformTree();
	protected FilteredTree myGuiplatformTree;
	protected boolean myHideJson = MyPreferences.getHideJson();
	protected TreeViewer viewer;

	@Override
	public void init(IWorkbench workbench) {
		// not needed
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout());

		Button btnCheckButton = new Button(control, SWT.CHECK);
		btnCheckButton.setText(Messages.PlatformSelectionPage_hide_third_party_url); 
		btnCheckButton.setSelection(this.myHideJson);
		btnCheckButton.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {

				PlatformSelectionPage.this.myHideJson = btnCheckButton.getSelection();
				MyPreferences.setHideJson(PlatformSelectionPage.this.myHideJson);
				PlatformSelectionPage.this.viewer.setInput(EMPTY_STRING);

			}
		});

		Text desc = new Text(control, SWT.READ_ONLY);
		desc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		desc.setBackground(parent.getBackground());
		desc.setText(Messages.platformSelectionTip);

		PatternFilter filter = new PatternFilter() {
			@Override
			protected boolean isLeafMatch(final Viewer viewer1, final Object element) {
				boolean isMatch = false;
				if (element instanceof InstallableVersion) {
					InstallableVersion ver = (InstallableVersion) element;
					isMatch |= wordMatches(ver.getVersion().toString());
					isMatch |= myWordMatches(ver.getPlatform());

				}
				if (element instanceof IndexFile) {
					IndexFile indexFile = (IndexFile) element;
					isMatch |= myWordMatches(indexFile);
				}
				if (element instanceof Package) {
					Package pac = (Package) element;
					isMatch |= myWordMatches(pac);
				}
				if (element instanceof Platform) {
					Platform platform = (Platform) element;
					isMatch |= myWordMatches(platform);

				}
				return isMatch;
			}

			private boolean myWordMatches(Platform platform) {
				boolean ret = wordMatches(platform.getName());
				ret |= wordMatches(platform.getArchitecture());
				ret |= wordMatches(platform.getBoards());
				ret |= myWordMatches(platform.getPackage());
				return ret;
			}

			private boolean myWordMatches(Package pac) {
				boolean ret = wordMatches(pac.getEmail());
				ret |= wordMatches(pac.getMaintainer());
				ret |= wordMatches(pac.getName());
				ret |= wordMatches(pac.getWebsiteURL().toString());
				ret |= wordMatches(pac.getIndexFile().toString());
				return ret;
			}

			private boolean myWordMatches(IndexFile indexFile) {

				return wordMatches(indexFile.getFullName());
			}
		};

		this.myGuiplatformTree = new FilteredTree(control, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION,
				filter, true) {

			@Override
			protected TreeViewer doCreateTreeViewer(Composite composite, int style) {

				CheckboxTreeViewer viewer1 = new CheckboxTreeViewer(composite);
				viewer1.setCheckStateProvider(new ICheckStateProvider() {
					@Override
					public boolean isChecked(Object element) {
						if (element instanceof InstallableVersion) {
							return ((InstallableVersion) element).isInstalled();
						}
						if (element instanceof IndexFile) {
							return ((IndexFile) element).isInstalled();
						}
						if (element instanceof Package) {
							return ((Package) element).isInstalled();
						}
						if (element instanceof Platform) {
							return ((Platform) element).isInstalled();
						}
						return false;
					}

					@Override
					public boolean isGrayed(Object element) {
						if (element instanceof InstallableVersion) {
							return false;
						}
						if (element instanceof IndexFile) {
							return ((IndexFile) element).isInstalled();
						}
						if (element instanceof Package) {
							return ((Package) element).isInstalled();
						}
						if (element instanceof Platform) {
							return ((Platform) element).isInstalled();
						}
						return false;
					}
				});
				viewer1.addCheckStateListener(new ICheckStateListener() {

					@Override
					public void checkStateChanged(CheckStateChangedEvent event) {

						Object element = event.getElement();
						if (element instanceof InstallableVersion) {
							InstallableVersion cur = (InstallableVersion) element;
							cur.setInstalled(event.getChecked());
						}

						PlatformSelectionPage.this.viewer.refresh();
					}
				});
				viewer1.setContentProvider(new ITreeContentProvider() {

					@Override
					public Object[] getElements(Object inputElement) {
						if (PlatformSelectionPage.this.myHideJson) {
							Set<Package> packages = PlatformSelectionPage.this.myPlatformTree.getAllPackages();
							return packages.toArray(new Object[packages.size()]);
						}
						Collection<IndexFile> indexFiles = PlatformSelectionPage.this.myPlatformTree.getAllIndexFiles();
						return indexFiles.toArray(new Object[indexFiles.size()]);

					}

					@Override
					public void dispose() {
						// nothing to do
					}

					@Override
					public void inputChanged(Viewer viewer11, Object oldInput, Object newInput) {
						// nothing to do
					}

					@Override
					public Object[] getChildren(Object parentElement) {
						if (parentElement instanceof IndexFile) {
							Collection<Package> packages = ((IndexFile) parentElement).getAllPackages();
							return packages.toArray(new Object[packages.size()]);
						}
						if (parentElement instanceof Package) {
							Collection<Platform> platforms = ((Package) parentElement).getPlatforms();
							return platforms.toArray(new Object[platforms.size()]);
						}
						if (parentElement instanceof Platform) {
							Collection<InstallableVersion> versions = ((Platform) parentElement).getVersions();
							InstallableVersion arrayVersions[]= versions.toArray(new InstallableVersion[versions.size()]);
							Arrays.sort(arrayVersions, Collections.reverseOrder());
							return arrayVersions;
						}

						return null;
					}

					@Override
					public Object getParent(Object element) {
						return null;
					}

					@Override
					public boolean hasChildren(Object element) {
						return !(element instanceof InstallableVersion);
					}
				});

				viewer1.setLabelProvider(new CellLabelProvider() {
					@Override
					public String getToolTipText(Object element) {
						if (element instanceof IndexFile) {
							return ((IndexFile) element).getFullName();

						}
						if (element instanceof Package) {
						    String NULL="NULL"; //$NON-NLS-1$
							Package pkg = (Package) element;
							String maintainer=pkg.getMaintainer();
							String email=pkg.getEmail();
							URL weburl=pkg.getWebsiteURL();
							String weburlString=NULL; 
							if(maintainer==null)maintainer=NULL; 
							if(email==null)email=NULL; 
							if(weburl!=null) weburlString=weburl.toString();
						
							return Messages.packageTooltip.replace(Messages.MAINTAINER, maintainer)
									.replace(Messages.EMAIL, email)
									.replace(Messages.URL, weburlString);

						}
						if (element instanceof Platform) {
							return ((Platform) element).getBoards();

						}
						if (element instanceof InstallableVersion) {
							return null;

						}
						return null;
					}

					@Override
					public org.eclipse.swt.graphics.Point getToolTipShift(Object object) {
						return new org.eclipse.swt.graphics.Point(5, 5);
					}

					@Override
					public int getToolTipDisplayDelayTime(Object object) {
						return 1000;
					}

					@Override
					public int getToolTipTimeDisplayed(Object object) {
						return 5000;
					}

					@Override
					public void update(ViewerCell cell) {
						if (cell.getElement() instanceof IndexFile) {
							cell.setText(((IndexFile) cell.getElement()).getNiceName());

						}
						if (cell.getElement() instanceof Package) {
							cell.setText(((Package) cell.getElement()).getName());

						}
						if (cell.getElement() instanceof Platform) {
							cell.setText(((Platform) cell.getElement()).getName());

						}
						if (cell.getElement() instanceof InstallableVersion) {
							cell.setText(((InstallableVersion) cell.getElement()).getVersion().toString());
						}

					}
				});

				return viewer1;
			}

		};
		this.viewer = this.myGuiplatformTree.getViewer();
		ColumnViewerToolTipSupport.enableFor(this.viewer);

		this.myGuiplatformTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		this.viewer.setInput(EMPTY_STRING);
		return control;
	}

	protected IStatus updateInstallation(IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(Activator.getId(), 0, Messages.ui_installing_platforms, null);
		PackageManager.setPlatformTree(this.myPlatformTree, monitor, status);
		return status;
	}

	@Override
	public boolean performOk() {
		Job installJob = new Job(Messages.ui_adopting_platforms) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(Messages.ui_adopting_platforms, 0);
				return updateInstallation(monitor);

			}
		};
		installJob.setPriority(Job.LONG);
		installJob.setUser(true);
		installJob.schedule();

		return true;
	}

}
