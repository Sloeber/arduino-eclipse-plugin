/*******************************************************************************
 * 
 * Copyright (c) 2007, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *	   Manuel Stahl - original idea and some remaining code fragments
 *     Thomas Holland - rewritten to be compatible with Eclipse 3.3 and the rest of the plugin
 *     
 * $Id: MCUselectPage.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.wizards;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.natures.AVRProjectNature;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.properties.ProjectPropertyManager;
import it.baeyens.avreclipse.core.toolinfo.GCC;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.managedbuilder.ui.wizards.MBSCustomPage;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSCustomPageData;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSCustomPageManager;
import org.eclipse.cdt.ui.wizards.CDTCommonProjectWizard;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;


/**
 * New Project Wizard Page to set the default target MCU and its frequency.
 * 
 * <p>
 * This Page takes the possible target MCU types and its default as well as the target MCU frequency
 * default directly from the winAVR toolchain as defined in the <code>plugin.xml</code>.
 * </p>
 * <p>
 * If changed, the new type and MCU frequency are written back to the winAVR toolchain as current
 * value and as default value for this project.
 * </p>
 * 
 * @author Manuel Stahl (thymythos@web.de)
 * @author Thomas Holland (thomas@innot.de)
 * @since 1.0
 */
public class MCUselectPage extends MBSCustomPage implements Runnable {

	
	private final static String			PROPERTY_MCU_NAME	= "mcuname";
	private final static String			PROPERTY_MCU_FREQ	= "mcufreq";

	private Composite					top;

	private Set<String>					fMCUids				= null;
	private String[]					fMCUNames			= null;

	private String						fDefaultMCUName		= null;
	private String						fDefaultFCPU		= null;

	// GUI Widgets
	private Combo						comboMCUtype;
	private Text						textMCUfreq;

	private final AVRProjectProperties	fProperties;

