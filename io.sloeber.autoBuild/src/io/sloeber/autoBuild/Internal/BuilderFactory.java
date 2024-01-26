/*******************************************************************************
 * Copyright (c) 2007, 2012 Intel Corporation and others.
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
 * Baltasar Belyavsky (Texas Instruments) - bug 340219: Project metadata files are saved unnecessarily
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import io.sloeber.autoBuild.extensionPoint.providers.CommonBuilder;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;

public class BuilderFactory {

    private static final String PREFIX = "org.eclipse.cdt.make.core"; //$NON-NLS-1$
    //	private static final String PREFIX_WITH_DOT = PREFIX + '.'; //$NON-NLS-1$

    static final String BUILD_COMMAND = PREFIX + ".buildCommand"; //$NON-NLS-1$
    static final String BUILD_LOCATION = PREFIX + ".buildLocation"; //$NON-NLS-1$
    static final String STOP_ON_ERROR = PREFIX + ".stopOnError"; //$NON-NLS-1$
    static final String USE_DEFAULT_BUILD_CMD = PREFIX + ".useDefaultBuildCmd"; //$NON-NLS-1$
    static final String BUILD_TARGET_AUTO = PREFIX + ".autoBuildTarget"; //$NON-NLS-1$
    static final String BUILD_TARGET_INCREMENTAL = PREFIX + ".incrementalBuildTarget"; //$NON-NLS-1$
    static final String BUILD_TARGET_FULL = PREFIX + ".fullBuildTarget"; //$NON-NLS-1$
    static final String BUILD_TARGET_CLEAN = PREFIX + ".cleanBuildTarget"; //$NON-NLS-1$
    static final String BUILD_FULL_ENABLED = PREFIX + ".enableFullBuild"; //$NON-NLS-1$
    static final String BUILD_CLEAN_ENABLED = PREFIX + ".enableCleanBuild"; //$NON-NLS-1$
    static final String BUILD_INCREMENTAL_ENABLED = PREFIX + ".enabledIncrementalBuild"; //$NON-NLS-1$
    static final String BUILD_AUTO_ENABLED = PREFIX + ".enableAutoBuild"; //$NON-NLS-1$
    static final String BUILD_ARGUMENTS = PREFIX + ".buildArguments"; //$NON-NLS-1$
    static final String ENVIRONMENT = PREFIX + ".environment"; //$NON-NLS-1$
    static final String BUILD_APPEND_ENVIRONMENT = PREFIX + ".append_environment"; //$NON-NLS-1$

    static public final String CONTENTS = PREFIX + ".contents"; //$NON-NLS-1$
    static public final String CONTENTS_BUILDER = PREFIX + ".builder"; //$NON-NLS-1$
    static public final String CONTENTS_BUILDER_CUSTOMIZATION = PREFIX + ".builderCustomization"; //$NON-NLS-1$
    static public final String CONTENTS_CONFIGURATION_IDS = PREFIX + ".configurationIds"; //$NON-NLS-1$

    //	static final String IDS = PREFIX + ".ids"; //$NON-NLS-1$
    public static final String CONFIGURATION_IDS = PREFIX + ".configurationIds"; //$NON-NLS-1$

    static final IBuilder[] EMPTY_BUILDERS_ARRAY = new IBuilder[0];
    static final String[] EMPTY_STRING_ARRAY = new String[0];
    static final IConfiguration[] EMPTY_CFG_ARAY = new IConfiguration[0];

    /**
     * Creates a new build-command containing data dynamically obtained from the
     * Builder.
     * 
     * @param project
     */
    public static ICommand createCommandFromBuilder(IProject project, IBuilder builder) throws CoreException {
        ICommand[] commands = project.getDescription().getBuildSpec();
        for (ICommand command : commands) {
            if (command.getBuilderName().equals(CommonBuilder.BUILDER_ID)) {
                command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, true);// builder.isAutoBuildEnable());
                command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);// builder.isFullBuildEnabled());
                command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, true);// builder.isIncrementalBuildEnabled());
                command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, true);// builder.isCleanBuildEnabled());
                return command;
            }
        }
        return null;

        //
        //        MapStorageElement el = new BuildArgsStorageElement("", null); //$NON-NLS-1$
        //        ((Builder) builder).serializeRawData(el);

        //        // always set to false - the raw data will always explicitly contain the build-command
        //        el.setAttribute(BuilderFactory.USE_DEFAULT_BUILD_CMD, Boolean.FALSE.toString());

        //        command.setArguments(el.toStringMap());

    }

    //  
    public static int applyBuilder(IProjectDescription eDes, IBuilder builder) {
        return applyBuilder(eDes, CommonBuilder.BUILDER_ID, builder);
    }

    public static final int CMD_UNDEFINED = -1;
    public static final int NO_CHANGES = 0;
    public static final int CMD_CHANGED = 1;

    private static int applyBuilder(IProjectDescription eDes, String eBuilderId, IBuilder builder) {
        //TOFIX what does this do? And if it does something it should use autoBuildNature not ManagedCProjectNature
        ICommand cmd = ManagedCProjectNature.getBuildSpec(eDes, eBuilderId);
        if (cmd == null)
            return CMD_UNDEFINED;

        if (applyBuilder(cmd, builder)) {
            ManagedCProjectNature.setBuildSpec(eDes, cmd);
            return CMD_CHANGED;
        }
        return NO_CHANGES;
    }

    private static boolean applyBuilder(ICommand cmd, IBuilder builder) {
        boolean changesMade = false;

        if (cmd.isBuilding(IncrementalProjectBuilder.AUTO_BUILD) != builder.isAutoBuildEnable()) {
            cmd.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, builder.isAutoBuildEnable());
            changesMade = true;
        }
        if (cmd.isBuilding(IncrementalProjectBuilder.FULL_BUILD) != true) {//builder.isFullBuildEnabled()) {
            cmd.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);// builder.isFullBuildEnabled());
            changesMade = true;
        }
        if (cmd.isBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD) != true) {//builder.isIncrementalBuildEnabled()) {
            cmd.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, true);//builder.isIncrementalBuildEnabled());
            changesMade = true;
        }
        if (cmd.isBuilding(IncrementalProjectBuilder.CLEAN_BUILD) != true) {//builder.isCleanBuildEnabled()) {
            cmd.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, true);//builder.isCleanBuildEnabled());
            changesMade = true;
        }
        return changesMade;
    }

}
