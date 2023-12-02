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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

//public static IBuilder createCustomBuilder(IConfiguration cfg, String builderId) throws CoreException {
//IBuilder builder = cfg.getBuilder();
//if (!builderId.equals(builder.getId())) {
//  builder = null;//TOFIX JABA  ManagedBuildManager.getExtensionBuilder(builderId);
//}
//
//if (builder != null)
//  return createCustomBuilder(cfg, builder);
//throw new CoreException(new Status(IStatus.ERROR, Activator.getId(), -1,
//      //TOFIX ManagedMakeMessages.getResourceString("BuilderFactory.1"),  //$NON-NLS-1$
//      "BuilderFactory.1", null)); //$NON-NLS-1$
//}
//
//public static IBuilder createCustomBuilder(IConfiguration cfg, IBuilder base) {
//String subId;
//String subName = base.getName();
//if (base.getSuperClass() != null) {
//  subId = ManagedBuildManager.calculateChildId(base.getSuperClass().getId(), null);
//} else {
//  subId = ManagedBuildManager.calculateChildId(base.getId(), null);
//}
//
//return new Builder(cfg.getToolChain(), subId, subName, (Builder) base);
//}

//public static IBuilder[] createBuilders(IProject project, Map<String, String> args) {
//IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
//IBuilder[] builders = null;
//if (info != null) {
//  if (args == null) {
//      IConfiguration cfg = info.getDefaultConfiguration();
//      IBuilder builder = cfg.getBuilder();
//      builders = new IBuilder[] { builder };
//
//  } else {
//      String type = args.get(CONTENTS);
//      //                if (type == null || CONTENTS_BUILDER_CUSTOMIZATION.equals(type)) {
//      //                    IConfiguration cfg = info.getDefaultConfiguration();
//      //                    IBuilder builder;
//      //                    if (args.size() == 0) {
//      //                        builder = cfg.getBuilder();
//      //                    } else {
//      //                        builder = createBuilder(cfg, args, true);
//      //                    }
//      //                    builders = new IBuilder[] { builder };
//      //
//      //                } else
//      if (CONTENTS_BUILDER.equals(type)) {
//          IConfiguration cfgs[] = configsFromMap(args, info);
//          if (cfgs.length != 0) {
//              List<IBuilder> list = new ArrayList<>(cfgs.length);
//              for (int i = 0; i < cfgs.length; i++) {
//                  IBuilder builder = createBuilder(cfgs[i], args);
//                  if (builder != null)
//                      list.add(builder);
//              }
//              builders = list.toArray(new IBuilder[list.size()]);
//          }
//
//      } else if (CONTENTS_CONFIGURATION_IDS.equals(type)) {
//          IConfiguration cfgs[] = configsFromMap(args, info);
//          if (cfgs.length != 0) {
//              List<IBuilder> list = new ArrayList<>(cfgs.length);
//              for (int i = 0; i < cfgs.length; i++) {
//                  list.add(cfgs[i].getBuilder());
//              }
//              builders = list.toArray(new IBuilder[list.size()]);
//          }
//
//      } else if ("org.eclipse.cdt.make.core.activeConfigSettings".equals(type)) { //$NON-NLS-1$
//          /* NOTE: Here, only for backwards-compatibility support, since bug 340219 was fixed.
//           * Existing projects will still be going through this execution path, but new
//           * projects will no longer store the active-configuration's builder-arguments in the
//           * build-command, and will be going through the "args == null" condition above.
//           */
//          IConfiguration cfg = info.getDefaultConfiguration();
//          IBuilder builder = cfg.getBuilder();
//          builders = new IBuilder[] { builder };
//
//      } /*else if (CONTENTS_BUILDER_CUSTOMIZATION.equals(type)){
//        String idsString = (String)args.get(CONFIGURATION_IDS);
//        if(idsString != null){
//            String[] ids = CDataUtil.stringToArray(idsString, SEPARATOR);
//            if(ids.length != 0){
//                IManagedProject mProj = info.getManagedProject();
//                List list = new ArrayList(ids.length);
//                for(int i = 0; i < ids.length; i++){
//                    IConfiguration cfg = mProj.getConfiguration(ids[i]);
//                    if(cfg != null){
//                        IBuilder builder = customizeBuilder(cfg.getEditableBuilder(), args);
//                        if(builder != null)
//                            list.add(builder);
//                    }
//                }
//                builders = (IBuilder[])list.toArray(new IBuilder[list.size()]);
//            }
//        }
//        }*/
//  }
//}
//
//if (builders != null)
//  return builders;
//return EMPTY_BUILDERS_ARRAY;
//}

