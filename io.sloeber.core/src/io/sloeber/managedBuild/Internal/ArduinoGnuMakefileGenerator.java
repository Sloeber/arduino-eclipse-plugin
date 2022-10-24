package io.sloeber.managedBuild.Internal;

import static io.sloeber.core.common.Const.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator2;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
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
import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;

/**
 * This is a specialized makefile generator that takes advantage of the
 * extensions present in Gnu Make.
 *
 * @since 1.2
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ArduinoGnuMakefileGenerator implements IManagedBuilderMakefileGenerator2 {
   
    /**
     * This class is used to recursively walk the project and determine which
     * modules contribute buildable source files.
     */
    protected class ResourceProxyVisitor implements IResourceProxyVisitor {
        private final ArduinoGnuMakefileGenerator generator;
        private Collection<IContainer> subdirList = new LinkedHashSet<IContainer>();

        Collection<IContainer> getSubdirList() {
            return subdirList;
        }

        public ResourceProxyVisitor(ArduinoGnuMakefileGenerator generator) {
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
    // Dependency file variables
    // private Vector dependencyMakefiles; // IPath's - relative to the top
    // build directory or absolute
    private ICSourceEntry srcEntries[];
    public ArduinoGnuMakefileGenerator() {
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
        //    initToolInfos();
        // set the top build dir path
        topBuildDir = project.getFile(info.getConfigurationName());
    }


    public boolean isSource(IPath path) {
        return !CDataUtil.isExcluded(path, srcEntries);
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
            return topBuildDir.getFullPath().removeFirstSegments(1);
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
        checkCancel();
        // Get the data for the makefile generation
        List<SubDirMakeGenerator> subDirMakeGenerators = new LinkedList<>();
        Set<MakeRule> subDirMakeRules = new HashSet<>();
        Collection<IContainer> foldersToBuild = new LinkedHashSet<>();

        for (IContainer res : foldersToInvestigate) {
            //For all the folders get the make rules for this folder
            SubDirMakeGenerator subDirMakeGenerator = new SubDirMakeGenerator(this, res);
            if (!subDirMakeGenerator.isEmpty()) {
                foldersToBuild.add(res);
                subDirMakeGenerators.add(subDirMakeGenerator);
                //also store all these rules in one set to provide them to the top make file
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
        //TOFIX also need to add macro's from main makefile

        SrcMakeGenerator.generateSourceMakefile(project, config, srcMacroNames, foldersToBuild);
        SrcMakeGenerator.generateObjectsMakefile(project, config, objMacroNames);
        topMakeFileGenerator.generateMakefile();

        checkCancel();
        // How did we do
            status = new MultiStatus(ManagedBuilderCorePlugin.getUniqueIdentifier(), IStatus.OK, "", null);

        //TOFIX this should be done differently
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
            builder = cfg.getEditableBuilder();
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
        //   initToolInfos();
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


    public IProject getProject() {
        return project;
    }
}
