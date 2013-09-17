package it.baeyens.arduino.tools;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.swt.widgets.Composite;

public class MyDirectoryFieldEditor extends DirectoryFieldEditor {

    String mySuffix = "";// the suffix to append to the given path

    @Override
    public String getStringValue() {
	// TODO Auto-generated method stub
	if (mySuffix.isEmpty()) {
	    return super.getStringValue();
	}
	return new Path(super.getStringValue()).append(mySuffix).toString();
    }

    public MyDirectoryFieldEditor(String name, String labelText, Composite parent, String suffix) {
	super(name, labelText, parent);
	mySuffix = suffix;
    }

}