//public static IBuilder createBuilderFromCommand(IConfiguration cfg, ICommand command) {
//Map<String, String> args = command.getArguments();
//if (!args.containsKey(IBuildObject.ID)) {
//  args.put(IBuildObject.ID, ManagedBuildManager.calculateChildId(command.getBuilderName(), null));
//}
//
//return createBuilder(cfg, args);
//}

//private static IBuilder createBuilder(IConfiguration cfg, Map<String, String> args) {
//  IToolChain tCh = cfg.getToolChain();
//  IBuilder cfgBuilder = cfg.getBuilder();
//
//  Builder builder;
//  //        if (customization) {
//  //            builder = (Builder) createCustomBuilder(cfg, cfgBuilder);
//  //
//  //            //adjusting settings
//  //            String tmp = args.get(ErrorParserManager.PREF_ERROR_PARSER);
//  //            if (tmp != null && tmp.length() == 0)
//  //                args.remove(ErrorParserManager.PREF_ERROR_PARSER);
//  //
//  //            tmp = args.get(USE_DEFAULT_BUILD_CMD);
//  //            if (tmp != null) {
//  //                if (Boolean.valueOf(tmp).equals(Boolean.TRUE)) {
//  //                    args.remove(IMakeCommonBuildInfo.BUILD_COMMAND);
//  //                    args.remove(IMakeCommonBuildInfo.BUILD_ARGUMENTS);
//  //                } else {
//  //                    args.put(IBuilder.ATTRIBUTE_IGNORE_ERR_CMD, ""); //$NON-NLS-1$
//  //                    args.put(IBuilder.ATTRIBUTE_PARALLEL_BUILD_CMD, ""); //$NON-NLS-1$
//  //                }
//  //            }
//  //            //end adjusting settings
//  //
//  //            MapStorageElement el = new BuildArgsStorageElement(args, null);
//  //            builder.loadFromProject(el);
//  //        } else {
//  if (args.get(IBuildObject.ID) == null) {
//      args.put(IBuildObject.ID, ManagedBuildManager.calculateChildId(cfg.getId(), null));
//  }
//  MapStorageElement el = new BuildArgsStorageElement(args, null);
//  builder = new Builder(tCh, el, ManagedBuildManager.getVersion().toString());
//  //        }
//
//  return builder;
//}
//private static IConfiguration[] configsFromMap(Map<String, String> map, IManagedBuildInfo info) {
//String ids[] = cfgIdsFromMap(map);
//if (ids.length == 0) {
//  IConfiguration cfg = info.getDefaultConfiguration();
//  if (cfg != null)
//      return new IConfiguration[] { cfg };
//  return EMPTY_CFG_ARAY;
//}
//IManagedProject mProj = info.getManagedProject();
//if (mProj != null)
//  return idsToConfigurations(ids, mProj.getConfigurations());
//return EMPTY_CFG_ARAY;
//}

