package it.baeyens.arduino.toolchain;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.core.settings.model.util.IPathSettingsContainerVisitor;
import org.eclipse.cdt.core.settings.model.util.PathSettingsContainer;
import org.eclipse.cdt.managedbuilder.core.BuildException;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFileInfo;
import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineGenerator;
import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedOutputNameProvider;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.cdt.managedbuilder.internal.core.Tool;
import org.eclipse.cdt.managedbuilder.internal.macros.BuildMacroProvider;
import org.eclipse.cdt.managedbuilder.internal.macros.FileContextData;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCalculator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCommands;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyInfo;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyPreBuild;
import org.eclipse.cdt.managedbuilder.makegen.gnu.IManagedBuildGnuToolInfo;
import org.eclipse.cdt.utils.EFSExtensionManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * This is a specialized makefile generator that takes advantage of the extensions present in Gnu Make.
 * 
 * @since 1.2
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({ "deprecation", "restriction" })
public class ArduinoGnuMakefileGenerator implements IManagedBuilderMakefileGenerator2 {
    private static final IPath DOT_SLASH_PATH = new Path("./"); //$NON-NLS-1$

    /**
     * This class walks the delta supplied by the build system to determine what resources have been changed. The logic is very simple. If a buildable
     * resource (non-header) has been added or removed, the directories in which they are located are "dirty" so the makefile fragments for them have
     * to be regenerated.
     * <p>
     * The actual dependencies are recalculated as a result of the build step itself. We are relying on make to do the right things when confronted
     * with a dependency on a moved header file. That said, make will treat the missing header file in a dependency rule as a target it has to build
     * unless told otherwise. These dummy targets are added to the makefile to avoid a missing target error.
     */
    public class ResourceDeltaVisitor implements IResourceDeltaVisitor {
	private final ArduinoGnuMakefileGenerator generator;
	// private IManagedBuildInfo info;
	@SuppressWarnings("hiding")
	private final IConfiguration config;

	/**
	 * The constructor
	 */
	public ResourceDeltaVisitor(ArduinoGnuMakefileGenerator generator, IManagedBuildInfo info) {
	    this.generator = generator;
	    this.config = info.getDefaultConfiguration();
	}

	public ResourceDeltaVisitor(ArduinoGnuMakefileGenerator generator, IConfiguration cfg) {
	    this.generator = generator;
	    this.config = cfg;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse .core.resources.IResourceDelta)
	 */
	@SuppressWarnings("incomplete-switch")
	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
	    // Should the visitor keep iterating in current directory
	    boolean keepLooking = false;
	    IResource resource = delta.getResource();
	    IResourceInfo rcInfo = config.getResourceInfo(resource.getProjectRelativePath(), false);
	    IFolderInfo fo = null;
	    boolean isSource = isSource(resource.getProjectRelativePath());
	    if (rcInfo instanceof IFolderInfo) {
		fo = (IFolderInfo) rcInfo;
	    }
	    // What kind of resource change has occurred
	    if (/* !rcInfo.isExcluded() && */isSource) {
		if (resource.getType() == IResource.FILE) {
		    String ext = resource.getFileExtension();
		    switch (delta.getKind()) {
		    case IResourceDelta.ADDED:
			if (!generator.isGeneratedResource(resource)) {
			    // This is a source file so just add its container
			    if (fo == null || fo.buildsFileType(ext)) {
				generator.appendModifiedSubdirectory(resource);
			    }
			}
			break;
		    case IResourceDelta.REMOVED:
			// we get this notification if a resource is moved too
			if (!generator.isGeneratedResource(resource)) {
			    // This is a source file so just add its container
			    if (fo == null || fo.buildsFileType(ext)) {
				generator.appendDeletedFile(resource);
				generator.appendModifiedSubdirectory(resource);
			    }
			}
			break;
		    default:
			keepLooking = true;
			break;
		    }
		}

		if (resource.getType() == IResource.FOLDER) {
		    // I only care about delete event
		    switch (delta.getKind()) {
		    case IResourceDelta.REMOVED:
			if (!generator.isGeneratedResource(resource)) {
			    generator.appendDeletedSubdirectory((IContainer) resource);
			}
			break;
		    }
		}
	    }
	    if (resource.getType() == IResource.PROJECT) {
		// If there is a zero-length delta, something the project
		// depends on has changed so just call make
		IResourceDelta[] children = delta.getAffectedChildren();
		if (children != null && children.length > 0) {
		    keepLooking = true;
		}
	    } else {
		// If the resource is part of the generated directory structure
		// don't recurse
		if (resource.getType() == IResource.ROOT || (isSource && !generator.isGeneratedResource(resource))) {
		    keepLooking = true;
		}
	    }

