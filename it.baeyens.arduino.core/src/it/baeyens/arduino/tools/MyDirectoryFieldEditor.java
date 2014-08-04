package it.baeyens.arduino.tools;

//import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.swt.widgets.Composite;

public class MyDirectoryFieldEditor {

    StringButtonFieldEditor theEditor;

    // String mySuffix = "";// the suffix to append to the given path
    //
    public String getStringValue() {
	// if (mySuffix.isEmpty()) {
	return theEditor.getStringValue();
	// }
	// return new Path(theEditor.getStringValue()).append(mySuffix).toString();
    }

    public MyDirectoryFieldEditor(String name, String labelText, Composite parent) {// , String suffix) {
	if (Platform.getOS().equals(Platform.OS_MACOSX)) {
	    theEditor = new FileFieldEditor(name, labelText, parent);
	} else {
	    theEditor = new DirectoryFieldEditor(name, labelText, parent);
	}
	// mySuffix = suffix;
    }

    public FieldEditor getfield() {
	return theEditor;
    }

}
