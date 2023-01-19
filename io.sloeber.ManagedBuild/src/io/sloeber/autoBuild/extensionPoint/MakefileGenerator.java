package io.sloeber.autoBuild.extensionPoint;

import static io.sloeber.autoBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.autoBuild.core.Messages.*;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

//import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.Internal.MakeRule;
import io.sloeber.autoBuild.Internal.SrcMakeGenerator;
import io.sloeber.autoBuild.Internal.SubDirMakeGenerator;
import io.sloeber.autoBuild.Internal.TopMakeFileGenerator;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;

/**
 * This is a specialized makefile generator that takes advantage of the
 * extensions present in Gnu Make.
 *
 * @since 1.2
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class MakefileGenerator implements IMakefileGenerator {

	/**
	 * This class is used to recursively walk the project and determine which
	 * modules contribute buildable source files.
	 */
	protected class ResourceProxyVisitor implements IResourceProxyVisitor {
		private final MakefileGenerator generator;
		private Collection<IContainer> subdirList = new LinkedHashSet<>();

		Collection<IContainer> getSubdirList() {
			return subdirList;
		}

		public ResourceProxyVisitor(MakefileGenerator generator) {
			this.generator = generator;

		}

		@Override
		public boolean visit(IResourceProxy proxy) throws CoreException {
			if (generator == null) {
				return false;
			}
			IResource resource = proxy.requestResource();
			boolean isSource = isSource(resource.getProjectRelativePath());
			// Is this a resource we should even consider
			switch (proxy.getType()) {
			case IResource.FILE:
				// If this resource has a Resource Configuration and is not
				// excluded or
				// if it has a file extension that one of the tools builds, add
				// the sudirectory to the list
				if (isSource) {
					subdirList.add(resource.getParent());
					return false;
				}
				return true;
			case IResource.FOLDER:
				if (!isSource || generator.isGeneratedResource(resource))
					return false;
				return true;
			}
			// Recurse into subdirectories
			return true;
		}
	}

	// Local variables needed by generator
	String buildTargetName;
	String buildTargetExt;
	IConfiguration config;
	/** Collection of Folders in which sources files have been modified */
	private IProgressMonitor monitor;
	private IProject project;
	// dependency files
	/** Collection of Containers which contribute source files to the build */

	private IFile topBuildDir;
	private List<ICSourceEntry> srcEntries;

	public MakefileGenerator() {
		super();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator
	 * #initialize(IProject, IManagedBuildInfo, IProgressMonitor)
	 */
	@Override
	public void initialize(IProject project, IManagedBuildInfo info, IProgressMonitor monitor) {

		this.project = project;

		// Save the monitor reference for reporting back to the user
		this.monitor = monitor;
		// Get the name of the build target
		buildTargetName = info.getBuildArtifactName();
		// Get its extension
		buildTargetExt = info.getBuildArtifactExtension();
		// try to resolve the build macros in the target extension
		buildTargetExt = resolveValueToMakefileFormat(buildTargetExt, "", " ",
				IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());
		// try to resolve the build macros in the target name
		String resolved = resolveValueToMakefileFormat(buildTargetName, "", " ",
				IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());
		if (resolved != null && (resolved = resolved.trim()).length() > 0)
			buildTargetName = resolved;
		if (buildTargetExt == null) {
			buildTargetExt = "";
		}
		// Cache the build tools
		config = info.getDefaultConfiguration();
		// initToolInfos();
		// set the top build dir path
		topBuildDir = project.getFile(info.getConfigurationName());
	}

	public boolean isSource(IPath path) {
		return true;// tofix jaba !CDataUtil.isExcluded(path, srcEntries);
	}

	@Override
	public void generateDependencies() throws CoreException {
	}

	@Override
	public MultiStatus generateMakefiles(IResourceDelta delta) throws CoreException {
		return regenerateMakefiles();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#
	 * getBuildWorkingDir()
	 */
	@Override
	public IPath getBuildWorkingDir() {
		if (topBuildDir != null) {
			return topBuildDir.getProjectRelativePath();
		}
		return null;
	}

	public IPath getBuildFolder() {
		return topBuildDir.getLocation();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#
	 * getMakefileName()
	 */
	@Override
	public String getMakefileName() {
		return MAKEFILE_NAME;
	}

	/*
	 * Is this resource a resource generated as a result of the build
	 */
	public boolean isGeneratedResource(IResource resource) {
		// Is this a generated directory ...
		IPath path = resource.getProjectRelativePath();
		CCorePlugin cCorePlugin = CCorePlugin.getDefault();
		ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(project);

		for (ICConfigurationDescription curConfig : prjCDesc.getConfigurations()) {
			String name = curConfig.getName();
			IPath root = new Path(name);
			// It is if it is a root of the resource pathname
			if (root.isPrefixOf(path))
				return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#
	 * regenerateDependencies()
	 */
	@Override
	public void regenerateDependencies(boolean force) throws CoreException {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#
	 * regenerateMakefiles()
	 */
	@Override
	public MultiStatus regenerateMakefiles() throws CoreException {
		MultiStatus status;
		// Visit the resources in the project
		ResourceProxyVisitor subDirVisitor = new ResourceProxyVisitor(this);
		project.accept(subDirVisitor, IResource.NONE);
		// See if the user has cancelled the build
		checkCancel();
		Collection<IContainer> foldersToInvestigate = subDirVisitor.getSubdirList();
		if (foldersToInvestigate.isEmpty()) {
			// String info =
			// ManagedMakeMessages.getFormattedString("MakefileGenerator.warning.no.source",
			// project.getName());
			String info = MessageFormat.format(MakefileGenerator_warning_no_source, project.getName());
			updateMonitor(info);
			status = new MultiStatus(Activator.getId(), IStatus.INFO, "", null);
			status.add(new Status(IStatus.INFO, Activator.getId(), NO_SOURCE_FOLDERS, info, null));
			return status;
		}
		// Create the top-level directory for the build output
		topBuildDir = project.getFile(config.getName());
		checkCancel();
		// Get the data for the makefile generation
		List<SubDirMakeGenerator> subDirMakeGenerators = new LinkedList<>();
		Set<MakeRule> subDirMakeRules = new HashSet<>();
		Collection<IContainer> foldersToBuild = new LinkedHashSet<>();

		for (IContainer res : foldersToInvestigate) {
			// For all the folders get the make rules for this folder
			SubDirMakeGenerator subDirMakeGenerator = new SubDirMakeGenerator(this, res);
			if (!subDirMakeGenerator.isEmpty()) {
				foldersToBuild.add(res);
				subDirMakeGenerators.add(subDirMakeGenerator);
				// also store all these rules in one set to provide them to the top make file
				subDirMakeRules.addAll(subDirMakeGenerator.getMakeRules());
			}
			checkCancel();
		}
		TopMakeFileGenerator topMakeFileGenerator = new TopMakeFileGenerator(this, subDirMakeRules, foldersToBuild);

		checkCancel();

		Set<String> srcMacroNames = new LinkedHashSet<>();
		Set<String> objMacroNames = new LinkedHashSet<>();
		for (SubDirMakeGenerator curSubDirMake : subDirMakeGenerators) {
			curSubDirMake.generateMakefile();
			srcMacroNames.addAll(curSubDirMake.getPrerequisiteMacros());
			srcMacroNames.addAll(curSubDirMake.getDependecyMacros());
			objMacroNames.addAll(curSubDirMake.getTargetMacros());
		}
		// TOFIX also need to add macro's from main makefile

		SrcMakeGenerator.generateSourceMakefile(project, config, srcMacroNames, foldersToBuild);
		SrcMakeGenerator.generateObjectsMakefile(project, config, objMacroNames);
		topMakeFileGenerator.generateMakefile();

		checkCancel();
		// How did we do
		status = new MultiStatus(Activator.getId(), IStatus.OK, "", null);

		// TOFIX this should be done differently
		// JABA SLOEBER create the size.awk file
		// ICConfigurationDescription confDesc =
		// ManagedBuildManager.getDescriptionForConfiguration(config);
		// IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
		// IFile sizeAwkFile1 =
		// root.getFile(topBuildDir.getFullPath().append("size.awk"));
		// File sizeAwkFile = sizeAwkFile1.getLocation().toFile();
		// String regex = Common.getBuildEnvironmentVariable(confDesc,
		// "recipe.size.regex", EMPTY);
		// String awkContent = "/" + regex + "/ {arduino_size += $2 }\n";
		// regex = Common.getBuildEnvironmentVariable(confDesc,
		// "recipe.size.regex.data", EMPTY);
		// awkContent += "/" + regex + "/ {arduino_data += $2 }\n";
		// regex = Common.getBuildEnvironmentVariable(confDesc,
		// "recipe.size.regex.eeprom", EMPTY);
		// awkContent += "/" + regex + "/ {arduino_eeprom += $2 }\n";
		// awkContent += "END { print \"\\n";
		// String max = Common.getBuildEnvironmentVariable(confDesc,
		// "upload.maximum_size", "10000");
		// awkContent += Messages.sizeReportSketch.replace("maximum_size", max);
		// awkContent += "\\n";
		// max = Common.getBuildEnvironmentVariable(confDesc,
		// "upload.maximum_data_size", "10000");
		// awkContent += Messages.sizeReportData.replace("maximum_data_size", max);
		// awkContent += "\\n";
		// awkContent += "\"}";
		//
		// try {
		// FileUtils.write(sizeAwkFile, awkContent, Charset.defaultCharset());
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// END JABA SLOEBER create the size.awk file

		return status;
	}

	/*************************************************************************
	 * M A K E F I L E G E N E R A T I O N C O M M O N M E T H O D S
	 ************************************************************************/

	/**
	 * Check whether the build has been cancelled. Cancellation requests propagated
	 * to the caller by throwing <code>OperationCanceledException</code>.
	 *
	 * @see org.eclipse.core.runtime.OperationCanceledException#OperationCanceledException()
	 */
	private void checkCancel() {
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
	}

	public IConfiguration getConfig() {
		return config;
	}

	private void updateMonitor(String msg) {
		if (monitor != null && !monitor.isCanceled()) {
			monitor.subTask(msg);
			monitor.worked(1);
		}
	}

	/**
	 * Return the configuration's top build directory as an absolute path
	 */
	public IFile getTopBuildDir() {
		return topBuildDir;
	}

	@Override
	public void initialize(int buildKind, IConfiguration cfg, IBuilder builder, IProgressMonitor monitor) {
		// Save the project so we can get path and member information
		this.project = cfg.getOwner().getProject();
		if (builder == null) {
			builder = cfg.getBuilder();
		}

		// Save the monitor reference for reporting back to the user
		this.monitor = monitor;
		// Get the build info for the project
		// info = info;
		// Get the name of the build target
		buildTargetName = cfg.getArtifactName();
		// Get its extension
		buildTargetExt = cfg.getArtifactExtension();
		// try to resolve the build macros in the target extension
		buildTargetExt = resolveValueToMakefileFormat(buildTargetExt, "", " ",
				IBuildMacroProvider.CONTEXT_CONFIGURATION, builder);
		// try to resolve the build macros in the target name
		String resolved = resolveValueToMakefileFormat(buildTargetName, "", " ",
				IBuildMacroProvider.CONTEXT_CONFIGURATION, builder);
		if (resolved != null) {
			resolved = resolved.trim();
			if (resolved.length() > 0)
				buildTargetName = resolved;
		}
		if (buildTargetExt == null) {
			buildTargetExt = "";
		}
		// Cache the build tools
		config = cfg;
		// initToolInfos();
		// set the top build dir path
		topBuildDir = project.getFile(cfg.getName());
		srcEntries = config.getSourceEntries();
		if (srcEntries.size() == 0) {
			srcEntries = new LinkedList<ICSourceEntry>();
			srcEntries.add(
					new CSourceEntry(Path.EMPTY, null, ICSettingEntry.RESOLVED | ICSettingEntry.VALUE_WORKSPACE_PATH));
		} else {
			// ICConfigurationDescription cfgDes =
			// ManagedBuildManager.getDescriptionForConfiguration(config);
			CCorePlugin cCorePlugin = CCorePlugin.getDefault();
			ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(project);
			ICConfigurationDescription cfgDes = prjCDesc.getConfigurationByName(config.getName());

			ICSourceEntry[] resolvedEntries = CDataUtil.resolveEntries(srcEntries.toArray(new ICSourceEntry[0]),
					cfgDes);
			for (ICSourceEntry curEntry : resolvedEntries) {
				srcEntries.add(curEntry);
			}
		}
	}

	public IProject getProject() {
		return project;
	}
}
