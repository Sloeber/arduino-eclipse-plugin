/*******************************************************************************
 * Copyright (c) 2004, 2020 Intel Corporation and others.
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
 * James Blackburn (Broadcom Corp.)
 *******************************************************************************/
package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.CommandLauncherManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import io.sloeber.autoBuild.extensionPoint.IBuildRunner;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.autoBuild.extensionPoint.IReservedMacroNameSupplier;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.extensionPoint.providers.BuildRunnerForMake;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IOptions;
import io.sloeber.schema.api.IToolChain;

public class Builder extends SchemaObject implements IBuilder {
    private static final String DEFAULT_TARGET_CLEAN = "clean"; //$NON-NLS-1$
    private static final String DEFAULT_TARGET_INCREMENTAL = "all"; //$NON-NLS-1$

    private String[] modelcommand;
    private String[] modelarguments;
    private String[] modelbuildfileGenerator;
    private String[] modelErrorParsers;
    private String[] modelvariableFormat;
    private String[] modelreservedMacroNames;
    private String[] modelReservedMacroNameSupplier;
    private String[] modelautoBuildTarget;
    private String[] modelincrementalBuildTarget;
    private String[] modelcleanBuildTarget;
    private String[] modelignoreErrCmd;
    private String[] modelparallelBuildCmd;
    private String[] modelisSystem;
    private String[] modelcommandLauncher;
    private String[] modelbuildRunner;

    //  Parent and children
    private IToolChain parent;
    private String[] reservedMacroNames;
    private ICommandLauncher fCommandLauncher;
    private IReservedMacroNameSupplier reservedMacroNameSupplier;
    private IBuildRunner fBuildRunner = null;
    private IMakefileGenerator myMakeFileGenerator;
    Set<String> myErrorParsers = new HashSet<>();

    private Options myOptionMap = new Options();

    /**
     * This constructor is called to create a builder defined by an extension point
     * in
     * a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parent
     *            The IToolChain parent of this builder, or <code>null</code> if
     *            defined at the top level
     * @param element
     *            The builder definition from the manifest file or a dynamic element
     *            provider
     * @param managedBuildRevision
     *            The fileVersion of Managed Buid System
     */
    public Builder(IToolChain parent, IExtensionPoint root, IConfigurationElement element) {
        this.parent = parent;
        loadNameAndID(root, element);

        modelcommand = getAttributes(COMMAND);
        modelarguments = getAttributes(ARGUMENTS);
        modelbuildfileGenerator = getAttributes(MAKEGEN_ID);
        modelErrorParsers = getAttributes(ERROR_PARSERS);

        modelvariableFormat = getAttributes(VARIABLE_FORMAT);
        modelreservedMacroNames = getAttributes(RESERVED_MACRO_NAMES);
        modelReservedMacroNameSupplier = getAttributes(RESERVED_MACRO_NAME_SUPPLIER);
        modelautoBuildTarget = getAttributes(ATTRIBUTE_TARGET_AUTO);
        modelincrementalBuildTarget = getAttributes(ATTRIBUTE_TARGET_INCREMENTAL);
        modelcleanBuildTarget = getAttributes(ATTRIBUTE_TARGET_CLEAN);
        modelignoreErrCmd = getAttributes(ATTRIBUTE_IGNORE_ERR_CMD);
        modelparallelBuildCmd = getAttributes(ATTRIBUTE_PARALLEL_BUILD_CMD);
        modelisSystem = getAttributes(IS_SYSTEM);
        modelcommandLauncher = getAttributes(ATTRIBUTE_COMMAND_LAUNCHER);
        modelbuildRunner = getAttributes(ATTRIBUTE_BUILD_RUNNER);

        IConfigurationElement[] optionElements = element.getChildren(IOptions.OPTION);
        for (IConfigurationElement optionElement : optionElements) {
            myOptionMap.add(new Option(this, root, optionElement));
        }

        myMakeFileGenerator = (IMakefileGenerator) createExecutableExtension(MAKEGEN_ID);
        reservedMacroNameSupplier = (IReservedMacroNameSupplier) createExecutableExtension(
                RESERVED_MACRO_NAME_SUPPLIER);

        fCommandLauncher = (ICommandLauncher) createExecutableExtension(ATTRIBUTE_COMMAND_LAUNCHER);
        if (fCommandLauncher == null) {
            fCommandLauncher = CommandLauncherManager.getInstance().getCommandLauncher();
        }
        fBuildRunner = (IBuildRunner) createExecutableExtension(ATTRIBUTE_BUILD_RUNNER);
        if (fBuildRunner == null) {
            fBuildRunner = new BuildRunnerForMake();
        }

        resolveFields();

    }

