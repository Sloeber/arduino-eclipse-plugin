
package io.sloeber.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.Defaults;
import io.sloeber.ui.Messages;

public class TargetSerialDisconnectPage extends PreferencePage implements IWorkbenchPreferencePage {
	private Text targetsText;


	@Override
	public boolean performOk() {
		ConfigurationPreferences.setDisconnectSerialTargets(targetsText.getText());
		return true;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		this.targetsText.setText(Defaults.getDefaultDisconnectSerialTargets());
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout());
		String selectedTargets = ConfigurationPreferences.getDisconnectSerialTargets();

		// Composite control = new Composite(parent, SWT.NONE);
		Label title = new Label(control, SWT.UP);
		title.setFont(control.getFont());

		title.setText(Messages.Add_Targets_To_force_serial_disconnect_when_run);
		title.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		this.targetsText = new Text(control, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);
		GridData gd = new GridData(GridData.FILL_BOTH);
		this.targetsText.setLayoutData(gd);
		this.targetsText.setText(selectedTargets);

		return control;
	}

	@Override
	public void init(IWorkbench arg0) {
		// Nothing to do here

	}

}
