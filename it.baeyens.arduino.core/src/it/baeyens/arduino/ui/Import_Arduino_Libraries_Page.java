package it.baeyens.arduino.ui;

import java.io.File;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Import_Arduino_Library_Page is the one and only page in the library import
 * wizard. It controls a text field and a browse button.
 * 
 * @author Jan Baeyens
 * 
 */
public class Import_Arduino_Libraries_Page extends WizardPage
	{
		private Tree LibrarySelector;
		TreeItem ArduinoLibItem;
		TreeItem PersonalLibItem;

		protected Import_Arduino_Libraries_Page(String name, IStructuredSelection selection)
			{
				super("wizardPage");
				setTitle("Import arduino libraries");
				setDescription("Use this page to select the libraries to import");
			}

		@Override
		public void createControl(Composite parent)
			{
				Composite composite = new Composite(parent, SWT.NONE);
				GridLayout theGridLayout = new GridLayout();
				GridData theGriddata;
				theGridLayout.numColumns = 1;
				composite.setLayout(theGridLayout);
				composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
				composite.setFont(parent.getFont());

				LibrarySelector = new Tree(composite, SWT.CHECK | SWT.BORDER);
				theGriddata = new GridData(GridData.FILL_BOTH);
				LibrarySelector.setLayoutData(theGriddata);
				// Get the data in the tree
				LibrarySelector.setRedraw(false);

				// Create Arduino Item
				ArduinoLibItem = new TreeItem(LibrarySelector, SWT.NONE);
				ArduinoLibItem.setText("Arduino Libs");
				// Add the arduino Libs
				AddLibs(ArduinoLibItem, ArduinoInstancePreferences.getArduinoLibraryPath());
				// Create Personal library Item
				PersonalLibItem = new TreeItem(LibrarySelector, SWT.NONE);
				PersonalLibItem.setText("personal Libs");
				// Add the personal Libs
				AddLibs(PersonalLibItem, ArduinoInstancePreferences.getPrivateLibraryPath());
				ArduinoLibItem.setExpanded(true);
				PersonalLibItem.setExpanded(true);

				LibrarySelector.addListener(SWT.Selection, new Listener()
					{
						@Override
						public void handleEvent(Event event)
							{
								if (event.detail == SWT.CHECK)
									{
										if ((event.item.equals(PersonalLibItem)) | (event.item.equals(ArduinoLibItem)))
											{
												event.detail = SWT.NONE;
												event.type = SWT.None;
												event.doit = false;
												try
													{
														LibrarySelector.setRedraw(false);
														PersonalLibItem.setChecked(false);
														ArduinoLibItem.setChecked(false);
													} finally
													{
														LibrarySelector.setRedraw(true);
													}
											}
									}
							}

					});
				// Turn drawing back on!
				LibrarySelector.setRedraw(true);
				setControl(composite);
			}

		private void AddLibs(TreeItem LibItem, IPath iPath)
			{
				File LibRoot = iPath.toFile();
				File LibFolder;
				String[] children = LibRoot.list();
				if (children == null)
					{
						// Either dir does not exist or is not a directory
					} else
					{
						for (int i = 0; i < children.length; i++)
							{
								// Get filename of file or directory
								LibFolder = iPath.append(children[i]).toFile();
								if (LibFolder.isDirectory())
									{
										TreeItem child = new TreeItem(LibItem, SWT.NONE);
										child.setText(children[i]);
									}
							}
					}
			}

		public boolean canFinish()
			{
				TreeItem[] AllItems = ArduinoLibItem.getItems();
				for (int CurItem = 0; CurItem < AllItems.length; CurItem++)
					{
						if (AllItems[CurItem].getChecked())
							return true;
					}
				AllItems = PersonalLibItem.getItems();// .get();// .getItems();
				for (int CurItem = 0; CurItem < AllItems.length; CurItem++)
					{
						if (AllItems[CurItem].getChecked())
							return true;
					}
				return false;
			}

		public boolean PerformFinish(IProject project)
			{
				boolean ret = true;
				TreeItem[] AllItems = ArduinoLibItem.getItems();
				for (int CurItem = 0; CurItem < AllItems.length; CurItem++)
					{
						if (AllItems[CurItem].getChecked())
							{
								try
									{
										ArduinoHelpers.addCodeFolder(project, ArduinoConst.PATH_VARIABLE_NAME_ARDUINO_LIB, AllItems[CurItem].getText());
									} catch (CoreException e)
									{
										e.printStackTrace();
										IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to import library ", e);
										Common.log(status);
										ret = false;
									}
							}
					}
				AllItems = PersonalLibItem.getItems();// .get();// .getItems();
				for (int CurItem = 0; CurItem < AllItems.length; CurItem++)
					{
						if (AllItems[CurItem].getChecked())
							{
								try
									{
										ArduinoHelpers.addCodeFolder(project, ArduinoConst.PATH_VARIABLE_NAME_PRIVATE_LIB, AllItems[CurItem].getText());
									} catch (CoreException e)
									{
										e.printStackTrace();
										IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to import library ", e);
										Common.log(status);
										ret = false;
									}
							}
					}
				return ret;
			}

	}