    private void resolveFields() {
        if (myName.isBlank()) {
            myName = getId();
        }
        reservedMacroNames = modelreservedMacroNames[SUPER].split(","); //$NON-NLS-1$

        if (modelcommand[SUPER].isBlank()) {
            modelcommand[SUPER] = "make"; //$NON-NLS-1$
        }
        if (modelcleanBuildTarget[SUPER].isBlank()) {
            modelcleanBuildTarget[SUPER] = DEFAULT_TARGET_CLEAN;
        }
        if (modelincrementalBuildTarget[SUPER].isBlank()) {
            modelincrementalBuildTarget[SUPER] = DEFAULT_TARGET_INCREMENTAL;
        }

        String parseArray[] = modelErrorParsers[SUPER].split(Pattern.quote(SEMICOLON));
        myErrorParsers.addAll(Arrays.asList(parseArray));

    }

    //  
    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    @Override
    public IToolChain getParent() {
        return parent;
    }

    @Override
    public String getCommand() {
        return modelcommand[SUPER];
    }

    @Override
    public String getArguments(boolean parallel, int numParallel, boolean stopOnError) {
        String ret = modelarguments[SUPER];
        String separator = EMPTY_STRING;
        if (!ret.isBlank()) {
            separator = BLANK;
        }
        if (!stopOnError) {
            ret = ret + separator + modelignoreErrCmd[SUPER];
            separator = BLANK;
        }
        if (parallel) {
            int actualParallel = numParallel;
            if (numParallel == PARRALLEL_BUILD_OPTIMAL_JOBS) {
                actualParallel = AutoBuildCommon.getOptimalParallelJobNum();
            }
            if (numParallel == PARRALLEL_BUILD_UNLIMITED_JOBS) {
                actualParallel = 999;
            }
            String parallelParameter = modelparallelBuildCmd[SUPER].replace(ASTERISK, Integer.toString(actualParallel));
            ret = ret + separator + parallelParameter;
            separator = BLANK;
        }
        return ret;
    }

    @Override
    public Set<String> getErrorParserList() {

        return myErrorParsers;

    }

    @Override
    public IMakefileGenerator getBuildFileGenerator() {
        return myMakeFileGenerator;
    }

    //TODO JABA get rid of this replace with "isMacroName"
    @Override
    public String[] getReservedMacroNames() {
        return reservedMacroNames;
    }

    //TODO JABA get rid of this replace with "isMacroName"
    @Override
    public IReservedMacroNameSupplier getReservedMacroNameSupplier() {
        return reservedMacroNameSupplier;
    }

    @Override
    public String getAutoBuildTarget() {
        return modelautoBuildTarget[SUPER];
    }

    @Override
    public String getCleanBuildTarget() {
        return modelcleanBuildTarget[SUPER];
    }

    @Override
    public String getIncrementalBuildTarget() {
        return modelincrementalBuildTarget[SUPER];
    }

    @Override
    public boolean isAutoBuildEnable() {
        return true;
    }

    @Override
    public boolean supportsBuild(boolean managed) {
        return false;
    }

    @Override
    public boolean supportsParallelBuild() {
        return !modelparallelBuildCmd[SUPER].isBlank();
    }

    @Override
    public boolean supportsStopOnError() {
        return !modelarguments[SUPER].isBlank();
    }

    @Override
    public ICommandLauncher getCommandLauncher() {
        return fCommandLauncher;
    }

    @Override
    public IBuildRunner getBuildRunner() throws CoreException {
        return fBuildRunner;
    }

    @Override
    public boolean appendEnvironment() {
        return true;
    }

    @Override
    public String getBuilderVariablePattern() {
        return modelvariableFormat[SUPER];
    }

}

//private ICOutputEntry[] getDefaultOutputSettings(IProject project) {
//Configuration cfg = (Configuration) getConfguration();
//if (cfg == null || cfg.isPreference()) {
//  return new ICOutputEntry[] {
//          new COutputEntry(Path.EMPTY, null, ICSettingEntry.VALUE_WORKSPACE_PATH | ICSettingEntry.RESOLVED) };
//}
//
////IFolder BuildFolder = ManagedBuildManager.getBuildFolder(cfg, this);
//IFolder BuildFolder = project.getFolder(cfg.getName());
//return new ICOutputEntry[] {
//      new COutputEntry(BuildFolder, null, ICSettingEntry.VALUE_WORKSPACE_PATH | ICSettingEntry.RESOLVED) };
//}