	    return keepLooking;
	}
    }

    /**
     * This class is used to recursively walk the project and determine which modules contribute buildable source files.
     */
    protected class ResourceProxyVisitor implements IResourceProxyVisitor {
	private final ArduinoGnuMakefileGenerator generator;
	@SuppressWarnings("hiding")
	private final IConfiguration config;

	// private IManagedBuildInfo info;

	/**
	 * Constructs a new resource proxy visitor to quickly visit project resources.
	 */
	public ResourceProxyVisitor(ArduinoGnuMakefileGenerator generator, IManagedBuildInfo info) {
	    this.generator = generator;
	    this.config = info.getDefaultConfiguration();
	}

	public ResourceProxyVisitor(ArduinoGnuMakefileGenerator generator, IConfiguration cfg) {
	    this.generator = generator;
	    this.config = cfg;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.resources.IResourceProxyVisitor#visit(org.eclipse .core.resources.IResourceProxy)
	 */
	@Override
	public boolean visit(IResourceProxy proxy) throws CoreException {
	    // No point in proceeding, is there
	    if (generator == null) {
		return false;
	    }

	    IResource resource = proxy.requestResource();
	    boolean isSource = isSource(resource.getProjectRelativePath());

	    // Is this a resource we should even consider
	    if (proxy.getType() == IResource.FILE) {
		// If this resource has a Resource Configuration and is not
		// excluded or
		// if it has a file extension that one of the tools builds, add
		// the sudirectory to the list
		// boolean willBuild = false;
		IResourceInfo rcInfo = config.getResourceInfo(resource.getProjectRelativePath(), false);
		if (isSource/* && !rcInfo.isExcluded() */) {
		    boolean willBuild = false;
		    if (rcInfo instanceof IFolderInfo) {
			String ext = resource.getFileExtension();
			if (((IFolderInfo) rcInfo).buildsFileType(ext) &&
			// If this file resource is a generated resource, then
			// it is uninteresting
				!generator.isGeneratedResource(resource)) {
			    willBuild = true;
			}
		    } else {
			willBuild = true;
		    }

		    if (willBuild)
			generator.appendBuildSubdirectory(resource);
		}
		// if (willBuild) {
		// if ((resConfig == null) || (!(resConfig.isExcluded()))) {
		// generator.appendBuildSubdirectory(resource);
		// }
		// }
		return false;
	    } else if (proxy.getType() == IResource.FOLDER) {

		if (!isSource || generator.isGeneratedResource(resource))
		    return false;
		return true;
	    }

	    // Recurse into subdirectories
	    return true;
	}

    }

    // String constants for makefile contents and messages
    private static final String COMMENT = "MakefileGenerator.comment"; //$NON-NLS-1$
    //private static final String AUTO_DEP = COMMENT + ".autodeps";	//$NON-NLS-1$
    //private static final String MESSAGE = "ManagedMakeBuilder.message";	//$NON-NLS-1$
    //private static final String BUILD_ERROR = MESSAGE + ".error";	//$NON-NLS-1$

    //private static final String DEP_INCL = COMMENT + ".module.dep.includes";	//$NON-NLS-1$
    private static final String HEADER = COMMENT + ".header"; //$NON-NLS-1$

    protected static final String MESSAGE_FINISH_BUILD = ManagedMakeMessages.getResourceString("MakefileGenerator.message.finish.build"); //$NON-NLS-1$
    protected static final String MESSAGE_FINISH_FILE = ManagedMakeMessages.getResourceString("MakefileGenerator.message.finish.file"); //$NON-NLS-1$
    protected static final String MESSAGE_START_BUILD = ManagedMakeMessages.getResourceString("MakefileGenerator.message.start.build"); //$NON-NLS-1$
    protected static final String MESSAGE_START_FILE = ManagedMakeMessages.getResourceString("MakefileGenerator.message.start.file"); //$NON-NLS-1$
    protected static final String MESSAGE_START_DEPENDENCY = ManagedMakeMessages.getResourceString("MakefileGenerator.message.start.dependency"); //$NON-NLS-1$
    protected static final String MESSAGE_NO_TARGET_TOOL = ManagedMakeMessages.getResourceString("MakefileGenerator.message.no.target"); //$NON-NLS-1$
    //private static final String MOD_INCL = COMMENT + ".module.make.includes";	//$NON-NLS-1$
    private static final String MOD_LIST = COMMENT + ".module.list"; //$NON-NLS-1$
    private static final String MOD_VARS = COMMENT + ".module.variables"; //$NON-NLS-1$
    private static final String MOD_RULES = COMMENT + ".build.rule"; //$NON-NLS-1$
    private static final String BUILD_TOP = COMMENT + ".build.toprules"; //$NON-NLS-1$
    private static final String ALL_TARGET = COMMENT + ".build.alltarget"; //$NON-NLS-1$
    private static final String MAINBUILD_TARGET = COMMENT + ".build.mainbuildtarget"; //$NON-NLS-1$
    private static final String BUILD_TARGETS = COMMENT + ".build.toptargets"; //$NON-NLS-1$
    private static final String SRC_LISTS = COMMENT + ".source.list"; //$NON-NLS-1$

    private static final String EMPTY_STRING = ""; //$NON-NLS-1$
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String OBJS_MACRO = "OBJS"; //$NON-NLS-1$
    private static final String MACRO_ADDITION_ADDPREFIX_HEADER = "${addprefix "; //$NON-NLS-1$
    private static final String MACRO_ADDITION_ADDPREFIX_SUFFIX = "," + WHITESPACE + LINEBREAK; //$NON-NLS-1$
    private static final String MACRO_ADDITION_PREFIX_SUFFIX = "+=" + WHITESPACE + LINEBREAK; //$NON-NLS-1$
    private static final String PREBUILD = "pre-build"; //$NON-NLS-1$
    private static final String MAINBUILD = "main-build"; //$NON-NLS-1$
    private static final String POSTBUILD = "post-build"; //$NON-NLS-1$
    private static final String SECONDARY_OUTPUTS = "secondary-outputs"; //$NON-NLS-1$

    // Enumerations
    public static final int PROJECT_RELATIVE = 1, PROJECT_SUBDIR_RELATIVE = 2, ABSOLUTE = 3;

    class ToolInfoHolder {
	ITool[] buildTools;
	boolean[] buildToolsUsed;
	ArduinoManagedBuildGnuToolInfo[] gnuToolInfos;
	Set<String> outputExtensionsSet;
	List<IPath> dependencyMakefiles;
    }

    // Local variables needed by generator
    private String buildTargetName;
    private String buildTargetExt;
    IConfiguration config;
    @SuppressWarnings("unused")
    // TOFIX get rid of this @SuppressWarnings("unused")
    private IBuilder builder;
    // private ITool[] buildTools;
    // private boolean[] buildToolsUsed;
    // private ArduinoManagedBuildGnuToolInfo[] gnuToolInfos;
    private PathSettingsContainer toolInfos;
    private Vector<IResource> deletedFileList;
    private Vector<IResource> deletedDirList;
    // private IManagedBuildInfo info;
    // private IConfiguration cfg
    private Vector<IResource> invalidDirList;
    /** Collection of Folders in which sources files have been modified */
    private Collection<IContainer> modifiedList;
    private IProgressMonitor monitor;
    IProject project;
    IResource[] projectResources;
    private Vector<String> ruleList;
    private Vector<String> depLineList; // String's of additional dependency
					// lines
    private Vector<String> depRuleList; // String's of rules for generating
					// dependency files
    /** Collection of Containers which contribute source files to the build */
    private Collection<IContainer> subdirList;
    private IPath topBuildDir; // Build directory - relative to the workspace
    // private Set outputExtensionsSet;
    // === Maps of macro names (String) to values (List)
    // Map of source file build variable names to a List of source file Path's
    final HashMap<String, List<IPath>> buildSrcVars = new HashMap<String, List<IPath>>();
    // Map of output file build variable names to a List of output file Path's
    final HashMap<String, List<IPath>> buildOutVars = new HashMap<String, List<IPath>>();
    // Map of dependency file build variable names to a List of
    // ArduinoGnuDependencyGroupInfo objects
    final HashMap<String, ArduinoGnuDependencyGroupInfo> buildDepVars = new HashMap<String, ArduinoGnuDependencyGroupInfo>();
    private final LinkedHashMap<String, String> topBuildOutVars = new LinkedHashMap<String, String>();
    // Dependency file variables
    // private Vector dependencyMakefiles; // IPath's - relative to the top
    // build directory or absolute

    private ICSourceEntry srcEntries[];

    private IOutputType usedOutType = null;

    public ArduinoGnuMakefileGenerator() {
	super();
    }

    /*************************************************************************
     * IManagedBuilderMakefileGenerator M E T H O D S
     ************************************************************************/

    @SuppressWarnings("hiding")
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator #initialize(IProject, IManagedBuildInfo, IProgressMonitor)
     */
    @Override
    public void initialize(IProject project, IManagedBuildInfo info, IProgressMonitor monitor) {
	// Save the project so we can get path and member information
	this.project = project;
	try {
	    projectResources = project.members();
	} catch (CoreException e) {
	    projectResources = null;
	}
	// Save the monitor reference for reporting back to the user
	this.monitor = monitor;
	// Get the build info for the project
	// this.info = info;
	// Get the name of the build target
	buildTargetName = info.getBuildArtifactName();
	// Get its extension
	buildTargetExt = info.getBuildArtifactExtension();

	try {
	    // try to resolve the build macros in the target extension
	    buildTargetExt = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(buildTargetExt, "", //$NON-NLS-1$
		    " ", //$NON-NLS-1$
		    IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());
	} catch (BuildMacroException e) {// JABA is not going to write this code
	}

	try {
	    // try to resolve the build macros in the target name
	    String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(buildTargetName, "", //$NON-NLS-1$
		    " ", //$NON-NLS-1$
		    IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());
	    if (resolved != null && (resolved = resolved.trim()).length() > 0)
		buildTargetName = resolved;
	} catch (BuildMacroException e) {// JABA is not going to write this code
	}

	if (buildTargetExt == null) {
	    buildTargetExt = new String();
	}
	// Cache the build tools
	config = info.getDefaultConfiguration();
	builder = config.getEditableBuilder();
	initToolInfos();
	// set the top build dir path
	topBuildDir = project.getFolder(info.getConfigurationName()).getFullPath();
    }

    /**
     * This method calls the dependency postprocessors defined for the tool chain
     */
    private void callDependencyPostProcessors(IResourceInfo rcInfo, ToolInfoHolder h, IFile depFile, IManagedDependencyGenerator2[] postProcessors, // This
																		    // array
																		    // is
																		    // the
																		    // same
																		    // size
																		    // as
																		    // the
																		    // buildTools
																		    // array
																		    // and
																		    // has
																		    // an
																		    // entry
																		    // set
																		    // when
																		    // the
																		    // corresponding
																		    // tool
																		    // has
																		    // a
																		    // dependency
																		    // calculator
	    boolean callPopulateDummyTargets, boolean force) throws CoreException {
	try {
	    // IPath path = depFile.getFullPath();
	    // path = inFullPathFromOutFullPath(path);
	    // IResourceInfo rcInfo = config.getResourceInfo(path, false);
	    // IFolderInfo fo;
	    // if(rcInfo instanceof IFileInfo){
	    // fo =
	    // (IFolderInfo)config.getResourceInfo(path.removeLastSegments(1),
	    // false);
	    // } else {
	    // fo = (IFolderInfo)rcInfo;
	    // }
	    // ToolInfoHolder h = getToolInfo(fo.getPath());
	    updateMonitor(ManagedMakeMessages.getFormattedString("ArduinoGnuMakefileGenerator.message.postproc.dep.file", depFile.getName())); //$NON-NLS-1$
	    if (postProcessors != null) {
		IPath absolutePath = new Path(EFSExtensionManager.getDefault().getPathFromURI(depFile.getLocationURI()));
		// Convert to build directory relative
		IPath depPath = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), absolutePath);
		for (int i = 0; i < postProcessors.length; i++) {
		    IManagedDependencyGenerator2 depGen = postProcessors[i];
		    if (depGen != null) {
			depGen.postProcessDependencyFile(depPath, config, h.buildTools[i], getTopBuildDir());
		    }
		}
	    }
	    if (callPopulateDummyTargets) {
		populateDummyTargets(rcInfo, depFile, force);
	    }
	} catch (CoreException e) {
	    throw e;
	} catch (IOException e) {// JABA is not going to write this code
	}
    }

    /**
     * This method collects the dependency postprocessors and file extensions defined for the tool chain
     */
    private boolean collectDependencyGeneratorInformation(ToolInfoHolder h, Vector<String> depExts, // Vector of dependency file extensions
	    IManagedDependencyGenerator2[] postProcessors) {

	boolean callPopulateDummyTargets = false;
	for (int i = 0; i < h.buildTools.length; i++) {
	    ITool tool = h.buildTools[i];
	    IManagedDependencyGeneratorType depType = tool.getDependencyGeneratorForExtension(tool.getDefaultInputExtension());
	    if (depType != null) {
		int calcType = depType.getCalculatorType();
		if (calcType <= IManagedDependencyGeneratorType.TYPE_OLD_TYPE_LIMIT) {
		    if (calcType == IManagedDependencyGeneratorType.TYPE_COMMAND) {
			callPopulateDummyTargets = true;
			depExts.add(DEP_EXT);
		    }
		} else {
		    if (calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS
			    || calcType == IManagedDependencyGeneratorType.TYPE_PREBUILD_COMMANDS) {
			IManagedDependencyGenerator2 depGen = (IManagedDependencyGenerator2) depType;
			String depExt = depGen.getDependencyFileExtension(config, tool);
			if (depExt != null) {
			    postProcessors[i] = depGen;
			    depExts.add(depExt);
			}
		    }
		}
	    }
	}
	return callPopulateDummyTargets;
    }

    protected boolean isSource(IPath path) {
	return !CDataUtil.isExcluded(path, srcEntries);
	// path = path.makeRelative();
	// for(int i = 0; i < srcPaths.length; i++){
	// if(srcPaths[i].isPrefixOf(path))
	// return true;
	// }
	// return false;
    }

    private class DepInfo {
	Vector<String> depExts;
	IManagedDependencyGenerator2[] postProcessors;
	boolean callPopulateDummyTargets;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator #generateDependencies()
     */
    @Override
    public void generateDependencies() throws CoreException {
	final PathSettingsContainer postProcs = PathSettingsContainer.createRootContainer();
	// Note: PopulateDummyTargets is a hack for the pre-3.x GCC compilers

	// Collect the methods that will need to be called
	toolInfos.accept(new IPathSettingsContainerVisitor() {
	    @SuppressWarnings("synthetic-access")
	    @Override
	    public boolean visit(PathSettingsContainer container) {
		ToolInfoHolder h = (ToolInfoHolder) container.getValue();
		Vector<String> depExts = new Vector<String>(); // Vector of
							       // dependency
							       // file
							       // extensions
		IManagedDependencyGenerator2[] postProcessors = new IManagedDependencyGenerator2[h.buildTools.length];
		boolean callPopulateDummyTargets = collectDependencyGeneratorInformation(h, depExts, postProcessors);

		// Is there anyone to call if we do find dependency files?
		if (!callPopulateDummyTargets) {
		    int i;
		    for (i = 0; i < postProcessors.length; i++) {
			if (postProcessors[i] != null)
			    break;
		    }
		    if (i == postProcessors.length)
			return true;
		}

		PathSettingsContainer child = postProcs.getChildContainer(container.getPath(), true, true);
		DepInfo di = new DepInfo();
		di.depExts = depExts;
		di.postProcessors = postProcessors;
		di.callPopulateDummyTargets = callPopulateDummyTargets;
		child.setValue(di);

		return true;
	    }
	});

	IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
	for (IResource res : getSubdirList()) {
	    // The builder creates a subdir with same name as source in the
	    // build location
	    IContainer subDir = (IContainer) res;
	    IPath projectRelativePath = subDir.getProjectRelativePath();
	    IResourceInfo rcInfo = config.getResourceInfo(projectRelativePath, false);
	    PathSettingsContainer cr = postProcs.getChildContainer(rcInfo.getPath(), false, true);
	    if (cr == null || cr.getValue() == null)
		continue;

	    DepInfo di = (DepInfo) cr.getValue();

	    ToolInfoHolder h = getToolInfo(projectRelativePath);
	    IPath buildRelativePath = topBuildDir.append(projectRelativePath);
	    IFolder buildFolder = root.getFolder(buildRelativePath);
	    if (buildFolder == null)
		continue;
	    if (!buildFolder.exists())
		continue;

	    // Find all of the dep files in the generated subdirectories
	    IResource[] files = buildFolder.members();
	    for (IResource file : files) {
		String fileExt = file.getFileExtension();
		for (String ext : di.depExts) {
		    if (ext.equals(fileExt)) {
			IFile depFile = root.getFile(file.getFullPath());
			if (depFile == null)
			    continue;
			callDependencyPostProcessors(rcInfo, h, depFile, di.postProcessors, di.callPopulateDummyTargets, false);
		    }
		}
	    }
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator #generateMakefiles(org.eclipse.core.resources.IResourceDelta)
     */
    @Override
    public MultiStatus generateMakefiles(IResourceDelta delta) throws CoreException {
	/*
	 * Let's do a sanity check right now.
	 * 
	 * 1. This is an incremental build, so if the top-level directory is not there, then a rebuild is needed.
	 */
	IFolder folder = project.getFolder(config.getName());
	if (!folder.exists()) {
	    return regenerateMakefiles();
	}

	// Return value
	MultiStatus status;

	// Visit the resources in the delta and compile a list of subdirectories
	// to regenerate
	updateMonitor(ManagedMakeMessages.getFormattedString("MakefileGenerator.message.calc.delta", project.getName())); //$NON-NLS-1$
	ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(this, config);
	delta.accept(visitor);
	checkCancel();

	// Get all the subdirectories participating in the build
	updateMonitor(ManagedMakeMessages.getFormattedString("MakefileGenerator.message.finding.sources", project.getName())); //$NON-NLS-1$
	ResourceProxyVisitor resourceVisitor = new ResourceProxyVisitor(this, config);
	project.accept(resourceVisitor, IResource.NONE);
	checkCancel();

	// Bug 303953: Ensure that if all resources have been removed from a
	// folder, than the folder still
	// appears in the subdir list so it's subdir.mk is correctly regenerated
	getSubdirList().addAll(getModifiedList());

	// Make sure there is something to build
	if (getSubdirList().isEmpty()) {
	    String info = ManagedMakeMessages.getFormattedString("MakefileGenerator.warning.no.source", project.getName()); //$NON-NLS-1$
	    updateMonitor(info);
	    status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.INFO, new String(), null);
	    status.add(new Status(IStatus.INFO, ManagedBuilderCorePlugin.getUniqueIdentifier(), NO_SOURCE_FOLDERS, info, null));
	    return status;
	}

	// Make sure the build directory is available
	topBuildDir = createDirectory(config.getName());
	checkCancel();

	// Make sure that there is a makefile containing all the folders
	// participating
	IPath srcsFilePath = topBuildDir.append(SRCSFILE_NAME);
	IFile srcsFileHandle = createFile(srcsFilePath);
	buildSrcVars.clear();
	buildOutVars.clear();
	buildDepVars.clear();
	topBuildOutVars.clear();
	populateSourcesMakefile(srcsFileHandle);
	checkCancel();

	// Regenerate any fragments that are missing for the exisiting
	// directories NOT modified
	for (IResource res : getSubdirList()) {
	    IContainer subdirectory = (IContainer) res;
	    if (!getModifiedList().contains(subdirectory)) {
		// Make sure the directory exists (it may have been deleted)
		if (!subdirectory.exists()) {
		    appendDeletedSubdirectory(subdirectory);
		    continue;
		}
		// Make sure a fragment makefile exists
		IPath fragmentPath = getBuildWorkingDir().append(subdirectory.getProjectRelativePath()).append(MODFILE_NAME);
		IFile makeFragment = project.getFile(fragmentPath);
		if (!makeFragment.exists()) {
		    // If one or both are missing, then add it to the list to be
		    // generated
		    getModifiedList().add(subdirectory);
		}
	    }
	}

	// Delete the old dependency files for any deleted resources
	for (IResource deletedFile : getDeletedFileList()) {
	    deleteDepFile(deletedFile);
	    deleteBuildTarget(deletedFile);
	}

	// Regenerate any fragments for modified directories
	for (IResource res : getModifiedList()) {
	    IContainer subDir = (IContainer) res;
	    // Make sure the directory exists (it may have been deleted)
	    if (!subDir.exists()) {
		appendDeletedSubdirectory(subDir);
		continue;
	    }
	    // populateFragmentMakefile(subDir); // See below
	    checkCancel();
	}

	// Recreate all module makefiles
	// NOTE WELL: For now, always recreate all of the fragment makefile.
	// This is necessary
	// in order to re-populate the buildVariable lists. In the future, the
	// list could
	// possibly segmented by subdir so that all fragments didn't need to be
	// regenerated
	for (IResource res : getSubdirList()) {
	    IContainer subDir = (IContainer) res;
	    try {
		populateFragmentMakefile(subDir);
	    } catch (CoreException e) {
		// Probably should ask user if they want to continue
		checkCancel();
		continue;
	    }
	    checkCancel();
	}

	// Calculate the inputs and outputs of the Tools to be generated in the
	// main makefile
	calculateToolInputsOutputs();
	checkCancel();

	// Re-create the top-level makefile
	IPath makefilePath = topBuildDir.append(MAKEFILE_NAME);
	IFile makefileHandle = createFile(makefilePath);
	populateTopMakefile(makefileHandle, false);
	checkCancel();

	// Remove deleted folders from generated build directory
	for (IResource res : getDeletedDirList()) {
	    IContainer subDir = (IContainer) res;
	    removeGeneratedDirectory(subDir);
	    checkCancel();
	}

	// How did we do
	if (!getInvalidDirList().isEmpty()) {
	    status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.WARNING, new String(), null);
	    // Add a new status for each of the bad folders
	    // TODO: fix error message
	    for (IResource res : getInvalidDirList()) {
		IContainer subDir = (IContainer) res;
		status.add(new Status(IStatus.WARNING, ManagedBuilderCorePlugin.getUniqueIdentifier(), SPACES_IN_PATH, subDir.getFullPath()
			.toString(), null));
	    }
	} else {
	    status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.OK, new String(), null);
	}

	return status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator #getBuildWorkingDir()
     */
    @Override
    public IPath getBuildWorkingDir() {
	if (topBuildDir != null) {
	    return topBuildDir.removeFirstSegments(1);
	}
	return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator #getMakefileName()
     */
    @Override
    public String getMakefileName() {
	return new String(MAKEFILE_NAME);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator #isGeneratedResource(org.eclipse.core.resources.IResource)
     */
    @Override
    public boolean isGeneratedResource(IResource resource) {
	// Is this a generated directory ...
	IPath path = resource.getProjectRelativePath();
	// TODO: fix to use builder output dir instead
	String[] configNames = ManagedBuildManager.getBuildInfo(project).getConfigurationNames();
	for (String name : configNames) {
	    IPath root = new Path(name);
	    // It is if it is a root of the resource pathname
	    if (root.isPrefixOf(path))
		return true;
	}

	return false;
    }

    private static void save(StringBuffer buffer, IFile file) throws CoreException {
	String encoding = null;
	try {
	    encoding = file.getCharset();
	} catch (CoreException ce) {
	    // use no encoding
	}

	byte[] bytes = null;
	if (encoding != null) {
	    try {
		bytes = buffer.toString().getBytes(encoding);
	    } catch (Exception e) {// JABA is not going to write this code
	    }
	} else {
	    bytes = buffer.toString().getBytes();
	}

	ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
	// use a platform operation to update the resource contents
	boolean force = true;
	file.setContents(stream, force, false, null); // Don't record history
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator #regenerateDependencies()
     */
    @Override
    public void regenerateDependencies(boolean force) throws CoreException {
	// A hack for the pre-3.x GCC compilers is to put dummy targets for deps
	final IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
	final CoreException[] es = new CoreException[1];

	toolInfos.accept(new IPathSettingsContainerVisitor() {
	    @SuppressWarnings("synthetic-access")
	    @Override
	    public boolean visit(PathSettingsContainer container) {
		ToolInfoHolder h = (ToolInfoHolder) container.getValue();
		// Collect the methods that will need to be called
		Vector<String> depExts = new Vector<String>(); // Vector of
							       // dependency
							       // file
							       // extensions
		IManagedDependencyGenerator2[] postProcessors = new IManagedDependencyGenerator2[h.buildTools.length];
		boolean callPopulateDummyTargets = collectDependencyGeneratorInformation(h, depExts, postProcessors);

		// Is there anyone to call if we do find dependency files?
		if (!callPopulateDummyTargets) {
		    int i;
		    for (i = 0; i < postProcessors.length; i++) {
			if (postProcessors[i] != null)
			    break;
		    }
		    if (i == postProcessors.length)
			return true;
		}

		IResourceInfo rcInfo = config.getResourceInfo(container.getPath(), false);
		for (IPath path : getDependencyMakefiles(h)) {
		    // The path to search for the dependency makefile
		    IPath relDepFilePath = topBuildDir.append(path);
		    IFile depFile = root.getFile(relDepFilePath);
		    if (depFile == null || !depFile.isAccessible())
			continue;
		    try {
			callDependencyPostProcessors(rcInfo, h, depFile, postProcessors, callPopulateDummyTargets, true);
		    } catch (CoreException e) {
			es[0] = e;
			return false;
		    }
		}
		return true;
	    }
	});

	if (es[0] != null)
	    throw es[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator #regenerateMakefiles()
     */
    @Override
    public MultiStatus regenerateMakefiles() throws CoreException {
	MultiStatus status;
	// Visit the resources in the project
	ResourceProxyVisitor visitor = new ResourceProxyVisitor(this, config);
	project.accept(visitor, IResource.NONE);

	// See if the user has cancelled the build
	checkCancel();

	// Populate the makefile if any buildable source files have been found
	// in the project
	if (getSubdirList().isEmpty()) {
	    String info = ManagedMakeMessages.getFormattedString("MakefileGenerator.warning.no.source", project.getName()); //$NON-NLS-1$
	    updateMonitor(info);
	    status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.INFO, new String(), null);
	    status.add(new Status(IStatus.INFO, ManagedBuilderCorePlugin.getUniqueIdentifier(), NO_SOURCE_FOLDERS, info, null));
	    return status;
	}

	// Create the top-level directory for the build output
	topBuildDir = createDirectory(config.getName());
	checkCancel();

	// Get the list of subdirectories
	IPath srcsFilePath = topBuildDir.append(SRCSFILE_NAME);
	IFile srcsFileHandle = createFile(srcsFilePath);
	buildSrcVars.clear();
	buildOutVars.clear();
	buildDepVars.clear();
	topBuildOutVars.clear();
	populateSourcesMakefile(srcsFileHandle);
	checkCancel();

	// Now populate the module makefiles
	for (IResource res : getSubdirList()) {
	    IContainer subDir = (IContainer) res;
	    try {
		populateFragmentMakefile(subDir);
	    } catch (CoreException e) {
		// Probably should ask user if they want to continue
		checkCancel();
		continue;
	    }
	    checkCancel();
	}

	// Calculate the inputs and outputs of the Tools to be generated in the
	// main makefile
	calculateToolInputsOutputs();
	checkCancel();

	// Create the top-level makefile
	IPath makefilePath = topBuildDir.append(MAKEFILE_NAME);
	IFile makefileHandle = createFile(makefilePath);
	populateTopMakefile(makefileHandle, true);
	checkCancel();

	// Now finish up by adding all the object files
	IPath objFilePath = topBuildDir.append(OBJECTS_MAKFILE);
	IFile objsFileHandle = createFile(objFilePath);
	populateObjectsMakefile(objsFileHandle);
	checkCancel();

	// How did we do
	if (!getInvalidDirList().isEmpty()) {
	    status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.WARNING, new String(), null);
	    // Add a new status for each of the bad folders
	    // TODO: fix error message
	    for (IResource dir : getInvalidDirList()) {
		status.add(new Status(IStatus.WARNING, ManagedBuilderCorePlugin.getUniqueIdentifier(), SPACES_IN_PATH, dir.getFullPath().toString(),
			null));
	    }
	} else {
	    status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.OK, new String(), null);
	}
	return status;
    }

    /*************************************************************************
     * M A K E F I L E S P O P U L A T I O N M E T H O D S
     ************************************************************************/

    /**
     * This method generates a "fragment" make file (subdir.mk). One of these is generated for each project directory/subdirectory that contains
     * source files.
     */
    protected void populateFragmentMakefile(IContainer module) throws CoreException {
	// Calculate the new directory relative to the build output
	IPath moduleRelativePath = module.getProjectRelativePath();
	IPath buildRoot = getBuildWorkingDir();
	if (buildRoot == null) {
	    return;
	}

	IPath moduleOutputPath = buildRoot.append(moduleRelativePath);
	updateMonitor(ManagedMakeMessages.getFormattedString("MakefileGenerator.message.gen.source.makefile", moduleOutputPath.toString())); //$NON-NLS-1$

	// Now create the directory
	IPath moduleOutputDir = createDirectory(moduleOutputPath.toString());

	// Create a module makefile
	IFile modMakefile = createFile(moduleOutputDir.append(MODFILE_NAME));
	StringBuffer makeBuf = new StringBuffer();
	makeBuf.append(addFragmentMakefileHeader());
	makeBuf.append(addSources(module));

	// Save the files
	save(makeBuf, modMakefile);
    }

    /**
     * The makefile generator generates a Macro for each type of output, other than final artifact, created by the build.
     * 
     * @param fileHandle
     *            The file that should be populated with the output
     */
    protected void populateObjectsMakefile(IFile fileHandle) throws CoreException {

	// Master list of "object" dependencies, i.e. dependencies between input
	// files and output files.
	StringBuffer macroBuffer = new StringBuffer();
	List<String> valueList;
	macroBuffer.append(addDefaultHeader());

	// Map of macro names (String) to its definition (List of Strings)
	HashMap<String, List<String>> outputMacros = new HashMap<String, List<String>>();

	// Add the predefined LIBS, USER_OBJS macros

	// Add the libraries this project depends on
	valueList = new ArrayList<String>();
	String[] libs = config.getLibs(buildTargetExt);
	for (String lib : libs) {
	    valueList.add(lib);
	}
	outputMacros.put("LIBS", valueList); //$NON-NLS-1$

	// Add the extra user-specified objects
	valueList = new ArrayList<String>();
	String[] userObjs = config.getUserObjects(buildTargetExt);
	for (String obj : userObjs) {
	    valueList.add(obj);
	}
	outputMacros.put("USER_OBJS", valueList); //$NON-NLS-1$

	// Write every macro to the file
	for (Entry<String, List<String>> entry : outputMacros.entrySet()) {
	    macroBuffer.append(entry.getKey() + " :="); //$NON-NLS-1$
	    valueList = entry.getValue();
	    for (String path : valueList) {
		// These macros will also be used within commands.
		// Make all the slashes go forward so they aren't
		// interpreted as escapes and get lost.
		// See https://bugs.eclipse.org/163672.
		path = path.replace('\\', '/');

		path = ensurePathIsGNUMakeTargetRuleCompatibleSyntax(path);

		macroBuffer.append(WHITESPACE);
		macroBuffer.append(path);
	    }
	    // terminate the macro definition line
	    macroBuffer.append(NEWLINE);
	    // leave a blank line before the next macro
	    macroBuffer.append(NEWLINE);
	}

	// For now, just save the buffer that was populated when the rules were
	// created
	save(macroBuffer, fileHandle);

    }

    protected void populateSourcesMakefile(IFile fileHandle) throws CoreException {
	// Add the comment
	StringBuffer buffer = addDefaultHeader();

	// Determine the set of macros
	toolInfos.accept(new IPathSettingsContainerVisitor() {

	    @Override
	    public boolean visit(PathSettingsContainer container) {
		ToolInfoHolder h = (ToolInfoHolder) container.getValue();
		ITool[] buildTools = h.buildTools;
		HashSet<String> handledInputExtensions = new HashSet<String>();
		String buildMacro;
		for (ITool buildTool : buildTools) {
		    if (buildTool.getCustomBuildStep())
			continue;
		    // Add the known sources macros
		    String[] extensionsList = buildTool.getAllInputExtensions();
		    for (String ext : extensionsList) {
			// create a macro of the form "EXTENSION_SRCS :="
			String extensionName = ext;
			if (// !getOutputExtensions().contains(extensionName) &&
			!handledInputExtensions.contains(extensionName)) {
			    handledInputExtensions.add(extensionName);
			    buildMacro = getSourceMacroName(extensionName).toString();
			    if (!buildSrcVars.containsKey(buildMacro)) {
				buildSrcVars.put(buildMacro, new ArrayList<IPath>());
			    }
			    // Add any generated dependency file macros
			    IManagedDependencyGeneratorType depType = buildTool.getDependencyGeneratorForExtension(extensionName);
			    if (depType != null) {
				int calcType = depType.getCalculatorType();
				if (calcType == IManagedDependencyGeneratorType.TYPE_COMMAND
					|| calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS
					|| calcType == IManagedDependencyGeneratorType.TYPE_PREBUILD_COMMANDS) {
				    buildMacro = getDepMacroName(extensionName).toString();
				    if (!buildDepVars.containsKey(buildMacro)) {
					buildDepVars.put(buildMacro, new ArduinoGnuDependencyGroupInfo(buildMacro,
						(calcType != IManagedDependencyGeneratorType.TYPE_PREBUILD_COMMANDS)));
				    }
				    if (!buildOutVars.containsKey(buildMacro)) {
					buildOutVars.put(buildMacro, new ArrayList<IPath>());
				    }
				}
			    }
			}
		    }
		    // Add the specified output build variables
		    IOutputType[] outTypes = buildTool.getOutputTypes();
		    if (outTypes != null && outTypes.length > 0) {
			for (IOutputType outputType : outTypes) {
			    buildMacro = outputType.getBuildVariable();
			    if (!buildOutVars.containsKey(buildMacro)) {
				buildOutVars.put(buildMacro, new ArrayList<IPath>());
			    }
			}
		    } else {
			// For support of pre-CDT 3.0 integrations.
			buildMacro = OBJS_MACRO;
			if (!buildOutVars.containsKey(buildMacro)) {
			    buildOutVars.put(buildMacro, new ArrayList<IPath>());
			}
		    }
		}
		return true;
	    }
	});
	// Add the macros to the makefile
	for (Entry<String, List<IPath>> entry : buildSrcVars.entrySet()) {
	    String macroName = entry.getKey();
	    buffer.append(macroName + WHITESPACE + ":=" + WHITESPACE + NEWLINE); //$NON-NLS-1$
	}
	Set<Entry<String, List<IPath>>> set = buildOutVars.entrySet();
	for (Entry<String, List<IPath>> entry : set) {
	    String macroName = entry.getKey();
	    buffer.append(macroName + WHITESPACE + ":=" + WHITESPACE + NEWLINE); //$NON-NLS-1$
	}

	// Add a list of subdirectories to the makefile
	buffer.append(NEWLINE + addSubdirectories());

	// Save the file
	save(buffer, fileHandle);
    }

    /**
     * Create the entire contents of the makefile.
     * 
     * @param fileHandle
     *            The file to place the contents in.
     * @param rebuild
     *            FLag signaling that the user is doing a full rebuild
     */
    protected void populateTopMakefile(IFile fileHandle, boolean rebuild) throws CoreException {
	StringBuffer buffer = new StringBuffer();

	// Add the header
	buffer.append(addTopHeader());

	// Add the macro definitions
	buffer.append(addMacros());

	// List to collect needed build output variables
	List<String> outputVarsAdditionsList = new ArrayList<String>();

	// Determine target rules
	StringBuffer targetRules = addTargets(outputVarsAdditionsList, rebuild);

	// Add outputMacros that were added to by the target rules
	buffer.append(writeTopAdditionMacros(outputVarsAdditionsList, getTopBuildOutputVars()));

	// Add target rules
	buffer.append(targetRules);

	// Save the file
	save(buffer, fileHandle);
    }

    /*************************************************************************
     * M A I N (makefile) M A K E F I L E M E T H O D S
     ************************************************************************/

    /**
     * Answers a <code>StringBuffer</code> containing the comment(s) for the top-level makefile.
     */
    @SuppressWarnings("static-method")
    protected StringBuffer addTopHeader() {
	return addDefaultHeader();
    }

    /**
	 */
    private StringBuffer addMacros() {
	StringBuffer buffer = new StringBuffer();

	// Add the ROOT macro
	//buffer.append("ROOT := .." + NEWLINE); //$NON-NLS-1$
	// buffer.append(NEWLINE);

	// include makefile.init supplementary makefile
	buffer.append("-include " + ROOT + SEPARATOR + MAKEFILE_INIT + NEWLINE); //$NON-NLS-1$
	buffer.append(NEWLINE);

	// Get the clean command from the build model
	buffer.append("RM := "); //$NON-NLS-1$

	// support macros in the clean command
	String cleanCommand = "rm -rf"; // config.getCleanCommand(); Modded by
					// JABA

	// try {
	// cleanCommand =
	// ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(config.getCleanCommand(),
	// EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_CONFIGURATION,
	// config);
	// } catch (BuildMacroException e) {// JABA is not going to write this
	// code
	// }

	buffer.append(cleanCommand + NEWLINE);

	buffer.append(NEWLINE);

	// Now add the source providers
	buffer.append(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(SRC_LISTS) + NEWLINE);
	buffer.append("-include sources.mk" + NEWLINE); //$NON-NLS-1$

	// Change the include of the "root" (our sketch) folder to be before libraries and other files
	// When it is after it is different to the Arduino IDE and can cause linking/combining issues
	buffer.append("-include subdir.mk" + NEWLINE); //$NON-NLS-1$

	// Add includes for each subdir in child-subdir-first order (required
	// for makefile rule matching to work).
	List<String> subDirList = new ArrayList<String>();
	for (IContainer subDir : getSubdirList()) {
	    IPath projectRelativePath = subDir.getProjectRelativePath();
	    if (!projectRelativePath.toString().equals("")) //$NON-NLS-1$
		subDirList.add(0, projectRelativePath.toString());
	}
	Collections.sort(subDirList, Collections.reverseOrder());
	for (String dir : subDirList) {
	    buffer.append("-include " + escapeWhitespaces(dir) + SEPARATOR + "subdir.mk" + NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
	}

	// Change the include of the "root" (our sketch) folder to be before libraries and other files
	//buffer.append("-include subdir.mk" + NEWLINE); //$NON-NLS-1$

	buffer.append("-include objects.mk" + NEWLINE + NEWLINE); //$NON-NLS-1$

	// Include generated dependency makefiles if non-empty AND a "clean" has
	// not been requested
	if (!buildDepVars.isEmpty()) {
	    buffer.append("ifneq ($(MAKECMDGOALS),clean)" + NEWLINE); //$NON-NLS-1$

	    for (Entry<String, ArduinoGnuDependencyGroupInfo> entry : buildDepVars.entrySet()) {
		String depsMacro = entry.getKey();
		ArduinoGnuDependencyGroupInfo info = entry.getValue();
		buffer.append("ifneq ($(strip $(" + depsMacro + ")),)" + NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
		if (info.conditionallyInclude) {
		    buffer.append("-include $(" + depsMacro + ")" + NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
		    buffer.append("include $(" + depsMacro + ")" + NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
		}
		buffer.append("endif" + NEWLINE); //$NON-NLS-1$
	    }

	    buffer.append("endif" + NEWLINE + NEWLINE); //$NON-NLS-1$
	}

	// Include makefile.defs supplemental makefile
	buffer.append("-include " + ROOT + SEPARATOR + MAKEFILE_DEFS + NEWLINE); //$NON-NLS-1$

	return (buffer.append(NEWLINE));
    }

    /**
     * Answers a <code>StringBuffer</code> containing all of the required targets to properly build the project.
     * 
     * @param outputVarsAdditionsList
     *            list to add needed build output variables to
     */
    private StringBuffer addTargets(List<String> outputVarsAdditionsList, boolean rebuild) {
	StringBuffer buffer = new StringBuffer();

	// IConfiguration config = info.getDefaultConfiguration();

	// Assemble the information needed to generate the targets
	String prebuildStep = config.getPrebuildStep();
	try {
	    // try to resolve the build macros in the prebuild step
	    prebuildStep = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(prebuildStep, EMPTY_STRING, WHITESPACE,
		    IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
	} catch (BuildMacroException e) {// JABA is not going to write this code
	}
	prebuildStep = prebuildStep.trim(); // Remove leading and trailing
					    // whitespace (and control
					    // characters)

	String postbuildStep = config.getPostbuildStep();
	try {
	    // try to resolve the build macros in the postbuild step
	    postbuildStep = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(postbuildStep, EMPTY_STRING, WHITESPACE,
		    IBuildMacroProvider.CONTEXT_CONFIGURATION, config);

	} catch (BuildMacroException e) {// JABA is not going to write this code
	}
	postbuildStep = postbuildStep.trim(); // Remove leading and trailing
					      // whitespace (and control
					      // characters)
	String preannouncebuildStep = config.getPreannouncebuildStep();
	String postannouncebuildStep = config.getPostannouncebuildStep();
	String targets = rebuild ? "clean all" : "all"; //$NON-NLS-1$ //$NON-NLS-2$

	ITool targetTool = config.calculateTargetTool();
	// if (targetTool == null) {
	// targetTool = info.getToolFromOutputExtension(buildTargetExt);
	// }

	// Get all the projects the build target depends on
	// If this configuration produces a static archive, building the archive
	// doesn't depend on the output
	// from any of the referenced configurations
	IConfiguration[] refConfigs = new IConfiguration[0];
	if (config.getBuildArtefactType() == null
		|| !ManagedBuildManager.BUILD_ARTEFACT_TYPE_PROPERTY_STATICLIB.equals(config.getBuildArtefactType().getId()))
	    refConfigs = ManagedBuildManager.getReferencedConfigurations(config);

	/*
	 * try { refdProjects = project.getReferencedProjects(); } catch (CoreException e) { // There are 2 exceptions; the project does not exist or
	 * it is not open // and neither conditions apply if we are building for it .... }
	 */
	// If a prebuild step exists, redefine the all target to be
	// all: {pre-build} main-build
	// and then reset the "traditional" all target to main-build
	// This will allow something meaningful to happen if the generated
	// makefile is
	// extracted and run standalone via "make all"
	//
	String defaultTarget = "all:"; //$NON-NLS-1$
	if (prebuildStep.length() > 0) {

	    // Add the comment for the "All" target
	    buffer.append(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(ALL_TARGET) + NEWLINE);

	    buffer.append(defaultTarget + WHITESPACE);
	    buffer.append(PREBUILD + WHITESPACE);

	    // Reset defaultTarget for now and for subsequent use, below
	    defaultTarget = MAINBUILD;
	    buffer.append(defaultTarget);

	    // Update the defaultTarget, main-build, by adding a colon, which is
	    // needed below
	    defaultTarget = defaultTarget.concat(COLON);
	    buffer.append(NEWLINE + NEWLINE);

	    // Add the comment for the "main-build" target
	    buffer.append(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(MAINBUILD_TARGET) + NEWLINE);
	} else
	    // Add the comment for the "All" target
	    buffer.append(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(ALL_TARGET) + NEWLINE);

	// Write out the all target first in case someone just runs make
	// all: <target_name> or mainbuild: <target_name>

	String outputPrefix = EMPTY_STRING;
	if (targetTool != null) {
	    outputPrefix = targetTool.getOutputPrefix();
	}
	buffer.append(defaultTarget + WHITESPACE + outputPrefix + ensurePathIsGNUMakeTargetRuleCompatibleSyntax(buildTargetName));
	if (buildTargetExt.length() > 0) {
	    buffer.append(DOT + buildTargetExt);
	}

	// Add the Secondary Outputs to the all target, if any
	IOutputType[] secondaryOutputs = config.getToolChain().getSecondaryOutputs();
	if (secondaryOutputs.length > 0) {
	    buffer.append(WHITESPACE + SECONDARY_OUTPUTS);
	}

	buffer.append(NEWLINE + NEWLINE);

	/*
	 * The build target may depend on other projects in the workspace. These are captured in the deps target: deps: <cd <Proj_Dep_1/build_dir>;
	 * $(MAKE) [clean all | all]>
	 */
	// Vector managedProjectOutputs = new Vector(refdProjects.length);
	// if (refdProjects.length > 0) {
	Vector<String> managedProjectOutputs = new Vector<String>(refConfigs.length);
	if (refConfigs.length > 0) {
	    boolean addDeps = true;
	    // if (refdProjects != null) {
	    for (IConfiguration depCfg : refConfigs) {
		// IProject dep = refdProjects[i];
		if (!depCfg.isManagedBuildOn())
		    continue;

		// if (!dep.exists()) continue;
		if (addDeps) {
		    buffer.append("dependents:" + NEWLINE); //$NON-NLS-1$
		    addDeps = false;
		}
		String buildDir = depCfg.getOwner().getLocation().toString();
		String depTargets = targets;
		// if (ManagedBuildManager.manages(dep)) {
		// Add the current configuration to the makefile path
		// IManagedBuildInfo depInfo =
		// ManagedBuildManager.getBuildInfo(dep);
		buildDir += SEPARATOR + depCfg.getName();

		// Extract the build artifact to add to the dependency list
		String depTarget = depCfg.getArtifactName();
		String depExt = depCfg.getArtifactExtension();

		try {
		    // try to resolve the build macros in the artifact extension
		    depExt = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(depExt, "", //$NON-NLS-1$
			    " ", //$NON-NLS-1$
			    IBuildMacroProvider.CONTEXT_CONFIGURATION, depCfg);
		} catch (BuildMacroException e) {// JABA is not going to write
						 // this code
		}

		try {
		    // try to resolve the build macros in the artifact name
		    String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(depTarget, "", //$NON-NLS-1$
			    " ", //$NON-NLS-1$
			    IBuildMacroProvider.CONTEXT_CONFIGURATION, depCfg);
		    if ((resolved = resolved.trim()).length() > 0)
			depTarget = resolved;
		} catch (BuildMacroException e) {// JABA is not going to write
						 // this code
		}

		String depPrefix = depCfg.getOutputPrefix(depExt);
		if (depCfg.needsRebuild()) {
		    depTargets = "clean all"; //$NON-NLS-1$
		}
		String dependency = buildDir + SEPARATOR + depPrefix + depTarget;
		if (depExt.length() > 0) {
		    dependency += DOT + depExt;
		}
		dependency = escapeWhitespaces(dependency);
		managedProjectOutputs.add(dependency);
		// }
		buffer.append(TAB
			+ "-cd" + WHITESPACE + escapeWhitespaces(buildDir) + WHITESPACE + LOGICAL_AND + WHITESPACE + "$(MAKE) " + depTargets + NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    // }
	    buffer.append(NEWLINE);
	}

	// Add the targets tool rules
	buffer.append(addTargetsRules(targetTool, outputVarsAdditionsList, managedProjectOutputs, (postbuildStep.length() > 0)));

	// Add the prebuild step target, if specified
	if (prebuildStep.length() > 0) {
	    buffer.append(PREBUILD + COLON + NEWLINE);
	    if (preannouncebuildStep.length() > 0) {
		buffer.append(TAB + DASH + AT + escapedEcho(preannouncebuildStep));
	    }
	    buffer.append(TAB + DASH + prebuildStep + NEWLINE);
	    buffer.append(TAB + DASH + AT + ECHO_BLANK_LINE + NEWLINE);
	}

	// Add the postbuild step, if specified
	if (postbuildStep.length() > 0) {
	    buffer.append(POSTBUILD + COLON + NEWLINE);
	    if (postannouncebuildStep.length() > 0) {
		buffer.append(TAB + DASH + AT + escapedEcho(postannouncebuildStep));
	    }
	    buffer.append(TAB + DASH + postbuildStep + NEWLINE);
	    buffer.append(TAB + DASH + AT + ECHO_BLANK_LINE + NEWLINE);
	}

	// Add the Secondary Outputs target, if needed
	if (secondaryOutputs.length > 0) {
	    buffer.append(SECONDARY_OUTPUTS + COLON);
	    Vector<String> outs2 = calculateSecondaryOutputs(secondaryOutputs);
	    for (int i = 0; i < outs2.size(); i++) {
		buffer.append(WHITESPACE + "$(" + outs2.get(i) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    buffer.append(NEWLINE + NEWLINE);
	}

	// Add all the needed dummy and phony targets
	buffer.append(".PHONY: all clean dependents" + NEWLINE); //$NON-NLS-1$
	buffer.append(".SECONDARY:"); //$NON-NLS-1$
	if (prebuildStep.length() > 0) {
	    buffer.append(WHITESPACE + MAINBUILD + WHITESPACE + PREBUILD);
	}
	if (postbuildStep.length() > 0) {
	    buffer.append(WHITESPACE + POSTBUILD);
	}
	buffer.append(NEWLINE);
	for (String output : managedProjectOutputs) {
	    buffer.append(output + COLON + NEWLINE);
	}
	buffer.append(NEWLINE);

	// Include makefile.targets supplemental makefile
	buffer.append("-include " + ROOT + SEPARATOR + MAKEFILE_TARGETS + NEWLINE); //$NON-NLS-1$

	return buffer;
    }

    /**
     * Returns the targets rules. The targets make file (top makefile) contains: 1 the rule for the final target tool 2 the rules for all of the tools
     * that use multipleOfType in their primary input type 3 the rules for all tools that use the output of #2 tools
     * 
     * @param outputVarsAdditionsList
     *            list to add needed build output variables to
     * @param managedProjectOutputs
     *            Other projects in the workspace that this project depends upon
     * @return StringBuffer
     */
    private StringBuffer addTargetsRules(ITool targetTool, List<String> outputVarsAdditionsList, Vector<String> managedProjectOutputs,
	    boolean postbuildStep) {
	StringBuffer buffer = new StringBuffer();
	// Add the comment
	buffer.append(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(BUILD_TOP) + NEWLINE);

	ToolInfoHolder h = (ToolInfoHolder) toolInfos.getValue();
	ITool[] buildTools = h.buildTools;
	boolean[] buildToolsUsed = h.buildToolsUsed;
	// Get the target tool and generate the rule
	if (targetTool != null) {
	    // Note that the name of the target we pass to addRuleForTool does
	    // not
	    // appear to be used there (and tool outputs are consulted
	    // directly), but
	    // we quote it anyway just in case it starts to use it in future.
	    if (addRuleForTool(targetTool, buffer, true, ensurePathIsGNUMakeTargetRuleCompatibleSyntax(buildTargetName), buildTargetExt,
		    outputVarsAdditionsList, managedProjectOutputs, postbuildStep)) {
		// Mark the target tool as processed
		for (int i = 0; i < buildTools.length; i++) {
		    if (targetTool == buildTools[i]) {
			buildToolsUsed[i] = true;
		    }
		}
	    }
	} else {
	    buffer.append(TAB + AT + escapedEcho(MESSAGE_NO_TARGET_TOOL + WHITESPACE + OUT_MACRO));
	}

	// Generate the rules for all Tools that specify
	// InputType.multipleOfType, and any Tools that
	// consume the output of those tools. This does not apply to pre-3.0
	// integrations, since
	// the only "multipleOfType" tool is the "target" tool
	for (int i = 0; i < buildTools.length; i++) {
	    ITool tool = buildTools[i];
	    IInputType type = tool.getPrimaryInputType();
	    if (type != null && type.getMultipleOfType()) {
		if (!buildToolsUsed[i]) {
		    addRuleForTool(tool, buffer, false, null, null, outputVarsAdditionsList, null, false);
		    // Mark the target tool as processed
		    buildToolsUsed[i] = true;
		    // Look for tools that consume the output
		    generateRulesForConsumers(tool, outputVarsAdditionsList, buffer);
		}
	    }
	}

	// Add the comment
	buffer.append(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(BUILD_TARGETS) + NEWLINE);

	// Always add a clean target
	buffer.append("clean:" + NEWLINE); //$NON-NLS-1$
	buffer.append(TAB + "-$(RM)" + WHITESPACE); //$NON-NLS-1$
	for (Entry<String, List<IPath>> entry : buildOutVars.entrySet()) {
	    String macroName = entry.getKey();
	    buffer.append("$(" + macroName + ")"); //$NON-NLS-1$	//$NON-NLS-2$
	}
	String outputPrefix = EMPTY_STRING;
	if (targetTool != null) {
	    outputPrefix = targetTool.getOutputPrefix();
	}
	String completeBuildTargetName = outputPrefix + buildTargetName;
	if (buildTargetExt.length() > 0) {
	    completeBuildTargetName = completeBuildTargetName + DOT + buildTargetExt;
	}
	if (completeBuildTargetName.contains(" ")) { //$NON-NLS-1$
	    buffer.append(WHITESPACE + "\"" + completeBuildTargetName + "\""); //$NON-NLS-1$	//$NON-NLS-2$
	} else {
	    buffer.append(WHITESPACE + completeBuildTargetName);
	}
	buffer.append(NEWLINE);
	buffer.append(TAB + DASH + AT + ECHO_BLANK_LINE + NEWLINE);

	return buffer;
    }

    /**
     * Create the rule
     * 
     * @param buffer
     *            Buffer to add makefile rules to
     * @param bTargetTool
     *            True if this is the target tool
     * @param targetName
     *            If this is the "targetTool", the target file name, else <code>null</code>
     * @param targetExt
     *            If this is the "targetTool", the target file extension, else <code>null</code>
     * @param outputVarsAdditionsList
     *            list to add needed build output variables to
     * @param managedProjectOutputs
     *            Other projects in the workspace that this project depends upon
     * @param bEmitPostBuildStepCall
     *            Emit post-build step invocation
     */
    protected boolean addRuleForTool(ITool tool, StringBuffer buffer, boolean bTargetTool, String targetName, String targetExt,
	    List<String> outputVarsAdditionsList, Vector<String> managedProjectOutputs, boolean bEmitPostBuildStepCall) {

	// Get the tool's inputs and outputs
	Vector<String> inputs = new Vector<String>();
	Vector<String> dependencies = new Vector<String>();
	Vector<String> outputs = new Vector<String>();
	Vector<String> enumeratedPrimaryOutputs = new Vector<String>();
	Vector<String> enumeratedSecondaryOutputs = new Vector<String>();
	Vector<String> outputVariables = new Vector<String>();
	Vector<String> additionalTargets = new Vector<String>();
	String outputPrefix = EMPTY_STRING;

	if (!getToolInputsOutputs(tool, inputs, dependencies, outputs, enumeratedPrimaryOutputs, enumeratedSecondaryOutputs, outputVariables,
		additionalTargets, bTargetTool, managedProjectOutputs)) {
	    return false;
	}

	// If we have no primary output, make all of the secondary outputs the
	// primary output
	if (enumeratedPrimaryOutputs.size() == 0) {
	    enumeratedPrimaryOutputs = enumeratedSecondaryOutputs;
	    enumeratedSecondaryOutputs.clear();
	}

	// Add the output variables for this tool to our list
	outputVarsAdditionsList.addAll(outputVariables);

	// Create the build rule
	String buildRule = EMPTY_STRING;
	String outflag = tool.getOutputFlag();

	String primaryOutputs = EMPTY_STRING;
	String primaryOutputsQuoted = EMPTY_STRING;
	boolean first = true;
	for (int i = 0; i < enumeratedPrimaryOutputs.size(); i++) {
	    String output = enumeratedPrimaryOutputs.get(i);
	    if (!first) {
		primaryOutputs += WHITESPACE;
		primaryOutputsQuoted += WHITESPACE;
	    }
	    first = false;
	    primaryOutputs += output;
	    primaryOutputsQuoted += ensurePathIsGNUMakeTargetRuleCompatibleSyntax(output);
	}

	buildRule += (primaryOutputsQuoted + COLON + WHITESPACE);

	first = true;
	String calculatedDependencies = EMPTY_STRING;
	for (int i = 0; i < dependencies.size(); i++) {
	    String input = dependencies.get(i);
	    if (!first)
		calculatedDependencies += WHITESPACE;
	    first = false;
	    calculatedDependencies += input;
	}
	buildRule += calculatedDependencies;

	// We can't have duplicates in a makefile
	if (getRuleList().contains(buildRule)) {// JABA is not going to write
						// this code
	} else {
	    getRuleList().add(buildRule);
	    buffer.append(buildRule + NEWLINE);
	    if (bTargetTool) {
		buffer.append(TAB + AT + escapedEcho(MESSAGE_START_BUILD + WHITESPACE + OUT_MACRO));
	    }
	    buffer.append(TAB + AT + escapedEcho(tool.getAnnouncement()));

	    // Get the command line for this tool invocation
	    String[] flags;
	    try {
		flags = tool.getToolCommandFlags(null, null);
	    } catch (BuildException ex) {
		// TODO report error
		flags = EMPTY_STRING_ARRAY;
	    }
	    String command = tool.getToolCommand();
	    try {
		// try to resolve the build macros in the tool command
		String resolvedCommand = ManagedBuildManager
			.getBuildMacroProvider()
			.resolveValueToMakefileFormat(command, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
				new FileContextData(null, null, null, tool)).replaceFirst(" -w ", " ");
		if ((resolvedCommand = resolvedCommand.trim()).length() > 0)
		    command = resolvedCommand;

	    } catch (BuildMacroException e) {// JABA is not going to write this
					     // code
	    }
	    String[] cmdInputs = inputs.toArray(new String[inputs.size()]);
	    IManagedCommandLineGenerator gen = tool.getCommandLineGenerator();
	    IManagedCommandLineInfo cmdLInfo = gen.generateCommandLineInfo(tool, command, flags, outflag, outputPrefix, primaryOutputs, cmdInputs,
		    tool.getCommandLinePattern());

	    // The command to build
	    String buildCmd = null;
	    if (cmdLInfo == null) {
		String toolFlags;
		try {
		    toolFlags = tool.getToolCommandFlagsString(null, null);
		} catch (BuildException ex) {
		    // TODO report error
		    toolFlags = EMPTY_STRING;
		}
		buildCmd = command + WHITESPACE + toolFlags + WHITESPACE + outflag + WHITESPACE + outputPrefix + primaryOutputs + WHITESPACE
			+ IN_MACRO;
	    } else
		buildCmd = cmdLInfo.getCommandLine();

	    // resolve any remaining macros in the command after it has been
	    // generated
	    try {
		String resolvedCommand = ManagedBuildManager
			.getBuildMacroProvider()
			.resolveValueToMakefileFormat(buildCmd, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
				new FileContextData(null, null, null, tool)).replaceFirst(" -w ", " ");
		if ((resolvedCommand = resolvedCommand.trim()).length() > 0)
		    buildCmd = resolvedCommand;

	    } catch (BuildMacroException e) {// JABA is not going to write this
					     // code
	    }

	    // buffer.append(TAB + AT + escapedEcho(buildCmd));
	    // buffer.append(TAB + AT + buildCmd);
	    buffer.append(TAB + buildCmd);

	    // TODO
	    // NOTE WELL: Dependency file generation is not handled for this
	    // type of Tool

	    // Echo finished message
	    buffer.append(NEWLINE);
	    buffer.append(TAB + AT + escapedEcho((bTargetTool ? MESSAGE_FINISH_BUILD : MESSAGE_FINISH_FILE) + WHITESPACE + OUT_MACRO));
	    buffer.append(TAB + AT + ECHO_BLANK_LINE);

	    // If there is a post build step, then add a recursive invocation of
	    // MAKE to invoke it after the main build
	    // Note that $(MAKE) will instantiate in the recusive invocation to
	    // the make command that was used to invoke
	    // the makefile originally
	    if (bEmitPostBuildStepCall) {
		buffer.append(TAB + MAKE + WHITESPACE + NO_PRINT_DIR + WHITESPACE + POSTBUILD + NEWLINE + NEWLINE);
	    } else {
		// Just emit a blank line
		buffer.append(NEWLINE);
	    }
	}

	// If we have secondary outputs, output dependency rules without
	// commands
	if (enumeratedSecondaryOutputs.size() > 0 || additionalTargets.size() > 0) {
	    String primaryOutput = enumeratedPrimaryOutputs.get(0);
	    Vector<String> addlOutputs = new Vector<String>();
	    addlOutputs.addAll(enumeratedSecondaryOutputs);
	    addlOutputs.addAll(additionalTargets);
	    for (int i = 0; i < addlOutputs.size(); i++) {
		String output = addlOutputs.get(i);
		String depLine = output + COLON + WHITESPACE + primaryOutput + WHITESPACE + calculatedDependencies + NEWLINE;
		if (!getDepLineList().contains(depLine)) {
		    getDepLineList().add(depLine);
		    buffer.append(depLine);
		}
	    }
	    buffer.append(NEWLINE);
	}
	return true;
    }

    /**
     * @param outputVarsAdditionsList
     *            list to add needed build output variables to
     * @param buffer
     *            buffer to add rules to
     */
    private void generateRulesForConsumers(ITool generatingTool, List<String> outputVarsAdditionsList, StringBuffer buffer) {
	// Generate a build rule for any tool that consumes the output of this
	// tool
	ToolInfoHolder h = (ToolInfoHolder) toolInfos.getValue();
	ITool[] buildTools = h.buildTools;
	boolean[] buildToolsUsed = h.buildToolsUsed;
	IOutputType[] outTypes = generatingTool.getOutputTypes();
	for (IOutputType outType : outTypes) {
	    String[] outExts = outType.getOutputExtensions(generatingTool);
	    String outVariable = outType.getBuildVariable();
	    if (outExts != null) {
		for (String outExt : outExts) {
		    for (int k = 0; k < buildTools.length; k++) {
			ITool tool = buildTools[k];
			if (!buildToolsUsed[k]) {
			    // Also has to match build variables if specified
			    IInputType inType = tool.getInputType(outExt);
			    if (inType != null) {
				String inVariable = inType.getBuildVariable();
				if ((outVariable == null && inVariable == null)
					|| (outVariable != null && inVariable != null && outVariable.equals(inVariable))) {
				    if (addRuleForTool(buildTools[k], buffer, false, null, null, outputVarsAdditionsList, null, false)) {
					buildToolsUsed[k] = true;
					// Look for tools that consume the
					// output
					generateRulesForConsumers(buildTools[k], outputVarsAdditionsList, buffer);
				    }
				}
			    }
			}
		    }
		}
	    }
	}
    }

    protected boolean getToolInputsOutputs(ITool tool, Vector<String> inputs, Vector<String> dependencies, Vector<String> outputs,
	    Vector<String> enumeratedPrimaryOutputs, Vector<String> enumeratedSecondaryOutputs, Vector<String> outputVariables,
	    Vector<String> additionalTargets, boolean bTargetTool, Vector<String> managedProjectOutputs) {
	ToolInfoHolder h = (ToolInfoHolder) toolInfos.getValue();
	ITool[] buildTools = h.buildTools;
	ArduinoManagedBuildGnuToolInfo[] gnuToolInfos = h.gnuToolInfos;
	// Get the information regarding the tool's inputs and outputs from the
	// objects
	// created by calculateToolInputsOutputs
	IManagedBuildGnuToolInfo toolInfo = null;
	for (int i = 0; i < buildTools.length; i++) {
	    if (tool == buildTools[i]) {
		toolInfo = gnuToolInfos[i];
		break;
	    }
	}
	if (toolInfo == null)
	    return false;

	// Populate the output Vectors
	inputs.addAll(toolInfo.getCommandInputs());
	outputs.addAll(toolInfo.getCommandOutputs());
	enumeratedPrimaryOutputs.addAll(toolInfo.getEnumeratedPrimaryOutputs());
	enumeratedSecondaryOutputs.addAll(toolInfo.getEnumeratedSecondaryOutputs());
	outputVariables.addAll(toolInfo.getOutputVariables());

	Vector<String> unprocessedDependencies = toolInfo.getCommandDependencies();
	for (String path : unprocessedDependencies) {
	    dependencies.add(ensurePathIsGNUMakeTargetRuleCompatibleSyntax(path));
	}
	additionalTargets.addAll(toolInfo.getAdditionalTargets());

	if (bTargetTool && managedProjectOutputs != null) {
	    for (String output : managedProjectOutputs) {
		dependencies.add(output);
	    }
	}
	return true;
    }

    protected Vector<String> calculateSecondaryOutputs(IOutputType[] secondaryOutputs) {
	ToolInfoHolder h = (ToolInfoHolder) toolInfos.getValue();
	ITool[] buildTools = h.buildTools;
	Vector<String> buildVars = new Vector<String>();
	for (int i = 0; i < buildTools.length; i++) {
	    // Add the specified output build variables
	    IOutputType[] outTypes = buildTools[i].getOutputTypes();
	    if (outTypes != null && outTypes.length > 0) {
		for (int j = 0; j < outTypes.length; j++) {
		    IOutputType outType = outTypes[j];
		    // Is this one of the secondary outputs?
		    // Look for an outputType with this ID, or one with a
		    // superclass with this id
		    thisType: for (int k = 0; k < secondaryOutputs.length; k++) {
			IOutputType matchType = outType;
			do {
			    if (matchType.getId().equals(secondaryOutputs[k].getId())) {
				buildVars.add(outType.getBuildVariable());
				break thisType;
			    }
			    matchType = matchType.getSuperClass();
			} while (matchType != null);
		    }
		}
	    }
	}
	return buildVars;
    }

    @SuppressWarnings("static-method")
    protected boolean isSecondaryOutputVar(ToolInfoHolder h, IOutputType[] secondaryOutputs, String varName) {
	ITool[] buildTools = h.buildTools;
	for (ITool buildTool : buildTools) {
	    // Add the specified output build variables
	    IOutputType[] outTypes = buildTool.getOutputTypes();
	    if (outTypes != null && outTypes.length > 0) {
		for (IOutputType outType : outTypes) {
		    // Is this one of the secondary outputs?
		    // Look for an outputType with this ID, or one with a
		    // superclass with this id
		    for (IOutputType secondaryOutput : secondaryOutputs) {
			IOutputType matchType = outType;
			do {
			    if (matchType.getId().equals(secondaryOutput.getId())) {
				if (outType.getBuildVariable().equals(varName)) {
				    return true;
				}
			    }
			    matchType = matchType.getSuperClass();
			} while (matchType != null);
		    }
		}
	    }
	}
	return false;
    }

    /*************************************************************************
     * S O U R C E S (sources.mk) M A K E F I L E M E T H O D S
     ************************************************************************/

    private StringBuffer addSubdirectories() {
	StringBuffer buffer = new StringBuffer();
	// Add the comment
	buffer.append(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(MOD_LIST) + NEWLINE);

	buffer.append("SUBDIRS := " + LINEBREAK); //$NON-NLS-1$

	// Get all the module names
	for (IResource container : getSubdirList()) {
	    updateMonitor(ManagedMakeMessages
		    .getFormattedString("MakefileGenerator.message.adding.source.folder", container.getFullPath().toString())); //$NON-NLS-1$
	    // Check the special case where the module is the project root
	    if (container.getFullPath() == project.getFullPath()) {
		buffer.append(DOT + WHITESPACE + LINEBREAK);
	    } else {
		IPath path = container.getProjectRelativePath();
		buffer.append(escapeWhitespaces(path.toString()) + WHITESPACE + LINEBREAK);
	    }
	}

	buffer.append(NEWLINE);
	return buffer;
    }

    /*************************************************************************
     * F R A G M E N T (subdir.mk) M A K E F I L E M E T H O D S
     ************************************************************************/

    /**
     * Returns a <code>StringBuffer</code> containing the comment(s) for a fragment makefile (subdir.mk).
     */
    @SuppressWarnings("static-method")
    protected StringBuffer addFragmentMakefileHeader() {
	return addDefaultHeader();
    }

    /**
     * Returns a <code>StringBuffer</code> containing makefile text for all of the sources contributed by a container (project directory/subdirectory)
     * to the fragement makefile
     * 
     * @param module
     *            project resource directory/subdirectory
     * @return StringBuffer generated text for the fragement makefile
     */
    protected StringBuffer addSources(IContainer module) throws CoreException {
	// Calculate the new directory relative to the build output
	IPath moduleRelativePath = module.getProjectRelativePath();
	String relativePath = moduleRelativePath.toString();
	relativePath += relativePath.length() == 0 ? "" : SEPARATOR; //$NON-NLS-1$

	// For build macros in the configuration, create a map which will map
	// them
	// to a string which holds its list of sources.
	LinkedHashMap<String, String> buildVarToRuleStringMap = new LinkedHashMap<String, String>();

	// Add statements that add the source files in this folder,
	// and generated source files, and generated dependency files
	// to the build macros
	for (Entry<String, List<IPath>> entry : buildSrcVars.entrySet()) {
	    String macroName = entry.getKey();
	    addMacroAdditionPrefix(buildVarToRuleStringMap, macroName, null, false);

	}
	for (Entry<String, List<IPath>> entry : buildOutVars.entrySet()) {
	    String macroName = entry.getKey();
	    addMacroAdditionPrefix(buildVarToRuleStringMap, macroName, "./" + relativePath, false); //$NON-NLS-1$
	}

	// String buffers
	StringBuffer buffer = new StringBuffer(); // Return buffer
	StringBuffer ruleBuffer = new StringBuffer(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(MOD_RULES) + NEWLINE);

	// Visit the resources in this folder and add each one to a sources
	// macro, and generate a build rule, if appropriate
	IResource[] resources = module.members();

	IResourceInfo rcInfo;
	IFolder folder = project.getFolder(config.getName());

	for (IResource resource : resources) {
	    if (resource.getType() == IResource.FILE) {
		// Check whether this resource is excluded from build
		IPath rcProjRelPath = resource.getProjectRelativePath();
		if (!isSource(rcProjRelPath))
		    continue;
		rcInfo = config.getResourceInfo(rcProjRelPath, false);
		// if( (rcInfo.isExcluded()) )
		// continue;
		addFragmentMakefileEntriesForSource(buildVarToRuleStringMap, ruleBuffer, folder, relativePath, resource,
			getPathForResource(resource), rcInfo, null, false);
	    }
	}

	// Write out the macro addition entries to the buffer
	buffer.append(writeAdditionMacros(buildVarToRuleStringMap));
	return buffer.append(ruleBuffer + NEWLINE);
    }

    /*
     * (non-Javadoc Adds the entries for a particular source file to the fragment makefile
     * 
     * @param buildVarToRuleStringMap map of build variable names to the list of files assigned to the variable
     * 
     * @param ruleBuffer buffer to add generated nmakefile text to
     * 
     * @param folder the top level build output directory
     * 
     * @param relativePath build output directory relative path of the current output directory
     * 
     * @param resource the source file for this invocation of the tool - this may be null for a generated output
     * 
     * @param sourceLocation the full path of the source
     * 
     * @param resConfig the IResourceConfiguration associated with this file or null
     * 
     * @param varName the build variable to add this invocation's outputs to if <code>null</code>, use the file extension to find the name
     * 
     * @param generatedSource if <code>true</code>, this file was generated by another tool in the tool-chain
     */
    protected void addFragmentMakefileEntriesForSource(LinkedHashMap<String, String> buildVarToRuleStringMap, StringBuffer ruleBuffer,
	    IFolder folder, String relativePath, IResource resource, IPath sourceLocation, IResourceInfo rcInfo, String varName,
	    boolean generatedSource) {

	// Determine which tool, if any, builds files with this extension
	String ext = sourceLocation.getFileExtension();
	ITool tool = null;

	// TODO: remove
	// IResourceConfiguration resConfig = null;
	// if(rcInfo instanceof IFileInfo){
	// resConfig = (IFileInfo)rcInfo;
	// }
	// end remove

	// Use the tool from the resource configuration if there is one
	if (rcInfo instanceof IFileInfo) {
	    IFileInfo fi = (IFileInfo) rcInfo;
	    ITool[] tools = fi.getToolsToInvoke();
	    if (tools != null && tools.length > 0) {
		tool = tools[0];
		// if(!tool.getCustomBuildStep())
		addToBuildVar(buildVarToRuleStringMap, ext, varName, relativePath, sourceLocation, generatedSource);
	    }
	}

	ToolInfoHolder h = getToolInfo(rcInfo.getPath());
	ITool buildTools[] = h.buildTools;

	// if(tool == null){
	// for (int j=0; j<buildTools.length; j++) {
	// if (buildTools[j].buildsFileType(ext)) {
	// if (tool == null) {
	// tool = buildTools[j];
	// }
	// addToBuildVar(buildVarToRuleStringMap, ext, varName, relativePath,
	// sourceLocation, generatedSource);
	// break;
	// }
	// }
	// }
	//
	// if(tool == null && rcInfo.getPath().segmentCount() != 0){
	if (tool == null) {
	    h = getToolInfo(Path.EMPTY);
	    buildTools = h.buildTools;

	    for (ITool buildTool : buildTools) {
		if (buildTool.buildsFileType(ext)) {
		    tool = buildTool;
		    addToBuildVar(buildVarToRuleStringMap, ext, varName, relativePath, sourceLocation, generatedSource);
		    break;
		}
	    }
	}

	if (tool != null) {
	    // Generate the rule to build this source file
	    IInputType primaryInputType = tool.getPrimaryInputType();
	    IInputType inputType = tool.getInputType(ext);
	    if ((primaryInputType != null && !primaryInputType.getMultipleOfType()) || (inputType == null && tool != config.calculateTargetTool())) {

		// Try to add the rule for the file
		Vector<IPath> generatedOutputs = new Vector<IPath>(); // IPath's
								      // - build
								      // directory
								      // relative
		Vector<IPath> generatedDepFiles = new Vector<IPath>(); // IPath's
								       // -
								       // build
								       // directory
								       // relative
								       // or
								       // absolute
		usedOutType = null; // MODED moved JABA JAn Baeyens get the out
				    // type from the add source call
		addRuleForSource(relativePath, ruleBuffer, resource, sourceLocation, rcInfo, generatedSource, generatedDepFiles, generatedOutputs);

		// If the rule generates a dependency file(s), add the file(s)
		// to the variable
		if (generatedDepFiles.size() > 0) {
		    for (int k = 0; k < generatedDepFiles.size(); k++) {
			IPath generatedDepFile = generatedDepFiles.get(k);
			addMacroAdditionFile(buildVarToRuleStringMap, getDepMacroName(ext).toString(), (generatedDepFile.isAbsolute() ? "" : "./") + //$NON-NLS-1$ //$NON-NLS-2$
				generatedDepFile.toString());
		    }
		}

		// If the generated outputs of this tool are input to another
		// tool,
		// 1. add the output to the appropriate macro
		// 2. If the tool does not have multipleOfType input, generate
		// the rule.

		// IOutputType outType = tool.getPrimaryOutputType(); //MODED
		// moved JABA JAn Baeyens get the out type from the add source
		// call
		String buildVariable = null;
		if (usedOutType != null) {
		    if (tool.getCustomBuildStep()) {
			// TODO: This is somewhat of a hack since a custom build
			// step
			// tool does not currently define a build variable
			if (generatedOutputs.size() > 0) {
			    IPath firstOutput = generatedOutputs.get(0);
			    String firstExt = firstOutput.getFileExtension();
			    ToolInfoHolder tmpH = getFolderToolInfo(rcInfo.getPath());
			    ITool[] tmpBuildTools = tmpH.buildTools;
			    for (ITool tmpBuildTool : tmpBuildTools) {
				if (tmpBuildTool.buildsFileType(firstExt)) {
				    String bV = tmpBuildTool.getPrimaryInputType().getBuildVariable();
				    if (bV.length() > 0) {
					buildVariable = bV;
					break;
				    }
				}
			    }
			}
		    } else {
			buildVariable = usedOutType.getBuildVariable();
		    }
		} else {
		    // For support of pre-CDT 3.0 integrations.
		    buildVariable = OBJS_MACRO;
		}

		for (int k = 0; k < generatedOutputs.size(); k++) {
		    IPath generatedOutput;
		    IResource generateOutputResource;
		    if (generatedOutputs.get(k).isAbsolute()) {
			// TODO: Should we use relative paths when possible
			// (e.g., see MbsMacroSupplier.calculateRelPath)
			generatedOutput = generatedOutputs.get(k);
			// If this file has an absolute path, then the
			// generateOutputResource will not be correct
			// because the file is not under the project. We use
			// this resource in the calls to the dependency
			// generator
			generateOutputResource = project.getFile(generatedOutput);
		    } else {
			generatedOutput = getPathForResource(project).append(getBuildWorkingDir()).append(generatedOutputs.get(k));
			generateOutputResource = project.getFile(getBuildWorkingDir().append(generatedOutputs.get(k)));
		    }
		    IResourceInfo nextRcInfo;
		    if (rcInfo instanceof IFileInfo) {
			nextRcInfo = config.getResourceInfo(rcInfo.getPath().removeLastSegments(1), false);
		    } else {
			nextRcInfo = rcInfo;
		    }
		    addFragmentMakefileEntriesForSource(buildVarToRuleStringMap, ruleBuffer, folder, relativePath, generateOutputResource,
			    generatedOutput, nextRcInfo, buildVariable, true);
		}
	    }
	} else {
	    // If this is a secondary input, add it to build vars
	    if (varName == null) {
		for (ITool buildTool : buildTools) {
		    if (buildTool.isInputFileType(ext)) {
			addToBuildVar(buildVarToRuleStringMap, ext, varName, relativePath, sourceLocation, generatedSource);
			break;
		    }
		}
	    }
	    // If this generated output is identified as a secondary output, add
	    // the file to the build variable
	    else {
		IOutputType[] secondaryOutputs = config.getToolChain().getSecondaryOutputs();
		if (secondaryOutputs.length > 0) {
		    if (isSecondaryOutputVar(h, secondaryOutputs, varName)) {
			addMacroAdditionFile(buildVarToRuleStringMap, varName, relativePath, sourceLocation, generatedSource);
		    }
		}
	    }
	}
    }

    /**
     * Gets a path for a resource by extracting the Path field from its location URI.
     * 
     * @return IPath
     * @since 6.0
     */
    @SuppressWarnings("static-method")
    protected IPath getPathForResource(IResource resource) {
	return new Path(resource.getLocationURI().getPath());
    }

    /**
     * Adds the source file to the appropriate build variable
     * 
     * @param buildVarToRuleStringMap
     *            map of build variable names to the list of files assigned to the variable
     * @param ext
     *            the file extension of the file
     * @param varName
     *            the build variable to add this invocation's outputs to if <code>null</code>, use the file extension to find the name
     * @param relativePath
     *            build output directory relative path of the current output directory
     * @param sourceLocation
     *            the full path of the source
     * @param generatedSource
     *            if <code>true</code>, this file was generated by another tool in the tool-chain
     */
    protected void addToBuildVar(LinkedHashMap<String, String> buildVarToRuleStringMap, String ext, String _varName, String relativePath,
	    IPath sourceLocation, boolean generatedSource) {
	String varName = _varName;
	List<IPath> varList = null;
	if (varName == null) {
	    // Get the proper source build variable based upon the extension
	    varName = getSourceMacroName(ext).toString();
	    varList = buildSrcVars.get(varName);
	} else {
	    varList = buildOutVars.get(varName);
	}
	// Add the resource to the list of all resources associated with a
	// variable.
	// Do not allow duplicates - there is no reason to and it can be 'bad' -
	// e.g., having the same object in the OBJS list can cause duplicate
	// symbol errors from the linker
	if ((varList != null) && !(varList.contains(sourceLocation))) {
	    // Since we don't know how these files will be used, we store them
	    // using a "location"
	    // path rather than a relative path
	    varList.add(sourceLocation);
	    if (!buildVarToRuleStringMap.containsKey(varName)) {
		// JABA is not going to write this code
		// TODO - is this an error?
	    } else {
		// Add the resource name to the makefile line that adds
		// resources to the build variable
		addMacroAdditionFile(buildVarToRuleStringMap, varName, relativePath, sourceLocation, generatedSource);
	    }
	}
    }

    @SuppressWarnings({ "static-method", "unused" })
    private IManagedCommandLineInfo generateToolCommandLineInfo(ITool tool, String sourceExtension, String[] flags, String outputFlag,
	    String outputPrefix, String outputName, String[] inputResources, IPath inputLocation, IPath outputLocation) {

	String cmd = tool.getToolCommand();
	// try to resolve the build macros in the tool command
	try {
	    String resolvedCommand = null;

	    if ((inputLocation != null && inputLocation.toString().indexOf(" ") != -1) || //$NON-NLS-1$
		    (outputLocation != null && outputLocation.toString().indexOf(" ") != -1)) //$NON-NLS-1$
	    {
		resolvedCommand = ManagedBuildManager.getBuildMacroProvider().resolveValue(cmd, "", //$NON-NLS-1$
			" ", //$NON-NLS-1$
			IBuildMacroProvider.CONTEXT_FILE, new FileContextData(inputLocation, outputLocation, null, tool)).replaceFirst(" -w ", " ");
	    }

	    else {
		resolvedCommand = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(cmd, "", //$NON-NLS-1$
			" ", //$NON-NLS-1$
			IBuildMacroProvider.CONTEXT_FILE, new FileContextData(inputLocation, outputLocation, null, tool)).replaceFirst(" -w ", " ");
	    }
	    if ((resolvedCommand = resolvedCommand.trim()).length() > 0)
		cmd = resolvedCommand;

	} catch (BuildMacroException e) {// JABA is not going to write this code
	}

	IManagedCommandLineGenerator gen = tool.getCommandLineGenerator();
	return gen.generateCommandLineInfo(tool, cmd, flags, outputFlag, outputPrefix, outputName, inputResources, tool.getCommandLinePattern());

    }

    /**
     * Create a rule for this source file. We create a pattern rule if possible.
     * 
     * This is an example of a pattern rule:
     * 
     * <relative_path>/%.<outputExtension>: ../<relative_path>/%.<inputExtension>
     * 
     * @echo Building file: $<
     * @echo Invoking tool xxx
     * @echo <tool> <flags> <output_flag><output_prefix>$@ $<
     * @<tool> <flags> <output_flag><output_prefix>$@ $< && \ echo -n $(@:%.o=%.d) ' <relative_path>/' >> $(@:%.o=%.d) && \ <tool> -P -MM -MG <flags>
     *         $< >> $(@:%.o=%.d)
     * @echo Finished building: $<
     * @echo ' '
     * 
     *       Note that the macros all come from the build model and are resolved to a real command before writing to the module makefile, so a real
     *       command might look something like: source1/%.o: ../source1/%.cpp
     * @echo Building file: $<
     * @echo Invoking tool xxx
     * @echo g++ -g -O2 -c -I/cygdrive/c/eclipse/workspace/Project/headers -o$@ $<
     * @g++ -g -O2 -c -I/cygdrive/c/eclipse/workspace/Project/headers -o$@ $< && \ echo -n $(@:%.o=%.d) ' source1/' >> $(@:%.o=%.d) && \ g++ -P -MM
     *      -MG -g -O2 -c -I/cygdrive/c/eclipse/workspace/Project/headers $< >> $(@:%.o=%.d)
     * @echo Finished building: $<
     * @echo ' '
     * 
     * @param relativePath
     *            top build output directory relative path of the current output directory
     * @param buffer
     *            buffer to populate with the build rule
     * @param resource
     *            the source file for this invocation of the tool
     * @param sourceLocation
     *            the full path of the source
     * @param rcInfo
     *            the IResourceInfo associated with this file or null
     * @param generatedSource
     *            <code>true</code> if the resource is a generated output
     * @param enumeratedOutputs
     *            vector of the filenames that are the output of this rule
     */
    @SuppressWarnings("null")
    protected void addRuleForSource(String relativePath, StringBuffer buffer, IResource resource, IPath sourceLocation, IResourceInfo rcInfo,
	    boolean generatedSource, Vector<IPath> generatedDepFiles, Vector<IPath> enumeratedOutputs) {

	String fileName = sourceLocation.removeFileExtension().lastSegment();
	String inputExtension = sourceLocation.getFileExtension();
	String outputExtension = null;

	ITool tool = null;
	if (rcInfo instanceof IFileInfo) {
	    IFileInfo fi = (IFileInfo) rcInfo;
	    ITool[] tools = fi.getToolsToInvoke();
	    if (tools != null && tools.length > 0) {
		tool = tools[0];
	    }
	} else {
	    IFolderInfo foInfo = (IFolderInfo) rcInfo;
	    tool = foInfo.getToolFromInputExtension(inputExtension);
	}

	ToolInfoHolder h = getToolInfo(rcInfo.getPath());

	if (tool != null)
	    outputExtension = tool.getOutputExtension(inputExtension);
	if (outputExtension == null)
	    outputExtension = EMPTY_STRING;

	// Get the dependency generator information for this tool and file
	// extension
	IManagedDependencyGenerator oldDepGen = null; // This interface is
						      // deprecated but still
						      // supported
	IManagedDependencyGenerator2 depGen = null; // This is the recommended
						    // interface
	IManagedDependencyInfo depInfo = null;
	IManagedDependencyCommands depCommands = null;
	IManagedDependencyPreBuild depPreBuild = null;
	IPath[] depFiles = null;
	boolean doDepGen = false;
	{
	    IManagedDependencyGeneratorType t = null;
	    if (tool != null)
		t = tool.getDependencyGeneratorForExtension(inputExtension);
	    if (t != null) {
		int calcType = t.getCalculatorType();
		if (calcType <= IManagedDependencyGeneratorType.TYPE_OLD_TYPE_LIMIT) {
		    oldDepGen = (IManagedDependencyGenerator) t;
		    doDepGen = (calcType == IManagedDependencyGeneratorType.TYPE_COMMAND);
		    if (doDepGen) {
			IPath depFile = Path.fromOSString(relativePath + fileName + DOT + DEP_EXT);
			getDependencyMakefiles(h).add(depFile);
			generatedDepFiles.add(depFile);
		    }
		} else {
		    depGen = (IManagedDependencyGenerator2) t;
		    doDepGen = (calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS);
		    IBuildObject buildContext = rcInfo;// (resConfig != null) ?
						       // (IBuildObject)resConfig
						       // :
						       // (IBuildObject)config;

		    depInfo = depGen.getDependencySourceInfo(resource.getProjectRelativePath(), resource, buildContext, tool, getBuildWorkingDir());

		    if (calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS) {
			depCommands = (IManagedDependencyCommands) depInfo;
			depFiles = depCommands.getDependencyFiles();
		    } else if (calcType == IManagedDependencyGeneratorType.TYPE_PREBUILD_COMMANDS) {
			depPreBuild = (IManagedDependencyPreBuild) depInfo;
			depFiles = depPreBuild.getDependencyFiles();
		    }
		    if (depFiles != null) {
			for (IPath depFile : depFiles) {
			    getDependencyMakefiles(h).add(depFile);
			    generatedDepFiles.add(depFile);
			}
		    }
		}
	    }
	}

	// Figure out the output paths
	String optDotExt = EMPTY_STRING;
	if (outputExtension.length() > 0)
	    optDotExt = DOT + outputExtension;

	Vector<IPath> ruleOutputs = new Vector<IPath>();
	Vector<IPath> enumeratedPrimaryOutputs = new Vector<IPath>(); // IPaths
								      // relative
								      // to the
								      // top
								      // build
								      // directory
	Vector<IPath> enumeratedSecondaryOutputs = new Vector<IPath>(); // IPaths
									// relative
									// to
									// the
									// top
									// build
									// directory
	usedOutType = tool.getPrimaryOutputType();
	calculateOutputsForSource(tool, relativePath, resource, sourceLocation, ruleOutputs, enumeratedPrimaryOutputs, enumeratedSecondaryOutputs);
	enumeratedOutputs.addAll(enumeratedPrimaryOutputs);
	enumeratedOutputs.addAll(enumeratedSecondaryOutputs);
	String primaryOutputName = null;
	if (enumeratedPrimaryOutputs.size() > 0) {
	    primaryOutputName = escapeWhitespaces(enumeratedPrimaryOutputs.get(0).toString());
	} else {
	    primaryOutputName = escapeWhitespaces(relativePath + fileName + optDotExt);
	}
	String otherPrimaryOutputs = EMPTY_STRING;
	for (int i = 1; i < enumeratedPrimaryOutputs.size(); i++) { // Starting
								    // with 1 is
								    // intentional
	    otherPrimaryOutputs += WHITESPACE + escapeWhitespaces(enumeratedPrimaryOutputs.get(i).toString());
	}

	// Output file location needed for the file-specific build macros
	IPath outputLocation = Path.fromOSString(primaryOutputName);
	if (!outputLocation.isAbsolute()) {
	    outputLocation = getPathForResource(project).append(getBuildWorkingDir()).append(primaryOutputName);
	}

	// A separate rule is needed for the resource in the case where explicit
	// file-specific macros
	// are referenced, or if the resource contains special characters in its
	// path (e.g., whitespace)

	/*
	 * fix for 137674
	 * 
	 * We only need an explicit rule if one of the following is true: - The resource is linked, and its full path to its real location contains
	 * special characters - The resource is not linked, but its project relative path contains special characters
	 */

	boolean resourceNameRequiresExplicitRule = (resource.isLinked() && containsSpecialCharacters(sourceLocation.toString()))
		|| (!resource.isLinked() && containsSpecialCharacters(resource.getProjectRelativePath().toString()));

	boolean needExplicitRuleForFile = resourceNameRequiresExplicitRule
		|| BuildMacroProvider.getReferencedExplitFileMacros(tool).length > 0
		|| BuildMacroProvider.getReferencedExplitFileMacros(tool.getToolCommand(), IBuildMacroProvider.CONTEXT_FILE, new FileContextData(
			sourceLocation, outputLocation, null, tool)).length > 0;

	// Get and resolve the command
	String cmd = tool.getToolCommand();

	try {
	    String resolvedCommand = null;
	    if (!needExplicitRuleForFile) {
		resolvedCommand = ManagedBuildManager
			.getBuildMacroProvider()
			.resolveValueToMakefileFormat(cmd, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
				new FileContextData(sourceLocation, outputLocation, null, tool)).replaceFirst(" -w ", " ");
	    } else {
		// if we need an explicit rule then don't use any builder
		// variables, resolve everything
		// to explicit strings
		resolvedCommand = ManagedBuildManager
			.getBuildMacroProvider()
			.resolveValue(cmd, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
				new FileContextData(sourceLocation, outputLocation, null, tool)).replaceFirst(" -w ", " ");
	    }

	    if ((resolvedCommand = resolvedCommand.trim()).length() > 0)
		cmd = resolvedCommand;

	} catch (BuildMacroException e) {// JABA is not going to write this code
	}

	String defaultOutputName = EMPTY_STRING;
	String primaryDependencyName = EMPTY_STRING;
	String patternPrimaryDependencyName = EMPTY_STRING;
	String home = (generatedSource) ? DOT : ROOT;
	String resourcePath = null;
	boolean patternRule = true;
	boolean isItLinked = false;
	if (resource.isLinked(IResource.CHECK_ANCESTORS)) {
	    // it IS linked, so use the actual location
	    isItLinked = true;
	    resourcePath = sourceLocation.toString();
	    // Need a hardcoded rule, not a pattern rule, as a linked file
	    // can reside in any path
	    defaultOutputName = escapeWhitespaces(relativePath + fileName + optDotExt);
	    primaryDependencyName = escapeWhitespaces(resourcePath);
	    patternRule = false;
	} else {
	    // Use the relative path (not really needed to store per se but in
	    // the future someone may want this)
	    resourcePath = relativePath;
	    // The rule and command to add to the makefile
	    if (rcInfo instanceof IFileInfo || needExplicitRuleForFile) {
		// Need a hardcoded rule, not a pattern rule
		defaultOutputName = escapeWhitespaces(resourcePath + fileName + optDotExt);
		patternRule = false;
	    } else {
		defaultOutputName = relativePath + WILDCARD + optDotExt;
	    }
	    primaryDependencyName = escapeWhitespaces(home + SEPARATOR + resourcePath + fileName + DOT + inputExtension);
	    patternPrimaryDependencyName = home + SEPARATOR + resourcePath + WILDCARD + DOT + inputExtension;
	} // end fix for PR 70491

	// If the tool specifies a dependency calculator of TYPE_BUILD_COMMANDS,
	// ask whether
	// the dependency commands are "generic" (i.e., we can use a pattern
	// rule)
	boolean needExplicitDependencyCommands = false;
	if (depCommands != null) {
	    needExplicitDependencyCommands = !depCommands.areCommandsGeneric();
	}

	// If we still think that we are using a pattern rule, check a few more
	// things
	if (patternRule) {
	    patternRule = false;
	    // Make sure that at least one of the rule outputs contains a %.
	    for (int i = 0; i < ruleOutputs.size(); i++) {
		String ruleOutput = ruleOutputs.get(i).toString();
		if (ruleOutput.indexOf('%') >= 0) {
		    patternRule = true;
		    break;
		}
	    }
	    if (patternRule) {
		patternRule = !needExplicitDependencyCommands;
	    }
	}

	// Begin building the rule for this source file
	String buildRule = EMPTY_STRING;

	if (patternRule) {
	    if (ruleOutputs.size() == 0) {
		buildRule += defaultOutputName;
	    } else {
		boolean first = true;
		for (int i = 0; i < ruleOutputs.size(); i++) {
		    String ruleOutput = ruleOutputs.get(i).toString();
		    if (ruleOutput.indexOf('%') >= 0) {
			if (first) {
			    first = false;
			} else {
			    buildRule += WHITESPACE;
			}
			buildRule += ruleOutput;
		    }
		}
	    }
	} else {
	    buildRule += primaryOutputName;
	}

	String buildRuleDependencies = primaryDependencyName;
	String patternBuildRuleDependencies = patternPrimaryDependencyName;

	// Other additional inputs
	// Get any additional dependencies specified for the tool in other
	// InputType elements and AdditionalInput elements
	IPath[] addlDepPaths = tool.getAdditionalDependencies();
	for (IPath addlDepPath : addlDepPaths) {
	    // Translate the path from project relative to build directory
	    // relative
	    IPath addlPath = addlDepPath;
	    if (!(addlPath.toString().startsWith("$("))) { //$NON-NLS-1$
		if (!addlPath.isAbsolute()) {
		    IPath tempPath = project.getLocation().append(new Path(ensureUnquoted(addlPath.toString())));
		    if (tempPath != null) {
			addlPath = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), tempPath);
		    }
		}
	    }
	    String suitablePath = ensurePathIsGNUMakeTargetRuleCompatibleSyntax(addlPath.toString());
	    buildRuleDependencies += WHITESPACE + suitablePath;
	    patternBuildRuleDependencies += WHITESPACE + suitablePath;
	}

	buildRule += COLON + WHITESPACE + (patternRule ? patternBuildRuleDependencies : buildRuleDependencies);

	// No duplicates in a makefile. If we already have this rule, don't add
	// it or the commands to build the file
	if (getRuleList().contains(buildRule)) {
	    // TODO: Should we assert that this is a pattern rule?
	} else {
	    getRuleList().add(buildRule);

	    // Echo starting message
	    buffer.append(buildRule + NEWLINE);
	    buffer.append(TAB + AT + escapedEcho(MESSAGE_START_FILE + WHITESPACE + IN_MACRO));
	    buffer.append(TAB + AT + escapedEcho(tool.getAnnouncement()));

	    // If the tool specifies a dependency calculator of
	    // TYPE_BUILD_COMMANDS, ask whether
	    // there are any pre-tool commands.
	    if (depCommands != null) {
		String[] preToolCommands = depCommands.getPreToolDependencyCommands();
		if (preToolCommands != null && preToolCommands.length > 0) {
		    for (String preCmd : preToolCommands) {
			try {
			    String resolvedCommand;
			    IBuildMacroProvider provider = ManagedBuildManager.getBuildMacroProvider();
			    if (!needExplicitRuleForFile) {
				resolvedCommand = provider.resolveValueToMakefileFormat(preCmd, EMPTY_STRING, WHITESPACE,
					IBuildMacroProvider.CONTEXT_FILE, new FileContextData(sourceLocation, outputLocation, null, tool))
					.replaceFirst(" -w ", " ");
			    } else {
				// if we need an explicit rule then don't use
				// any builder
				// variables, resolve everything to explicit
				// strings
				resolvedCommand = provider.resolveValue(preCmd, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
					new FileContextData(sourceLocation, outputLocation, null, tool)).replaceFirst(" -w ", " ");
			    }
			    if (resolvedCommand != null)
				buffer.append(resolvedCommand + NEWLINE);
			} catch (BuildMacroException e) {// JABA is not going to
							 // write this code
			}
		    }
		}
	    }

	    // Generate the command line

	    Vector<String> inputs = new Vector<String>();
	    inputs.add(IN_MACRO);

	    // Other additional inputs
	    // Get any additional dependencies specified for the tool in other
	    // InputType elements and AdditionalInput elements
	    IPath[] addlInputPaths = getAdditionalResourcesForSource(tool);
	    for (IPath addlInputPath : addlInputPaths) {
		// Translate the path from project relative to build directory
		// relative
		IPath addlPath = addlInputPath;
		if (!(addlPath.toString().startsWith("$("))) { //$NON-NLS-1$
		    if (!addlPath.isAbsolute()) {
			IPath tempPath = getPathForResource(project).append(addlPath);
			if (tempPath != null) {
			    addlPath = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), tempPath);
			}
		    }
		}
		inputs.add(addlPath.toString());
	    }
	    String[] inputStrings = inputs.toArray(new String[inputs.size()]);

	    String[] flags = null;
	    // Get the tool command line options
	    try {
		flags = tool.getToolCommandFlags(sourceLocation, outputLocation);
	    } catch (BuildException ex) {
		// TODO add some routines to catch this
		flags = EMPTY_STRING_ARRAY;
	    }

	    // If we have a TYPE_BUILD_COMMANDS dependency generator, determine
	    // if there are any options that
	    // it wants added to the command line
	    if (depCommands != null) {
		flags = addDependencyOptions(depCommands, flags);
	    }

	    IManagedCommandLineInfo cmdLInfo = null;
	    String outflag = null;
	    String outputPrefix = null;

	    if (rcInfo instanceof IFileInfo || needExplicitRuleForFile || needExplicitDependencyCommands) {
		outflag = tool.getOutputFlag();
		outputPrefix = tool.getOutputPrefix();

		// Call the command line generator
		IManagedCommandLineGenerator cmdLGen = tool.getCommandLineGenerator();
		cmdLInfo = cmdLGen.generateCommandLineInfo(tool, cmd, flags, outflag, outputPrefix, OUT_MACRO + otherPrimaryOutputs, inputStrings,
			tool.getCommandLinePattern());

	    } else {
		outflag = tool.getOutputFlag();// config.getOutputFlag(outputExtension);
		outputPrefix = tool.getOutputPrefix();// config.getOutputPrefix(outputExtension);

		// Call the command line generator
		cmdLInfo = generateToolCommandLineInfo(tool, inputExtension, flags, outflag, outputPrefix, OUT_MACRO + otherPrimaryOutputs,
			inputStrings, sourceLocation, outputLocation);
	    }

	    // The command to build
	    String buildCmd;
	    if (cmdLInfo != null) {
		buildCmd = cmdLInfo.getCommandLine();
	    } else {
		StringBuffer buildFlags = new StringBuffer();
		for (String flag : flags) {
		    if (flag != null) {
			buildFlags.append(flag + WHITESPACE);
		    }
		}
		buildCmd = cmd + WHITESPACE + buildFlags.toString().trim() + WHITESPACE + outflag + WHITESPACE + outputPrefix + OUT_MACRO
			+ otherPrimaryOutputs + WHITESPACE + IN_MACRO;
	    }

	    // resolve any remaining macros in the command after it has been
	    // generated
	    try {
		String resolvedCommand;
		IBuildMacroProvider provider = ManagedBuildManager.getBuildMacroProvider();
		if (!needExplicitRuleForFile) {
		    resolvedCommand = provider.resolveValueToMakefileFormat(buildCmd, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
			    new FileContextData(sourceLocation, outputLocation, null, tool)).replaceFirst(" -w ", " ");
		} else {
		    // if we need an explicit rule then don't use any builder
		    // variables, resolve everything to explicit strings
		    resolvedCommand = provider.resolveValue(buildCmd, EMPTY_STRING, WHITESPACE, IBuildMacroProvider.CONTEXT_FILE,
			    new FileContextData(sourceLocation, outputLocation, null, tool)).replaceFirst(" -w ", " ");
		}

		if ((resolvedCommand = resolvedCommand.trim()).length() > 0)
		    buildCmd = resolvedCommand;

	    } catch (BuildMacroException e) {// JABA is not going to write this
					     // code
	    }

	    // buffer.append(TAB + AT + escapedEcho(buildCmd));
	    // buffer.append(TAB + AT + buildCmd);
	    buffer.append(TAB + buildCmd);

	    // Determine if there are any dependencies to calculate
	    if (doDepGen) {
		// Get the dependency rule out of the generator
		String[] depCmds = null;
		if (oldDepGen != null) {
		    depCmds = new String[1];
		    depCmds[0] = oldDepGen.getDependencyCommand(resource, ManagedBuildManager.getBuildInfo(project));
		} else {
		    if (depCommands != null) {
			depCmds = depCommands.getPostToolDependencyCommands();
		    }
		}

		if (depCmds != null) {
		    for (String depCmd : depCmds) {
			// Resolve any macros in the dep command after it has
			// been generated.
			// Note: do not trim the result because it will strip
			// out necessary tab characters.
			buffer.append(WHITESPACE + LOGICAL_AND + WHITESPACE + LINEBREAK);
			try {
			    if (!needExplicitRuleForFile) {
				depCmd = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(depCmd, EMPTY_STRING, WHITESPACE,
					IBuildMacroProvider.CONTEXT_FILE, new FileContextData(sourceLocation, outputLocation, null, tool));
			    }

			    else {
				depCmd = ManagedBuildManager.getBuildMacroProvider().resolveValue(depCmd, EMPTY_STRING, WHITESPACE,
					IBuildMacroProvider.CONTEXT_FILE, new FileContextData(sourceLocation, outputLocation, null, tool));
			    }

			} catch (BuildMacroException e) {// JABA is not going to
							 // write this code
			}

			buffer.append(depCmd);
		    }
		}
	    }

	    // Echo finished message
	    buffer.append(NEWLINE);
	    buffer.append(TAB + AT + escapedEcho(MESSAGE_FINISH_FILE + WHITESPACE + IN_MACRO));
	    buffer.append(TAB + AT + ECHO_BLANK_LINE + NEWLINE);
	}

	// Determine if there are calculated dependencies
	IPath[] addlDeps = null; // IPath's that are relative to the build
				 // directory
	IPath[] addlTargets = null; // IPath's that are relative to the build
				    // directory
	String calculatedDependencies = null;
	boolean addedDepLines = false;
	String depLine;
	if (oldDepGen != null && oldDepGen.getCalculatorType() != IManagedDependencyGeneratorType.TYPE_COMMAND) {
	    addlDeps = oldCalculateDependenciesForSource(oldDepGen, tool, relativePath, resource);
	} else {
	    if (depGen != null && depGen.getCalculatorType() == IManagedDependencyGeneratorType.TYPE_CUSTOM) {
		if (depInfo instanceof IManagedDependencyCalculator) {
		    IManagedDependencyCalculator depCalculator = (IManagedDependencyCalculator) depInfo;
		    addlDeps = calculateDependenciesForSource(depCalculator);
		    addlTargets = depCalculator.getAdditionalTargets();
		}
	    }
	}

	if (addlDeps != null && addlDeps.length > 0) {
	    calculatedDependencies = new String();
	    for (IPath addlDep : addlDeps) {
		calculatedDependencies += WHITESPACE + escapeWhitespaces(addlDep.toString());
	    }
	}

	if (calculatedDependencies != null) {
	    depLine = primaryOutputName + COLON + calculatedDependencies + NEWLINE;
	    if (!getDepLineList().contains(depLine)) {
		getDepLineList().add(depLine);
		addedDepLines = true;
		buffer.append(depLine);
	    }
	}

	// Add any additional outputs here using dependency lines
	Vector<IPath> addlOutputs = new Vector<IPath>();
	if (enumeratedPrimaryOutputs.size() > 1) {
	    // Starting with 1 is intentional in order to skip the primary
	    // output
	    for (int i = 1; i < enumeratedPrimaryOutputs.size(); i++)
		addlOutputs.add(enumeratedPrimaryOutputs.get(i));
	}
	addlOutputs.addAll(enumeratedSecondaryOutputs);
	if (addlTargets != null) {
	    for (IPath addlTarget : addlTargets)
		addlOutputs.add(addlTarget);
	}
	for (int i = 0; i < addlOutputs.size(); i++) {
	    depLine = escapeWhitespaces(addlOutputs.get(i).toString()) + COLON + WHITESPACE + primaryOutputName;
	    if (calculatedDependencies != null)
		depLine += calculatedDependencies;
	    depLine += NEWLINE;
	    if (!getDepLineList().contains(depLine)) {
		getDepLineList().add(depLine);
		addedDepLines = true;
		buffer.append(depLine);
	    }
	}
	if (addedDepLines) {
	    buffer.append(NEWLINE);
	}

	// If we are using a dependency calculator of type
	// TYPE_PREBUILD_COMMANDS,
	// get the rule to build the dependency file
	if (depPreBuild != null && depFiles != null) {
	    addedDepLines = false;
	    String[] preBuildCommands = depPreBuild.getDependencyCommands();
	    if (preBuildCommands != null) {
		depLine = ""; //$NON-NLS-1$
		// Can we use a pattern rule?
		patternRule = !isItLinked && !needExplicitRuleForFile && depPreBuild.areCommandsGeneric();
		// Begin building the rule
		for (int i = 0; i < depFiles.length; i++) {
		    if (i > 0)
			depLine += WHITESPACE;
		    if (patternRule) {
			optDotExt = EMPTY_STRING;
			String depExt = depFiles[i].getFileExtension();
			if (depExt != null && depExt.length() > 0)
			    optDotExt = DOT + depExt;
			depLine += escapeWhitespaces(relativePath + WILDCARD + optDotExt);
		    } else {
			depLine += escapeWhitespaces((depFiles[i]).toString());
		    }
		}
		depLine += COLON + WHITESPACE + (patternRule ? patternBuildRuleDependencies : buildRuleDependencies);
		if (!getDepRuleList().contains(depLine)) {
		    getDepRuleList().add(depLine);
		    addedDepLines = true;
		    buffer.append(depLine + NEWLINE);
		    buffer.append(TAB + AT + escapedEcho(MESSAGE_START_DEPENDENCY + WHITESPACE + OUT_MACRO));
		    for (String preBuildCommand : preBuildCommands) {
			depLine = preBuildCommand;
			// Resolve macros
			try {
			    if (!needExplicitRuleForFile) {
				depLine = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(depLine, EMPTY_STRING, WHITESPACE,
					IBuildMacroProvider.CONTEXT_FILE, new FileContextData(sourceLocation, outputLocation, null, tool));
			    }

			    else {
				depLine = ManagedBuildManager.getBuildMacroProvider().resolveValue(depLine, EMPTY_STRING, WHITESPACE,
					IBuildMacroProvider.CONTEXT_FILE, new FileContextData(sourceLocation, outputLocation, null, tool));
			    }

			} catch (BuildMacroException e) {// JABA is not going to
							 // write this code
			}
			// buffer.append(TAB + AT + escapedEcho(depLine));
			// buffer.append(TAB + AT + depLine + NEWLINE);
			buffer.append(TAB + depLine + NEWLINE);
		    }
		}
		if (addedDepLines) {
		    buffer.append(TAB + AT + ECHO_BLANK_LINE + NEWLINE);
		}
	    }
	}
    }

    /*
     * Add any dependency calculator options to the tool options
     */
    @SuppressWarnings("static-method")
    private String[] addDependencyOptions(IManagedDependencyCommands depCommands, String[] flags) {
	String[] depOptions = depCommands.getDependencyCommandOptions();
	if (depOptions != null && depOptions.length > 0) {
	    int flagsLen = flags.length;
	    String[] flagsCopy = new String[flags.length + depOptions.length];
	    for (int i = 0; i < flags.length; i++) {
		flagsCopy[i] = flags[i];
	    }
	    for (int i = 0; i < depOptions.length; i++) {
		// FIXME: SJA Patch begins here.
		if (depOptions[i].startsWith("-MT")) {
		    depOptions[i] = "-MT\"$@\"";
		}
		// FIXME: SJA Patch ends here.
		flagsCopy[i + flagsLen] = depOptions[i];
	    }
	    return flagsCopy;
	}
	return flags;
    }

    /**
     * Returns any additional resources specified for the tool in other InputType elements and AdditionalInput elements
     */
    protected IPath[] getAdditionalResourcesForSource(ITool tool) {
	List<IPath> allRes = new ArrayList<IPath>();
	IInputType[] types = tool.getInputTypes();
	for (IInputType type : types) {
	    // Additional resources come from 2 places.
	    // 1. From AdditionalInput childen
	    IPath[] res = type.getAdditionalResources();
	    for (IPath re : res) {
		allRes.add(re);
	    }
	    // 2. From InputTypes that other than the primary input type
	    if (!type.getPrimaryInput() && type != tool.getPrimaryInputType()) {
		String var = type.getBuildVariable();
		if (var != null && var.length() > 0) {
		    allRes.add(Path.fromOSString("$(" + type.getBuildVariable() + ")")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
		    // Use file extensions
		    String[] typeExts = type.getSourceExtensions(tool);
		    for (IResource projectResource : projectResources) {
			if (projectResource.getType() == IResource.FILE) {
			    String fileExt = projectResource.getFileExtension();
			    if (fileExt == null) {
				fileExt = ""; //$NON-NLS-1$
			    }
			    for (String typeExt : typeExts) {
				if (fileExt.equals(typeExt)) {
				    allRes.add(projectResource.getProjectRelativePath());
				    break;
				}
			    }
			}
		    }
		}

		// If an assignToOption has been specified, set the value of the
		// option to the inputs
		IOption assignToOption = tool.getOptionBySuperClassId(type.getAssignToOptionId());
		IOption option = tool.getOptionBySuperClassId(type.getOptionId());
		if (assignToOption != null && option == null) {
		    try {
			int optType = assignToOption.getValueType();
			IResourceInfo rcInfo = tool.getParentResourceInfo();
			if (rcInfo != null) {
			    if (optType == IOption.STRING) {
				String optVal = ""; //$NON-NLS-1$
				for (int j = 0; j < allRes.size(); j++) {
				    if (j != 0) {
					optVal += " "; //$NON-NLS-1$
				    }
				    String resPath = allRes.get(j).toString();
				    if (!resPath.startsWith("$(")) { //$NON-NLS-1$
					IResource addlResource = project.getFile(resPath);
					if (addlResource != null) {
					    IPath addlPath = addlResource.getLocation();
					    if (addlPath != null) {
						resPath = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), addlPath).toString();
					    }
					}
				    }
				    optVal += ManagedBuildManager.calculateRelativePath(getTopBuildDir(), Path.fromOSString(resPath)).toString();
				}
				ManagedBuildManager.setOption(rcInfo, tool, assignToOption, optVal);
			    } else if (optType == IOption.STRING_LIST || optType == IOption.LIBRARIES || optType == IOption.OBJECTS
				    || optType == IOption.INCLUDE_FILES || optType == IOption.LIBRARY_PATHS || optType == IOption.LIBRARY_FILES
				    || optType == IOption.MACRO_FILES) {
				// TODO: do we need to do anything with undefs
				// here?
				// Note that the path(s) must be translated from
				// project relative
				// to top build directory relative
				String[] paths = new String[allRes.size()];
				for (int j = 0; j < allRes.size(); j++) {
				    paths[j] = allRes.get(j).toString();
				    if (!paths[j].startsWith("$(")) { //$NON-NLS-1$
					IResource addlResource = project.getFile(paths[j]);
					if (addlResource != null) {
					    IPath addlPath = addlResource.getLocation();
					    if (addlPath != null) {
						paths[j] = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), addlPath).toString();
					    }
					}
				    }
				}
				ManagedBuildManager.setOption(rcInfo, tool, assignToOption, paths);
			    } else if (optType == IOption.BOOLEAN) {
				boolean b = false;
				if (allRes.size() > 0)
				    b = true;
				ManagedBuildManager.setOption(rcInfo, tool, assignToOption, b);
			    } else if (optType == IOption.ENUMERATED || optType == IOption.TREE) {
				if (allRes.size() > 0) {
				    String s = allRes.get(0).toString();
				    ManagedBuildManager.setOption(rcInfo, tool, assignToOption, s);
				}
			    }
			    allRes.clear();
			}
		    } catch (BuildException ex) {// JABA is not going to write
						 // this code
		    }
		}
	    }
	}
	return allRes.toArray(new IPath[allRes.size()]);
    }

    /**
     * Returns the output <code>IPath</code>s for this invocation of the tool with the specified source file
     * 
     * The priorities for determining the names of the outputs of a tool are: 1. If the tool is the build target and primary output, use artifact name
     * & extension - This case does not apply here... 2. If an option is specified, use the value of the option 3. If a nameProvider is specified,
     * call it 4. If outputNames is specified, use it 5. Use the name pattern to generate a transformation macro so that the source names can be
     * transformed into the target names using the built-in string substitution functions of <code>make</code>.
     * 
     * @param relativePath
     *            build output directory relative path of the current output directory
     * @param ruleOutputs
     *            Vector of rule IPaths that are relative to the build directory
     * @param enumeratedPrimaryOutputs
     *            Vector of IPaths of primary outputs that are relative to the build directory
     * @param enumeratedSecondaryOutputs
     *            Vector of IPaths of secondary outputs that are relative to the build directory
     */
    protected void calculateOutputsForSource(ITool tool, String relativePath, IResource resource, IPath sourceLocation, Vector<IPath> ruleOutputs,
	    Vector<IPath> enumeratedPrimaryOutputs, Vector<IPath> enumeratedSecondaryOutputs) {
	String inExt = sourceLocation.getFileExtension();
	String outExt = tool.getOutputExtension(inExt);
	// IResourceInfo rcInfo = tool.getParentResourceInfo();

	IOutputType[] outTypes = tool.getOutputTypes();
	if (outTypes != null && outTypes.length > 0) {
	    for (IOutputType type : outTypes) {
		boolean primaryOutput = type.getPrimaryOutput(); // MODDED BY
								 // JABA
		// if (primaryOutput && ignorePrimary) continue;
		String outputPrefix = type.getOutputPrefix();

		// Resolve any macros in the outputPrefix
		// Note that we cannot use file macros because if we do a clean
		// we need to know the actual name of the file to clean, and
		// cannot use any builder variables such as $@. Hence we use the
		// next best thing, i.e. configuration context.

		// figure out the configuration we're using
		// IBuildObject toolParent = tool.getParent();
		// IConfiguration config = null;
		// if the parent is a config then we're done
		// if (toolParent instanceof IConfiguration)
		// config = (IConfiguration) toolParent;
		// else if (toolParent instanceof IToolChain) {
		// // must be a toolchain
		// config = (IConfiguration) ((IToolChain) toolParent)
		// .getParent();
		// }
		//
		// else if (toolParent instanceof IResourceConfiguration) {
		// config = (IConfiguration) ((IResourceConfiguration)
		// toolParent)
		// .getParent();
		// }

		// else {
		// // bad
		// throw new AssertionError(
		// "tool parent must be one of configuration, toolchain, or resource configuration");
		// }

		// if (config != null) {

		try {

		    if (containsSpecialCharacters(sourceLocation.toString())) {
			outputPrefix = ManagedBuildManager.getBuildMacroProvider().resolveValue(outputPrefix, "", //$NON-NLS-1$
				" ", //$NON-NLS-1$
				IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
		    } else {
			outputPrefix = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(outputPrefix, "", //$NON-NLS-1$
				" ", //$NON-NLS-1$
				IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
		    }
		}

		catch (BuildMacroException e) {// JABA is not going to write
					       // this code
		}

		// }

		boolean multOfType = type.getMultipleOfType();
		IOption option = tool.getOptionBySuperClassId(type.getOptionId());
		IManagedOutputNameProvider nameProvider = type.getNameProvider();
		String[] outputNames = type.getOutputNames();

		// 1. If the tool is the build target and this is the primary
		// output,
		// use artifact name & extension
		// Not appropriate here...
		// 2. If an option is specified, use the value of the option
		if (option != null) {
		    try {
			List<String> outputList = new ArrayList<String>();
			int optType = option.getValueType();
			if (optType == IOption.STRING) {
			    outputList.add(outputPrefix + option.getStringValue());
			} else if (optType == IOption.STRING_LIST || optType == IOption.LIBRARIES || optType == IOption.OBJECTS
				|| optType == IOption.INCLUDE_FILES || optType == IOption.LIBRARY_PATHS || optType == IOption.LIBRARY_FILES
				|| optType == IOption.MACRO_FILES) {
			    @SuppressWarnings("unchecked")
			    List<String> value = (List<String>) option.getValue();
			    outputList = value;
			    ((Tool) tool).filterValues(optType, outputList);
			    // Add outputPrefix to each if necessary
			    if (outputPrefix.length() > 0) {
				for (int j = 0; j < outputList.size(); j++) {
				    outputList.set(j, outputPrefix + outputList.get(j));
				}
			    }
			}
			for (int j = 0; j < outputList.size(); j++) {
			    String outputName = outputList.get(j);

			    // try to resolve the build macros in the output
			    // names
			    try {

				String resolved = null;

				if (containsSpecialCharacters(sourceLocation.toString())) {
				    resolved = ManagedBuildManager.getBuildMacroProvider().resolveValue(outputName, "", //$NON-NLS-1$
					    " ", //$NON-NLS-1$
					    IBuildMacroProvider.CONTEXT_FILE, new FileContextData(sourceLocation, null, option, tool));
				}

				else {
				    resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(outputName, "", //$NON-NLS-1$
					    " ", //$NON-NLS-1$
					    IBuildMacroProvider.CONTEXT_FILE, new FileContextData(sourceLocation, null, option, tool));
				}

				if ((resolved = resolved.trim()).length() > 0)
				    outputName = resolved;
			    } catch (BuildMacroException e) {// JABA is not
							     // going to write
							     // this code
			    }

			    IPath outPath = Path.fromOSString(outputName);
			    // If only a file name is specified, add the
			    // relative path of this output directory
			    if (outPath.segmentCount() == 1) {
				outPath = Path.fromOSString(relativePath + outputList.get(j));
			    }
			    if (primaryOutput) {
				ruleOutputs.add(j, outPath);
				enumeratedPrimaryOutputs.add(j, resolvePercent(outPath, sourceLocation));
			    } else {
				ruleOutputs.add(outPath);
				enumeratedSecondaryOutputs.add(resolvePercent(outPath, sourceLocation));
			    }
			}
		    } catch (BuildException ex) {// JABA is not going to write
						 // this code
		    }
		} else
		// 3. If a nameProvider is specified, call it
		if (nameProvider != null) {
		    IPath[] inPaths = new IPath[1];
		    inPaths[0] = resource.getProjectRelativePath();// ;
								   // sourceLocation.removeFirstSegments(project.getLocation().segmentCount());
		    IPath[] outPaths = null;
		    try {
			IManagedOutputNameProviderJaba newNameProvider = (IManagedOutputNameProviderJaba) nameProvider;
			outPaths = newNameProvider.getOutputNames(project, config, tool, inPaths);
		    } catch (Exception e) {
			// The provided class is not a
			// IManagedOutputNameProviderJaba class;
			Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
				"The provided class is not of type IManagedOutputNameProviderJaba", e));
		    }
		    if (outPaths != null) { // MODDED BY JABA ADDED to handle
					    // null as return value
			usedOutType = type; // MODDED By JABA added to retun the
					    // output type used to generate the
					    // command line
			for (int j = 0; j < outPaths.length; j++) {
			    IPath outPath = outPaths[j];
			    String outputName = outPaths[j].toString();

			    // try to resolve the build macros in the output
			    // names
			    try {

				String resolved = null;

				if (containsSpecialCharacters(sourceLocation.toString())) {
				    resolved = ManagedBuildManager.getBuildMacroProvider().resolveValue(outputName, "", //$NON-NLS-1$
					    " ", //$NON-NLS-1$
					    IBuildMacroProvider.CONTEXT_FILE, new FileContextData(sourceLocation, null, option, tool));
				}

				else {
				    resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(outputName, "", //$NON-NLS-1$
					    " ", //$NON-NLS-1$
					    IBuildMacroProvider.CONTEXT_FILE, new FileContextData(sourceLocation, null, option, tool));
				}

				if ((resolved = resolved.trim()).length() > 0)
				    outputName = resolved;
			    } catch (BuildMacroException e) {// JABA is not
							     // going to write
							     // this code
			    }

			    // If only a file name is specified, add the
			    // relative path of this output directory
			    if (outPath.segmentCount() == 1) {
				outPath = Path.fromOSString(relativePath + outPath.toString());
			    }
			    if (primaryOutput) {
				ruleOutputs.add(j, outPath);
				enumeratedPrimaryOutputs.add(j, resolvePercent(outPath, sourceLocation));
			    } else {
				ruleOutputs.add(outPath);
				enumeratedSecondaryOutputs.add(resolvePercent(outPath, sourceLocation));
			    }
			}
		    }// MODDED BY JABA ADDED
		} else
		// 4. If outputNames is specified, use it
		if (outputNames != null) {
		    for (int j = 0; j < outputNames.length; j++) {
			String outputName = outputNames[j];
			try {
			    // try to resolve the build macros in the output
			    // names
			    String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(outputName, "", //$NON-NLS-1$
				    " ", //$NON-NLS-1$
				    IBuildMacroProvider.CONTEXT_FILE, new FileContextData(sourceLocation, null, option, tool));
			    if ((resolved = resolved.trim()).length() > 0)
				outputName = resolved;
			} catch (BuildMacroException e) {// JABA is not going to
							 // write this code
			}

			IPath outPath = Path.fromOSString(outputName);
			// If only a file name is specified, add the relative
			// path of this output directory
			if (outPath.segmentCount() == 1) {
			    outPath = Path.fromOSString(relativePath + outPath.toString());
			}
			if (primaryOutput) {
			    ruleOutputs.add(j, outPath);
			    enumeratedPrimaryOutputs.add(j, resolvePercent(outPath, sourceLocation));
			} else {
			    ruleOutputs.add(outPath);
			    enumeratedSecondaryOutputs.add(resolvePercent(outPath, sourceLocation));
			}
		    }
		} else {
		    // 5. Use the name pattern to generate a transformation
		    // macro
		    // so that the source names can be transformed into the
		    // target names
		    // using the built-in string substitution functions of
		    // <code>make</code>.
		    if (multOfType) {
			// This case is not handled - a nameProvider or
			// outputNames must be specified
			// TODO - report error
		    } else {
			String namePattern = type.getNamePattern();
			IPath namePatternPath = null;
			if (namePattern == null || namePattern.length() == 0) {
			    namePattern = relativePath + outputPrefix + IManagedBuilderMakefileGenerator.WILDCARD;
			    if (outExt != null && outExt.length() > 0) {
				namePattern += DOT + outExt;
			    }
			    namePatternPath = Path.fromOSString(namePattern);
			} else {
			    if (outputPrefix.length() > 0) {
				namePattern = outputPrefix + namePattern;
			    }
			    namePatternPath = Path.fromOSString(namePattern);
			    // If only a file name is specified, add the
			    // relative path of this output directory
			    if (namePatternPath.segmentCount() == 1) {
				namePatternPath = Path.fromOSString(relativePath + namePatternPath.toString());
			    }
			}

			if (primaryOutput) {
			    ruleOutputs.add(0, namePatternPath);
			    enumeratedPrimaryOutputs.add(0, resolvePercent(namePatternPath, sourceLocation));
			} else {
			    ruleOutputs.add(namePatternPath);
			    enumeratedSecondaryOutputs.add(resolvePercent(namePatternPath, sourceLocation));
			}
		    }
		}
	    }
	} else {
	    // For support of pre-CDT 3.0 integrations.
	    // NOTE WELL: This only supports the case of a single "target tool"
	    // that consumes exactly all of the object files, $OBJS, produced
	    // by other tools in the build and produces a single output.
	    // In this case, the output file name is the input file name with
	    // the output extension.

	    // if (!ignorePrimary) {
	    String outPrefix = tool.getOutputPrefix();
	    IPath outPath = Path.fromOSString(relativePath + outPrefix + WILDCARD);
	    outPath = outPath.addFileExtension(outExt);
	    ruleOutputs.add(0, outPath);
	    enumeratedPrimaryOutputs.add(0, resolvePercent(outPath, sourceLocation));
	    // }
	}
    }

    /**
     * If the path contains a %, returns the path resolved using the resource name
     * 
     */
    @SuppressWarnings("static-method")
    protected IPath resolvePercent(IPath outPath, IPath sourceLocation) {
	// Get the input file name
	String fileName = sourceLocation.removeFileExtension().lastSegment();
	// Replace the % with the file name
	String outName = outPath.toOSString().replaceAll("%", fileName); //$NON-NLS-1$
	IPath result = Path.fromOSString(outName);
	return DOT_SLASH_PATH.isPrefixOf(outPath) ? DOT_SLASH_PATH.append(result) : result;
    }

    /**
     * Returns the dependency <code>IPath</code>s for this invocation of the tool with the specified source file
     * 
     * @param depGen
     *            the dependency calculator
     * @param tool
     *            tool used to build the source file
     * @param relativePath
     *            build output directory relative path of the current output directory
     * @param resource
     *            source file to scan for dependencies
     * @return Vector of IPaths that are relative to the build directory
     */
    protected IPath[] oldCalculateDependenciesForSource(IManagedDependencyGenerator depGen, ITool tool, String relativePath, IResource resource) {
	Vector<IPath> deps = new Vector<IPath>();
	int type = depGen.getCalculatorType();

	switch (type) {

	case IManagedDependencyGeneratorType.TYPE_INDEXER:
	case IManagedDependencyGeneratorType.TYPE_EXTERNAL:
	    IResource[] res = depGen.findDependencies(resource, project);
	    if (res != null) {
		for (IResource re : res) {
		    IPath dep = null;
		    if (re != null) {
			IPath addlPath = re.getLocation();
			if (addlPath != null) {
			    dep = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), addlPath);
			}
		    }
		    if (dep != null) {
			deps.add(dep);
		    }
		}
	    }
	    break;

	case IManagedDependencyGeneratorType.TYPE_NODEPS:
	default:
	    break;
	}
	return deps.toArray(new IPath[deps.size()]);
    }

    /**
     * Returns the dependency <code>IPath</code>s relative to the build directory
     * 
     * @param depCalculator
     *            the dependency calculator
     * @return IPath[] that are relative to the build directory
     */
    protected IPath[] calculateDependenciesForSource(IManagedDependencyCalculator depCalculator) {
	IPath[] addlDeps = depCalculator.getDependencies();
	if (addlDeps != null) {
	    for (int i = 0; i < addlDeps.length; i++) {
		if (!addlDeps[i].isAbsolute()) {
		    // Convert from project relative to build directory relative
		    IPath absolutePath = project.getLocation().append(addlDeps[i]);
		    addlDeps[i] = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), absolutePath);
		}
	    }
	}
	return addlDeps;
    }

    /*************************************************************************
     * M A K E F I L E G E N E R A T I O N C O M M O N M E T H O D S
     ************************************************************************/

    /**
     * Generates a source macro name from a file extension
     */
    @SuppressWarnings("static-method")
    public StringBuffer getSourceMacroName(String extensionName) {
	StringBuffer macroName = new StringBuffer();

	// We need to handle case sensitivity in file extensions (e.g. .c vs
	// .C), so if the
	// extension was already upper case, tack on an "UPPER_" to the macro
	// name.
	// In theory this means there could be a conflict if you had for
	// example,
	// extensions .c_upper, and .C, but realistically speaking the chances
	// of this are
	// practically nil so it doesn't seem worth the hassle of generating a
	// truly
	// unique name.
	if (extensionName.equals(extensionName.toUpperCase())) {
	    macroName.append(extensionName.toUpperCase() + "_UPPER"); //$NON-NLS-1$
	} else {
	    // lower case... no need for "UPPER_"
	    macroName.append(extensionName.toUpperCase());
	}
	macroName.append("_SRCS"); //$NON-NLS-1$
	return macroName;
    }

    /**
     * Generates a generated dependency file macro name from a file extension
     */
    @SuppressWarnings("static-method")
    public StringBuffer getDepMacroName(String extensionName) {
	StringBuffer macroName = new StringBuffer();

	// We need to handle case sensitivity in file extensions (e.g. .c vs
	// .C), so if the
	// extension was already upper case, tack on an "UPPER_" to the macro
	// name.
	// In theory this means there could be a conflict if you had for
	// example,
	// extensions .c_upper, and .C, but realistically speaking the chances
	// of this are
	// practically nil so it doesn't seem worth the hassle of generating a
	// truly
	// unique name.
	if (extensionName.equals(extensionName.toUpperCase())) {
	    macroName.append(extensionName.toUpperCase() + "_UPPER"); //$NON-NLS-1$
	} else {
	    // lower case... no need for "UPPER_"
	    macroName.append(extensionName.toUpperCase());
	}
	macroName.append("_DEPS"); //$NON-NLS-1$
	return macroName;
    }

    /**
     * Answers all of the output extensions that the target of the build has tools defined to work on.
     * 
     * @return a <code>Set</code> containing all of the output extensions
     */
    @SuppressWarnings("static-method")
    public Set<String> getOutputExtensions(ToolInfoHolder h) {
	if (h.outputExtensionsSet == null) {
	    // The set of output extensions which will be produced by this tool.
	    // It is presumed that this set is not very large (likely < 10) so
	    // a HashSet should provide good performance.
	    h.outputExtensionsSet = new HashSet<String>();

	    // For each tool for the target, lookup the kinds of sources it
	    // outputs
	    // and add that to our list of output extensions.
	    for (ITool tool : h.buildTools) {
		String[] outputs = tool.getAllOutputExtensions();
		if (outputs != null) {
		    h.outputExtensionsSet.addAll(Arrays.asList(outputs));
		}
	    }
	}
	return h.outputExtensionsSet;
    }

    /**
     * This method postprocesses a .d file created by a build. It's main job is to add dummy targets for the header files dependencies. This prevents
     * make from aborting the build if the header file does not exist.
     * 
     * A secondary job is to work in tandem with the "echo" command that is used by some tool-chains in order to get the "targets" part of the
     * dependency rule correct.
     * 
     * This method adds a comment to the beginning of the dependency file which it checks for to determine if this dependency file has already been
     * updated.
     * 
     * @return a <code>true</code> if the dependency file is modified
     */
    static public boolean populateDummyTargets(IConfiguration cfg, IFile makefile, boolean force) throws CoreException, IOException {
	return populateDummyTargets(cfg.getRootFolderInfo(), makefile, force);
    }

    static public boolean populateDummyTargets(IResourceInfo rcInfo, IFile makefile, boolean force) throws CoreException, IOException {

	if (makefile == null || !makefile.exists())
	    return false;

	// Get the contents of the dependency file
	InputStream contentStream = makefile.getContents(false);
	Reader in = new InputStreamReader(contentStream);
	StringBuffer inBuffer = null;
	int chunkSize = contentStream.available();
	inBuffer = new StringBuffer(chunkSize);
	char[] readBuffer = new char[chunkSize];
	int n = in.read(readBuffer);
	while (n > 0) {
	    inBuffer.append(readBuffer);
	    n = in.read(readBuffer);
	}
	contentStream.close();
	in.close();

	// The rest of this operation is equally expensive, so
	// if we are doing an incremental build, only update the
	// files that do not have a comment
	String inBufferString = inBuffer.toString();
	if (!force && inBufferString.startsWith(COMMENT_SYMBOL)) {
	    return false;
	}

	// Try to determine if this file already has dummy targets defined.
	// If so, we will only add the comment.
	String[] bufferLines = inBufferString.split("[\\r\\n]"); //$NON-NLS-1$
	for (String bufferLine : bufferLines) {
	    if (bufferLine.endsWith(":")) { //$NON-NLS-1$
		StringBuffer outBuffer = addDefaultHeader();
		outBuffer.append(inBuffer);
		save(outBuffer, makefile);
		return true;
	    }
	}

	// Reconstruct the buffer tokens into useful chunks of dependency
	// information
	Vector<String> bufferTokens = new Vector<String>(Arrays.asList(inBufferString.split("\\s"))); //$NON-NLS-1$
	Vector<String> deps = new Vector<String>(bufferTokens.size());
	Iterator<String> tokenIter = bufferTokens.iterator();
	while (tokenIter.hasNext()) {
	    String token = tokenIter.next();
	    if (token.lastIndexOf("\\") == token.length() - 1 && token.length() > 1) { //$NON-NLS-1$
		// This is escaped so keep adding to the token until we find the
		// end
		while (tokenIter.hasNext()) {
		    String nextToken = tokenIter.next();
		    token += WHITESPACE + nextToken;
		    if (!nextToken.endsWith("\\")) { //$NON-NLS-1$
			break;
		    }
		}
	    }
	    deps.add(token);
	}
	deps.trimToSize();

	// Now find the header file dependencies and make dummy targets for them
	boolean save = false;
	StringBuffer outBuffer = null;

	// If we are doing an incremental build, only update the files that do
	// not have a comment
	String firstToken;
	try {
	    firstToken = deps.get(0);
	} catch (ArrayIndexOutOfBoundsException e) {
	    // This makes no sense so bail
	    return false;
	}

	// Put the generated comments in the output buffer
	if (!firstToken.startsWith(COMMENT_SYMBOL)) {
	    outBuffer = addDefaultHeader();
	} else {
	    outBuffer = new StringBuffer();
	}

	// Some echo implementations misbehave and put the -n and newline in the
	// output
	if (firstToken.startsWith("-n")) { //$NON-NLS-1$

	    // Now let's parse:
	    // Win32 outputs -n '<path>/<file>.d <path>/'
	    // POSIX outputs -n <path>/<file>.d <path>/
	    // Get the dep file name
	    String secondToken;
	    try {
		secondToken = deps.get(1);
	    } catch (ArrayIndexOutOfBoundsException e) {
		secondToken = new String();
	    }
	    if (secondToken.startsWith("'")) { //$NON-NLS-1$
		// This is the Win32 implementation of echo (MinGW without MSYS)
		outBuffer.append(secondToken.substring(1) + WHITESPACE);
	    } else {
		outBuffer.append(secondToken + WHITESPACE);
	    }

	    // The relative path to the build goal comes next
	    String thirdToken;
	    try {
		thirdToken = deps.get(2);
	    } catch (ArrayIndexOutOfBoundsException e) {
		thirdToken = new String();
	    }
	    int lastIndex = thirdToken.lastIndexOf("'"); //$NON-NLS-1$
	    if (lastIndex != -1) {
		if (lastIndex == 0) {
		    outBuffer.append(WHITESPACE);
		} else {
		    outBuffer.append(thirdToken.substring(0, lastIndex - 1));
		}
	    } else {
		outBuffer.append(thirdToken);
	    }

	    // Followed by the target output by the compiler plus ':'
	    // If we see any empty tokens here, assume they are the result of
	    // a line feed output by "echo" and skip them
	    String fourthToken;
	    int nToken = 3;
	    try {
		do {
		    fourthToken = deps.get(nToken++);
		} while (fourthToken.length() == 0);

	    } catch (ArrayIndexOutOfBoundsException e) {
		fourthToken = new String();
	    }
	    outBuffer.append(fourthToken + WHITESPACE);

	    // Followed by the actual dependencies
	    try {
		for (String nextElement : deps) {
		    if (nextElement.endsWith("\\")) { //$NON-NLS-1$
			outBuffer.append(nextElement + NEWLINE + WHITESPACE);
		    } else {
			outBuffer.append(nextElement + WHITESPACE);
		    }
		}
	    } catch (IndexOutOfBoundsException e) {// JABA is not going to write
						   // this code
	    }

	} else {
	    outBuffer.append(inBuffer);
	}

	outBuffer.append(NEWLINE);
	save = true;

	IFolderInfo fo = null;
	if (rcInfo instanceof IFolderInfo) {
	    fo = (IFolderInfo) rcInfo;
	} else {
	    IConfiguration c = rcInfo.getParent();
	    fo = (IFolderInfo) c.getResourceInfo(rcInfo.getPath().removeLastSegments(1), false);
	}
	// Dummy targets to add to the makefile
	for (String dummy : deps) {
	    IPath dep = new Path(dummy);
	    String extension = dep.getFileExtension();
	    if (fo.isHeaderFile(extension)) {
		/*
		 * The formatting here is <dummy_target>:
		 */
		outBuffer.append(dummy + COLON + NEWLINE + NEWLINE);
	    }
	}

	// Write them out to the makefile
	if (save) {
	    save(outBuffer, makefile);
	    return true;
	}
	return false;
    }

    /**
     * prepend all instanced of '\' or '"' with a backslash
     * 
     * @return resulting string
     */
    static public String escapedEcho(String string) {
	String escapedString = string.replaceAll("'", "'\"'\"'"); //$NON-NLS-1$ //$NON-NLS-2$
	return ECHO + WHITESPACE + SINGLE_QUOTE + escapedString + SINGLE_QUOTE + NEWLINE;
    }

    static public String ECHO_BLANK_LINE = ECHO + WHITESPACE + SINGLE_QUOTE + WHITESPACE + SINGLE_QUOTE + NEWLINE;

    /**
     * Outputs a comment formatted as follows: ##### ....... ##### # <Comment message> ##### ....... #####
     */
    static protected StringBuffer addDefaultHeader() {
	StringBuffer buffer = new StringBuffer();
	outputCommentLine(buffer);
	buffer.append(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(HEADER) + NEWLINE);
	outputCommentLine(buffer);
	buffer.append(NEWLINE);
	return buffer;
    }

    /**
     * Put COLS_PER_LINE comment charaters in the argument.
     */
    static protected void outputCommentLine(StringBuffer buffer) {
	for (int i = 0; i < COLS_PER_LINE; i++) {
	    buffer.append(COMMENT_SYMBOL);
	}
	buffer.append(NEWLINE);
    }

    static public boolean containsSpecialCharacters(String path) {
	return path.matches(".*(\\s|[\\{\\}\\(\\)\\$\\@%=;]).*"); //$NON-NLS-1$
    }

    /**
     * Answers the argument with all whitespaces replaced with an escape sequence.
     */
    static public String escapeWhitespaces(String path) {
	// Escape the spaces in the path/filename if it has any
	String[] segments = path.split("\\s"); //$NON-NLS-1$
	if (segments.length > 1) {
	    StringBuffer escapedPath = new StringBuffer();
	    for (int index = 0; index < segments.length; ++index) {
		escapedPath.append(segments[index]);
		if (index + 1 < segments.length) {
		    escapedPath.append("\\ "); //$NON-NLS-1$
		}
	    }
	    return escapedPath.toString().trim();
	}
	return path;
    }

    /**
     * Adds a macro addition prefix to a map of macro names to entries. Entry prefixes look like: C_SRCS += \ ${addprefix $(ROOT)/, \
     */
    // TODO fix comment
    @SuppressWarnings("static-method")
    protected void addMacroAdditionPrefix(LinkedHashMap<String, String> map, String macroName, String relativePath, boolean addPrefix) {
	// there is no entry in the map, so create a buffer for this macro
	StringBuffer tempBuffer = new StringBuffer();
	tempBuffer.append(macroName + WHITESPACE + MACRO_ADDITION_PREFIX_SUFFIX);
	if (addPrefix) {
	    tempBuffer.append(MACRO_ADDITION_ADDPREFIX_HEADER + relativePath + MACRO_ADDITION_ADDPREFIX_SUFFIX);
	}

	// have to store the buffer in String form as StringBuffer is not a
	// sublcass of Object
	map.put(macroName, tempBuffer.toString());
    }

    /**
     * Adds a file to an entry in a map of macro names to entries. File additions look like: example.c, \
     */
    @SuppressWarnings("static-method")
    protected void addMacroAdditionFile(HashMap<String, String> map, String macroName, String _filename) {
	String filename = _filename;
	StringBuffer buffer = new StringBuffer();
	buffer.append(map.get(macroName));

	// escape whitespace in the filename
	filename = escapeWhitespaces(filename);

	buffer.append(filename + WHITESPACE + LINEBREAK);
	// re-insert string in the map
	map.put(macroName, buffer.toString());
    }

    /**
     * Adds a file to an entry in a map of macro names to entries. File additions look like: example.c, \
     */
    protected void addMacroAdditionFile(HashMap<String, String> map, String macroName, String relativePath, IPath sourceLocation,
	    boolean generatedSource) {
	// Add the source file path to the makefile line that adds source files
	// to the build variable
	String srcName;
	IPath projectLocation = getPathForResource(project);
	IPath dirLocation = projectLocation;
	if (generatedSource) {
	    dirLocation = dirLocation.append(getBuildWorkingDir());
	}
	if (dirLocation.isPrefixOf(sourceLocation)) {
	    IPath srcPath = sourceLocation.removeFirstSegments(dirLocation.segmentCount()).setDevice(null);
	    if (generatedSource) {
		srcName = "./" + srcPath.toString(); //$NON-NLS-1$
	    } else {
		srcName = ROOT + "/" + srcPath.toString(); //$NON-NLS-1$
	    }
	} else {
	    if (generatedSource && !sourceLocation.isAbsolute()) {
		srcName = "./" + relativePath + sourceLocation.lastSegment().toString(); //$NON-NLS-1$
	    } else {
		// TODO: Should we use relative paths when possible (e.g., see
		// MbsMacroSupplier.calculateRelPath)
		srcName = sourceLocation.toString();
	    }
	}
	addMacroAdditionFile(map, macroName, srcName);
    }

    /**
     * Adds file(s) to an entry in a map of macro names to entries. File additions look like: example.c, \
     */
    @SuppressWarnings("static-method")
    public void addMacroAdditionFiles(HashMap<String, String> map, String macroName, Vector<String> filenames) {
	StringBuffer buffer = new StringBuffer();
	buffer.append(map.get(macroName));
	for (int i = 0; i < filenames.size(); i++) {
	    String filename = filenames.get(i);
	    if (filename.length() > 0) {
		buffer.append(filename + WHITESPACE + LINEBREAK);
	    }
	}
	// re-insert string in the map
	map.put(macroName, buffer.toString());
    }

    /**
     * Write all macro addition entries in a map to the buffer
     */
    @SuppressWarnings("static-method")
    protected StringBuffer writeAdditionMacros(LinkedHashMap<String, String> map) {
	StringBuffer buffer = new StringBuffer();
	// Add the comment
	buffer.append(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(MOD_VARS) + NEWLINE);

	for (String macroString : map.values()) {
	    // Check if we added any files to the rule
	    // Currently, we do this by comparing the end of the rule buffer to
	    // MACRO_ADDITION_PREFIX_SUFFIX
	    if (!(macroString.endsWith(MACRO_ADDITION_PREFIX_SUFFIX))) {
		StringBuffer currentBuffer = new StringBuffer();

		// Remove the final "/"
		if (macroString.endsWith(LINEBREAK)) {
		    macroString = macroString.substring(0, (macroString.length() - 2)) + NEWLINE;
		}
		currentBuffer.append(macroString);

		currentBuffer.append(NEWLINE);

		// append the contents of the buffer to the master buffer for
		// the whole file
		buffer.append(currentBuffer);
	    }
	}
	return buffer.append(NEWLINE);
    }

    /**
     * Write all macro addition entries in a map to the buffer
     */
    @SuppressWarnings("static-method")
    protected StringBuffer writeTopAdditionMacros(List<String> varList, HashMap<String, String> varMap) {
	StringBuffer buffer = new StringBuffer();
	// Add the comment
	buffer.append(COMMENT_SYMBOL + WHITESPACE + ManagedMakeMessages.getResourceString(MOD_VARS) + NEWLINE);

	for (int i = 0; i < varList.size(); i++) {
	    String addition = varMap.get(varList.get(i));
	    StringBuffer currentBuffer = new StringBuffer();
	    currentBuffer.append(addition);
	    currentBuffer.append(NEWLINE);

	    // append the contents of the buffer to the master buffer for the
	    // whole file
	    buffer.append(currentBuffer);
	}
	return buffer.append(NEWLINE);
    }

    /**
     * Calculates the inputs and outputs for tools that will be generated in the top makefile. This information is used by the top level makefile
     * generation methods.
     */
    protected void calculateToolInputsOutputs() {

	toolInfos.accept(new IPathSettingsContainerVisitor() {
	    @Override
	    public boolean visit(PathSettingsContainer container) {
		ToolInfoHolder h = (ToolInfoHolder) container.getValue();
		ITool[] buildTools = h.buildTools;
		ArduinoManagedBuildGnuToolInfo[] gnuToolInfos = h.gnuToolInfos;
		// We are "done" when the information for all tools has been
		// calculated,
		// or we are not making any progress
		boolean done = false;
		boolean lastChance = false;
		int[] doneState = new int[buildTools.length];

		// Identify the target tool
		ITool targetTool = config.calculateTargetTool();
		// if (targetTool == null) {
		// targetTool = info.getToolFromOutputExtension(buildTargetExt);
		// }

		// Initialize the tool info array and the done state

		if (buildTools.length != 0 && buildTools[0].getCustomBuildStep())
		    return true;

		for (int i = 0; i < buildTools.length; i++) {
		    if ((buildTools[i] == targetTool)) {
			String ext = config.getArtifactExtension();
			// try to resolve the build macros in the artifact
			// extension
			try {
			    ext = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(ext, "", //$NON-NLS-1$
				    " ", //$NON-NLS-1$
				    IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
			} catch (BuildMacroException e) {// JABA is not going to
							 // write this code
			}

			String name = config.getArtifactName();
			// try to resolve the build macros in the artifact name
			try {
			    String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(name, "", //$NON-NLS-1$
				    " ", //$NON-NLS-1$
				    IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
			    if ((resolved = resolved.trim()).length() > 0)
				name = resolved;
			} catch (BuildMacroException e) {// JABA is not going to
							 // write this code
			}

			gnuToolInfos[i] = new ArduinoManagedBuildGnuToolInfo(project, buildTools[i], true, name, ext);
		    } else {
			gnuToolInfos[i] = new ArduinoManagedBuildGnuToolInfo(project, buildTools[i], false, null, null);
		    }
		    doneState[i] = 0;
		}

		// Initialize the build output variable to file additions map
		LinkedHashMap<String, String> map = getTopBuildOutputVars();
		Set<Entry<String, List<IPath>>> set = buildOutVars.entrySet();
		for (Entry<String, List<IPath>> entry : set) {
		    String macroName = entry.getKey();
		    addMacroAdditionPrefix(map, macroName, "", false); //$NON-NLS-1$
		}

		// Set of input extensions for which macros have been created so
		// far
		HashSet<String> handledDepsInputExtensions = new HashSet<String>();
		HashSet<String> handledOutsInputExtensions = new HashSet<String>();

		while (!done) {
		    int[] testState = new int[doneState.length];
		    for (int i = 0; i < testState.length; i++)
			testState[i] = 0;

		    // Calculate inputs
		    for (int i = 0; i < gnuToolInfos.length; i++) {
			if (gnuToolInfos[i].areInputsCalculated()) {
			    testState[i]++;
			} else {
			    if (gnuToolInfos[i].calculateInputs(ArduinoGnuMakefileGenerator.this, config, projectResources, h, lastChance)) {
				testState[i]++;
			    }
			}
		    }
		    // Calculate dependencies
		    for (int i = 0; i < gnuToolInfos.length; i++) {
			if (gnuToolInfos[i].areDependenciesCalculated()) {
			    testState[i]++;
			} else {
			    if (gnuToolInfos[i].calculateDependencies(ArduinoGnuMakefileGenerator.this, config, handledDepsInputExtensions, h,
				    lastChance)) {
				testState[i]++;
			    }
			}
		    }
		    // Calculate outputs
		    for (int i = 0; i < gnuToolInfos.length; i++) {
			if (gnuToolInfos[i].areOutputsCalculated()) {
			    testState[i]++;
			} else {
			    if (gnuToolInfos[i].calculateOutputs(ArduinoGnuMakefileGenerator.this, config, handledOutsInputExtensions, lastChance)) {
				testState[i]++;
			    }
			}
		    }
		    // Are all calculated? If so, done.
		    done = true;
		    for (int element : testState) {
			if (element != 3) {
			    done = false;
			    break;
			}
		    }

		    // Test our "done" state vs. the previous "done" state.
		    // If we have made no progress, give it a "last chance" and
		    // then quit
		    if (!done) {
			done = true;
			for (int i = 0; i < testState.length; i++) {
			    if (testState[i] != doneState[i]) {
				done = false;
				break;
			    }
			}
		    }
		    if (done) {
			if (!lastChance) {
			    lastChance = true;
			    done = false;
			}
		    }
		    if (!done) {
			doneState = testState;
		    }
		}
		return true;
	    }
	});
    }

    /**
     * Returns the (String) list of files associated with the build variable
     * 
     * @param variable
     *            the variable name
     * @param locationType
     *            the format in which we want the filenames returned
     * @param directory
     *            project relative directory path used with locationType == DIRECTORY_RELATIVE
     * @param getAll
     *            only return the list if all tools that are going to contrubute to this variable have done so.
     * @return List
     */
    public List<String> getBuildVariableList(ToolInfoHolder h, String variable, int locationType, IPath directory, boolean getAll) {
	ArduinoManagedBuildGnuToolInfo[] gnuToolInfos = h.gnuToolInfos;
	boolean done = true;
	for (int i = 0; i < gnuToolInfos.length; i++) {
	    if (!gnuToolInfos[i].areOutputVariablesCalculated()) {
		done = false;
	    }
	}
	if (!done && getAll)
	    return null;
	List<IPath> list = buildSrcVars.get(variable);
	if (list == null) {
	    list = buildOutVars.get(variable);
	}

	List<String> fileList = null;
	if (list != null) {
	    // Convert the items in the list to the location-type wanted by the
	    // caller,
	    // and to a string list
	    IPath dirLocation = null;
	    if (locationType != ABSOLUTE) {
		dirLocation = project.getLocation();
		if (locationType == PROJECT_SUBDIR_RELATIVE) {
		    dirLocation = dirLocation.append(directory);
		}
	    }
	    for (int i = 0; i < list.size(); i++) {
		IPath path = list.get(i);
		if (locationType != ABSOLUTE) {
		    if (dirLocation != null && dirLocation.isPrefixOf(path)) {
			path = path.removeFirstSegments(dirLocation.segmentCount()).setDevice(null);
		    }
		}
		if (fileList == null) {
		    fileList = new Vector<String>();
		}
		fileList.add(path.toString());
	    }
	}

	return fileList;
    }

    /**
     * Returns the map of build variables to list of files
     * 
     * @return HashMap
     */
    public HashMap<String, List<IPath>> getBuildOutputVars() {
	return buildOutVars;
    }

    /**
     * Returns the map of build variables used in the top makefile to list of files
     * 
     * @return HashMap
     */
    public LinkedHashMap<String, String> getTopBuildOutputVars() {
	return topBuildOutVars;
    }

    /**
     * Returns the list of known build rules. This keeps me from generating duplicate rules for known file extensions.
     * 
     * @return List
     */
    protected Vector<String> getRuleList() {
	if (ruleList == null) {
	    ruleList = new Vector<String>();
	}
	return ruleList;
    }

    /**
     * Returns the list of known dependency lines. This keeps me from generating duplicate lines.
     * 
     * @return List
     */
    protected Vector<String> getDepLineList() {
	if (depLineList == null) {
	    depLineList = new Vector<String>();
	}
	return depLineList;
    }

    /**
     * Returns the list of known dependency file generation lines. This keeps me from generating duplicate lines.
     * 
     * @return List
     */
    protected Vector<String> getDepRuleList() {
	if (depRuleList == null) {
	    depRuleList = new Vector<String>();
	}
	return depRuleList;
    }

    /*************************************************************************
     * R E S O U R C E V I S I T O R M E T H O D S
     ************************************************************************/

    /**
     * Adds the container of the argument to the list of folders in the project that contribute source files to the build. The resource visitor has
     * already established that the build model knows how to build the files. It has also checked that the resource is not generated as part of the
     * build.
     */
    protected void appendBuildSubdirectory(IResource resource) {
	IContainer container = resource.getParent();
	// Only add the container once
	if (!getSubdirList().contains(container))
	    getSubdirList().add(container);
    }

    /**
     * Adds the container of the argument to a list of subdirectories that are to be deleted. As a result, the directories that are generated for the
     * output should be removed as well.
     */
    protected void appendDeletedSubdirectory(IContainer container) {
	// No point in adding a folder if the parent is already there
	IContainer parent = container.getParent();
	if (!getDeletedDirList().contains(container) && !getDeletedDirList().contains(parent)) {
	    getDeletedDirList().add(container);
	}
    }

    /**
     * If a file is removed from a source folder (either because of a delete or move action on the part of the user), the makefilegenerator has to
     * remove the dependency makefile along with the old build goal
     */
    protected void appendDeletedFile(IResource resource) {
	// Cache this for now
	getDeletedFileList().add(resource);
    }

    /**
     * Adds the container of the argument to a list of subdirectories that are part of an incremental rebuild of the project. The makefile fragments
     * for these directories will be regenerated as a result of the build.
     */
    protected void appendModifiedSubdirectory(IResource resource) {
	IContainer container = resource.getParent();

	if (!getModifiedList().contains(container)) {
	    getModifiedList().add(container);
	}
    }

    /*************************************************************************
     * O T H E R M E T H O D S
     ************************************************************************/

    protected void cancel(String message) {
	if (monitor != null && !monitor.isCanceled()) {
	    throw new OperationCanceledException(message);
	}
    }

    /**
     * Check whether the build has been cancelled. Cancellation requests propagated to the caller by throwing <code>OperationCanceledException</code>.
     * 
     * @see org.eclipse.core.runtime.OperationCanceledException#OperationCanceledException()
     */
    protected void checkCancel() {
	if (monitor != null && monitor.isCanceled()) {
	    throw new OperationCanceledException();
	}
    }

    /**
     * Return or create the folder needed for the build output. If we are creating the folder, set the derived bit to true so the CM system ignores
     * the contents. If the resource exists, respect the existing derived setting.
     */
    private IPath createDirectory(String dirName) throws CoreException {
	// Create or get the handle for the build directory
	IFolder folder = project.getFolder(dirName);
	if (!folder.exists()) {
	    // Make sure that parent folders exist
	    IPath parentPath = (new Path(dirName)).removeLastSegments(1);
	    // Assume that the parent exists if the path is empty
	    if (!parentPath.isEmpty()) {
		IFolder parent = project.getFolder(parentPath);
		if (!parent.exists()) {
		    createDirectory(parentPath.toString());
		}
	    }

	    // Now make the requested folder
	    try {
		folder.create(true, true, null);
	    } catch (CoreException e) {
		if (e.getStatus().getCode() == IResourceStatus.PATH_OCCUPIED)
		    folder.refreshLocal(IResource.DEPTH_ZERO, null);
		else
		    throw e;
	    }

	    // Make sure the folder is marked as derived so it is not added to
	    // CM
	    if (!folder.isDerived()) {
		folder.setDerived(true, null);
	    }
	}

	return folder.getFullPath();
    }

    /**
     * Return or create the makefile needed for the build. If we are creating the resource, set the derived bit to true so the CM system ignores the
     * contents. If the resource exists, respect the existing derived setting.
     */
    private IFile createFile(IPath makefilePath) throws CoreException {
	// Create or get the handle for the makefile
	IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
	IFile newFile = root.getFileForLocation(makefilePath);
	if (newFile == null) {
	    newFile = root.getFile(makefilePath);
	}
	// Create the file if it does not exist
	ByteArrayInputStream contents = new ByteArrayInputStream(new byte[0]);
	try {
	    newFile.create(contents, false, new SubProgressMonitor(monitor, 1));
	    // Make sure the new file is marked as derived
	    if (!newFile.isDerived()) {
		newFile.setDerived(true, null);
	    }

	} catch (CoreException e) {
	    // If the file already existed locally, just refresh to get contents
	    if (e.getStatus().getCode() == IResourceStatus.PATH_OCCUPIED)
		newFile.refreshLocal(IResource.DEPTH_ZERO, null);
	    else
		throw e;
	}

	return newFile;
    }

    private void deleteBuildTarget(IResource deletedFile) {
	// Get the project relative path of the file
	String fileName = getFileName(deletedFile);
	String srcExtension = deletedFile.getFileExtension();
	IPath folderPath = inFullPathFromOutFullPath(deletedFile.getFullPath().removeLastSegments(1));
	if (folderPath != null) {
	    folderPath = folderPath.removeFirstSegments(1);
	} else {
	    folderPath = new Path(""); //$NON-NLS-1$
	}
	IResourceInfo rcInfo = config.getResourceInfo(folderPath, false);
	if (rcInfo instanceof IFileInfo) {
	    rcInfo = config.getResourceInfo(folderPath.removeLastSegments(1), false);
	}
	String targetExtension = ((IFolderInfo) rcInfo).getOutputExtension(srcExtension);
	if (targetExtension != "") //$NON-NLS-1$
	    fileName += DOT + targetExtension;
	IPath projectRelativePath = deletedFile.getProjectRelativePath().removeLastSegments(1);
	IPath targetFilePath = getBuildWorkingDir().append(projectRelativePath).append(fileName);
	IResource depFile = project.findMember(targetFilePath);
	if (depFile != null && depFile.exists()) {
	    try {
		depFile.delete(true, new SubProgressMonitor(monitor, 1));
	    } catch (CoreException e) {
		// This had better be allowed during a build

	    }
	}
    }

    private IPath inFullPathFromOutFullPath(IPath path) {
	IPath inPath = null;
	if (topBuildDir.isPrefixOf(path)) {
	    inPath = path.removeFirstSegments(topBuildDir.segmentCount());
	    inPath = project.getFullPath().append(path);
	}
	return inPath;
    }

    private void deleteDepFile(IResource deletedFile) {
	// Calculate the top build directory relative paths of the dependency
	// files
	IPath[] depFilePaths = null;
	ITool tool = null;
	IManagedDependencyGeneratorType depType = null;
	String sourceExtension = deletedFile.getFileExtension();
	IPath folderPath = inFullPathFromOutFullPath(deletedFile.getFullPath().removeLastSegments(1));
	if (folderPath != null) {
	    folderPath = folderPath.removeFirstSegments(1);
	} else {
	    folderPath = new Path(""); //$NON-NLS-1$
	}
	ToolInfoHolder h = getToolInfo(folderPath);
	ITool[] tools = h.buildTools;
	for (int index = 0; index < tools.length; ++index) {
	    if (tools[index].buildsFileType(sourceExtension)) {
		tool = tools[index];
		depType = tool.getDependencyGeneratorForExtension(sourceExtension);
		break;
	    }
	}
	if (depType != null) {
	    int calcType = depType.getCalculatorType();
	    if (calcType == IManagedDependencyGeneratorType.TYPE_COMMAND) {
		depFilePaths = new IPath[1];
		IPath absolutePath = deletedFile.getLocation();
		depFilePaths[0] = ManagedBuildManager.calculateRelativePath(getTopBuildDir(), absolutePath);
		depFilePaths[0] = depFilePaths[0].removeFileExtension().addFileExtension(DEP_EXT);
	    } else if (calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS
		    || calcType == IManagedDependencyGeneratorType.TYPE_PREBUILD_COMMANDS) {
		IManagedDependencyGenerator2 depGen = (IManagedDependencyGenerator2) depType;
		IManagedDependencyInfo depInfo = depGen.getDependencySourceInfo(deletedFile.getProjectRelativePath(), deletedFile, config, tool,
			getBuildWorkingDir());
		if (depInfo != null) {
		    if (calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS) {
			IManagedDependencyCommands depCommands = (IManagedDependencyCommands) depInfo;
			depFilePaths = depCommands.getDependencyFiles();
		    } else if (calcType == IManagedDependencyGeneratorType.TYPE_PREBUILD_COMMANDS) {
			IManagedDependencyPreBuild depPreBuild = (IManagedDependencyPreBuild) depInfo;
			depFilePaths = depPreBuild.getDependencyFiles();
		    }
		}
	    }
	}

	// Delete the files if they exist
	if (depFilePaths != null) {
	    for (IPath dfp : depFilePaths) {
		IPath depFilePath = getBuildWorkingDir().append(dfp);
		IResource depFile = project.findMember(depFilePath);
		if (depFile != null && depFile.exists()) {
		    try {
			depFile.delete(true, new SubProgressMonitor(monitor, 1));
		    } catch (CoreException e) {
			// This had better be allowed during a build
		    }
		}
	    }
	}
    }

    /**
     * Returns the current build configuration.
     * 
     * @return String
     * @since 8.0
     */
    protected IConfiguration getConfig() {
	return config;
    }

    /**
     * Returns the build target extension.
     * 
     * @return String
     * @since 8.0
     */
    protected String getBuildTargetExt() {
	return buildTargetExt;
    }

    /**
     * Returns the build target name.
     * 
     * @return String
     * @since 8.0
     */
    protected String getBuildTargetName() {
	return buildTargetName;
    }

    /**
     * @return Returns the deletedDirList.
     */
    private Vector<IResource> getDeletedDirList() {
	if (deletedDirList == null) {
	    deletedDirList = new Vector<IResource>();
	}
	return deletedDirList;
    }

    private Vector<IResource> getDeletedFileList() {
	if (deletedFileList == null) {
	    deletedFileList = new Vector<IResource>();
	}
	return deletedFileList;
    }

    @SuppressWarnings("static-method")
    private List<IPath> getDependencyMakefiles(ToolInfoHolder h) {
	if (h.dependencyMakefiles == null) {
	    h.dependencyMakefiles = new ArrayList<IPath>();
	}
	return h.dependencyMakefiles;
    }

    /**
     * Strips off the file extension from the argument and returns the name component in a <code>String</code>
     */
    @SuppressWarnings("static-method")
    private String getFileName(IResource file) {
	String answer = new String();
	String lastSegment = file.getName();
	int extensionSeparator = lastSegment.lastIndexOf(DOT);
	if (extensionSeparator != -1) {
	    answer = lastSegment.substring(0, extensionSeparator);
	}
	return answer;
    }

    /**
     * Answers a Vector containing a list of directories that are invalid for the build for some reason. At the moment, the only reason a directory
     * would not be considered for the build is if it contains a space in the relative path from the project root.
     * 
     * @return a a list of directories that are invalid for the build
     */
    private Vector<IResource> getInvalidDirList() {
	if (invalidDirList == null) {
	    invalidDirList = new Vector<IResource>();
	}
	return invalidDirList;
    }

    /**
     * @return Collection of Containers which contain modified source files
     */
    private Collection<IContainer> getModifiedList() {
	if (modifiedList == null)
	    modifiedList = new LinkedHashSet<IContainer>();
	return modifiedList;
    }

    /**
     * @return Collection of subdirectories (IContainers) contributing source code to the build
     */
    private Collection<IContainer> getSubdirList() {
	if (subdirList == null)
	    subdirList = new LinkedHashSet<IContainer>();
	return subdirList;
    }

    private void removeGeneratedDirectory(IContainer subDir) {
	try {
	    // The source directory isn't empty
	    if (subDir.exists() && subDir.members().length > 0)
		return;
	} catch (CoreException e) {
	    // The resource doesn't exist so we should delete the output folder
	}

	// Figure out what the generated directory name is and delete it
	IPath moduleRelativePath = subDir.getProjectRelativePath();
	IPath buildRoot = getBuildWorkingDir();
	if (buildRoot == null) {
	    return;
	}
	IPath moduleOutputPath = buildRoot.append(moduleRelativePath);
	IFolder folder = project.getFolder(moduleOutputPath);
	if (folder.exists()) {
	    try {
		folder.delete(true, new SubProgressMonitor(monitor, 1));
	    } catch (CoreException e) {
		Common.log(new Status(IStatus.INFO, ArduinoConst.CORE_PLUGIN_ID, "Folder deletion failed " + folder.toString(), e));
	    }
	}
    }

    protected void updateMonitor(String msg) {
	if (monitor != null && !monitor.isCanceled()) {
	    monitor.subTask(msg);
	    monitor.worked(1);
	}
    }

    /**
     * Return the configuration's top build directory as an absolute path
     */
    public IPath getTopBuildDir() {
	return getPathForResource(project).append(getBuildWorkingDir());
    }

    /**
     * Process a String denoting a filepath in a way compatible for GNU Make rules, handling windows drive letters and whitespace appropriately.
     * <p>
     * <p>
     * The context these paths appear in is on the right hand side of a rule header. i.e.
     * <p>
     * <p>
     * target : dep1 dep2 dep3
     * <p>
     * 
     * @param path
     *            the String denoting the path to process
     * @throws NullPointerException
     *             is path is null
     * @return a suitable Make rule compatible path
     */
    /* see https://bugs.eclipse.org/bugs/show_bug.cgi?id=129782 */
    @SuppressWarnings("static-method")
    public String ensurePathIsGNUMakeTargetRuleCompatibleSyntax(String path) {
	return escapeWhitespaces(ensureUnquoted(path));
    }

    /**
     * Strips outermost quotes of Strings of the form "a" and 'a' or returns the original string if the input is not of this form.
     * 
     * @throws NullPointerException
     *             if path is null
     * @return a String without the outermost quotes (if the input has them)
     */
    public static String ensureUnquoted(String path) {
	boolean doubleQuoted = path.startsWith("\"") && path.endsWith("\""); //$NON-NLS-1$ //$NON-NLS-2$
	boolean singleQuoted = path.startsWith("'") && path.endsWith("'"); //$NON-NLS-1$ //$NON-NLS-2$
	return doubleQuoted || singleQuoted ? path.substring(1, path.length() - 1) : path;
    }

    @SuppressWarnings("hiding")
    @Override
    public void initialize(int buildKind, IConfiguration cfg, IBuilder _builder, IProgressMonitor monitor) {
	IBuilder builder = _builder;
	// Save the project so we can get path and member information
	this.project = cfg.getOwner().getProject();
	if (builder == null) {
	    builder = cfg.getEditableBuilder();
	}
	try {
	    projectResources = project.members();
	} catch (CoreException e) {
	    projectResources = null;
	}
	// Save the monitor reference for reporting back to the user
	this.monitor = monitor;
	// Get the build info for the project
	// this.info = info;
	// Get the name of the build target
	buildTargetName = cfg.getArtifactName();
	// Get its extension
	buildTargetExt = cfg.getArtifactExtension();

	try {
	    // try to resolve the build macros in the target extension
	    buildTargetExt = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(buildTargetExt, "", //$NON-NLS-1$
		    " ", //$NON-NLS-1$
		    IBuildMacroProvider.CONTEXT_CONFIGURATION, builder);
	} catch (BuildMacroException e) {// JABA is not going to write this code
	}

	try {
	    // try to resolve the build macros in the target name
	    String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(buildTargetName, "", //$NON-NLS-1$
		    " ", //$NON-NLS-1$
		    IBuildMacroProvider.CONTEXT_CONFIGURATION, builder);
	    if (resolved != null) {
		resolved = resolved.trim();
		if (resolved.length() > 0)
		    buildTargetName = resolved;
	    }
	} catch (BuildMacroException e) {// JABA is not going to write this code
	}

	if (buildTargetExt == null) {
	    buildTargetExt = new String();
	}
	// Cache the build tools
	config = cfg;
	this.builder = builder;

	initToolInfos();
	// set the top build dir path
	topBuildDir = project.getFolder(cfg.getName()).getFullPath();

	srcEntries = config.getSourceEntries();
	if (srcEntries.length == 0) {
	    srcEntries = new ICSourceEntry[] { new CSourceEntry(Path.EMPTY, null, ICSettingEntry.RESOLVED | ICSettingEntry.VALUE_WORKSPACE_PATH) };
	} else {
	    ICConfigurationDescription cfgDes = ManagedBuildManager.getDescriptionForConfiguration(config);
	    srcEntries = CDataUtil.resolveEntries(srcEntries, cfgDes);
	}
    }

    private void initToolInfos() {
	toolInfos = PathSettingsContainer.createRootContainer();

	IResourceInfo rcInfos[] = config.getResourceInfos();
	for (IResourceInfo rcInfo : rcInfos) {
	    if (rcInfo.isExcluded() /* && !((ResourceInfo)rcInfo).isRoot() */)
		continue;

	    ToolInfoHolder h = getToolInfo(rcInfo.getPath(), true);
	    if (rcInfo instanceof IFolderInfo) {
		IFolderInfo fo = (IFolderInfo) rcInfo;
		h.buildTools = fo.getFilteredTools();
		h.buildToolsUsed = new boolean[h.buildTools.length];
		h.gnuToolInfos = new ArduinoManagedBuildGnuToolInfo[h.buildTools.length];
	    } else {
		IFileInfo fi = (IFileInfo) rcInfo;
		h.buildTools = fi.getToolsToInvoke();
		h.buildToolsUsed = new boolean[h.buildTools.length];
		h.gnuToolInfos = new ArduinoManagedBuildGnuToolInfo[h.buildTools.length];
	    }
	}
    }

    private ToolInfoHolder getToolInfo(IPath path) {
	return getToolInfo(path, false);
    }

    private ToolInfoHolder getFolderToolInfo(IPath _path) {
	IPath path = _path;
	IResourceInfo rcInfo = config.getResourceInfo(path, false);
	while (rcInfo instanceof IFileInfo) {
	    path = path.removeLastSegments(1);
	    rcInfo = config.getResourceInfo(path, false);
	}
	return getToolInfo(path, false);
    }

    private ToolInfoHolder getToolInfo(IPath path, boolean create) {
	PathSettingsContainer child = toolInfos.getChildContainer(path, create, create);
	ToolInfoHolder h = null;
	if (child != null) {
	    h = (ToolInfoHolder) child.getValue();
	    if (h == null && create) {
		h = new ToolInfoHolder();
		child.setValue(h);
	    }
	}
	return h;
    }
}
