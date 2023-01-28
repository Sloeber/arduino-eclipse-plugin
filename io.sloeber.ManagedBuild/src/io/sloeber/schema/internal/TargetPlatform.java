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
package io.sloeber.schema.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.Internal.BuildTargetPlatformData;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.api.ITargetPlatform;
import io.sloeber.schema.api.IToolChain;

public class TargetPlatform extends SchemaObject implements ITargetPlatform {

	String[] modelIsAbstract;
    String[] modelOsList; 
    String[] modelArchList;
    String[] modelBinaryParser;
    
    private IToolChain parent;
    //  Managed Build model attributes
    private boolean isAbstract;
    private Set<String> osList=new HashSet<>();
    private Set<String> archList=new HashSet<>();
    private Set<String> binaryParserList=new HashSet<>();
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
    public TargetPlatform(IToolChain parent, IExtensionPoint root, IConfigurationElement element) {
        this.parent = parent;

        loadNameAndID(root, element);
        
         modelIsAbstract   =getAttributes(IS_ABSTRACT);
         modelOsList       =getAttributes(       OS_LIST );
         modelArchList     =getAttributes(     ARCH_LIST );
         modelBinaryParser =getAttributes( BINARY_PARSER);

         isAbstract = Boolean.parseBoolean(modelIsAbstract[ORIGINAL]);

        // Get the comma-separated list of valid OS
        String os = element.getAttribute(OS_LIST);
        if (!modelOsList[SUPER].isBlank()) {
            String[] osTokens = modelOsList[SUPER].split(","); //$NON-NLS-1$
            for (String token:osTokens) {
                osList.add(token.trim());
            }
        }

        // Get the comma-separated list of valid Architectures
        String arch = element.getAttribute(ARCH_LIST);
        if (arch != null) {
            String[] archTokens = arch.split(","); //$NON-NLS-1$
            for (int j = 0; j < archTokens.length; ++j) {
                archList.add(archTokens[j].trim());
            }
        }

        // Get the IDs of the binary parsers from a semi-colon-separated list.
        String bpars = element.getAttribute(BINARY_PARSER);
        if (bpars != null) {
            String[] bparsTokens = CDataUtil.stringToArray(bpars, ";"); //$NON-NLS-1$
            for (int j = 0; j < bparsTokens.length; ++j) {
                binaryParserList.add(bparsTokens[j].trim());
            }
        }

        fTargetPlatformData = new BuildTargetPlatformData(this);
    }

    @Override
    public IToolChain getParent() {
        return parent;
    }