//public static String[] toBuildAttributes(String name) {
//
//  //        if (ATTRIBUTE_TARGET_INCREMENTAL.equals(name)) {
//  //            return new String[] { BUILD_TARGET_INCREMENTAL, BuilderFactory.BUILD_TARGET_INCREMENTAL, BUILD_TARGET_FULL,
//  //                    BuilderFactory.BUILD_TARGET_FULL };
//  //        } else if (ATTRIBUTE_TARGET_AUTO.equals(name)) {
//  //            return new String[] { BUILD_TARGET_AUTO, BuilderFactory.BUILD_TARGET_AUTO };
//  //        } else if (ATTRIBUTE_TARGET_CLEAN.equals(name)) {
//  //            return new String[] { BUILD_TARGET_CLEAN, BuilderFactory.BUILD_TARGET_CLEAN };
//  //        } else if (ATTRIBUTE_BUILD_PATH.equals(name)) {
//  //            return new String[] { BUILD_LOCATION, BuilderFactory.BUILD_LOCATION };
//  //        } else if (COMMAND.equals(name)) {
//  //            return new String[] { BUILD_COMMAND, BuilderFactory.BUILD_COMMAND };
//  //        } else if (ARGUMENTS.equals(name)) {
//  //            return new String[] { BUILD_ARGUMENTS, BuilderFactory.BUILD_ARGUMENTS };
//  //        } else if (ATTRIBUTE_STOP_ON_ERR.equals(name)) {
//  //            return new String[] { BuilderFactory.STOP_ON_ERROR };
//  //        } //TODO else if(BuilderFactory.USE_DEFAULT_BUILD_CMD.equals(name)){
//  //          //    return getCommand();
//  //          //}
//  //        else if (ATTRIBUTE_INCREMENTAL_ENABLED.equals(name)) {
//  //            return new String[] { BuilderFactory.BUILD_INCREMENTAL_ENABLED, BuilderFactory.BUILD_FULL_ENABLED };
//  //        } else if (ATTRIBUTE_CLEAN_ENABLED.equals(name)) {
//  //            return new String[] { BuilderFactory.BUILD_CLEAN_ENABLED };
//  //        } else if (ATTRIBUTE_AUTO_ENABLED.equals(name)) {
//  //            return new String[] { BuilderFactory.BUILD_AUTO_ENABLED };
//  //        } else if (ATTRIBUTE_ENVIRONMENT.equals(name)) {
//  //            return new String[] { BuilderFactory.ENVIRONMENT };
//  //        } else if (ATTRIBUTE_APPEND_ENVIRONMENT.equals(name)) {
//  //            return new String[] { BuilderFactory.BUILD_APPEND_ENVIRONMENT };
//  //        } else if (ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS.equals(name)) {
//  //            return new String[] { ErrorParserManager.PREF_ERROR_PARSER };
//  //        }
//
//  return new String[0];
//}

// public static String toBuilderAttribute(String name) {
//
//      //        if (BUILD_TARGET_INCREMENTAL.equals(name) || BuilderFactory.BUILD_TARGET_INCREMENTAL.equals(name)
//      //                || BUILD_TARGET_FULL.equals(name) || BuilderFactory.BUILD_TARGET_FULL.equals(name)) {
//      //            return ATTRIBUTE_TARGET_INCREMENTAL;
//      //        } else if (BUILD_TARGET_AUTO.equals(name) || BuilderFactory.BUILD_TARGET_AUTO.equals(name)) {
//      //            return ATTRIBUTE_TARGET_AUTO;
//      //        } else if (BUILD_TARGET_CLEAN.equals(name) || BuilderFactory.BUILD_TARGET_CLEAN.equals(name)) {
//      //            return ATTRIBUTE_TARGET_CLEAN;
//      //        } else if (BUILD_LOCATION.equals(name) || BuilderFactory.BUILD_LOCATION.equals(name)) {
//      //            return ATTRIBUTE_BUILD_PATH;
//      //        } else if (BUILD_COMMAND.equals(name) || BuilderFactory.BUILD_COMMAND.equals(name)) {
//      //            return COMMAND;
//      //        } else if (BUILD_ARGUMENTS.equals(name) || BuilderFactory.BUILD_ARGUMENTS.equals(name)) {
//      //            return ARGUMENTS;
//      //        } else if (BuilderFactory.STOP_ON_ERROR.equals(name)) {
//      //            return ATTRIBUTE_STOP_ON_ERR;
//      //        } //TO DO else if(BuilderFactory.USE_DEFAULT_BUILD_CMD.equals(name)){
//      //          //    return getCommand();
//      //          //}
//      //        else if (BuilderFactory.BUILD_INCREMENTAL_ENABLED.equals(name)
//      //                || BuilderFactory.BUILD_FULL_ENABLED.equals(name)) {
//      //            return ATTRIBUTE_INCREMENTAL_ENABLED;
//      //        } else if (BuilderFactory.BUILD_CLEAN_ENABLED.equals(name)) {
//      //            return ATTRIBUTE_CLEAN_ENABLED;
//      //        } else if (BuilderFactory.BUILD_AUTO_ENABLED.equals(name)) {
//      //            return ATTRIBUTE_AUTO_ENABLED;
//      //        } else if (BuilderFactory.ENVIRONMENT.equals(name)) {
//      //            return ATTRIBUTE_ENVIRONMENT;
//      //        } else if (BuilderFactory.BUILD_APPEND_ENVIRONMENT.equals(name)) {
//      //            return ATTRIBUTE_APPEND_ENVIRONMENT;
//      //        } else if (ErrorParserManager.PREF_ERROR_PARSER.equals(name)) {
//      //            return ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS;
//      //        }
//      return null;
//  }

//    @Override
//    public String toString() {
//
//        //        String version1 = ManagedBuildManager.getVersionFromIdAndVersion(getId());
//        //        if (version1 != null) {
//        //            StringBuilder buf = new StringBuilder();
//        //            buf.append(myName);
//        //            buf.append(" (v").append(version1).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
//        //            return buf.toString();
//        //        }
//        return myName;
//    }
