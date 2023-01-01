/*******************************************************************************
 * Copyright (c) 2005, 2020 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Intel Corporation - Initial API and implementation
 *     IBM Corporation
 *     Marc-Andre Laperle
 *     EclipseSource
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.internal.core.SafeStringInterner;
//import org.eclipse.cdt.managedbuilder.core.BuildException;
//import org.eclipse.cdt.managedbuilder.core.IAdditionalInput;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IBuilder;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IFileInfo;
//import org.eclipse.cdt.managedbuilder.core.IInputType;
//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.core.IOutputType;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
//import org.eclipse.cdt.managedbuilder.internal.macros.BuildMacroProvider;
//import org.eclipse.cdt.managedbuilder.internal.macros.IMacroContextInfo;
//import org.eclipse.cdt.managedbuilder.internal.macros.OptionContextData;
//import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
//import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.utils.EFSExtensionManager;
import org.eclipse.cdt.utils.cdtvariables.CdtVariableResolver;
import org.eclipse.cdt.utils.cdtvariables.SupplierBasedCdtVariableSubstitutor;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.BuildMacroException;
import io.sloeber.autoBuild.api.IAdditionalInput;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IBuilder;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IFileInfo;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IOutputType;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.autoBuild.core.Activator;

public class AdditionalInput implements IAdditionalInput {

    private static final String BUILD_VARIABLE_STATIC_LIB = "ARCHIVES"; //$NON-NLS-1$
    private static final String BUILD_VARIABLE_SHARED_LIB = "LIBRARIES"; //$NON-NLS-1$

    private String[] expandedNames;

    //  Superclass
    //  Parent and children
    private IInputType fParent;
    //  Managed Build model attributes
    private String fPaths;
    private Integer fKind;
    //  Miscellaneous
    private boolean fIsExtensionAdditionalInput = false;
    private boolean fIsDirty = false;
    private boolean fResolved = true;
    private boolean fRebuildState;

    /*
     *  C O N S T R U C T O R S
     */

    /**
     * This constructor is called to create an AdditionalInput defined by an
     * extension point in
     * a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parent
     *            The IInputType parent of this AdditionalInput
     * @param element
     *            The AdditionalInput definition from the manifest file or a dynamic
     *            element
     *            provider
     */
    public AdditionalInput(IInputType parent, IManagedConfigElement element) {
        this.fParent = parent;
        fIsExtensionAdditionalInput = true;

        // setup for resolving
        fResolved = false;

        loadFromManifest(element);
    }

    /**
     * This constructor is called to create an AdditionalInput whose attributes and
     * children will be
     * added by separate calls.
     *
     * @param parent
     *            The parent of the an AdditionalInput
     * @param isExtensionElement
     *            Indicates whether this is an extension element or a managed
     *            project element
     */
    public AdditionalInput(InputType parent, boolean isExtensionElement) {
        this.fParent = parent;
        fIsExtensionAdditionalInput = isExtensionElement;
        if (!isExtensionElement) {
            setDirty(true);
            setRebuildState(true);
        }
    }

    /**
     * Create an <code>AdditionalInput</code> based on the specification stored in
     * the
     * project file (.cdtbuild).
     *
     * @param parent
     *            The <code>ITool</code> the AdditionalInput will be added to.
     * @param element
     *            The XML element that contains the AdditionalInput settings.
     */
    public AdditionalInput(IInputType parent, ICStorageElement element) {
        this.fParent = parent;
        fIsExtensionAdditionalInput = false;

        // Initialize from the XML attributes
        loadFromProject(element);
    }

    /**
     * Create an <code>AdditionalInput</code> based upon an existing
     * AdditionalInput.
     *
     * @param parent
     *            The <code>IInputType</code> the AdditionalInput will be added to.
     * @param additionalInput
     *            The existing AdditionalInput to clone.
     */
    public AdditionalInput(IInputType parent, AdditionalInput additionalInput) {
        this(parent, additionalInput, false);
    }

    public AdditionalInput(IInputType parent, AdditionalInput additionalInput, boolean retainRebuildState) {
        this.fParent = parent;
        fIsExtensionAdditionalInput = false;

        //  Copy the remaining attributes
        if (additionalInput.fPaths != null) {
            fPaths = additionalInput.fPaths;
        }

        if (additionalInput.fKind != null) {
            fKind = additionalInput.fKind;
        }

        if (retainRebuildState) {
            setDirty(additionalInput.fIsDirty);
            setRebuildState(additionalInput.fRebuildState);
        } else {
            setDirty(true);
            setRebuildState(true);
        }
    }

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */

    /* (non-Javadoc)
     * Loads the AdditionalInput information from the ManagedConfigElement specified in the
     * argument.
     *
     * @param element Contains the AdditionalInput information
     */
    protected void loadFromManifest(IManagedConfigElement element) {

        // path
        fPaths = SafeStringInterner.safeIntern(element.getAttribute(IAdditionalInput.PATHS));

        // kind
        String kindStr = element.getAttribute(IAdditionalInput.KIND);
        if (kindStr == null || kindStr.equals(ADDITIONAL_INPUT_DEPENDENCY)) {
            fKind = Integer.valueOf(KIND_ADDITIONAL_INPUT_DEPENDENCY);
        } else if (kindStr.equals(ADDITIONAL_INPUT)) {
            fKind = Integer.valueOf(KIND_ADDITIONAL_INPUT);
        } else if (kindStr.equals(ADDITIONAL_DEPENDENCY)) {
            fKind = Integer.valueOf(KIND_ADDITIONAL_DEPENDENCY);
        }
    }

    /* (non-Javadoc)
     * Initialize the AdditionalInput information from the XML element
     * specified in the argument
     *
     * @param element An XML element containing the AdditionalInput information
     */
    protected void loadFromProject(ICStorageElement element) {

        // path
        if (element.getAttribute(IAdditionalInput.PATHS) != null) {
            fPaths = SafeStringInterner.safeIntern(element.getAttribute(IAdditionalInput.PATHS));
        }

        // kind
        if (element.getAttribute(IAdditionalInput.KIND) != null) {
            String kindStr = element.getAttribute(IAdditionalInput.KIND);
            if (kindStr == null || kindStr.equals(ADDITIONAL_INPUT_DEPENDENCY)) {
                fKind = Integer.valueOf(KIND_ADDITIONAL_INPUT_DEPENDENCY);
            } else if (kindStr.equals(ADDITIONAL_INPUT)) {
                fKind = Integer.valueOf(KIND_ADDITIONAL_INPUT);
            } else if (kindStr.equals(ADDITIONAL_DEPENDENCY)) {
                fKind = Integer.valueOf(KIND_ADDITIONAL_DEPENDENCY);
            }
        }
    }

    /**
     * Persist the AdditionalInput to the project file.
     */
    public void serialize(ICStorageElement element) {

        if (fPaths != null) {
            element.setAttribute(IAdditionalInput.PATHS, fPaths);
        }

        if (fKind != null) {
            String str;
            switch (getKind()) {
            case KIND_ADDITIONAL_INPUT:
                str = ADDITIONAL_INPUT;
                break;
            case KIND_ADDITIONAL_DEPENDENCY:
                str = ADDITIONAL_DEPENDENCY;
                break;
            case KIND_ADDITIONAL_INPUT_DEPENDENCY:
                str = ADDITIONAL_INPUT_DEPENDENCY;
                break;
            default:
                str = ""; //$NON-NLS-1$
                break;
            }
            element.setAttribute(IAdditionalInput.KIND, str);
        }

        // I am clean now
        fIsDirty = false;
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IAdditionalInput#getParent()
     */
    @Override
    public IInputType getParent() {
        return fParent;
    }

    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IAdditionalInput#getPaths()
     */
    @Override
    public String[] getPaths() {
        if (fPaths == null) {
            return null;
        }
        String[] nameTokens = CDataUtil.stringToArray(fPaths, ";"); //$NON-NLS-1$
        return nameTokens;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IAdditionalInput#setPaths()
     */
    @Override
    public void setPaths(String newPaths) {
        if (fPaths == null && newPaths == null)
            return;
        if (fPaths == null || newPaths == null || !(fPaths.equals(newPaths))) {
            fPaths = newPaths;
            fIsDirty = true;
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IAdditionalInput#getKind()
     */
    @Override
    public int getKind() {
        if (fKind == null) {
            return KIND_ADDITIONAL_INPUT_DEPENDENCY;
        }
        return fKind.intValue();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IAdditionalInput#setKind()
     */
    @Override
    public void setKind(int newKind) {
        if (fKind == null || !(fKind.intValue() == newKind)) {
            fKind = Integer.valueOf(newKind);
            fIsDirty = true;
            setRebuildState(true);
        }
    }

    /*
     *  O B J E C T   S T A T E   M A I N T E N A N C E
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IAdditionalInput#isExtensionElement()
     */
    public boolean isExtensionElement() {
        return fIsExtensionAdditionalInput;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IAdditionalInput#isDirty()
     */
    @Override
    public boolean isDirty() {
        // This shouldn't be called for an extension AdditionalInput
        if (fIsExtensionAdditionalInput)
            return false;
        return fIsDirty;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IAdditionalInput#setDirty(boolean)
     */
    @Override
    public void setDirty(boolean isDirty) {
        this.fIsDirty = isDirty;
    }

    /* (non-Javadoc)
     *  Resolve the element IDs to interface references
     */
    public void resolveReferences() {
        if (!fResolved) {
            fResolved = true;
        }
    }

    public boolean needsRebuild() {
        // This shouldn't be called for an extension AdditionalInput
        if (fIsExtensionAdditionalInput)
            return false;
        if (fRebuildState)
            return fRebuildState;
        if (fKind.intValue() == IAdditionalInput.KIND_ADDITIONAL_DEPENDENCY
                || fKind.intValue() == IAdditionalInput.KIND_ADDITIONAL_INPUT_DEPENDENCY || isLibrariesInput()) {
            IToolChain toolChain = getToolChain();
            /* toolChain can be null e.g. in tools for custom build steps */
            if (toolChain != null && !toolChain.isExtensionElement()) {
                long artifactTimeStamp = getArtifactTimeStamp(toolChain);
                if (0 != artifactTimeStamp) {
                    String[] paths = getPaths();
                    for (int i = 0; i < paths.length; ++i) {
                        if (paths[i].length() == 0)
                            continue;
                        if (dependencyChanged(paths[i], artifactTimeStamp))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private long getArtifactTimeStamp(IToolChain toolChain) {
        IBuilder builder = toolChain.getBuilder();
        IConfiguration configuration = toolChain.getParent();
        IFolder buildFolder = ManagedBuildManager.getBuildFolder(configuration, builder);
        if (buildFolder != null) {

            String artifactName = configuration.getArtifactName();
            String artifactExt = configuration.getArtifactExtension();
            String artifactPref = configuration.getOutputPrefix(artifactExt);
            if (artifactName.length() > 0) {
                if (artifactExt.length() > 0)
                    artifactName += "." + artifactExt; //$NON-NLS-1$
                if (artifactPref.length() > 0)
                    artifactName = artifactPref + artifactName;
                try {
                    artifactName = ManagedBuildManager.getBuildMacroProvider().resolveValue(artifactName, "", //$NON-NLS-1$
                            " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, configuration); //$NON-NLS-1$
                } catch (BuildMacroException e) {
                }

                URI buildArtifactURI = EFSExtensionManager.getDefault().append(buildFolder.getLocationURI(), artifactName);

                try {
                    IFileStore artifact = EFS.getStore(buildArtifactURI);
                    org.eclipse.core.filesystem.IFileInfo info = (artifact == null) ? null : artifact.fetchInfo();
                    if ((info != null) && info.exists()) {
                        return info.getLastModified();
                    }
                } catch (CoreException e) {
                    // if we can't even inquire about it, then assume it doesn't exist
                }
            }
        }
        return 0;
    }

    private boolean isLibrariesInput() {
        // libraries are of the "additionalinput" kind, not "additionalinputdependency" because otherwise the
        // external make builder would generate makefiles with $(LIBS) in the dependency list, resulting in
        // failure to build dependency -lxyz etc.
        return (fKind.intValue() == IAdditionalInput.KIND_ADDITIONAL_INPUT
                && Arrays.asList(getPaths()).contains("$(LIBS)")); //$NON-NLS-1$
    }

    private boolean dependencyChanged(String sPath, long artefactTimeStamp) {
        try {
            IToolChain toolChain = getToolChain();
            IConfiguration configuration = toolChain.getParent();
            if (fIsDirty || (null == expandedNames)) {
                if ("$(LIBS)".equals(sPath)) //$NON-NLS-1$
                    expandedNames = getDepLibs();
                else if ("$(USER_OBJS)".equals(sPath)) //$NON-NLS-1$
                    expandedNames = getDepObjs(configuration);
                else {
                    expandedNames = getDepFiles(sPath);
                }
            }
            for (int j = 0; j < expandedNames.length; ++j) {
                if (expandedNames[j] != null) {
                    IFileStore file = getEFSFile(expandedNames[j]);
                    org.eclipse.core.filesystem.IFileInfo info = (file == null) ? null : file.fetchInfo();
                    if ((info != null) && info.exists() && (info.getLastModified() > artefactTimeStamp)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // we'll have to assume that the dependency didn't change if we couldn't get its timestamp
            Activator.log(e);
        }
        return false;
    }

    private IToolChain getToolChain() {
        IBuildObject bo = fParent.getParent().getParent();
        IToolChain tCh = null;
        if (bo instanceof IToolChain) {
            tCh = ((IToolChain) bo);
        } else if (bo instanceof IFileInfo) {
            tCh = ((ResourceConfiguration) bo).getBaseToolChain();
        }
        return tCh;
    }

    private String[] getDepLibs() throws CoreException, BuildException, CdtVariableException {
        IOption[] options = fParent.getParent().getOptions();
        String[] libNames = null;
        List<String> libPaths = null;
        for (int i = 0; i < options.length; ++i) {
            int type = options[i].getValueType();
            if (type == IOption.LIBRARIES) {
                libNames = options[i].getLibraries();
            } else if (type == IOption.LIBRARY_PATHS) {
                if (null == libPaths)
                    libPaths = new ArrayList<>();
                libPaths.addAll(Arrays.asList(restoreLibraryPaths(options[i])));
            }
        }

        if ((libNames != null) && (libPaths != null)) {
            IToolChain toolChain = getToolChain();
            for (int i = 0; i < libNames.length; ++i) {
                URI uri = findLibrary(toolChain, libNames[i], libPaths);
                libNames[i] = (uri == null) ? null : uri.toString();
            }
            return libNames;
        }
        return new String[0];
    }

    private String[] restoreLibraryPaths(IOption option) throws BuildException, CdtVariableException {
        @SuppressWarnings("unchecked")
        List<String> libPaths = (List<String>) option.getValue();
        String[] dirs = libPaths.toArray(new String[libPaths.size()]);
        dirs = substituteEnvVars(option, dirs);
        return dirs;
    }

    final static Pattern extPattern = Pattern.compile("(?!\\.)(\\d+(\\.\\d+(\\.\\d+)?)?)(?![\\d\\.])$"); //$NON-NLS-1$

    private URI findLibrary(IToolChain toolChain, final String libName, List<String> dirs) throws CoreException {
        final String libSO = getDynamicLibPrefix(toolChain) + libName + '.' + getDynamicLibExtension(toolChain);
        final String libA = getStaticLibPrefix(toolChain) + libName + '.' + getStaticLibExtension(toolChain);

        class LibFilter {
            public boolean accept(String name) {
                if (equals(libA, name))
                    return true;
                if (!startsWith(name, libSO))
                    return false;
                if (libSO.length() == name.length())
                    return true; // we don't necessarily have a version extension
                if (name.length() <= libSO.length() + 1 || name.charAt(libSO.length()) != '.')
                    return false;
                String ext = name.substring(libSO.length() + 1);

                // Check the version extension to be in form of "<Major>.<Minor>.<Build>",
                // for example: "1.10.0", "1.10", "1" are the valid version extensions
                //
                return extPattern.matcher(ext).matches();
            }

            boolean equals(String a, String b) {
                return a.equals(b);
            }

            boolean startsWith(String string, String prefix) {
                return string.startsWith(prefix);
            }

        }
        class CaseInsensitiveLibFilter extends LibFilter {
            @Override
            boolean equals(String a, String b) {
                return a.equalsIgnoreCase(b);
            }

            @Override
            boolean startsWith(String string, String prefix) {
                return string.toLowerCase().startsWith(prefix.toLowerCase());
            }
        }

        for (Iterator<String> i = dirs.iterator(); i.hasNext();) {
            IFileStore dir = getEFSFile(i.next());
            LibFilter filter = dir.getFileSystem().isCaseSensitive() ? new LibFilter() : new CaseInsensitiveLibFilter();
            for (IFileStore child : dir.childStores(EFS.NONE, null)) {
                if (filter.accept(child.getName())) {
                    return child.toURI();
                }
            }
        }
        return null;
    }

    /**
     * Gets an EFS file-store for the specified path or URI, which may be a local
     * filesystem path or may be a more abstract URI.
     *
     * @param pathOrURI
     *            a local filesystem path or URI
     *
     * @return the file store, if one could be determined
     */
    private static IFileStore getEFSFile(String pathOrURI) {
        IFileStore result;

        try {
            // try it as a URI
            result = EFS.getStore(URI.create(pathOrURI));
        } catch (Exception e) {
            // most likely, the path is not a URI, so assume a local file and try again
            result = EFS.getLocalFileSystem().getStore(new Path(pathOrURI));
        }

        return result;
    }

    private String[] substituteEnvVars(IOption option, String[] paths) throws CdtVariableException {
        BuildMacroProvider provider = (BuildMacroProvider) ManagedBuildManager.getBuildMacroProvider();
        IMacroContextInfo info = provider.getMacroContextInfo(IBuildMacroProvider.CONTEXT_OPTION,
                new OptionContextData(option, fParent));
        String delimiter = ManagedBuildManager.getEnvironmentVariableProvider().getDefaultDelimiter();
        String inexVal = " "; //$NON-NLS-1$
        SupplierBasedCdtVariableSubstitutor subst = provider.getMacroSubstitutor(info, inexVal, delimiter);

        String[] newPaths = CdtVariableResolver.resolveStringListValues(paths, subst, false);
        for (int i = 0; i < newPaths.length; ++i) {
            String newPath = newPaths[i];
            int len = newPath.length();
            if ((len > 1) && (newPath.charAt(0) == '\"') && (newPath.charAt(len - 1) == '\"'))
                newPaths[i] = newPaths[i].substring(1, len - 1);
        }
        return newPaths;
    }

    private static String getStaticLibPrefix(IToolChain toolChain) {
        IOutputType type = findOutputType(toolChain, BUILD_VARIABLE_STATIC_LIB);
        if (null == type)
            return "lib"; //$NON-NLS-1$
        return type.getOutputPrefix();
    }

    private static String getStaticLibExtension(IToolChain toolChain) {
        IOutputType type = findOutputType(toolChain, BUILD_VARIABLE_STATIC_LIB);
        if (null == type || type.getOutputExtensionsAttribute().length == 0) {
            return "a"; //$NON-NLS-1$
        }
        return type.getOutputExtensionsAttribute()[0];
    }

    private static String getDynamicLibPrefix(IToolChain toolChain) {
        IOutputType type = findOutputType(toolChain, BUILD_VARIABLE_SHARED_LIB);
        if (null == type)
            return "lib"; //$NON-NLS-1$
        return type.getOutputPrefix();
    }

    private static String getDynamicLibExtension(IToolChain toolChain) {
        IOutputType type = findOutputType(toolChain, BUILD_VARIABLE_SHARED_LIB);
        if (null == type || type.getOutputExtensionsAttribute().length == 0) {
            return "so"; //$NON-NLS-1$
        }
        return type.getOutputExtensionsAttribute()[0];
    }

    public static IOutputType findOutputType(IToolChain toolChain, String buildVariable) {
        // if we're determining whether to re-build an executable, then it won't have an output
        // type for shared libraries from which we can get a filename extension or prefix.
        // We have to scan the extension toolchain
        toolChain = ManagedBuildManager.getExtensionToolChain(toolChain);
        ITool[] tools = toolChain.getTools();
        for (int i = 0; i < tools.length; ++i) {
            IOutputType[] oTypes = tools[i].getOutputTypes();
            for (int j = 0; j < oTypes.length; ++j) {
                if (buildVariable.equals(oTypes[j].getBuildVariable()))
                    return oTypes[j];
            }
        }
        return null;
    }

    private String[] getDepObjs(IConfiguration configuration) throws BuildException, CdtVariableException {
        IOption[] options = fParent.getParent().getOptions();
        String[] userObjs = null;
        for (int i = 0; i < options.length; ++i) {
            int type = options[i].getValueType();
            if (type == IOption.OBJECTS) {
                userObjs = options[i].getUserObjects();
                return substituteEnvVars(options[i], userObjs);
            }
        }
        return new String[0];
    }

    private String[] getDepFiles(String sPath) {
        return new String[0];
    }

    public void setRebuildState(boolean rebuild) {
        if (isExtensionElement() && rebuild)
            return;

        fRebuildState = rebuild;
    }

}
