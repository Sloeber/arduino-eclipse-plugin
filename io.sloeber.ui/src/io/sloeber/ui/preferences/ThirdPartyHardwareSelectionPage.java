
package io.sloeber.ui.preferences;

import org.eclipse.cdt.core.parser.util.StringUtil;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;

public class ThirdPartyHardwareSelectionPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private static final String KEY_UPDATE_JASONS = "Update jsons files"; //$NON-NLS-1$
	private Text urlsText;
	BooleanFieldEditor upDateJsons;

	public ThirdPartyHardwareSelectionPage() {
		super(org.eclipse.jface.preference.FieldEditorPreferencePage.GRID);
		setDescription(Messages.json_maintain);
		setPreferenceStore(new ScopedPreferenceStore(ConfigurationScope.INSTANCE, MyPreferences.NODE_ARDUINO));
	}

	@Override
	public boolean performOk() {
		PackageManager.setJsonURLs(this.urlsText.getText().split(System.lineSeparator()));
		Preferences.setUpdateJsonFiles(this.upDateJsons.getBooleanValue());
		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		this.urlsText.setText(PackageManager.getDefaultURLs());	
	}

	@Override
	protected void createFieldEditors() {
		String selectedJsons[] = PackageManager.getJsonURLList();
		final Composite parent = getFieldEditorParent();
		// Composite control = new Composite(parent, SWT.NONE);
		Label title = new Label(parent, SWT.UP);
		title.setFont(parent.getFont());

		title.setText(Messages.ui_url_for_index_file);
		title.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		this.urlsText = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);
		GridData gd = new GridData(GridData.FILL_BOTH);
		this.urlsText.setLayoutData(gd);
		this.urlsText.setText(StringUtil.join(selectedJsons, System.lineSeparator()));

		this.upDateJsons = new BooleanFieldEditor(KEY_UPDATE_JASONS, Messages.json_update, BooleanFieldEditor.DEFAULT,
				parent);
		addField(this.upDateJsons);
		IPreferenceStore prefstore=getPreferenceStore();
		prefstore.setValue(KEY_UPDATE_JASONS, Preferences.getUpdateJsonFiles());
		prefstore.setDefault(KEY_UPDATE_JASONS, true);
		
		final Hyperlink link = new Hyperlink(parent, SWT.NONE);
		link.setText(Messages.json_find);
		link.setHref("https://github.com/arduino/Arduino/wiki/Unofficial-list-of-3rd-party-boards-support-urls"); //$NON-NLS-1$
		link.setUnderlined(true);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent he) {
				try {
					org.eclipse.swt.program.Program.launch(link.getHref().toString());
				} catch (IllegalArgumentException e) {
					Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.json_browser_fail, e));
				}
			}
		});

	}

	@Override
	public void init(IWorkbench arg0) {
		// Nothing to do here

	}

}