//
//private static IConfiguration[] idsToConfigurations(String ids[], IConfiguration allCfgs[]) {
//List<IConfiguration> list = new ArrayList<>(ids.length);
//for (int i = 0; i < ids.length; i++) {
//  String id = ids[i];
//  for (int j = 0; j < allCfgs.length; j++) {
//      if (allCfgs[j].getId().equals(id)) {
//          list.add(allCfgs[j]);
//          break;
//      }
//  }
//}
//return list.toArray(new IConfiguration[list.size()]);
//}
//public static Map<String, String> createBuildArgs(IConfiguration cfgs[], IBuilder builder) {
//Map<String, String> map = builderToMap(builder);
//cfgsToMap(cfgs, map);
//map.put(CONTENTS, /*CONTENTS_BUILDER_CUSTOMIZATION*/CONTENTS_BUILDER);
//return map;
//}
//private static Map<String, String> builderToMap(IBuilder builder) {
//MapStorageElement el = new MapStorageElement("", null); //$NON-NLS-1$
//((Builder) builder).serialize(el, false);
//
//return el.toStringMap();
//}

//private static class BuildArgsStorageElement extends MapStorageElement {
//
//     public BuildArgsStorageElement(Map<String, String> map, MapStorageElement parent) {
//         super(map, parent);
//     }
//
//     public BuildArgsStorageElement(String name, MapStorageElement parent) {
//         super(name, parent);
//     }
//
//     @Override
//     public String getAttribute(String name) {
//         String value = super.getAttribute(name);
//         if (value == null) {
//             String keys[] = Builder.toBuildAttributes(name);
//             for (int i = 0; i < keys.length; i++) {
//                 value = super.getAttribute(keys[i]);
//                 if (value != null)
//                     break;
//             }
//         }
//         return value;
//     }
//
//     //        protected String getMapKey(String name) {
//     //            if(name.indexOf('.') == -1)
//     //                return PREFIX_WITH_DOT + name;
//     //            return super.getMapKey(name);
//     //        }
//
//     @Override
//     public void setAttribute(String name, String value) {
//         String[] names = Builder.toBuildAttributes(name);
//         String attrName = names.length != 0 ? names[names.length - 1] : null;
//
//         if (attrName == null && BuilderFactory.USE_DEFAULT_BUILD_CMD.equals(name))
//             attrName = BuilderFactory.USE_DEFAULT_BUILD_CMD;
//
//         if (attrName != null)
//             super.setAttribute(attrName, value);
//     }
//
//     @Override
//     protected MapStorageElement createChildElement(Map<String, String> childMap) {
//         return new BuildArgsStorageElement(childMap, this);
//     }
//
//     @Override
//     protected MapStorageElement createChildElement(String name) {
//         return new BuildArgsStorageElement(name, this);
//     }
// }
//
// /*    public static IMakeBuilderInfo create(Preferences prefs, String builderID, boolean useDefaults) {
//       return new BuildInfoPreference(prefs, builderID, useDefaults);
//   }
// 
//   public static IMakeBuilderInfo create(IProject project, String builderID) throws CoreException {
//       return new BuildInfoProject(project, builderID);
//   }
// 
//   public static IMakeBuilderInfo create(Map args, String builderID) {
//       return new BuildInfoMap(args, builderID);
//   }
//   */
// /*
//   private static IBuilder customizeBuilder(IBuilder builder, Map args){
//       if(args.get(IBuilder.ID) == null){
//           args = new HashMap(args);
//           String id = builder.getSuperClass().getId();
//           id = ManagedBuildManager.calculateChildId(id, null);
//           args.put(IBuilder.ID, id);
//       }
//       MapStorageElement el = new MapStorageElement(args, null);
// 
//       Builder builder = new Builder(builder.getParent(), )
// 
// 
//   }
// */

//public static ICommand getBuildSpec(IProjectDescription description, String builderID) {
//ICommand[] commands = description.getBuildSpec();
//for (int i = 0; i < commands.length; ++i) {
//  if (commands[i].getBuilderName().equals(builderID)) {
//      return commands[i];
//  }
//}
//return null;
//}