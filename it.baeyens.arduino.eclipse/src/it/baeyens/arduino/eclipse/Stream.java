package it.baeyens.arduino.eclipse;

import it.baeyens.avreclipse.AVRPlugin;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;



public class Stream {
	/**
	 * Initialize the file contents to contents of the given resource.
	 */
	public static InputStream openContentStream(String title, String Resource) throws CoreException {

		/* We want to be truly OS-agnostic */
		final String newline = System.getProperty("line.separator");

		String line;
		StringBuffer stringBuffer = new StringBuffer();

		try {
			InputStream input = NewArduinoSketchWizard.class.getResourceAsStream(Resource);
			// "templates/index-xhtml-template.resource");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			try {

				while ((line = reader.readLine()) != null) {
					line = line.replaceAll("\\{title\\}", title);
					stringBuffer.append(line);
					stringBuffer.append(newline);
				}

			} finally {
				reader.close();
			}

		} catch (IOException ioe) {
			IStatus status = new Status(IStatus.ERROR, "NewFileWizard", IStatus.OK, ioe.getLocalizedMessage(), null);
			AVRPlugin.getDefault().log(status);
			throw new CoreException(status);
		}

		return new ByteArrayInputStream(stringBuffer.toString().getBytes());

	}

}
