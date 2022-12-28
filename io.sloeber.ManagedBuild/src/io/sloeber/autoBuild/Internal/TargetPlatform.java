/*******************************************************************************
 * Copyright (c) 2004, 2016 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 * IBM Corporation
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.internal.core.SafeStringInterner;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.IProjectType;
import io.sloeber.autoBuild.api.ITargetPlatform;
import io.sloeber.autoBuild.api.IToolChain;

public class TargetPlatform extends BuildObject implements ITargetPlatform {

    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    //  Superclass
    private ITargetPlatform superClass;
    private String superClassId;
    //  Parent and children
    private IToolChain parent;
    //  Managed Build model attributes
    private String unusedChildren;
    private String errorParserIds;
    private Boolean isAbstract;
    private List<String> osList;
    private List<String> archList;
    private List<String> binaryParserList;
    //  Miscellaneous
    private boolean isExtensionTargetPlatform = false;
    private boolean isDirty = false;
    private boolean resolved = true;
    private BuildTargetPlatformData fTargetPlatformData;

    /*
     *  C O N S T R U C T O R S
     */

    /**
     * This constructor is called to create a TargetPlatform defined by an
     * extension point in a plugin manifest file, or returned by a dynamic element
     * provider
     *
     * @param parent
     *            The IToolChain parent of this TargetPlatform, or <code>null</code>
     *            if
     *            defined at the top level
     * @param element
     *            The TargetPlatform definition from the manifest file or a dynamic
     *            element
     *            provider
     * @param managedBuildRevision
     *            the fileVersion of Managed Build System
     */
    public TargetPlatform(IToolChain parent, IManagedConfigElement element, String managedBuildRevision) {
        this.parent = parent;
        isExtensionTargetPlatform = true;

        // setup for resolving
        resolved = false;

        setManagedBuildRevision(managedBuildRevision);
        loadFromManifest(element);

        // Hook me up to the Managed Build Manager
        ManagedBuildManager.addExtensionTargetPlatform(this);
    }

    /**
     * This constructor is called to create a TargetPlatform whose attributes and
     * children will be
     * added by separate calls.
     *
     * @param parent
     *            The parent of the builder, if any
     * @param superClass
     *            The superClass, if any
     * @param Id
     *            The id for the new tool chain
     * @param name
     *            The name for the new tool chain
     * @param isExtensionElement
     *            Indicates whether this is an extension element or a managed
     *            project element
     */
    public TargetPlatform(ToolChain parent, ITargetPlatform superClass, String Id, String name,
            boolean isExtensionElement) {
        this.parent = parent;
        this.superClass = superClass;
        setManagedBuildRevision(parent.getManagedBuildRevision());
        if (this.superClass != null) {
            superClassId = this.superClass.getId();
        }
        setId(Id);
        setName(name);

        isExtensionTargetPlatform = isExtensionElement;
        if (isExtensionElement) {
            // Hook me up to the Managed Build Manager
            ManagedBuildManager.addExtensionTargetPlatform(this);
        } else {
            fTargetPlatformData = new BuildTargetPlatformData(this);
            setDirty(true);
        }
    }

    /**
     * Create a <code>TargetPlatform</code> based on the specification stored in the
     * project file (.cdtbuild).
     *
     * @param parent
     *            The <code>IToolChain</code> the TargetPlatform will be added to.
     * @param element
     *            The XML element that contains the TargetPlatform settings.
     * @param managedBuildRevision
     *            the fileVersion of Managed Build System
     */
    public TargetPlatform(IToolChain parent, ICStorageElement element, String managedBuildRevision) {
        this.parent = parent;
        isExtensionTargetPlatform = false;
        fTargetPlatformData = new BuildTargetPlatformData(this);

        setManagedBuildRevision(managedBuildRevision);
        // Initialize from the XML attributes
        loadFromProject(element);
    }

    /**
     * Create a <code>TargetPlatform</code> based upon an existing TargetPlatform.
     *
     * @param parent
     *            The <code>IToolChain</code> the TargetPlatform will be added to.
     * @param targetPlatform
     *            The existing TargetPlatform to clone.
     */
    public TargetPlatform(IToolChain parent, String Id, String name, TargetPlatform targetPlatform) {
        this.parent = parent;

        superClass = targetPlatform.isExtensionTargetPlatform ? targetPlatform : targetPlatform.superClass;
        if (superClass != null) {
            //			if (targetPlatform.superClassId != null) {
            superClassId = superClass.getId();// targetPlatform.superClassId;
            //			}
        }
        setId(Id);
        setName(name);
        isExtensionTargetPlatform = false;
        fTargetPlatformData = new BuildTargetPlatformData(this);

        setManagedBuildRevision(targetPlatform.getManagedBuildRevision());

        //  Copy the remaining attributes
        if (targetPlatform.unusedChildren != null) {
            unusedChildren = targetPlatform.unusedChildren;
        }
        if (targetPlatform.errorParserIds != null) {
            errorParserIds = targetPlatform.errorParserIds;
        }
        if (targetPlatform.isAbstract != null) {
            isAbstract = targetPlatform.isAbstract;
        }
        if (targetPlatform.osList != null) {
            osList = new ArrayList<>(targetPlatform.osList);
        }
        if (targetPlatform.archList != null) {
            archList = new ArrayList<>(targetPlatform.archList);
        }
        if (targetPlatform.binaryParserList != null) {
            binaryParserList = new ArrayList<>(targetPlatform.binaryParserList);
        }

        setDirty(true);
    }

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */

    /* (non-Javadoc)
     * Loads the target platform information from the ManagedConfigElement specified in the
     * argument.
     *
     * @param element Contains the tool-chain information
     */
    protected void loadFromManifest(IManagedConfigElement element) {
        ManagedBuildManager.putConfigElement(this, element);

        // id
        setId(SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.ID)));

        // Get the name
        setName(SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.NAME)));

        // superClass
        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));

        // Get the unused children, if any
        unusedChildren = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.UNUSED_CHILDREN));

        // isAbstract
        String isAbs = element.getAttribute(IProjectType.IS_ABSTRACT);
        if (isAbs != null) {
            isAbstract = Boolean.parseBoolean(isAbs);
        }

        // Get the comma-separated list of valid OS
        String os = element.getAttribute(OS_LIST);
        if (os != null) {
            osList = new ArrayList<>();
            String[] osTokens = os.split(","); //$NON-NLS-1$
            for (int i = 0; i < osTokens.length; ++i) {
                osList.add(osTokens[i].trim());
            }
        }

        // Get the comma-separated list of valid Architectures
        String arch = element.getAttribute(ARCH_LIST);
        if (arch != null) {
            archList = new ArrayList<>();
            String[] archTokens = arch.split(","); //$NON-NLS-1$
            for (int j = 0; j < archTokens.length; ++j) {
                archList.add(SafeStringInterner.safeIntern(archTokens[j].trim()));
            }
        }

        // Get the IDs of the binary parsers from a semi-colon-separated list.
        String bpars = element.getAttribute(BINARY_PARSER);
        if (bpars != null) {
            binaryParserList = new ArrayList<>();
            String[] bparsTokens = CDataUtil.stringToArray(bpars, ";"); //$NON-NLS-1$
            for (int j = 0; j < bparsTokens.length; ++j) {
                binaryParserList.add(SafeStringInterner.safeIntern(bparsTokens[j].trim()));
            }
        }
    }

    /* (non-Javadoc)
     * Initialize the target platform information from the XML element
     * specified in the argument
     *
     * @param element An XML element containing the target platform information
     */
    protected void loadFromProject(ICStorageElement element) {

        // id (unique, do not intern)
        setId(element.getAttribute(IBuildObject.ID));

        // name
        if (element.getAttribute(IBuildObject.NAME) != null) {
            setName(SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.NAME)));
        }

        // superClass
        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));
        if (superClassId != null && superClassId.length() > 0) {
            superClass = ManagedBuildManager.getExtensionTargetPlatform(superClassId);
            if (superClass == null) {
                // TODO:  Report error
            }
        }

        // Get the unused children, if any
        if (element.getAttribute(IProjectType.UNUSED_CHILDREN) != null) {
            unusedChildren = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.UNUSED_CHILDREN));
        }

        // isAbstract
        if (element.getAttribute(IProjectType.IS_ABSTRACT) != null) {
            String isAbs = element.getAttribute(IProjectType.IS_ABSTRACT);
            if (isAbs != null) {
                isAbstract = Boolean.parseBoolean(isAbs);
            }
        }

        // Get the comma-separated list of valid OS
        if (element.getAttribute(OS_LIST) != null) {
            String os = element.getAttribute(OS_LIST);
            if (os != null) {
                osList = new ArrayList<>();
                String[] osTokens = os.split(","); //$NON-NLS-1$
                for (int i = 0; i < osTokens.length; ++i) {
                    osList.add(SafeStringInterner.safeIntern(osTokens[i].trim()));
                }
            }
        }

        // Get the comma-separated list of valid Architectures
        if (element.getAttribute(ARCH_LIST) != null) {
            String arch = element.getAttribute(ARCH_LIST);
            if (arch != null) {
                archList = new ArrayList<>();
                String[] archTokens = arch.split(","); //$NON-NLS-1$
                for (int j = 0; j < archTokens.length; ++j) {
                    archList.add(SafeStringInterner.safeIntern(archTokens[j].trim()));
                }
            }
        }

        // Get the semi-colon-separated list of binaryParserIds
        if (element.getAttribute(BINARY_PARSER) != null) {
            String bpars = element.getAttribute(BINARY_PARSER);
            if (bpars != null) {
                binaryParserList = new ArrayList<>();
                String[] bparsTokens = CDataUtil.stringToArray(bpars, ";"); //$NON-NLS-1$
                for (int j = 0; j < bparsTokens.length; ++j) {
                    binaryParserList.add(SafeStringInterner.safeIntern(bparsTokens[j].trim()));
                }
            }
        }

    }

    /**
     * Persist the target platform to the project file.
     */
    public void serialize(ICStorageElement element) {
        if (superClass != null)
            element.setAttribute(IProjectType.SUPERCLASS, superClass.getId());

        element.setAttribute(IBuildObject.ID, id);

        if (name != null) {
            element.setAttribute(IBuildObject.NAME, name);
        }

        if (unusedChildren != null) {
            element.setAttribute(IProjectType.UNUSED_CHILDREN, unusedChildren);
        }

        if (isAbstract != null) {
            element.setAttribute(IProjectType.IS_ABSTRACT, isAbstract.toString());
        }

        if (binaryParserList != null) {
            Iterator<String> bparsIter = binaryParserList.listIterator();
            String listValue = EMPTY_STRING;
            while (bparsIter.hasNext()) {
                String current = bparsIter.next();
                listValue += current;
                if ((bparsIter.hasNext())) {
                    listValue += ";"; //$NON-NLS-1$
                }
            }
            element.setAttribute(BINARY_PARSER, listValue);
        }

        if (osList != null) {
            Iterator<String> osIter = osList.listIterator();
            String listValue = EMPTY_STRING;
            while (osIter.hasNext()) {
                String current = osIter.next();
                listValue += current;
                if ((osIter.hasNext())) {
                    listValue += ","; //$NON-NLS-1$
                }
            }
            element.setAttribute(OS_LIST, listValue);
        }

        if (archList != null) {
            Iterator<String> archIter = archList.listIterator();
            String listValue = EMPTY_STRING;
            while (archIter.hasNext()) {
                String current = archIter.next();
                listValue += current;
                if ((archIter.hasNext())) {
                    listValue += ","; //$NON-NLS-1$
                }
            }
            element.setAttribute(ARCH_LIST, listValue);
        }

        // I am clean now
        isDirty = false;
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITargetPlatform#getParent()
     */
    @Override
    public IToolChain getParent() {
        return parent;
    }

    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITargetPlatform#getSuperClass()
     */
    @Override
    public ITargetPlatform getSuperClass() {
        return superClass;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITargetPlatform#getName()
     */
    @Override
    public String getName() {
        return (name == null && superClass != null) ? superClass.getName() : name;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITargetPlatform#isAbstract()
     */
    @Override
    public boolean isAbstract() {
        if (isAbstract != null) {
            return isAbstract.booleanValue();
        } else {
            return false; // Note: no inheritance from superClass
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITargetPlatform#getUnusedChildren()
     */
    @Override
    public String getUnusedChildren() {
        if (unusedChildren != null) {
            return unusedChildren;
        } else
            return EMPTY_STRING; // Note: no inheritance from superClass
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITargetPlatform#getBinaryParserId()
     * @deprecated
     */
    @Override
    public String getBinaryParserId() {
        String[] ids = getBinaryParserList();
        if (ids.length > 0)
            return ids[0];
        return EMPTY_STRING;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITargetPlatform#getBinaryParserList()
     */
    @Override
    public String[] getBinaryParserList() {
        if (binaryParserList == null) {
            // If I have a superClass, ask it
            if (superClass != null) {
                return superClass.getBinaryParserList();
            } else {
                return new String[0];
            }
        }
        return binaryParserList.toArray(new String[binaryParserList.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITargetPlatform#getArchList()
     */
    @Override
    public String[] getArchList() {
        if (archList == null) {
            // Ask superClass for its list
            if (superClass != null) {
                return superClass.getArchList();
            } else {
                // I have no superClass and no defined list
                return new String[] { "all" }; //$NON-NLS-1$
            }
        }
        return archList.toArray(new String[archList.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITargetPlatform#getOSList()
     */
    @Override
    public String[] getOSList() {
        if (osList == null) {
            // Ask superClass for its list
            if (superClass != null) {
                return superClass.getOSList();
            } else {
                // I have no superClass and no defined filter list
                return new String[] { "all" }; //$NON-NLS-1$
            }
        }
        return osList.toArray(new String[osList.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IBuilder#setBinaryParserId(String)
     * @deprecated
     */
    @Override
    public void setBinaryParserId(String id) {
        if (id == null) {
            setBinaryParserList(new String[0]);
        } else {
            setBinaryParserList(new String[] { id });
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IBuilder#setBinaryParserList(String[])
     */
    @Override
    public void setBinaryParserList(String[] ids) {
        if (ids != null) {
            if (binaryParserList == null) {
                binaryParserList = new ArrayList<>();
            } else {
                binaryParserList.clear();
            }
            for (int i = 0; i < ids.length; i++) {
                binaryParserList.add(ids[i]);
            }
        } else {
            binaryParserList = null;
        }
        setDirty(true);
    }

    /* (non-Javadoc)
     * Sets the isAbstract attribute
     */
    @Override
    public void setIsAbstract(boolean b) {
        isAbstract = b;
        setDirty(true);
    }

    /* (non-Javadoc)
     * Sets the OS list.
     *
     * @param String[] The list of OS names
     */
    @Override
    public void setOSList(String[] OSs) {
        if (osList == null) {
            osList = new ArrayList<>();
        } else {
            osList.clear();
        }
        for (int i = 0; i < OSs.length; i++) {
            osList.add(OSs[i]);
        }
        setDirty(true);
    }

    /* (non-Javadoc)
     * Sets the architecture list.
     *
     * @param String[] The list of OS names
     */
    @Override
    public void setArchList(String[] archs) {
        if (archList == null) {
            archList = new ArrayList<>();
        } else {
            archList.clear();
        }
        for (int i = 0; i < archs.length; i++) {
            archList.add(archs[i]);
        }
        setDirty(true);
    }

    /*
     *  O B J E C T   S T A T E   M A I N T E N A N C E
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuilder#isExtensionElement()
     */
    @Override
    public boolean isExtensionElement() {
        return isExtensionTargetPlatform;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuilder#isDirty()
     */
    @Override
    public boolean isDirty() {
        // This shouldn't be called for an extension Builder
        if (isExtensionTargetPlatform)
            return false;
        return isDirty;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuilder#setDirty(boolean)
     */
    @Override
    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    /* (non-Javadoc)
     *  Resolve the element IDs to interface references
     */
    public void resolveReferences() {
        if (!resolved) {
            resolved = true;
            // Resolve superClass
            if (superClassId != null && superClassId.length() > 0) {
                superClass = ManagedBuildManager.getExtensionTargetPlatform(superClassId);
                if (superClass == null) {
                    // Report error
                    ManagedBuildManager.outputResolveError("superClass", //$NON-NLS-1$
                            superClassId, "targetPlatform", //$NON-NLS-1$
                            getId());
                }
            }
        }
    }

    /**
     * @return Returns the version.
     */
    @Override
    public Version getVersion() {
        if (version == null) {
            if (getParent() != null) {
                return getParent().getVersion();
            }
        }
        return version;
    }

    @Override
    public void setVersion(Version version) {
        // Do nothing
    }

    @Override
    public CTargetPlatformData getTargetPlatformData() {
        return fTargetPlatformData;
    }

}
