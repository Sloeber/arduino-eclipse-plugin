
package io.sloeber.ui.preferences;

import static io.sloeber.ui.Activator.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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

import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.VersionNumber;
import io.sloeber.core.api.Json.ArduinoPackage;
import io.sloeber.core.api.Json.ArduinoPlatform;
import io.sloeber.core.api.Json.ArduinoPlatformPackageIndex;
import io.sloeber.core.api.Json.ArduinoPlatformVersion;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;

public class PlatformSelectionPage extends PreferencePage implements IWorkbenchPreferencePage {
	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	// platform index json, package,platform,versions structure
	private HashMap<String, HashMap<String, HashMap<String, InstallableVersion[]>>> myShownPlatforms = new HashMap<>();

	private boolean mustBeInstalled(ArduinoPlatform platform) {
		ArduinoPackage parentPkg = platform.getParent();
		ArduinoPlatformPackageIndex parentIndex = parentPkg.getPackageIndex();
		InstallableVersion[] inScopeVersions = myShownPlatforms.get(parentIndex.getID()).get(parentPkg.getID())
				.get(platform.getID());
		for (InstallableVersion version : inScopeVersions) {
			if (version.mustBeInstalled()) {
				return true;
			}
		}
		return false;
	}

	private boolean mustBeInstalled(ArduinoPlatformPackageIndex packageIndex) {
		HashMap<String, HashMap<String, InstallableVersion[]>> inScopeVersions = myShownPlatforms
				.get(packageIndex.getID());
		for (HashMap<String, InstallableVersion[]> platform : inScopeVersions.values()) {
			for (InstallableVersion[] versions : platform.values()) {
				for (InstallableVersion version : versions) {
					if (version.mustBeInstalled()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean mustBeInstalled(ArduinoPackage pkg) {
		ArduinoPlatformPackageIndex parentIndex = pkg.getPackageIndex();
		HashMap<String, InstallableVersion[]> inScopeVersions = myShownPlatforms.get(parentIndex.getID())
				.get(pkg.getID());
		for (InstallableVersion[] versions : inScopeVersions.values()) {
			for (InstallableVersion version : versions) {
				if (version.mustBeInstalled()) {
					return true;
				}
			}
		}
		return false;
	}

	public static class InstallableVersion implements Comparable<InstallableVersion> {
		private ArduinoPlatformVersion myPlatformm;
		private boolean myMustBeInstalled;

		public InstallableVersion(ArduinoPlatformVersion platformm) {
			myPlatformm = platformm;
			myMustBeInstalled = myPlatformm.isInstalled();
		}

		public boolean mustBeInstalled() {
			return myMustBeInstalled;
		}

		public VersionNumber getVersion() {
			return myPlatformm.getVersion();
		}

		public boolean isInstalled() {
			return myPlatformm.isInstalled();
		}

		public void setMustBeInstalled(boolean mustBeInstalled) {
			this.myMustBeInstalled = mustBeInstalled;
		}

		@Override
		public int compareTo(InstallableVersion o) {
			return getVersion().compareTo(o.getVersion());
		}

		public ArduinoPlatformVersion getPlatform() {
			return myPlatformm;
		}

	}

	public PlatformSelectionPage() {
		for (ArduinoPlatformPackageIndex curPackageIndex : BoardsManager.getPackageIndices()) {
			String pkgIndexID = curPackageIndex.getID();
			HashMap<String, HashMap<String, InstallableVersion[]>> packageMap = new HashMap<>();
			for (ArduinoPackage curPackage : curPackageIndex.getPackages()) {
				HashMap<String, InstallableVersion[]> platformMap = new HashMap<>();
				String pkgID = curPackage.getID();
				for (ArduinoPlatform curPlatform : curPackage.getPlatforms()) {
					String platformID = curPlatform.getID();
					Collection<ArduinoPlatformVersion> platformVersions = curPlatform.getVersions();
					InstallableVersion versions[] = new InstallableVersion[platformVersions.size()];
					int index = 0;
					for (ArduinoPlatformVersion curPlatformversion : platformVersions) {
						versions[index++] = new InstallableVersion(curPlatformversion);
					}
//					InstallableVersion arrayVersions[] = versions.toArray(new InstallableVersion[versions.size()]);
//					Arrays.sort(arrayVersions, Collections.reverseOrder());
					platformMap.put(platformID, versions);
				}
				packageMap.put(pkgID, platformMap);
			}
			myShownPlatforms.put(pkgIndexID, packageMap);
		}
	}

//	protected PlatformTree myPlatformTree = new PackageManager.PlatformTree();
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
				if (element instanceof ArduinoPlatformPackageIndex) {
					ArduinoPlatformPackageIndex indexFile = (ArduinoPlatformPackageIndex) element;
					isMatch |= myWordMatches(indexFile);
				}
				if (element instanceof ArduinoPackage) {
					ArduinoPackage pac = (ArduinoPackage) element;
					isMatch |= myWordMatches(pac);
				}
				if (element instanceof ArduinoPlatformVersion) {
					ArduinoPlatformVersion platform = (ArduinoPlatformVersion) element;
					isMatch |= myWordMatches(platform);

				}
				return isMatch;
			}

			private boolean myWordMatches(ArduinoPlatformVersion arduinoPlatformVersion) {
				boolean ret = wordMatches(arduinoPlatformVersion.getName());
				ret |= wordMatches(arduinoPlatformVersion.getArchitecture());
				ret |= wordMatches(arduinoPlatformVersion.getConcattenatedBoardNames());
				ret |= myWordMatches(arduinoPlatformVersion.getParent().getParent());
				return ret;
			}

			private boolean myWordMatches(ArduinoPackage pac) {
				boolean ret = wordMatches(pac.getEmail());
				ret |= wordMatches(pac.getMaintainer());
				ret |= wordMatches(pac.getName());
				ret |= wordMatches(pac.getWebsiteURL().toString());
				ret |= wordMatches(pac.getPackageIndex().getJsonFile().toString());
				return ret;
			}

			private boolean myWordMatches(ArduinoPlatformPackageIndex indexFile) {

				return wordMatches(indexFile.getID());
			}
		};

		this.myGuiplatformTree = new FilteredTree(control, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION,
				filter, true, true) {

			@Override
			protected TreeViewer doCreateTreeViewer(Composite composite, int style) {

				CheckboxTreeViewer viewer1 = new CheckboxTreeViewer(composite);
				viewer1.setCheckStateProvider(new ICheckStateProvider() {
					@Override
					public boolean isChecked(Object element) {
						if (element instanceof InstallableVersion) {
							return ((InstallableVersion) element).mustBeInstalled();
						}
						if (element instanceof ArduinoPlatformPackageIndex) {
							return mustBeInstalled((ArduinoPlatformPackageIndex) element);
						}
						if (element instanceof ArduinoPackage) {
							return mustBeInstalled((ArduinoPackage) element);
						}
						if (element instanceof ArduinoPlatform) {
							return mustBeInstalled((ArduinoPlatform) element);
						}
						return false;
					}

					@Override
					public boolean isGrayed(Object element) {
						if (element instanceof InstallableVersion) {
							return false;
						}
						if (element instanceof ArduinoPlatformPackageIndex) {
							return mustBeInstalled((ArduinoPlatformPackageIndex) element);
						}
						if (element instanceof ArduinoPackage) {
							return mustBeInstalled((ArduinoPackage) element);
						}
						if (element instanceof ArduinoPlatform) {
							return mustBeInstalled((ArduinoPlatform) element);
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
							cur.setMustBeInstalled(event.getChecked());
						}

						PlatformSelectionPage.this.viewer.refresh();
					}
				});
				viewer1.setContentProvider(new ITreeContentProvider() {

					@Override
					public Object[] getElements(Object inputElement) {
						if (PlatformSelectionPage.this.myHideJson) {
							List<ArduinoPackage> packages = BoardsManager.getPackages();
							return packages.toArray(new Object[packages.size()]);
						}
						Collection<ArduinoPlatformPackageIndex> indexFiles = BoardsManager.getPackageIndices();
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
						if (parentElement instanceof ArduinoPlatformPackageIndex) {
							Collection<ArduinoPackage> packages = ((ArduinoPlatformPackageIndex) parentElement).getPackages();
							return packages.toArray(new Object[packages.size()]);
						}
						if (parentElement instanceof ArduinoPackage) {
							Collection<ArduinoPlatform> platforms = ((ArduinoPackage) parentElement).getPlatforms();
							ArduinoPlatform platformArray[] = platforms.toArray(new ArduinoPlatform[platforms.size()]);
							Arrays.sort(platformArray);
							return platformArray;
						}
						if (parentElement instanceof ArduinoPlatform) {
							ArduinoPlatform platform = (ArduinoPlatform) parentElement;
							ArduinoPackage parentPackage = platform.getParent();
							ArduinoPlatformPackageIndex parentIndex = parentPackage.getPackageIndex();
							return myShownPlatforms.get(parentIndex.getID()).get(parentPackage.getID())
									.get(platform.getID());
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
						if (element instanceof ArduinoPlatformPackageIndex) {
							return ((ArduinoPlatformPackageIndex) element).getID();

						}
						if (element instanceof ArduinoPackage) {
							String NULL = "NULL"; //$NON-NLS-1$
							ArduinoPackage pkg = (ArduinoPackage) element;
							String maintainer = pkg.getMaintainer();
							String email = pkg.getEmail();
							String weburlString = pkg.getWebsiteURL();
							if (maintainer == null)
								maintainer = NULL;
							if (email == null)
								email = NULL;
							if (weburlString == null)
								weburlString = NULL;

							return Messages.packageTooltip.replace(Messages.MAINTAINER, maintainer)
									.replace(Messages.EMAIL, email).replace(Messages.URL, weburlString);

						}
						if (element instanceof ArduinoPlatformVersion) {
							return ((ArduinoPlatformVersion) element).getConcattenatedBoardNames();

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
						if (cell.getElement() instanceof ArduinoPlatformPackageIndex) {
							cell.setText(((ArduinoPlatformPackageIndex) cell.getElement()).getName());

						}
						if (cell.getElement() instanceof ArduinoPackage) {
							cell.setText(((ArduinoPackage) cell.getElement()).getName());

						}
						if (cell.getElement() instanceof ArduinoPlatform) {
							cell.setText(((ArduinoPlatform) cell.getElement()).getName());

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
		List<ArduinoPlatformVersion> platformsToInstall = new LinkedList<>();
		List<ArduinoPlatformVersion> platformsToRemove = new LinkedList<>();
		for (HashMap<String, HashMap<String, InstallableVersion[]>> packageIndex : myShownPlatforms.values()) {
			for (HashMap<String, InstallableVersion[]> arduinoPackage : packageIndex.values()) {
				for (InstallableVersion[] versions : arduinoPackage.values()) {
					for (InstallableVersion version : versions) {
						if (version.isInstalled() != version.mustBeInstalled()) {
							if (version.mustBeInstalled()) {
								platformsToInstall.add(version.getPlatform());
							} else {
								platformsToRemove.add(version.getPlatform());
							}
						}
					}
				}
			}
		}

		MultiStatus status = new MultiStatus(PLUGIN_ID, 0, Messages.ui_installing_platforms, null);
		BoardsManager.updatePlatforms(platformsToInstall, platformsToRemove, monitor, status);
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