    @Override
    public String getName() {
        return myName ;
    }



    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITargetPlatform#getBinaryParserList()
     */
    @Override
    public Set<String> getBinaryParserList() {
        return binaryParserList;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITargetPlatform#getArchList()
     */
    @Override
    public Set<String> getArchList() {
        return archList;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITargetPlatform#getOSList()
     */
    @Override
    public Set<String> getOSList() {
        return osList;
    }



    @Override
    public CTargetPlatformData getTargetPlatformData() {
        return fTargetPlatformData;
    }

}

//    /**
//     * Create a <code>TargetPlatform</code> based on the specification stored in the
//     * project file (.cdtbuild).
//     *
//     * @param parent
//     *            The <code>IToolChain</code> the TargetPlatform will be added to.
//     * @param element
//     *            The XML element that contains the TargetPlatform settings.
//     * @param managedBuildRevision
//     *            the fileVersion of Managed Build System
//     */
//    public TargetPlatform(IToolChain parent, ICStorageElement element) {
//        this.parent = parent;
//        isExtensionTargetPlatform = false;
//        fTargetPlatformData = new BuildTargetPlatformData(this);
//
//        // Initialize from the XML attributes
//
//        // id (unique, do not intern)
//        loadNameAndID(element);
//
//        // superClass
//        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));
//        if (superClassId != null && superClassId.length() > 0) {
//            superClass = null;//TOFIX JABA ManagedBuildManager.getExtensionTargetPlatform(superClassId);
//            if (superClass == null) {
//                // TODO:  Report error
//            }
//        }
//
//        // isAbstract
//        if (element.getAttribute(IS_ABSTRACT) != null) {
//            String isAbs = element.getAttribute(IS_ABSTRACT);
//            if (isAbs != null) {
//                isAbstract = Boolean.parseBoolean(isAbs);
//            }
//        }
//
//        // Get the comma-separated list of valid OS
//        if (element.getAttribute(OS_LIST) != null) {
//            String os = element.getAttribute(OS_LIST);
//            if (os != null) {
//                osList = new ArrayList<>();
//                String[] osTokens = os.split(","); //$NON-NLS-1$
//                for (int i = 0; i < osTokens.length; ++i) {
//                    osList.add(SafeStringInterner.safeIntern(osTokens[i].trim()));
//                }
//            }
//        }
//
//        // Get the comma-separated list of valid Architectures
//        if (element.getAttribute(ARCH_LIST) != null) {
//            String arch = element.getAttribute(ARCH_LIST);
//            if (arch != null) {
//                archList = new ArrayList<>();
//                String[] archTokens = arch.split(","); //$NON-NLS-1$
//                for (int j = 0; j < archTokens.length; ++j) {
//                    archList.add(SafeStringInterner.safeIntern(archTokens[j].trim()));
//                }
//            }
//        }
//
//        // Get the semi-colon-separated list of binaryParserIds
//        if (element.getAttribute(BINARY_PARSER) != null) {
//            String bpars = element.getAttribute(BINARY_PARSER);
//            if (bpars != null) {
//                binaryParserList = new ArrayList<>();
//                String[] bparsTokens = CDataUtil.stringToArray(bpars, ";"); //$NON-NLS-1$
//                for (int j = 0; j < bparsTokens.length; ++j) {
//                    binaryParserList.add(SafeStringInterner.safeIntern(bparsTokens[j].trim()));
//                }
//            }
//        }
//
//    }

/*
 *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
 */

//    /**
//     * Persist the target platform to the project file.
//     */
//    public void serialize(ICStorageElement element) {
//        if (superClass != null)
//            element.setAttribute(IBuildObject.SUPERCLASS, superClass.getId());
//
//        element.setAttribute(IBuildObject.ID, id);
//
//        if (name != null) {
//            element.setAttribute(IBuildObject.NAME, name);
//        }
//
//        if (isAbstract != null) {
//            element.setAttribute(IS_ABSTRACT, isAbstract.toString());
//        }
//
//        if (binaryParserList != null) {
//            Iterator<String> bparsIter = binaryParserList.listIterator();
//            String listValue = EMPTY_STRING;
//            while (bparsIter.hasNext()) {
//                String current = bparsIter.next();
//                listValue += current;
//                if ((bparsIter.hasNext())) {
//                    listValue += ";"; //$NON-NLS-1$
//                }
//            }
//            element.setAttribute(BINARY_PARSER, listValue);
//        }
//
//        if (osList != null) {
//            Iterator<String> osIter = osList.listIterator();
//            String listValue = EMPTY_STRING;
//            while (osIter.hasNext()) {
//                String current = osIter.next();
//                listValue += current;
//                if ((osIter.hasNext())) {
//                    listValue += ","; //$NON-NLS-1$
//                }
//            }
//            element.setAttribute(OS_LIST, listValue);
//        }
//
//        if (archList != null) {
//            Iterator<String> archIter = archList.listIterator();
//            String listValue = EMPTY_STRING;
//            while (archIter.hasNext()) {
//                String current = archIter.next();
//                listValue += current;
//                if ((archIter.hasNext())) {
//                    listValue += ","; //$NON-NLS-1$
//                }
//            }
//            element.setAttribute(ARCH_LIST, listValue);
//        }
//
//        // I am clean now
//        isDirty = false;
//    }

//@Override
//public ITargetPlatform getSuperClass() {
//    return superClass;
//}
//@Override
//public boolean isAbstract() {
//  return isAbstract;
//}
///* (non-Javadoc)
//* @see org.eclipse.cdt.core.build.managed.IBuilder#setBinaryParserList(String[])
//*/
//@Override
//public void setBinaryParserList(String[] ids) {
// if (ids != null) {
//     if (binaryParserList == null) {
//         binaryParserList = new ArrayList<>();
//     } else {
//         binaryParserList.clear();
//     }
//     for (int i = 0; i < ids.length; i++) {
//         binaryParserList.add(ids[i]);
//     }
// } else {
//     binaryParserList = null;
// }
// setDirty(true);
//}
//
//@Override
//public void setIsAbstract(boolean b) {
// isAbstract = b;
// setDirty(true);
//}
//
//@Override
//public void setOSList(String[] OSs) {
// if (osList == null) {
//     osList = new ArrayList<>();
// } else {
//     osList.clear();
// }
// for (int i = 0; i < OSs.length; i++) {
//     osList.add(OSs[i]);
// }
// setDirty(true);
//}
//
//@Override
//public void setArchList(String[] archs) {
// if (archList == null) {
//     archList = new ArrayList<>();
// } else {
//     archList.clear();
// }
// for (int i = 0; i < archs.length; i++) {
//     archList.add(archs[i]);
// }
// setDirty(true);
//}
//
//@Override
//public boolean isExtensionElement() {
// return isExtensionTargetPlatform;
//}
//
//@Override
//public boolean isDirty() {
// // This shouldn't be called for an extension Builder
// if (isExtensionTargetPlatform)
//     return false;
// return isDirty;
//}
//@Override
//public void setDirty(boolean isDirty) {
//  this.isDirty = isDirty;
//}