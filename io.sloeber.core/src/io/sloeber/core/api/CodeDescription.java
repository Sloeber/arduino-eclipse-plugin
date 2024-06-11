package io.sloeber.core.api;

import static io.sloeber.core.api.Common.*;
import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.internal.Example;
import io.sloeber.core.tools.FileModifiers;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Stream;

/**
 * A class to describe the code that needs to be attached to the project
 *
 * @author jan
 *
 */

public class CodeDescription implements ICodeProvider {
	public enum CodeTypes {
		None, defaultIno, defaultCPP, CustomTemplate, sample
	}

	static private final String DEFAULT_SKETCH_BASE = "sketch"; //$NON-NLS-1$
	public static final String DEFAULT_SKETCH_INO = DEFAULT_SKETCH_BASE + ".ino"; //$NON-NLS-1$
	public static final String DEFAULT_SKETCH_CPP = DEFAULT_SKETCH_BASE + ".cpp"; //$NON-NLS-1$
	public static final String DEFAULT_SKETCH_H = DEFAULT_SKETCH_BASE + ".h"; //$NON-NLS-1$
	//
	// template Sketch information

	static private final String SLOEBER_SKETCH_TEMPLATE_FOLDER = ENV_KEY_SLOEBER_START + "TEMPLATE_FOLDER"; //$NON-NLS-1$
	static private final String SLOEBER_SKETCH_TEMPLATE_USE_DEFAULT = ENV_KEY_SLOEBER_START + "TEMPLATE_USE_DEFAULT"; //$NON-NLS-1$

	private CodeTypes myCodeType;
	private IPath myTemPlateFoldername;
	private boolean myMakeLinks = false;
	private Set<IExample> myExamples = new HashSet<>();
	private Map<String, String> myReplacers = null;
	private String myCodeFolder ="src"; //$NON-NLS-1$

	public IPath getTemPlateFoldername() {
		return myTemPlateFoldername;
	}

	/**
	 * set key value pairs that will be used to do a replace all in the code the key
	 * is the search key and the value is the replacement The keys are regular
	 * expressions. The idea is to use clear keys like #include {include} in the
	 * source code note that the keys \{include\} \{title\} \{SerialMonitorSerial\}
	 * are already used. Please use other keys as your replacements will overwrite
	 * the ones already used
	 *
	 * @param myReplacers a list of find an replace key value pairs. null is no
	 *                    replace needed
	 */
	public void setReplacers(Map<String, String> myReplacers) {
		this.myReplacers = myReplacers;
	}

	public CodeDescription(CodeTypes codeType) {
		myCodeType = codeType;
	}

	public static CodeDescription createNone() {
		return new CodeDescription(CodeTypes.None);
	}

	public static CodeDescription createDefaultIno() {
		return new CodeDescription(CodeTypes.defaultIno);
	}

	public static CodeDescription createDefaultCPP() {
		return new CodeDescription(CodeTypes.defaultCPP);
	}

	public static CodeDescription createExample(boolean link, Set<IExample> examples) {
		CodeDescription codeDescriptor = new CodeDescription(CodeTypes.sample);
		codeDescriptor.myMakeLinks = link;
		codeDescriptor.myExamples.addAll(examples ) ;
		return codeDescriptor;
	}

	public static CodeDescription createCustomTemplate(IPath temPlateFoldername) {
		CodeDescription codeDescriptor = new CodeDescription(CodeTypes.CustomTemplate);
		codeDescriptor.myTemPlateFoldername = temPlateFoldername;
		return codeDescriptor;
	}

