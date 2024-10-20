
package io.sloeber.ui.preferences;

import static io.sloeber.ui.Activator.*;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.cdt.ui.newui.MultiLineTextFieldEditor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import io.sloeber.arduinoFramework.api.BoardsManager;
import io.sloeber.ui.JsonMultiLineTextFieldEditor;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;

public class ThirdPartyHardwareSelectionPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	@Override
	protected void initialize() {
		super.initialize();
		urlsText.setText(BoardsManager.getJsonURLList());
	}

	private static final String KEY_JSONS = "Jsons files"; //$NON-NLS-1$
	private JsonMultiLineTextFieldEditor urlsText;

	public ThirdPartyHardwareSelectionPage() {
		super(org.eclipse.jface.preference.FieldEditorPreferencePage.FLAT);
		setDescription(Messages.json_maintain);
		setPreferenceStore(new ScopedPreferenceStore(ConfigurationScope.INSTANCE, MyPreferences.NODE_ARDUINO));
	}

	@Override
	public boolean performOk() {
		HashSet<String> toSetList = new HashSet<>(Arrays.asList(urlsText.getStringValue().split(System.lineSeparator())));
		BoardsManager.setPackageURLs(toSetList);
		BoardsManager.update(false);

		return super.performOk();
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		urlsText.setText(BoardsManager.getDefaultJsonURLs());
	}

	@Override
	protected void createFieldEditors() {
		final Composite parent = getFieldEditorParent();
		GridData gd = new GridData(GridData.FILL,GridData.BEGINNING,true,false);
		parent.setLayoutData(gd);

		urlsText =new JsonMultiLineTextFieldEditor(KEY_JSONS,Messages.ui_url_for_index_file,MultiLineTextFieldEditor.UNLIMITED,MultiLineTextFieldEditor.VALIDATE_ON_KEY_STROKE,parent);
		addField(urlsText);


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
					log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.json_browser_fail, e));
				}
			}
		});

	}

	@Override
	public void init(IWorkbench arg0) {
		// Nothing to do here

	}

}
