package io.sloeber.core.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.tools.FileModifiers;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Stream;

/**
 * A class to describe the code that needs to be attached to the project
 *
 * @author jan
 *
 */

public class CodeDescriptor {
	public enum CodeTypes {
		defaultIno, defaultCPP, CustomTemplate, sample
	}

	static public final String DEFAULT_SKETCH_BASE = "sketch"; //$NON-NLS-1$
	static public final String DEFAULT_SKETCH_INO = DEFAULT_SKETCH_BASE + ".ino"; //$NON-NLS-1$
	static public final String DEFAULT_SKETCH_CPP = DEFAULT_SKETCH_BASE + ".cpp"; //$NON-NLS-1$
	static public final String DEFAULT_SKETCH_H = DEFAULT_SKETCH_BASE + ".h"; //$NON-NLS-1$
	//
	// template Sketch information

	public static final String ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER = Const.ENV_KEY_JANTJE_START + "TEMPLATE_FOLDER"; //$NON-NLS-1$
	public static final String ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT = Const.ENV_KEY_JANTJE_START
			+ "TEMPLATE_USE_DEFAULT"; //$NON-NLS-1$

	private CodeTypes myCodeType;
	private IPath myTemPlateFoldername;
	private boolean myMakeLinks = false;
	private ArrayList<IPath> myExamples = new ArrayList<>();
	private Map< String,  String> myReplacers=null;

	public IPath getTemPlateFoldername() {
		return myTemPlateFoldername;
	}

	/**
	 * set key value pairs that will be used to do a replace all in the code
	 * the key is the search key and the value is the replacement
	 * The keys are regular expressions.
	 * The idea is to use clear keys like #include {include} in the source code
	 * note that the keys 
	 * \{include\} \{title\} \{SerialMonitorSerial\}
	 * are already used. Please use other keys as your replacements will overwrite the
	 * ones already used
	 * 
	 * @param myReplacers a list of find an replace key value pairs. null is no replace needed
	 */
	public void setReplacers(Map<String, String> myReplacers) {
		this.myReplacers = myReplacers;
	}

	private CodeDescriptor(CodeTypes codeType) {
		myCodeType = codeType;
	}

	public static CodeDescriptor createDefaultIno() {
		return new CodeDescriptor(CodeTypes.defaultIno);
	}

	public static CodeDescriptor createDefaultCPP() {
		return new CodeDescriptor(CodeTypes.defaultCPP);
	}

	public static CodeDescriptor createExample(boolean link, ArrayList<IPath> sampleFolders) {
		CodeDescriptor codeDescriptor = new CodeDescriptor(CodeTypes.sample);
		codeDescriptor.myMakeLinks = link;
		codeDescriptor.myExamples = sampleFolders;
		return codeDescriptor;
	}

	public static CodeDescriptor createCustomTemplate(IPath temPlateFoldername) {
		CodeDescriptor codeDescriptor = new CodeDescriptor(CodeTypes.CustomTemplate);
		codeDescriptor.myTemPlateFoldername = temPlateFoldername;
		return codeDescriptor;
	}

	public static CodeDescriptor createLastUsed() {

		String typeDescriptor = InstancePreferences.getString(ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT,
				new String());
		CodeTypes codeType = codeTypeFromDescription(typeDescriptor);
		CodeDescriptor ret = new CodeDescriptor(codeType);
		ret.myTemPlateFoldername = new Path(
				InstancePreferences.getString(ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER, new String()));
		ret.loadLastUsedExamples();
		return ret;
	}

	private static CodeTypes codeTypeFromDescription(String typeDescriptor) {

		for (CodeTypes codeType : CodeTypes.values()) {
			if (codeType.toString().equals(typeDescriptor)) {
				return codeType;
			}
		}
		return CodeTypes.defaultIno;

	}

	/**
	 * Save the setting in the last used
	 */
	public void save() {
		if (myTemPlateFoldername != null) {
			InstancePreferences.setGlobalValue(ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER,
					myTemPlateFoldername.toString());
		}
		InstancePreferences.setGlobalValue(ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT, myCodeType.toString());
		saveLastUsedExamples();
	}

