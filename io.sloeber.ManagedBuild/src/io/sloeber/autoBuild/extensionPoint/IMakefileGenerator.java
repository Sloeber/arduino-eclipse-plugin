/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.extensionPoint;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;

import io.sloeber.schema.api.IBuilder;

/**
 * @since 11.0
 *
 */
public interface IMakefileGenerator {

    public final String AT = "@"; //$NON-NLS-1$
    public final String COLON = ":"; //$NON-NLS-1$
    public final int COLS_PER_LINE = 80;
    public final String COMMENT_SYMBOL = "#"; //$NON-NLS-1$
    public final String DOLLAR_SYMBOL = "$"; //$NON-NLS-1$
    public final String DEP_EXT = "d"; //$NON-NLS-1$
    public final String DEPFILE_NAME = "subdir.dep"; //$NON-NLS-1$
    public final String DOT = "."; //$NON-NLS-1$
    public final String DASH = "-"; //$NON-NLS-1$
    public final String ECHO = "echo"; //$NON-NLS-1$
    public final String IN_MACRO = "$<"; //$NON-NLS-1$
    public final String LINEBREAK = "\\\n"; //$NON-NLS-1$
    public final String LOGICAL_AND = "&&"; //$NON-NLS-1$
    public final String MAKEFILE_DEFS = "makefile.defs"; //$NON-NLS-1$
    public final String MAKEFILE_INIT = "makefile.init"; //$NON-NLS-1$
    public final String MAKEFILE_NAME = "makefile"; //$NON-NLS-1$
    public final String MAKEFILE_TARGETS = "makefile.targets"; //$NON-NLS-1$
    public final String MAKE = "$(MAKE)"; //$NON-NLS-1$
    public final String NO_PRINT_DIR = "--no-print-directory"; //$NON-NLS-1$

    public final String MODFILE_NAME = "subdir.mk"; //$NON-NLS-1$
    public final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    public final String OBJECTS_MAKFILE = "objects.mk"; //$NON-NLS-1$
    public final String OUT_MACRO = "$@"; //$NON-NLS-1$
    public final String ROOT = ".."; //$NON-NLS-1$
    public final String SEPARATOR = "/"; //$NON-NLS-1$
    public final String SINGLE_QUOTE = "'"; //$NON-NLS-1$
    public final String SRCSFILE_NAME = "sources.mk"; //$NON-NLS-1$
    public final String TAB = "\t"; //$NON-NLS-1$
    public final String WHITESPACE = " "; //$NON-NLS-1$
    public final String WILDCARD = "%"; //$NON-NLS-1$

    // Generation error codes
    public static final int SPACES_IN_PATH = 0;
    public static final int NO_SOURCE_FOLDERS = 1;

    /**
     * This method initializes the makefile generator
     */

    public void initialize(int buildKind, IProject iProject, ICConfigurationDescription cfg, IBuilder builder);

    public void generateDependencies(IProgressMonitor monitor) throws CoreException;

    /**
     * Clients call this method when an incremental rebuild is required. The
     * argument
     * contains a set of resource deltas that will be used to determine which
     * subdirectories need a new makefile and dependency list (if any).
     */
    public MultiStatus generateMakefiles(IResourceDelta delta,IProgressMonitor monitor) throws CoreException;



    public void regenerateDependencies(boolean force,IProgressMonitor monitor) throws CoreException;

    public MultiStatus regenerateMakefiles(IProgressMonitor monitor) throws CoreException;



}
