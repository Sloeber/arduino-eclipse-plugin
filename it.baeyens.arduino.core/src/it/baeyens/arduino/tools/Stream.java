package it.baeyens.arduino.tools;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.ui.NewArduinoSketchWizard;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * the Stream class is used to read the board.txt file
 * 
 * @author Trump
 * 
 */
public class Stream {
    /**
     * Initialize the file contents to contents of the given resource.
     */
    public static InputStream openContentStream(String title, String Include, String Resource, boolean isFile) throws CoreException {

	/* We want to be truly OS-agnostic */
	final String newline = System.getProperty("line.separator");

	String line;
	StringBuffer stringBuffer = new StringBuffer();
	InputStream input = null;
	try {

	    if (isFile) {
		input = new FileInputStream(Resource);
	    } else {
		input = NewArduinoSketchWizard.class.getResourceAsStream(Resource);
	    }
	    // "templates/index-xhtml-template.resource");

	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input));) {

		while ((line = reader.readLine()) != null) {
		    line = line.replaceAll("\\{title\\}", title).replaceAll("\\{Include\\}", Include);
		    stringBuffer.append(line);
		    stringBuffer.append(newline);
		}

	    }

	} catch (IOException ioe) {
	    if (input != null) {
		try {
		    input.close();
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	    input = null;
	    IStatus status = new Status(IStatus.ERROR, "NewFileWizard", IStatus.OK, ioe.getLocalizedMessage(), null);
	    Common.log(status);
	    throw new CoreException(status);
	} finally {
	    if (input != null) {
		try {
		    input.close();
		} catch (IOException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	}

	return new ByteArrayInputStream(stringBuffer.toString().getBytes());

    }

}