	/**
	 * given the source descriptor, add the sources to the project returns a set
	 * of libraries that need to be installed
	 **/
	@SuppressWarnings("nls")
	public Set<String> createFiles(IProject project, IProgressMonitor monitor) throws CoreException {
		Set<String> libraries = new TreeSet<>();

		save();
		Map<String, String> replacers=new TreeMap<>();
		replacers.put("\\{Include\\}", "Arduino.h");
		replacers.put("\\{title\\}",project.getName());
		if(myReplacers!=null) {
		  replacers.putAll(myReplacers);
		}


		switch (myCodeType) {
		case defaultIno:
			Helpers.addFileToProject(project, new Path(project.getName() + ".ino"),
					Stream.openContentStream("/io/sloeber/core/templates/" + DEFAULT_SKETCH_INO, false,replacers),
					monitor, false);
			break;
		case defaultCPP:
			Helpers.addFileToProject(project, new Path(project.getName() + ".cpp"),
					Stream.openContentStream("/io/sloeber/core/templates/" + DEFAULT_SKETCH_CPP, false,replacers),
					monitor, false);
			Helpers.addFileToProject(project, new Path(project.getName() + ".h"),
					Stream.openContentStream("/io/sloeber/core/templates/" + DEFAULT_SKETCH_H, false,replacers),
					monitor, false);
			break;
		case CustomTemplate:
			IPath folderName = myTemPlateFoldername;
			String files[] = folderName.toFile().list();
			if (files == null) {
				Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
						"No files found in template folder :" + folderName, null));
			} else {
				for (String file : files) {
					if (!(file.equals(".") || file.equals(".."))) {
						File sourceFile = folderName.append(file).toFile();
						String renamedFile = file;
						if (DEFAULT_SKETCH_INO.equalsIgnoreCase(file)) {
							renamedFile = project.getName() + ".ino";
						}
						Helpers.addFileToProject(project, new Path(renamedFile),
								Stream.openContentStream( sourceFile.toString(), true,replacers),
								monitor, false);
					}
				}
			}

			break;
		case sample:
			try {
				for (IPath curPath : myExamples) {
					if (myMakeLinks) {
						Helpers.linkDirectory(project, curPath, new Path("/")); //$NON-NLS-1$
					} else {
						FileUtils.copyDirectory(curPath.toFile(), project.getLocation().toFile());
						FileModifiers.addPragmaOnce(curPath);
					}
					libraries.add(getLibraryName(curPath));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}
		return libraries;
	}

	@SuppressWarnings("nls")
	private void loadLastUsedExamples() {
		String examplePathNames[] = InstancePreferences
				.getString(Const.KEY_LAST_USED_EXAMPLES, Defaults.getPrivateLibraryPath()).split("\n");

		for (String curpath : examplePathNames) {
			myExamples.add(new Path(curpath));
		}
	}

	public ArrayList<IPath> getExamples() {
		return myExamples;
	}

	private void saveLastUsedExamples() {
		if (myExamples != null) {
			String toStore = StringUtils.join(myExamples, "\n"); //$NON-NLS-1$
			InstancePreferences.setGlobalValue(Const.KEY_LAST_USED_EXAMPLES, toStore);
		} else {
			InstancePreferences.setGlobalValue(Const.KEY_LAST_USED_EXAMPLES, new String());
		}

	}

	public CodeTypes getCodeType() {
		return myCodeType;
	}

	/**
	 * Get the name of the example for this project descriptor This is only
	 * "known in case of examples" as in all other cases the name will be
	 * project related which is unknown in this case. This method only exists to
	 * support unit testing where one knows only 1 example is selected
	 *
	 * @return the name of the first selected example in case of sample in all
	 *         other cases null
	 */
	public String getExampleName() {
		switch (myCodeType) {
		case sample:
			return myExamples.get(0).lastSegment();
		default:
			break;
		}
		return null;
	}

	/**
	 * Get the name of the library for this project descriptor This is only
	 * "known in case of examples" as in all other cases the name will be
	 * project related which is unknown in this case. This method only exists to
	 * support unit testing where one knows only 1 example is selected
	 *
	 * @return the name of the first selected example in case of sample in all
	 *         other cases null
	 */
	public String getLibraryName() {
		switch (myCodeType) {
		case sample:
			return getLibraryName(myExamples.get(0));
		default:
			break;
		}
		return null;
	}

	@SuppressWarnings("nls")
	public static String getLibraryName(IPath examplePath) {
		if ("libraries".equalsIgnoreCase(examplePath.removeLastSegments(4).lastSegment())) {
			return examplePath.removeLastSegments(3).lastSegment();
		}
		if ("libraries".equalsIgnoreCase(examplePath.removeLastSegments(5).lastSegment())) {
			return examplePath.removeLastSegments(4).lastSegment();
		}
		return examplePath.removeLastSegments(2).lastSegment();
	}

	public boolean isLinkedExample() {
		
		return (myCodeType==CodeTypes.sample)&&myMakeLinks;
	}
	/**
	 * returns the path of the first linked example.
	 * This method is used to add a include folders to the FIRST linked example
	 * You could argue that ALL linked examples should be in the
	 * include path ...
	 * I don't want to support this use case because if you want to do 
	 * so you should know what you are doing and you don't need this
	 * 
	 * and if you do need this; do it yourself or become a serious patron
	 * 
	 * @return the path to the first linked example or NULL
	 */
	public IPath getLinkedExamplePath()
	{
		if(!isLinkedExample())return null;
		return myExamples.get(0);
	}
}
