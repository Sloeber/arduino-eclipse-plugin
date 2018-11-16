package io.sloeber.core.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.common.Common;

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
	public static InputStream openContentStream(String Resource, boolean isFile,Map<String, String> replacers)
			throws CoreException {

		/* We want to be truly OS-agnostic */
		final String newline = System.getProperty("line.separator"); //$NON-NLS-1$

		String line;
		StringBuffer stringBuffer = new StringBuffer();
		InputStream input = null;
		try {

			if (isFile) {
				input = new FileInputStream(Resource);
			} else {
				input = Stream.class.getResourceAsStream(Resource);
			}
			if (input == null) {
				Common.log(new Status(IStatus.ERROR, Activator.getId(),
						"openContentStream: resource " + Resource + " not found.\nThe file will not be processed!")); //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}
			// "templates/index-xhtml-template.resource");

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(input));) {

				while ((line = reader.readLine()) != null) {
					for (Entry<String, String> currentReplace : replacers.entrySet()) {
						line = line.replaceAll(currentReplace.getKey(), currentReplace.getValue());
					}
					stringBuffer.append(line);
					stringBuffer.append(newline);
				}
			}

		} catch (IOException ioe) {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
					e.printStackTrace();
				}
			}
			input = null;
			IStatus status = new Status(IStatus.ERROR, "NewFileWizard", IStatus.OK, ioe.getLocalizedMessage(), null); //$NON-NLS-1$
			Common.log(status);
			throw new CoreException(status);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
					e.printStackTrace();
				}
			}
		}

		return new ByteArrayInputStream(stringBuffer.toString().getBytes());

	}

}
