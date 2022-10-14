package io.sloeber.managedBuild.Internal;

import static io.sloeber.core.common.Const.*;
import static io.sloeber.managedBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.managedBuild.Internal.ManagedBuildConstants.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.core.settings.model.util.IPathSettingsContainerVisitor;
import org.eclipse.cdt.core.settings.model.util.PathSettingsContainer;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFileInfo;
import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyCommands;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator2;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyInfo;
import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyPreBuild;
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

import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;

/**
 * This is a specialized makefile generator that takes advantage of the
 * extensions present in Gnu Make.
 *
 * @since 1.2
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings({ "deprecation", "restriction", "nls", "unused", "synthetic-access", "static-method", "unchecked",
        "hiding" })
public class ArduinoGnuMakefileGenerator implements IManagedBuilderMakefileGenerator2 {
    SubDirMakeGenerator mySubDirMakeGenerator = null;
    TopMakeFileGenerator myTopMakeFileGenerator = null;
    SrcMakeGenerator mySrcMakeGenerator = null;

    /**
     * This class walks the delta supplied by the build system to determine what
     * resources have been changed. The logic is very simple. If a buildable
     * resource (non-header) has been added or removed, the directories in which
     * they are located are "dirty" so the makefile fragments for them have to be
     * regenerated.
     * <p>
     * The actual dependencies are recalculated as a result of the build step
     * itself. We are relying on make to do the right things when confronted with a
     * dependency on a moved header file. That said, make will treat the missing
     * header file in a dependency rule as a target it has to build unless told
     * otherwise. These dummy targets are added to the makefile to avoid a missing
     * target error.
     */
    public class ResourceDeltaVisitor implements IResourceDeltaVisitor {
        private final ArduinoGnuMakefileGenerator generator;
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
         * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.
         * core.resources.IResourceDelta)
         */
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
            if (isSource) {
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
     * This class is used to recursively walk the project and determine which
     * modules contribute buildable source files.
     */
    protected class ResourceProxyVisitor implements IResourceProxyVisitor {
        private final ArduinoGnuMakefileGenerator generator;
        private final IConfiguration config;

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
         * @see org.eclipse.core.resources.IResourceProxyVisitor#visit(org.eclipse.
         * core.resources.IResourceProxy)
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
                if (isSource) {
                    boolean willBuild = false;
                    if (rcInfo instanceof IFolderInfo) {
                        String ext = resource.getFileExtension();
                        if (((IFolderInfo) rcInfo).buildsFileType(ext) && !generator.isGeneratedResource(resource)) {
                            willBuild = true;
                        }
                    } else {
                        willBuild = true;
                    }
                    if (willBuild)
                        generator.appendBuildSubdirectory(resource);
                }
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

    // Local variables needed by generator
    String buildTargetName;
    String buildTargetExt;
    IConfiguration config;
    private IBuilder builder;
    protected PathSettingsContainer toolInfos;
    private Vector<IResource> deletedFileList;
    private Vector<IResource> deletedDirList;
    private Vector<IResource> invalidDirList;
    /** Collection of Folders in which sources files have been modified */
    private Collection<IContainer> modifiedList;
    private IProgressMonitor monitor;
    private IProject project;
    IResource[] projectResources;
    private Vector<String> ruleList;

    // lines
    private Vector<String> depRuleList;
    // dependency files
    /** Collection of Containers which contribute source files to the build */
    private Collection<IContainer> subdirList;
    private IFile topBuildDir;
    final HashMap<String, List<IPath>> buildSrcVars = new HashMap<>();
    final HashMap<String, List<IPath>> buildOutVars = new HashMap<>();
    final HashMap<String, ArduinoGnuDependencyGroupInfo> buildDepVars = new HashMap<>();
    private final LinkedHashMap<String, String> topBuildOutVars = new LinkedHashMap<>();
    // Dependency file variables
    // private Vector dependencyMakefiles; // IPath's - relative to the top
    // build directory or absolute
    private ICSourceEntry srcEntries[];
    // Added by jaba
    IOutputType usedOutType = null;
    // End of added by jaba

    public ArduinoGnuMakefileGenerator() {
        super();
    }

    /*************************************************************************
     * IManagedBuilderMakefileGenerator M E T H O D S
     ************************************************************************/
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator
     * #initialize(IProject, IManagedBuildInfo, IProgressMonitor)
     */
    @Override
    public void initialize(IProject project, IManagedBuildInfo info, IProgressMonitor monitor) {

        this.project = project;
        mySubDirMakeGenerator = new SubDirMakeGenerator(this);
        myTopMakeFileGenerator = new TopMakeFileGenerator(this);
        mySrcMakeGenerator = new SrcMakeGenerator(this);
        try {
            projectResources = project.members();
        } catch (CoreException e) {
            projectResources = null;
        }
        // Save the monitor reference for reporting back to the user
        this.monitor = monitor;
        // Get the name of the build target
        buildTargetName = info.getBuildArtifactName();
        // Get its extension
        buildTargetExt = info.getBuildArtifactExtension();
        try {
            // try to resolve the build macros in the target extension
            buildTargetExt = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(buildTargetExt,
                    "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());
        } catch (BuildMacroException e) {
            /* JABA is not going to write this code */
        }
        try {
            // try to resolve the build macros in the target name
            String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(buildTargetName,
                    "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());
            if (resolved != null && (resolved = resolved.trim()).length() > 0)
                buildTargetName = resolved;
        } catch (BuildMacroException e) {
            /* JABA is not going to write this code */
        }
        if (buildTargetExt == null) {
            buildTargetExt = "";
        }
        // Cache the build tools
        config = info.getDefaultConfiguration();
        builder = config.getEditableBuilder();
        initToolInfos();
        // set the top build dir path
        topBuildDir = project.getFile(info.getConfigurationName());
    }

    /**
     * This method calls the dependency postprocessors defined for the tool chain
     */
    private void callDependencyPostProcessors(IResourceInfo rcInfo, ToolInfoHolder h, IFile depFile,
            IManagedDependencyGenerator2[] postProcessors, boolean callPopulateDummyTargets, boolean force)
            throws CoreException {
        try {
            updateMonitor(ManagedMakeMessages
                    .getFormattedString("ArduinoGnuMakefileGenerator.message.postproc.dep.file", depFile.getName()));
            if (postProcessors != null) {
                IPath absolutePath = new Path(
                        EFSExtensionManager.getDefault().getPathFromURI(depFile.getLocationURI()));
                // Convert to build directory relative
                IPath depPath = ManagedBuildManager.calculateRelativePath(getTopBuildDir().getFullPath(), absolutePath);
                for (int i = 0; i < postProcessors.length; i++) {
                    IManagedDependencyGenerator2 depGen = postProcessors[i];
                    if (depGen != null) {
                        depGen.postProcessDependencyFile(depPath, config, h.buildTools[i],
                                getTopBuildDir().getFullPath());
                    }
                }
            }
            if (callPopulateDummyTargets) {
                populateDummyTargets(rcInfo, depFile, force);
            }
        } catch (CoreException e) {
            throw e;
        } catch (IOException e) {
            /* JABA is not going to write this code */
        }
    }

    /**
     * This method collects the dependency postprocessors and file extensions
     * defined for the tool chain
     */
    private boolean collectDependencyGeneratorInformation(ToolInfoHolder h, Vector<String> depExts,
            IManagedDependencyGenerator2[] postProcessors) {
        boolean callPopulateDummyTargets = false;
        for (int i = 0; i < h.buildTools.length; i++) {
            ITool tool = h.buildTools[i];
            IManagedDependencyGeneratorType depType = tool
                    .getDependencyGeneratorForExtension(tool.getDefaultInputExtension());
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

    public boolean isSource(IPath path) {
        return !CDataUtil.isExcluded(path, srcEntries);
    }

    private class DepInfo {
        Vector<String> depExts;
        IManagedDependencyGenerator2[] postProcessors;
        boolean callPopulateDummyTargets;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator
     * #generateDependencies()
     */
    @Override
    public void generateDependencies() throws CoreException {
        final PathSettingsContainer postProcs = PathSettingsContainer.createRootContainer();
        // Note: PopulateDummyTargets is a hack for the pre-3.x GCC compilers
        // Collect the methods that will need to be called
        toolInfos.accept(new IPathSettingsContainerVisitor() {
            @Override
            public boolean visit(PathSettingsContainer container) {
                ToolInfoHolder h = (ToolInfoHolder) container.getValue();
                Vector<String> depExts = new Vector<>();
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
            ToolInfoHolder h = ToolInfoHolder.getToolInfo(this, projectRelativePath);
            IPath buildRelativePath = topBuildDir.getFullPath().append(projectRelativePath);
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
                        callDependencyPostProcessors(rcInfo, h, depFile, di.postProcessors, di.callPopulateDummyTargets,
                                false);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#
     * generateMakefiles(org.eclipse.core.resources.IResourceDelta)
     */
    @Override
    public MultiStatus generateMakefiles(IResourceDelta delta) throws CoreException {
        /*
         * Let's do a sanity check right now. 1. This is an incremental build, so if the
         * top-level directory is not there, then a rebuild is needed.
         */
        IFolder folder = project.getFolder(config.getName());
        if (!folder.exists()) {
            return regenerateMakefiles();
        }
        // Return value
        MultiStatus status;
        // Visit the resources in the delta and compile a list of subdirectories
        // to regenerate
        updateMonitor(
                ManagedMakeMessages.getFormattedString("MakefileGenerator.message.calc.delta", project.getName()));
        ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(this, config);
        delta.accept(visitor);
        checkCancel();
        // Get all the subdirectories participating in the build
        updateMonitor(
                ManagedMakeMessages.getFormattedString("MakefileGenerator.message.finding.sources", project.getName()));
        ResourceProxyVisitor resourceVisitor = new ResourceProxyVisitor(this, config);
        project.accept(resourceVisitor, IResource.NONE);
        checkCancel();
        // Bug 303953: Ensure that if all resources have been removed from a
        // folder, than the folder still
        // appears in the subdir list so it's subdir.mk is correctly regenerated
        getSubdirList().addAll(getModifiedList());
        // Make sure there is something to build
        if (getSubdirList().isEmpty()) {
            String info = ManagedMakeMessages.getFormattedString("MakefileGenerator.warning.no.source",
                    project.getName());
            updateMonitor(info);
            status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.INFO, "", null);
            status.add(new Status(IStatus.INFO, ManagedBuilderCorePlugin.getUniqueIdentifier(), NO_SOURCE_FOLDERS, info,
                    null));
            return status;
        }
        // Make sure the build directory is available
        topBuildDir = project.getFile(config.getName());
        createDirectory(project, config.getName());
        checkCancel();
        // Make sure that there is a makefile containing all the folders
        // participating
        IPath srcsFilePath = topBuildDir.getFullPath().append(SRCSFILE_NAME);
        IFile srcsFileHandle = createFile(srcsFilePath);
        buildSrcVars.clear();
        buildOutVars.clear();
        buildDepVars.clear();
        topBuildOutVars.clear();
        mySrcMakeGenerator.populateSourcesMakefile(srcsFileHandle, toolInfos, subdirList);
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
                IPath fragmentPath = getBuildWorkingDir().append(subdirectory.getProjectRelativePath())
                        .append(MODFILE_NAME);
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
                mySubDirMakeGenerator.populateFragmentMakefile(subDir);
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
        IPath makefilePath = topBuildDir.getFullPath().append(MAKEFILE_NAME);
        IFile makefileHandle = createFile(makefilePath);
        myTopMakeFileGenerator.populateTopMakefile(makefileHandle, false);
        checkCancel();
        // Remove deleted folders from generated build directory
        for (IResource res : getDeletedDirList()) {
            IContainer subDir = (IContainer) res;
            removeGeneratedDirectory(subDir);
            checkCancel();
        }
        // How did we do
        if (!getInvalidDirList().isEmpty()) {
            status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.WARNING, "", null);
            // Add a new status for each of the bad folders
            // TODO: fix error message
            for (IResource res : getInvalidDirList()) {
                IContainer subDir = (IContainer) res;
                status.add(new Status(IStatus.WARNING, ManagedBuilderCorePlugin.getUniqueIdentifier(), SPACES_IN_PATH,
                        subDir.getFullPath().toString(), null));
            }
        } else {
            status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.OK, "", null);
        }
        return status;
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
            return topBuildDir.getFullPath().removeFirstSegments(1);
        }
        return null;
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
     * (non-Javadoc)
     *
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#
     * isGeneratedResource(org.eclipse.core.resources.IResource)
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

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#
     * regenerateDependencies()
     */
    @Override
    public void regenerateDependencies(boolean force) throws CoreException {
        // A hack for the pre-3.x GCC compilers is to put dummy targets for deps
        final IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
        final CoreException[] es = new CoreException[1];
        toolInfos.accept(new IPathSettingsContainerVisitor() {
            @Override
            public boolean visit(PathSettingsContainer container) {
                ToolInfoHolder h = (ToolInfoHolder) container.getValue();
                // Collect the methods that will need to be called
                Vector<String> depExts = new Vector<String>();
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
                    IPath relDepFilePath = topBuildDir.getFullPath().append(path);
                    IFile depFile = root.getFile(relDepFilePath);
                    if (depFile == null || !depFile.isAccessible())
                        continue;
                    try {
                        callDependencyPostProcessors(rcInfo, h, depFile, postProcessors, callPopulateDummyTargets,
                                true);
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
     * @see org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator#
     * regenerateMakefiles()
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
            String info = ManagedMakeMessages.getFormattedString("MakefileGenerator.warning.no.source",
                    project.getName());
            updateMonitor(info);
            status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.INFO, "", null);
            status.add(new Status(IStatus.INFO, ManagedBuilderCorePlugin.getUniqueIdentifier(), NO_SOURCE_FOLDERS, info,
                    null));
            return status;
        }
        // Create the top-level directory for the build output
        topBuildDir = project.getFile(config.getName());
        createDirectory(project, config.getName());
        checkCancel();
        // Get the list of subdirectories
        IPath srcsFilePath = topBuildDir.getFullPath().append(SRCSFILE_NAME);
        IFile srcsFileHandle = createFile(srcsFilePath);
        buildSrcVars.clear();
        buildOutVars.clear();
        buildDepVars.clear();
        topBuildOutVars.clear();
        mySrcMakeGenerator.populateSourcesMakefile(srcsFileHandle, toolInfos, subdirList);
        checkCancel();
        // Now populate the module makefiles
        for (IResource res : getSubdirList()) {
            IContainer subDir = (IContainer) res;
            try {
                mySubDirMakeGenerator.populateFragmentMakefile(subDir);
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
        IPath makefilePath = topBuildDir.getFullPath().append(MAKEFILE_NAME);
        IFile makefileHandle = createFile(makefilePath);
        myTopMakeFileGenerator.populateTopMakefile(makefileHandle, true);
        // JABA SLOEBER create the size.awk file
        ICConfigurationDescription confDesc = ManagedBuildManager.getDescriptionForConfiguration(config);
        IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
        IFile sizeAwkFile1 = root.getFile(topBuildDir.getFullPath().append("size.awk"));
        File sizeAwkFile = sizeAwkFile1.getLocation().toFile();
        String regex = Common.getBuildEnvironmentVariable(confDesc, "recipe.size.regex", EMPTY);
        String awkContent = "/" + regex + "/ {arduino_size += $2 }\n";
        regex = Common.getBuildEnvironmentVariable(confDesc, "recipe.size.regex.data", EMPTY);
        awkContent += "/" + regex + "/ {arduino_data += $2 }\n";
        regex = Common.getBuildEnvironmentVariable(confDesc, "recipe.size.regex.eeprom", EMPTY);
        awkContent += "/" + regex + "/ {arduino_eeprom += $2 }\n";
        awkContent += "END { print \"\\n";
        String max = Common.getBuildEnvironmentVariable(confDesc, "upload.maximum_size", "10000");
        awkContent += Messages.sizeReportSketch.replace("maximum_size", max);
        awkContent += "\\n";
        max = Common.getBuildEnvironmentVariable(confDesc, "upload.maximum_data_size", "10000");
        awkContent += Messages.sizeReportData.replace("maximum_data_size", max);
        awkContent += "\\n";
        awkContent += "\"}";

        try {
            FileUtils.write(sizeAwkFile, awkContent, Charset.defaultCharset());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // END JABA SLOEBER create the size.awk file
        checkCancel();
        // Now finish up by adding all the object files
        IPath objFilePath = topBuildDir.getLocation().append(OBJECTS_MAKFILE);
        IFile objsFileHandle = createFile(objFilePath);
        populateObjectsMakefile(objsFileHandle);
        checkCancel();
        // How did we do
        if (!getInvalidDirList().isEmpty()) {
            status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.WARNING, "", null);
            // Add a new status for each of the bad folders
            // TODO: fix error message
            for (IResource dir : getInvalidDirList()) {
                status.add(new Status(IStatus.WARNING, ManagedBuilderCorePlugin.getUniqueIdentifier(), SPACES_IN_PATH,
                        dir.getFullPath().toString(), null));
            }
        } else {
            status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.OK, "", null);
        }
        return status;
    }

    /**
     * The makefile generator generates a Macro for each type of output, other than
     * final artifact, created by the build.
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
        outputMacros.put("LIBS", valueList);
        // Add the extra user-specified objects
        valueList = new ArrayList<String>();
        String[] userObjs = config.getUserObjects(buildTargetExt);
        for (String obj : userObjs) {
            valueList.add(obj);
        }
        outputMacros.put("USER_OBJS", valueList);
        // Write every macro to the file
        for (Entry<String, List<String>> entry : outputMacros.entrySet()) {
            macroBuffer.append(entry.getKey()).append(" :=");
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

    /*************************************************************************
     * M A K E F I L E G E N E R A T I O N C O M M O N M E T H O D S
     ************************************************************************/

    /**
     * Answers all of the output extensions that the target of the build has tools
     * defined to work on.
     *
     * @return a <code>Set</code> containing all of the output extensions
     */
    public Set<String> getOutputExtensions(ToolInfoHolder h) {
        if (h.outputExtensionsSet == null) {
            // The set of output extensions which will be produced by this tool.
            // It is presumed that this set is not very large (likely < 10) so
            // a HashSet should provide good performance.
            h.outputExtensionsSet = new HashSet<>();
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
     * Adds file(s) to an entry in a map of macro names to entries. File additions
     * look like: example.c, \
     */
    public void addMacroAdditionFiles(HashMap<String, String> map, String macroName, List<String> list) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(map.get(macroName));
        for (int i = 0; i < list.size(); i++) {
            String filename = list.get(i);
            if (filename.length() > 0) {
                filename = ensurePathIsGNUMakeTargetRuleCompatibleSyntax(filename);
                buffer.append(new Path(filename).toOSString()).append(WHITESPACE).append(LINEBREAK);
            }
        }
        // re-insert string in the map
        map.put(macroName, buffer.toString());
    }

    /**
     * Calculates the inputs and outputs for tools that will be generated in the top
     * makefile. This information is used by the top level makefile generation
     * methods.
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
                            ext = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(ext, "", " ",
                                    IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
                        } catch (BuildMacroException e) {
                            /* JABA is not going to write this code */
                        }
                        String name = config.getArtifactName();
                        // try to resolve the build macros in the artifact name
                        try {
                            String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(
                                    name, "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, config);
                            if ((resolved = resolved.trim()).length() > 0)
                                name = resolved;
                        } catch (BuildMacroException e) {
                            /* JABA is not going to write this code */
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
                    // for projects with specific setting on folders/files do
                    // not clear the macro value on subsequent passes
                    if (!map.containsKey(macroName)) {
                        addMacroAdditionPrefix(map, macroName, "", false);
                    }
                }
                // Set of input extensions for which macros have been created so
                // far
                HashSet<String> handledDepsInputExtensions = new HashSet<>();
                HashSet<String> handledOutsInputExtensions = new HashSet<>();
                while (!done) {
                    int[] testState = new int[doneState.length];
                    for (int i = 0; i < testState.length; i++)
                        testState[i] = 0;
                    // Calculate inputs
                    for (int i = 0; i < gnuToolInfos.length; i++) {
                        if (gnuToolInfos[i].areInputsCalculated()) {
                            testState[i]++;
                        } else {
                            if (gnuToolInfos[i].calculateInputs(ArduinoGnuMakefileGenerator.this, config,
                                    projectResources, h, lastChance)) {
                                testState[i]++;
                            }
                        }
                    }
                    // Calculate dependencies
                    for (int i = 0; i < gnuToolInfos.length; i++) {
                        if (gnuToolInfos[i].areDependenciesCalculated()) {
                            testState[i]++;
                        } else {
                            if (gnuToolInfos[i].calculateDependencies(ArduinoGnuMakefileGenerator.this, config,
                                    handledDepsInputExtensions, h, lastChance)) {
                                testState[i]++;
                            }
                        }
                    }
                    // Calculate outputs
                    for (int i = 0; i < gnuToolInfos.length; i++) {
                        if (gnuToolInfos[i].areOutputsCalculated()) {
                            testState[i]++;
                        } else {
                            if (gnuToolInfos[i].calculateOutputs(ArduinoGnuMakefileGenerator.this, config,
                                    handledOutsInputExtensions, lastChance)) {
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
     *            project relative directory path used with locationType ==
     *            DIRECTORY_RELATIVE
     * @param getAll
     *            only return the list if all tools that are going to contrubute to
     *            this variable have done so.
     * @return List
     */
    public List<String> getBuildVariableList(ToolInfoHolder h, String variable, int locationType, IPath directory,
            boolean getAll) {
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
     * Returns the list of known build rules. This keeps me from generating
     * duplicate rules for known file extensions.
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
     * Returns the list of known dependency file generation lines. This keeps me
     * from generating duplicate lines.
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
     * Adds the container of the argument to the list of folders in the project that
     * contribute source files to the build. The resource visitor has already
     * established that the build model knows how to build the files. It has also
     * checked that the resource is not generated as part of the build.
     */
    protected void appendBuildSubdirectory(IResource resource) {
        IContainer container = resource.getParent();
        // Only add the container once
        if (!getSubdirList().contains(container))
            getSubdirList().add(container);
    }

    /**
     * Adds the container of the argument to a list of subdirectories that are to be
     * deleted. As a result, the directories that are generated for the output
     * should be removed as well.
     */
    protected void appendDeletedSubdirectory(IContainer container) {
        // No point in adding a folder if the parent is already there
        IContainer parent = container.getParent();
        if (!getDeletedDirList().contains(container) && !getDeletedDirList().contains(parent)) {
            getDeletedDirList().add(container);
        }
    }

    /**
     * If a file is removed from a source folder (either because of a delete or move
     * action on the part of the user), the makefilegenerator has to remove the
     * dependency makefile along with the old build goal
     */
    protected void appendDeletedFile(IResource resource) {
        // Cache this for now
        getDeletedFileList().add(resource);
    }

    /**
     * Adds the container of the argument to a list of subdirectories that are part
     * of an incremental rebuild of the project. The makefile fragments for these
     * directories will be regenerated as a result of the build.
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
     * Check whether the build has been cancelled. Cancellation requests propagated
     * to the caller by throwing <code>OperationCanceledException</code>.
     *
     * @see org.eclipse.core.runtime.OperationCanceledException#OperationCanceledException()
     */
    protected void checkCancel() {
        if (monitor != null && monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    private void deleteBuildTarget(IResource deletedFile) {
        // Get the project relative path of the file
        String fileName = getFileName(deletedFile);
        String srcExtension = deletedFile.getFileExtension();
        IPath folderPath = inFullPathFromOutFullPath(deletedFile.getFullPath().removeLastSegments(1));
        if (folderPath != null) {
            folderPath = folderPath.removeFirstSegments(1);
        } else {
            folderPath = new Path("");
        }
        IResourceInfo rcInfo = config.getResourceInfo(folderPath, false);
        if (rcInfo instanceof IFileInfo) {
            rcInfo = config.getResourceInfo(folderPath.removeLastSegments(1), false);
        }
        String targetExtension = ((IFolderInfo) rcInfo).getOutputExtension(srcExtension);
        if (!targetExtension.isEmpty())
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
        IPath topBuildPath = topBuildDir.getFullPath();
        if (topBuildPath.isPrefixOf(path)) {
            inPath = path.removeFirstSegments(topBuildPath.segmentCount());
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
            folderPath = new Path("");
            ToolInfoHolder h = ToolInfoHolder.getToolInfo(this, folderPath);
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
                    depFilePaths[0] = ManagedBuildManager.calculateRelativePath(getTopBuildDir().getFullPath(),
                            absolutePath);
                    depFilePaths[0] = depFilePaths[0].removeFileExtension().addFileExtension(DEP_EXT);
                } else if (calcType == IManagedDependencyGeneratorType.TYPE_BUILD_COMMANDS
                        || calcType == IManagedDependencyGeneratorType.TYPE_PREBUILD_COMMANDS) {
                    IManagedDependencyGenerator2 depGen = (IManagedDependencyGenerator2) depType;
                    IManagedDependencyInfo depInfo = depGen.getDependencySourceInfo(
                            deletedFile.getProjectRelativePath(), deletedFile, config, tool, getBuildWorkingDir());
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

    public List<IPath> getDependencyMakefiles(ToolInfoHolder h) {
        if (h.dependencyMakefiles == null) {
            h.dependencyMakefiles = new ArrayList<IPath>();
        }
        return h.dependencyMakefiles;
    }

    /**
     * Strips off the file extension from the argument and returns the name
     * component in a <code>String</code>
     */
    private String getFileName(IResource file) {
        String answer = "";
        String lastSegment = file.getName();
        int extensionSeparator = lastSegment.lastIndexOf(DOT);
        if (extensionSeparator != -1) {
            answer = lastSegment.substring(0, extensionSeparator);
        }
        return answer;
    }

    /**
     * Answers a Vector containing a list of directories that are invalid for the
     * build for some reason. At the moment, the only reason a directory would not
     * be considered for the build is if it contains a space in the relative path
     * from the project root.
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
     * @return Collection of subdirectories (IContainers) contributing source code
     *         to the build
     */
    public Collection<IContainer> getSubdirList() {
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
                // JABA added some logging
                Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID, "Folder deletion failed " + folder.toString(), //
                        e));
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
    public IFile getTopBuildDir() {
        return topBuildDir;
    }

    @Override
    public void initialize(int buildKind, IConfiguration cfg, IBuilder builder, IProgressMonitor monitor) {
        // Save the project so we can get path and member information
        this.project = cfg.getOwner().getProject();
        mySubDirMakeGenerator = new SubDirMakeGenerator(this);
        myTopMakeFileGenerator = new TopMakeFileGenerator(this);
        mySrcMakeGenerator = new SrcMakeGenerator(this);
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
        // info = info;
        // Get the name of the build target
        buildTargetName = cfg.getArtifactName();
        // Get its extension
        buildTargetExt = cfg.getArtifactExtension();
        try {
            // try to resolve the build macros in the target extension
            buildTargetExt = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(buildTargetExt,
                    "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, builder);
        } catch (BuildMacroException e) {
            /* JABA is not going to write this code */
        }
        try {
            // try to resolve the build macros in the target name
            String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(buildTargetName,
                    "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, builder);
            if (resolved != null) {
                resolved = resolved.trim();
                if (resolved.length() > 0)
                    buildTargetName = resolved;
            }
        } catch (BuildMacroException e) {
            /* JABA is not going to write this code */
        }
        if (buildTargetExt == null) {
            buildTargetExt = "";
        }
        // Cache the build tools
        config = cfg;
        this.builder = builder;
        initToolInfos();
        // set the top build dir path
        topBuildDir = project.getFile(cfg.getName());
        srcEntries = config.getSourceEntries();
        if (srcEntries.length == 0) {
            srcEntries = new ICSourceEntry[] {
                    new CSourceEntry(Path.EMPTY, null, ICSettingEntry.RESOLVED | ICSettingEntry.VALUE_WORKSPACE_PATH) };
        } else {
            ICConfigurationDescription cfgDes = ManagedBuildManager.getDescriptionForConfiguration(config);
            srcEntries = CDataUtil.resolveEntries(srcEntries, cfgDes);
        }
    }

    private void initToolInfos() {
        toolInfos = PathSettingsContainer.createRootContainer();
        IResourceInfo rcInfos[] = config.getResourceInfos();
        for (IResourceInfo rcInfo : rcInfos) {
            if (rcInfo.isExcluded())
                continue;
            ToolInfoHolder h = ToolInfoHolder.getToolInfo(this, rcInfo.getPath(), true);
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

    public List<String> getDepLineList() {
        return mySubDirMakeGenerator.getDepLineList();
    }

    public PathSettingsContainer getToolInfos() {
        return toolInfos;
    }

    public IProject getProject() {
        return project;
    }
}