	/**
	 * Constructor for the Wizard Page.
	 * 
	 * <p>
	 * Gets the list of supported MCUs from the compiler and sets the default values.
	 * </p>
	 * 
	 */
	public MCUselectPage() {
		// If the user does not click on "next", this constructor is
		// the only thing called before the "run" method.
		// Therefore we'll set the defaults here. They are set as
		// page properties, as this seems to be the only way to pass
		// values to the run() method.

		this.pageID = ArduinoConst.MCU_SELECT_PAGE_ID;

		fProperties = ProjectPropertyManager.getDefaultProperties();

		// Get the list of supported MCU id's from the compiler
		// The list is then converted into an array of MCU names
		try {
			fMCUids = GCC.getDefault().getMCUList();
		} catch (IOException e) {
			// Could not start avr-gcc. Pop an Error Dialog and continue with an empty list
			IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
					"Could not execute avr-gcc. Please check the AVR paths in the preferences.", e);
			ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					"AVR-GCC Execution fault", null, status);
			fMCUids = new HashSet<String>();
		}
		String[] allmcuids = fMCUids.toArray(new String[fMCUids.size()]);
		fMCUNames = new String[fMCUids.size()];
		for (int i = 0; i < allmcuids.length; i++) {
			fMCUNames[i] = AVRMCUidConverter.id2name(allmcuids[i]);
		}
		Arrays.sort(fMCUNames);

		// get the defaults
		fDefaultMCUName = fProperties.getMCUId();
		fDefaultFCPU = fProperties.getFCPU();

		// Set the default values as page properties
		MBSCustomPageManager.addPageProperty(ArduinoConst.MCU_SELECT_PAGE_ID, PROPERTY_MCU_NAME, fDefaultMCUName);
		MBSCustomPageManager.addPageProperty(ArduinoConst.MCU_SELECT_PAGE_ID, PROPERTY_MCU_FREQ, fDefaultFCPU);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.IWizardPage#getName()
	 */
	@Override
	public String getName() {
		return "AVR Cross Target Hardware Selection Page";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		// some general layout work
		GridData gridData = new GridData(GridData.FILL_BOTH);
		gridData.horizontalAlignment = GridData.END;
		top = new Composite(parent, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(gridData);

		// The MCU Selection Combo Widget
		Label labelMCUtype = new Label(top, SWT.NONE);
		labelMCUtype.setText("MCU Type:");

		comboMCUtype = new Combo(top, SWT.READ_ONLY | SWT.DROP_DOWN);
		comboMCUtype.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		comboMCUtype
				.setToolTipText("Target MCU Type. Can be changed later via the project properties");
		comboMCUtype.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				String value = comboMCUtype.getText();
				MBSCustomPageManager.addPageProperty(ArduinoConst.MCU_SELECT_PAGE_ID, PROPERTY_MCU_NAME, value);
			}
		});
		comboMCUtype.setItems(fMCUNames);
		comboMCUtype.select(comboMCUtype.indexOf(AVRMCUidConverter.id2name(fDefaultMCUName)));

		// The CPU Frequency Selection Text Widget
		Label labelMCUfreq = new Label(top, SWT.NONE);
		labelMCUfreq.setText("MCU Frequency (Hz):");

		textMCUfreq = new Text(top, SWT.BORDER | SWT.SINGLE);
		textMCUfreq.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		textMCUfreq
				.setToolTipText("Target MCU Clock Frequency. Can be changed later via the project properties");
		textMCUfreq.addListener(SWT.FocusOut, new Listener() {
			@Override
			public void handleEvent(Event e) {
				String value = textMCUfreq.getText();
				MBSCustomPageManager.addPageProperty(ArduinoConst.MCU_SELECT_PAGE_ID, PROPERTY_MCU_FREQ, value);
			}
		});
		// filter non-digits from the input
		textMCUfreq.addListener(SWT.Verify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				String text = event.text;
				for (int i = 0; i < text.length(); i++) {
					char ch = text.charAt(i);
					if (!('0' <= ch && ch <= '9')) {
						event.doit = false;
						return;
					}
				}
			}
		});
		textMCUfreq.setText(fDefaultFCPU);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
	 */
	@Override
	public void dispose() {
		top.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getControl()
	 */
	@Override
	public Control getControl() {
		return top;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Define the AVR target properties";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getImage()
	 */
	@Override
	public Image getImage() {
		return wizard.getDefaultPageImage();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getMessage()
	 */
	@Override
	public String getMessage() {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#getTitle()
	 */
	@Override
	public String getTitle() {
		return "AVR Target Hardware Properties";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#performHelp()
	 */
	@Override
	public void performHelp() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(String description) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setImageDescriptor(org.eclipse.jface.resource.ImageDescriptor)
	 */
	@Override
	public void setImageDescriptor(ImageDescriptor image) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(String title) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		top.setVisible(visible);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.ui.wizards.MBSCustomPage#isCustomPageComplete()
	 */
	@Override
	protected boolean isCustomPageComplete() {
		// We only change defaults, so this page is always complete
		return true;
	}

	/**
	 * Operation for the MCUSelectPage.
	 * 
	 * This is called when the finish button of the new Project Wizard has been pressed. It will get
	 * the new Project and set the project options as selected by the user (or to the default
	 * values).
	 * 
	 */
	@Override
	public void run() {

		// At this point the new project has been created and its
		// configuration(s) with their toolchains have been set up.

		// Is there a more elegant way to get to the Project?
		MBSCustomPageData pagedata = MBSCustomPageManager.getPageData(this.pageID);
		CDTCommonProjectWizard wizz = (CDTCommonProjectWizard) pagedata.getWizardPage().getWizard();
		IProject project = wizz.getLastProject();

		ProjectPropertyManager projpropsmanager = ProjectPropertyManager
				.getPropertyManager(project);
		AVRProjectProperties props = projpropsmanager.getProjectProperties();

		// Set the Project properties according to the selected values

		// Get the id of the selected MCU and store it
		String mcuname = (String) MBSCustomPageManager.getPageProperty(ArduinoConst.MCU_SELECT_PAGE_ID, PROPERTY_MCU_NAME);
		String mcuid = AVRMCUidConverter.name2id(mcuname);
		props.setMCUId(mcuid);

		// Set the F_CPU and store it
		String fcpu = (String) MBSCustomPageManager.getPageProperty(ArduinoConst.MCU_SELECT_PAGE_ID, PROPERTY_MCU_FREQ);
		props.setFCPU(fcpu);

		try {
			props.save();
		} catch (BackingStoreException e) {
			IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
					"Could not write project properties to the preferences.", e);

			ErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					"AVR Project Wizard Error", null, status);
		}

		// Add the AVR Nature to the project
		try {
			AVRProjectNature.addAVRNature(project);
		} catch (CoreException ce) {
			// addAVRNature() should not cause an Exception, but just in case we log it.
			IStatus status = new Status(IStatus.ERROR,ArduinoConst.CORE_PLUGIN_ID,
					"Could not add AVR nature to project [" + project.toString() + "]", ce);
			AVRPlugin.getDefault().log(status);
		}

	}
}
