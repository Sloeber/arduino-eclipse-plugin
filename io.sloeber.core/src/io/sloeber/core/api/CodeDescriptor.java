package io.sloeber.core.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import io.sloeber.common.Const;
import io.sloeber.common.InstancePreferences;
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

	private CodeTypes codeType;
	private IPath myTemPlateFoldername;
	private boolean myMakeLinks = false;
	private ArrayList<Path> myLastUsedExamples = new ArrayList<>();

	public IPath getTemPlateFoldername() {
		return this.myTemPlateFoldername;
	}

	private CodeDescriptor(CodeTypes codeType) {
		this.codeType = codeType;
	}

	public static CodeDescriptor createDefaultIno() {
		return new CodeDescriptor(CodeTypes.defaultIno);
	}

	public static CodeDescriptor createDefaultCPP() {
		return new CodeDescriptor(CodeTypes.defaultCPP);
	}

	public static CodeDescriptor createSample(boolean link, ArrayList<Path> sampleFolders) {
		CodeDescriptor codeDescriptor = new CodeDescriptor(CodeTypes.sample);
		codeDescriptor.myMakeLinks = link;
		codeDescriptor.myLastUsedExamples = sampleFolders;
		return codeDescriptor;
	}

	public static CodeDescriptor createCustomTemplate(IPath temPlateFoldername) {
		CodeDescriptor codeDescriptor = new CodeDescriptor(CodeTypes.CustomTemplate);
		codeDescriptor.myTemPlateFoldername = temPlateFoldername;
		return codeDescriptor;
	}

	public static CodeDescriptor createLastUsed() {

		String typeDescriptor = InstancePreferences.getGlobalString(Const.ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT,
				Const.EMPTY_STRING);
		CodeTypes codeType = codeTypeFromDescription(typeDescriptor);
		CodeDescriptor ret = new CodeDescriptor(codeType);
		ret.myTemPlateFoldername = new Path(
				InstancePreferences.getGlobalString(Const.ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER, Const.EMPTY_STRING));
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
		if (this.myTemPlateFoldername != null) {
			InstancePreferences.setGlobalValue(Const.ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER,
					this.myTemPlateFoldername.toString());
		}
		InstancePreferences.setGlobalValue(Const.ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT, this.codeType.toString());
		saveLastUsedExamples();
	}

	/*
	 * given the source descriptor, add the sources to the project
	 */
	@SuppressWarnings("nls")
	public void createFiles(IProject project, IProgressMonitor monitor) throws CoreException {

		this.save();
		String Include = "Arduino.h";

		switch (this.codeType) {
		case defaultIno:
			Helpers.addFileToProject(project, new Path(project.getName() + ".ino"), Stream.openContentStream(
					project.getName(), Include, "/io/sloeber/core/templates/sketch.ino", false), monitor, false);
			break;
		case defaultCPP:
			Helpers.addFileToProject(project, new Path(project.getName() + ".cpp"), Stream.openContentStream(
					project.getName(), Include, "/io/sloeber/core/templates/sketch.cpp", false), monitor, false);
			Helpers.addFileToProject(project, new Path(project.getName() + ".h"),
					Stream.openContentStream(project.getName(), Include, "/io/sloeber/core/templates/sketch.h", false),
					monitor, false);
			break;
		case CustomTemplate:
			IPath folderName = this.myTemPlateFoldername;
			String files[] = folderName.toFile().list();
			for (String file : files) {
				if (!(file.equals(".") || file.equals(".."))) {
					File sourceFile = folderName.append(file).toFile();
					Helpers.addFileToProject(project, new Path(file),
							Stream.openContentStream(project.getName(), Include, sourceFile.toString(), true), monitor,
							false);
				}
			}

			break;
		case sample:
			try {
				for (Path curPath : this.myLastUsedExamples) {
					if (this.myMakeLinks) {
						Helpers.linkDirectory(project, curPath, new Path("/"));
					} else {
						FileUtils.copyDirectory(curPath.toFile(), project.getLocation().toFile());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		default:

			break;
		}
	}

	@SuppressWarnings("nls")
	private void loadLastUsedExamples() {
		String examplePathNames[] = InstancePreferences
				.getGlobalString(Const.KEY_LAST_USED_EXAMPLES, Defaults.getPrivateLibraryPath()).split("\n");

		for (String curpath : examplePathNames) {
			this.myLastUsedExamples.add(new Path(curpath));
		}
	}

	public ArrayList<Path> getLastUsedExamples() {
		return this.myLastUsedExamples;
	}

	private void saveLastUsedExamples() {
		if (this.myLastUsedExamples != null) {
			String toStore = StringUtils.join(this.myLastUsedExamples, "\n"); //$NON-NLS-1$
			InstancePreferences.setGlobalValue(Const.KEY_LAST_USED_EXAMPLES, toStore);
		} else {
			InstancePreferences.setGlobalValue(Const.KEY_LAST_USED_EXAMPLES, Const.EMPTY_STRING);
		}

	}

	public CodeTypes getCodeType() {
		return this.codeType;
	}

}
