package io.sloeber.ui;

import org.eclipse.cdt.core.parser.util.StringUtil;
import org.eclipse.cdt.ui.newui.MultiLineTextFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;


/**
 * A class to change MultiLineTextFieldEditor so it puts the label on top of the
 * text field and the text field expands bot horizontal and verical
 * Note I needed a FLAT parent FieldEditorPreferencePage (GRID did not work)
 * 	public ThirdPartyHardwareSelectionPage() {
 *		super(org.eclipse.jface.preference.FieldEditorPreferencePage.FLAT);
 */
public class JsonMultiLineTextFieldEditor extends MultiLineTextFieldEditor{

	public JsonMultiLineTextFieldEditor(String name, String labelText, int width, int strategy, Composite parent)  {
		super( name,  labelText,  width,strategy,  parent);
	}

	/**
	 * I want 1 column
	 */
	@Override
	public int getNumberOfControls() {
		return 1;
	}


	/**
	 * I want a GridData that has horizontal fill
	 */
	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		super.doFillIntoGrid( parent,  numColumns) ;

		Text textField = getTextControl(parent);
		textField.setLayoutData( new GridData(SWT.FILL,SWT.BEGINNING,true,false));

	}

	public void setText(String[] text) {
		String actualText=StringUtil.join(text, System.lineSeparator());
		getPreferenceStore().setValue(getPreferenceName(), actualText);
		setStringValue(actualText );
	}

}
