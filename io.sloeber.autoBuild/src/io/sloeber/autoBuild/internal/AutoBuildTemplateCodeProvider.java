package io.sloeber.autoBuild.internal;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

import io.sloeber.autoBuild.api.ICodeProvider;

public class AutoBuildTemplateCodeProvider implements ICodeProvider {
	private String myID = null;
	private String myName = null;
	private String myDescription = null;
	private boolean myContainsCppcode = false;
	private Set<String> myBuildArtifactTypes = new HashSet<>();
	private IPath myTemplateFolder;
	private String myCodeFolder="src"; //$NON-NLS-1$

	@SuppressWarnings("nls")
	public AutoBuildTemplateCodeProvider(Bundle bundle, IConfigurationElement element)
			throws IOException, URISyntaxException {
		myID = element.getAttribute(ID);
		myName = element.getAttribute(NAME);
		myDescription = element.getAttribute(DESCRIPTION);
		myContainsCppcode = Boolean.valueOf(element.getAttribute("ContainsCppCode")).booleanValue();
		String buildArtifacts = element.getAttribute("SupportedArtifactTypes");
		if (buildArtifacts != null) {
			myBuildArtifactTypes.addAll(Arrays.asList(buildArtifacts.split(";")));
		}
		String providedPath = element.getAttribute("CodeLocation");
		Path path = new Path(providedPath);
		URL fileURL = FileLocator.find(bundle, path, null);
		if (fileURL == null) {
			System.err.println("For template code with name " + myName + " and ID " + myID + " the path is not found "
					+ providedPath);
		}
		URL resolvedFileURL = FileLocator.toFileURL(fileURL);
		myTemplateFolder = new Path(resolvedFileURL.toURI().getPath());
	}

	@Override
	public boolean createFiles(IContainer targetContainer, IProgressMonitor monitor) {
		try {
			File templateFolder = myTemplateFolder.toFile();
			return recursiveCreateFiles(templateFolder, targetContainer, monitor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean recursiveCreateFiles(File templateFolder, IContainer targetFolder, IProgressMonitor monitor) {
		try {
			for (File curMember : templateFolder.listFiles()) {
				if (curMember.isFile()) {
					File sourceFile = curMember;
					IFile targetFile = targetFolder.getFile(IPath.fromOSString( sourceFile.getName()));

					try (InputStream theFileStream = new FileInputStream(sourceFile.toString())) {
						targetFile.create(theFileStream, true, monitor);
					} catch (IOException e) {
						e.printStackTrace();
					}

				} else {
					// curmember is a folder
					IFolder newtargetFolder = targetFolder.getFolder(IPath.fromOSString(curMember.getName()));
					newtargetFolder.create(true, true, monitor);
					recursiveCreateFiles(curMember, newtargetFolder, monitor);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean supports(String buildArtifactType) {
		if (myBuildArtifactTypes.size() == 0) {
			return true;
		}
		return myBuildArtifactTypes.contains(buildArtifactType);
	}

	@Override
	public boolean supports(String buildArtifactType, String natureID) {
		if (!supports(buildArtifactType)) {
			return false;
		}
		if (myContainsCppcode) {
			return !CProjectNature.C_NATURE_ID.equals(natureID);
		}
		return true;
	}

	@Override
	public String getName() {
		return myName;
	}

	@Override
	public String getID() {
		return myID;
	}

	@Override
	public String getDescription() {
		return myDescription;
	}

	@Override
	public boolean getContainsCppCode() {
		return myContainsCppcode;
	}

	@Override
	public String getCodeFolder() {
		return myCodeFolder;
	}

	@Override
	public void setCodeFolder(String codeFolder) {
		myCodeFolder=codeFolder;

	}

}