	public static CodeDescription createLastUsed(BoardDescription boardDescription) {

		String typeDescriptor = InstancePreferences.getString(SLOEBER_SKETCH_TEMPLATE_USE_DEFAULT, new String());
		CodeTypes codeType = codeTypeFromDescription(typeDescriptor);
		CodeDescription ret = new CodeDescription(codeType);
		ret.myTemPlateFoldername = new Path(
				InstancePreferences.getString(SLOEBER_SKETCH_TEMPLATE_FOLDER, new String()));
		ret.loadLastUsedExamples(boardDescription);
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
	private void save() {
		if (myTemPlateFoldername != null) {
			InstancePreferences.setGlobalValue(SLOEBER_SKETCH_TEMPLATE_FOLDER, myTemPlateFoldername.toString());
		}
		InstancePreferences.setGlobalValue(SLOEBER_SKETCH_TEMPLATE_USE_DEFAULT, myCodeType.toString());
		saveLastUsedExamples();
	}

	/**
	 * given the source descriptor, add the sources to the project returns a set of
	 * libraries that need to be installed
	 **/
	Set<IArduinoLibraryVersion> getNeededLibraries() {
		Set<IArduinoLibraryVersion> libraries = new HashSet<>();

		if (myCodeType == CodeTypes.sample) {
			for (IExample curExample : myExamples) {
				libraries.add(curExample.getArduinoLibrary());
			}
		}
		libraries.remove(null);
		return libraries;
	}

	@SuppressWarnings("nls")
	private void loadLastUsedExamples(BoardDescription boardDescription) {
		String examplePathNames[] = InstancePreferences
				.getString(KEY_LAST_USED_EXAMPLES, Defaults.getPrivateLibraryPath()).split("\n");

		Map<String, IExample> allExamples =LibraryManager.getExamplesAll(boardDescription);
			for(IExample curExample:allExamples.values()) {
				String saveString=curExample.toSaveString();
				for (String curSaveString : examplePathNames) {
					if(saveString.equals(curSaveString)) {
						myExamples.add(curExample);
					}
			}
		}
	}

	public Set<IExample> getExamples() {
		return new HashSet<>(myExamples);
	}

	private void saveLastUsedExamples() {
		String toStore =new String();
		if (myExamples != null &&myExamples.size()>0) {
			Set<Example> examples=new HashSet<>();
			for(IExample curExample:myExamples) {
				if(curExample instanceof Example) {
					examples.add((Example)curExample);
				}
			}
			if(examples.size()>0) {
			toStore = examples.stream().map(Example::toSaveString).collect(Collectors.joining(NEWLINE));

			}
		}
		InstancePreferences.setGlobalValue(KEY_LAST_USED_EXAMPLES, toStore);

	}

	public CodeTypes getCodeType() {
		return myCodeType;
	}

	/**
	 * Get the name of the example for this project descriptor This is only "known
	 * in case of examples" as in all other cases the name will be project related
	 * which is unknown in this case. This method only exists to support unit
	 * testing where one knows only 1 example is selected
	 *
	 * @return the name of the first selected example in case of sample in all other
	 *         cases null
	 */
	public String getExampleName() {
		switch (myCodeType) {
		case sample:
			IExample example=myExamples.iterator().next();
			return example.getCodeLocation().lastSegment();
		default:
			break;
		}
		return null;
	}

	/**
	 * Get the name of the library for this project descriptor This is only "known
	 * in case of examples" as in all other cases the name will be project related
	 * which is unknown in this case. This method only exists to support unit
	 * testing where one knows only 1 example is selected
	 *
	 * @return the name of the first selected example in case of sample in all other
	 *         cases null
	 */
	public String getLibraryName() {
		switch (myCodeType) {
		case sample:
			IExample example=myExamples.iterator().next();
			return example.getArduinoLibrary().getName();
		default:
			break;
		}
		return null;
	}

//	@SuppressWarnings("nls")
//	private static String getLibraryName(IPath examplePath) {
//		if (ConfigurationPreferences.getInstallationPathExamples().isPrefixOf(examplePath)) {
//			return null;
//		}
//		if ("libraries".equalsIgnoreCase(examplePath.removeLastSegments(4).lastSegment())) {
//			return examplePath.removeLastSegments(3).lastSegment();
//		}
//		if ("libraries".equalsIgnoreCase(examplePath.removeLastSegments(5).lastSegment())) {
//			return examplePath.removeLastSegments(4).lastSegment();
//		}
//		return examplePath.removeLastSegments(2).lastSegment();
//	}

	public boolean isLinkedExample() {

		return (myCodeType == CodeTypes.sample) && myMakeLinks;
	}

	/**
	 * returns the path of the first linked example. This method is used to add a
	 * include folders to the FIRST linked example You could argue that ALL linked
	 * examples should be in the include path ... I don't want to support this use
	 * case because if you want to do so you should know what you are doing and you
	 * don't need this
	 *
	 *
	 * @return the path to the first linked example or NULL
	 */
	public IExample getLinkedExample() {
		if (!isLinkedExample()||myExamples.size()==0)
			return null;
		return myExamples.iterator().next();
	}

	@SuppressWarnings("nls")
	@Override
	public boolean createFiles(IContainer scrContainer, IProgressMonitor monitor) {
		try {
			IProject project = scrContainer.getProject();

			save();
			Map<String, String> replacers = new TreeMap<>();
			replacers.put("{Include}", "Arduino.h");
			replacers.put("{title}", project.getName());
			if (myReplacers != null) {
				replacers.putAll(myReplacers);
			}

			switch (myCodeType) {
			case None:
				break;
			case defaultIno:
				Helpers.addFileToProject(scrContainer.getFile(IPath.fromOSString( project.getName() + ".ino")),
						Stream.openContentStream("/io/sloeber/core/templates/" + DEFAULT_SKETCH_INO, false, replacers),
						monitor, false);
				break;
			case defaultCPP:
				Helpers.addFileToProject(scrContainer.getFile(IPath.fromOSString(project.getName() + ".cpp")),
						Stream.openContentStream("/io/sloeber/core/templates/" + DEFAULT_SKETCH_CPP, false, replacers),
						monitor, false);
				Helpers.addFileToProject(scrContainer.getFile(IPath.fromOSString(project.getName() + ".h")),
						Stream.openContentStream("/io/sloeber/core/templates/" + DEFAULT_SKETCH_H, false, replacers),
						monitor, false);
				break;
			case CustomTemplate:
				IPath folderName = myTemPlateFoldername;
				String files[] = folderName.toFile().list();
				if (files == null) {
					log(new Status(IStatus.WARNING, CORE_PLUGIN_ID, "No files found in template folder :" + folderName,
							null));
				} else {
					for (String file : files) {
						if (!(file.equals(".") || file.equals(".."))) {
							File sourceFile = folderName.append(file).toFile();
							String renamedFile = file;
							if (DEFAULT_SKETCH_INO.equalsIgnoreCase(file)) {
								renamedFile = project.getName() + ".ino";
							}
							try (InputStream theFileStream = Stream.openContentStream(sourceFile.toString(), true,
									replacers);) {
								Helpers.addFileToProject(scrContainer.getFile(IPath.fromOSString(renamedFile)), theFileStream, monitor, false);
							} catch (IOException e) {
								log(new Status(IStatus.WARNING, CORE_PLUGIN_ID,
										"Failed to add template file :" + sourceFile.toString(), e));
							}

						}
					}
				}

				break;
			case sample:
				try {
					for (IExample curExample : myExamples) {
						IPath curPath=curExample.getCodeLocation();
						if (myMakeLinks) {
							if(scrContainer instanceof IFolder) {
							IFolder folder=(IFolder)scrContainer;
							Helpers.linkDirectory( curPath, folder);
							}
							else {
								log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
										"Can not create links to project root"));
							}
						} else {
							//Files.copy(curPath.toPath(), targetFolder.getLocation().toPath());
							FileUtils.copyDirectory(curPath.toFile(), scrContainer.getLocation().toFile());
	                        FileModifiers.addPragmaOnce(scrContainer.getLocation());
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
			}

		} catch (CoreException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean supports(String buildArtifactType) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supports(String buildArtifactType, String natureID) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		return "Sloeber code provider"; //$NON-NLS-1$
	}

	@Override
	public String getDescription() {
		return "a provider for sloeber projects"; //$NON-NLS-1$
	}

	@Override
	public String getID() {
		return "io.slober.core.code.provider"; //$NON-NLS-1$
	}

	@Override
	public boolean getContainsCppCode() {
		return true;
	}

	@Override
	public String getCodeFolder() {
		return myCodeFolder ;
	}

	@Override
	public void setCodeFolder(String codeFolder) {
		myCodeFolder=codeFolder;

	}

	@Override
	public ICodeProvider createCopy() {
		CodeDescription ret=new CodeDescription(myCodeType);
		ret.myTemPlateFoldername=myTemPlateFoldername;
		ret.myMakeLinks = myMakeLinks;
		ret.myExamples = new HashSet<>(myExamples);
		//TODO check whether myReplacers needs to be copied
		// Map<String, String> myReplacers = null;
		ret.myCodeFolder =myCodeFolder;
		return ret;
	}
}
