package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoBoards;
import it.baeyens.arduino.tools.ArduinoHelpers;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import java.util.ArrayList;

/**
 * This class is linked to page in the import wizard.
 * 
 * @author Brody Kenrick
 * Based on code in ArduinoSettingsPage by Jan Baeyens
 * 
 */
@SuppressWarnings("unused")
public class BuildConfigurationsPage extends WizardPage implements IWizardPage {
	private final int ncol = 2;
	
	final Shell shell = new Shell();
	private Button button_BldCfg_AVaRICE    = null;

	private Listener completeListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			setPageComplete(true);
		}
	};

	public BuildConfigurationsPage(String pageName) {
		super(pageName);
		setPageComplete(true);
	}

	public BuildConfigurationsPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
		setPageComplete(true);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);

		draw(composite);

		setControl(composite);

		setPageComplete(true);
	}

	private static void createLabel(Composite parent, int ncol, String t) {
		Label line = new Label(parent, SWT.HORIZONTAL | SWT.BOLD);
		line.setText(t);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	    }
	
	private static void createLine(Composite parent, int ncol) {
		Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	    }
	
	public void draw(Composite composite) {

		// Create the desired layout for this wizard page
		GridLayout theGridLayout = new GridLayout();
		theGridLayout.numColumns = ncol;
		composite.setLayout(theGridLayout);

		GridData theGriddata;
		
		createLabel(composite, ncol, "Leave these alone if you don't know you want them.");
		createLabel(composite, ncol, "The default configuration that mimics the Arduino IDE (named Release) will always be created.");
		
		//Checkbox for an additional Build Configuration -- AVaRICE
		createLine(composite, ncol);
		createLabel(composite, ncol, "AVaRICE");
		button_BldCfg_AVaRICE = new Button(composite, SWT.CHECK);
		button_BldCfg_AVaRICE.setText("Create a " + "Debug_AVaRICE" + " build configuration.");
		button_BldCfg_AVaRICE.setVisible(true);
		createLabel(composite, ncol, "For creating binaries ready to be debugged with AVaRICE (e.g. using Dragon AVR).");

		// Add any additional configurations -- below here


		// Add any additional configurations -- above here

		Dialog.applyDialogFont(composite);
	}
	
	/**
	 * A "struct" for the configs - names and tool chain IDs.
	 * 
	 * @author Brody Kenrick
	 * 
	 */
	public class ConfigurationDescriptor
	{
	    public final String Name;
	    public final String ToolchainID;
	    public final boolean DebugCompilerSettings;

	    public ConfigurationDescriptor( String Name, String ToolchainID, boolean DebugCompilerSettings){
	        this.Name                  = Name;
	        this.ToolchainID           = ToolchainID;
	        this.DebugCompilerSettings = DebugCompilerSettings;
	      }
	}
	
	public ArrayList<ConfigurationDescriptor> getBuildConfigurationDescriptors() {
		// TODO: Consider renaming Release to ArduinoIDEConfig
		// JABA:I don't think his is a good idea "standard" or "arduino" may be better
	    // Note that changing Release invalidates all existing workspaces. So if we change this timing will be very important.
		ArrayList<ConfigurationDescriptor> alCfgs = new ArrayList<ConfigurationDescriptor>();
		
		ConfigurationDescriptor cfgTCidPair = new ConfigurationDescriptor("Release","it.baeyens.arduino.core.toolChain.release",false);
		alCfgs.add( cfgTCidPair ); //Always have the release build here
		
		//If this button is selected (checked) we add the configuration
		if(button_BldCfg_AVaRICE.getSelection())
		{
			// Debug has same toolchain as release
			ConfigurationDescriptor cfgTCidPair2 = new ConfigurationDescriptor("Debug_AVaRICE", "it.baeyens.arduino.core.toolChain.release",true);
			alCfgs.add( cfgTCidPair2 );
		}

		return alCfgs;
	}


}
