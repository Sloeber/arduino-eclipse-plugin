
package io.sloeber.ui.preferences;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import io.sloeber.core.api.Preferences;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;

public class TargetSerialDisconnectPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private static final String KEY_SERIAL_DISCONNECT_TARGETS = "Targets to disconnect Serial when build"; //$NON-NLS-1$
	private Text targetsText;

	public TargetSerialDisconnectPage() {
		super(org.eclipse.jface.preference.FieldEditorPreferencePage.GRID);
		setDescription(Messages.json_maintain);
		setPreferenceStore(new ScopedPreferenceStore(ConfigurationScope.INSTANCE, MyPreferences.NODE_ARDUINO));
	}

	@Override
	public boolean performOk() {
		Preferences.setDisconnectSerialTargets(targetsText.getText());
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		this.targetsText.setText(Preferences.getDefaultDisconnectSerialTargets());
	}

	@Override
	protected void createFieldEditors() {
		String selectedTargets = Preferences.getDisconnectSerialTargets();
		final Composite parent = getFieldEditorParent();
		// Composite control = new Composite(parent, SWT.NONE);
		Label title = new Label(parent, SWT.UP);
		title.setFont(parent.getFont());

		title.setText(Messages.ui_url_for_index_file);
		title.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		this.targetsText = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);
		GridData gd = new GridData(GridData.FILL_BOTH);
		this.targetsText.setLayoutData(gd);
		this.targetsText.setText(selectedTargets);

		IPreferenceStore prefstore=getPreferenceStore();
		prefstore.setValue(KEY_SERIAL_DISCONNECT_TARGETS, selectedTargets);
//		prefstore.setDefault(KEY_SERIAL_DISCONNECT_TARGETS, true);

	}

	@Override
	public void init(IWorkbench arg0) {
		// Nothing to do here

	}

}
