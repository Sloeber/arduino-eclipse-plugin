/*******************************************************************************
 * Copyright (c) 2007, 2021 Intel Corporation and others.
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
 * Dmitry Kozlov (CodeSourcery) - Build error highlighting and navigation
 *                                Save build output (bug 294106)
 * Andrew Gvozdev (Quoin Inc)   - Saving build output implemented in different way (bug 306222)
 * Umair Sair (Mentor Graphics) - Project dependencies are not built in the correct order (bug 546407)
 * Umair Sair (Mentor Graphics) - Setting current project for markers creation (bug 545976)
 * Torbjörn Svensson (STMicroelectronics) - bug #571134
 *******************************************************************************/
package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;
import static io.sloeber.autoBuild.Internal.BuilderFactory.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.resources.ACBuilder;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IIncrementalProjectBuilder2;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import io.sloeber.autoBuild.Internal.MapStorageElement;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.schema.api.IBuilder;

public class CommonBuilder extends ACBuilder implements IIncrementalProjectBuilder2 {

	public final static String BUILDER_ID = "io.sloeber.autoBuild.integration.CommonBuilder"; //$NON-NLS-1$

	//private static final String ERROR_HEADER = "automakefileBuilder error ["; //$NON-NLS-1$
	private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
	private static final String TRACE_FOOTER = "]: "; //$NON-NLS-1$
	private static final String TRACE_HEADER = "automakefileBuilder trace ["; //$NON-NLS-1$
	public static boolean VERBOSE = false;

	static private final Set<IProject> projectsThatAreBuilding = new HashSet<>();

	public CommonBuilder() {
	}

	private static void outputTrace(String resourceName, String message) {
		if (VERBOSE) {
			System.out.println(TRACE_HEADER + resourceName + TRACE_FOOTER + message + NEWLINE);
		}
	}


	private static boolean isCdtProjectCreated(IProject project) {
		ICProjectDescription des = CoreModel.getDefault().getProjectDescription(project, false);
		return des != null && !des.isCdtProjectCreating();
	}

	/**
	 * @see IncrementalProjectBuilder#build
	 */
	@Override
	protected IProject[] build(int kind,Map<String, String> args, IProgressMonitor monitor)
			throws CoreException {
		if (DEBUG_EVENTS)
			printEvent(kind, args);

		IProject project = getProject();
		if (!isCdtProjectCreated(project)) {
			System.err.println("The build is cancelled as the project has not yet been created."); //$NON-NLS-1$
			return null;
		}

		//Mark the project as building to avoid it to be build when being build
		synchronized (projectsThatAreBuilding) {
		if (projectsThatAreBuilding.contains(project)) {
			// this project is already building so do not try to build it again.
			// this caters for A->depends on B->depends on A
			return null;
		}
		projectsThatAreBuilding.add(project);
		}


		outputTrace(project.getName(), ">>build requested, type = " + kind); //$NON-NLS-1$

		ICProjectDescription cdtProjectDescription = CCorePlugin.getDefault().getProjectDescription(project, false);
		
		Set<ICConfigurationDescription> cfgToBuild= getConfigsToBuild( cdtProjectDescription, kind, args);

		
		//For the configurations to build: get the cdt referenced configurations
		Set<ICConfigurationDescription> referencedCfgs=new HashSet<>();
		for(ICConfigurationDescription curConfig:cfgToBuild) {
			referencedCfgs.addAll( Arrays.asList( CoreModelUtil
					.getReferencedConfigurationDescriptions(curConfig, false)));
		}
		
		//build the cdt referenced configurations
		Set<IProject> cdtReferencedProjects=new HashSet<>();
		for(ICConfigurationDescription curConfig:referencedCfgs) {
			IProject curProject = curConfig.getProjectDescription().getProject();
			if(cdtReferencedProjects.contains(curProject)) {
				continue;
			}
			Set<ICConfigurationDescription> toBuildCfgs=new HashSet<>();
			for(ICConfigurationDescription searchConfig:referencedCfgs) {
				IProject searchProject = searchConfig.getProjectDescription().getProject();
				if(curProject==searchProject) {
					toBuildCfgs.add(searchConfig);
				}
				cdtReferencedProjects.add(curProject);
				//Ask eclipse to build these configs of this project
				buildProjectConfigs(curProject,toBuildCfgs,kind,monitor);
			}
		}
		
		//Build the projects this project references in the eclipse way (without configuration)
		//that have not already been handled before
		Set<IProject> eclipseReferencedProjects=new HashSet<>();
		eclipseReferencedProjects.addAll(Arrays.asList(project.getReferencedProjects()));
		eclipseReferencedProjects.removeAll(cdtReferencedProjects);
		for(IProject curProject:eclipseReferencedProjects) {
			//build the referenced project
			curProject.build(kind, monitor);
		}

		
		//now that all referenced projects and configs are build.
		//build the configs requested to build
		for(ICConfigurationDescription curConfig:cfgToBuild) {
			AutoBuildConfigurationDescription autoConf = AutoBuildConfigurationDescription
					.getFromConfig(curConfig);
		buildProjectConfiguration(kind, autoConf, monitor);
		}

		outputTrace(project.getName(), "<<done build requested, type = " + kind); //$NON-NLS-1$
		synchronized (projectsThatAreBuilding) {
			projectsThatAreBuilding.remove(project);
		}

		return null;
	}






	private static void buildProjectConfigs(IProject project , Set<ICConfigurationDescription> toBuildCfgs, int kind, IProgressMonitor localmonitor) {
		Map<String,String>cfgIdArgs=createBuildArgs(toBuildCfgs);
		try {
		ICommand[] commands = project.getDescription().getBuildSpec();
		for (ICommand command : commands) {
			Map<String, String> args = command.getArguments();
			if (args == null) {
				args = new HashMap<>(cfgIdArgs);
			} else {
				args.putAll(cfgIdArgs);
			}

			if (localmonitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			
				project.build(kind, command.getBuilderName(), args,localmonitor);

		}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
    private static Map<String, String> createBuildArgs( Set<ICConfigurationDescription>cfgs) {
        Map<String, String> map = new HashMap<>();
        map.put(CONFIGURATION_IDS, MapStorageElement.encodeList(getCfgIds(cfgs)));
        map.put(CONTENTS, CONTENTS_CONFIGURATION_IDS);
        return map;
    }


    private static List<String> getCfgIds(Set<ICConfigurationDescription>cfgs) {
    	List<String> ids = new ArrayList<>();
        for (ICConfigurationDescription cfg: cfgs) {
            ids.add( cfg.getId());
        }
        return ids;
    }

	@Override
	protected void startupOnInitialize() {
		super.startupOnInitialize();

	}
	/**
	 * Get the configurations that are requested to build and that should be build
	 * This takes into account the arguments that can reference multiple configurations and the
	 * configuration settings in regards to the build type
	 * 
	 * @param cdtProjectDescription the project configuration description of the project to build
	 * @param args the args passed to the build command
	 * @param kind the kind of build as provided by the build method
	 * @return a list of configurations to build. In case no configurations to build an empty list is returned.
	 * This method does not return null.
	 */
	private static Set<ICConfigurationDescription> getConfigsToBuild(ICProjectDescription cdtProjectDescription,int kind, Map<String, String> args) {
		// get the configurations that need to be build
				Set<ICConfigurationDescription> cfgToBuild = new HashSet<>();
				if (args == null) {
					cfgToBuild.add(cdtProjectDescription.getActiveConfiguration());

				} else {
					String argsContentType = args.get(CONTENTS);
					if (argsContentType == null) {
						argsContentType = EMPTY_STRING;
					}
					switch (argsContentType) {
					default: 
						cfgToBuild.add(cdtProjectDescription.getActiveConfiguration());
						break;
					case CONTENTS_BUILDER: 
						/*
						 * JABA: not sure what this is about. keep it as active config for now
						 */
						cfgToBuild.add(cdtProjectDescription.getActiveConfiguration());
						break;
					case CONTENTS_CONFIGURATION_IDS: 
						for (String curConfigId : cfgIdsFromMap(args)) {
							// JABA: Should I log that a config is not found?
							cfgToBuild.add(cdtProjectDescription.getConfigurationById(curConfigId));
						}
						break;
					case CONTENTS_BUILDER_CUSTOMIZATION: 
						/*
						 * JABA: again I am not sure what this is about. keep it as active config for now
						 */
						cfgToBuild.add(cdtProjectDescription.getActiveConfiguration());
//						String idsString = args.get(CONFIGURATION_IDS);
//						if (idsString != null) {
//							String[] ids = CDataUtil.stringToArray(idsString, SEPARATOR);
//							if (ids.length != 0) {
//								IManagedProject mProj = info.getManagedProject();
//								List list = new ArrayList(ids.length);
//								for (int i = 0; i < ids.length; i++) {
//									IConfiguration cfg = mProj.getConfiguration(ids[i]);
//									if (cfg != null) {
//										IBuilder builder = customizeBuilder(cfg.getEditableBuilder(), args);
//										if (builder != null)
//											list.add(builder);
//									}
//								}
//								builders = (IBuilder[]) list.toArray(new IBuilder[list.size()]);
//							}
//						}
						break;
					
					}
				}

				//remove null configurations that may have been added
				cfgToBuild.remove(null);
				
				
				//check whether all the configs to build should actually be build for 
				// this kind of build
				String projectName=cdtProjectDescription.getProject().getName();
				Set<ICConfigurationDescription> cfgToIgnore = new HashSet<>();
				for(ICConfigurationDescription curConfig:cfgToBuild) {
					AutoBuildConfigurationDescription autoData = AutoBuildConfigurationDescription.getFromConfig(curConfig);
					switch (kind) {
					case INCREMENTAL_BUILD:
						if (!autoData.isIncrementalBuildEnabled()) {
							outputTrace(projectName,curConfig.getName()+" >>The config is setup to ignore incremental builds "); 
							cfgToIgnore.add(curConfig);
						}
						break;
					case AUTO_BUILD:
						if (!autoData.isAutoBuildEnabled()) {
							outputTrace(projectName,curConfig.getName()+ ">>The config is setup to ignore auto builds "); 
							cfgToIgnore.add(curConfig);
						}
						break;
					}
				}
				cfgToBuild.removeAll(cfgToIgnore);
				return cfgToBuild;

	}

	private void buildProjectConfiguration(int kind, AutoBuildConfigurationDescription autoData, 
			IProgressMonitor monitor) throws CoreException {
		ICConfigurationDescription cConfDesc = autoData.getCdtConfigurationDescription();
		String configName = cConfDesc.getName();
		IProject project = autoData.getProject();
		IBuildRunner builder=autoData.getBuildRunner();
		IConsole console = CCorePlugin.getDefault().getConsole();
		console.start(project);
		outputTrace(project.getName(), "building cfg " + configName //$NON-NLS-1$
				+ " with builder " + builder.getName()); //$NON-NLS-1$



		try {
			// Set the current project for markers creation
			setCurrentProject(project);
			if (builder.invokeBuild(kind, autoData,  this, this, console, monitor)) {
				forgetLastBuiltState();
			}
		} catch (CoreException e) {
			forgetLastBuiltState();
			Activator.log(e);
			throw e;
		}

		checkCancel(monitor);
	}





	@Override
	protected final void clean(IProgressMonitor monitor) throws CoreException {
		throw new IllegalStateException(
				"Unexpected/incorrect call to old clean method. Client code must call clean(Map,IProgressMonitor)"); //$NON-NLS-1$
	}

	@Override
	public void clean(Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		if (DEBUG_EVENTS)
			printEvent(IncrementalProjectBuilder.CLEAN_BUILD, args);

		IProject curProject = getProject();

		if (!isCdtProjectCreated(curProject))
			return;
		ICProjectDescription cdtProjectDescription = CCorePlugin.getDefault().getProjectDescription(curProject, false);
		ICConfigurationDescription cdtConfigurationDescription = cdtProjectDescription.getActiveConfiguration();
		AutoBuildConfigurationDescription autoData = AutoBuildConfigurationDescription
				.getFromConfig(cdtConfigurationDescription);
		performExternalClean(autoData, false, monitor);
	}

	private void performExternalClean(AutoBuildConfigurationDescription autoData, boolean separateJob,
			IProgressMonitor monitor) throws CoreException {
		IProject project = autoData.getProject();
		IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
		final ISchedulingRule rule = ruleFactory.modifyRule(project);
		IBuilder builder = autoData.getConfiguration().getBuilder();
		IConsole console = CCorePlugin.getDefault().getConsole();
		console.start(project);

		if (separateJob) {
			Job backgroundJob = new Job("CDT Common Builder") { //$NON-NLS-1$
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
				 * IProgressMonitor)
				 */
				@Override
				protected IStatus run(IProgressMonitor monitor2) {
					try {
						ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {

							@Override
							public void run(IProgressMonitor monitor3) throws CoreException {
								// Set the current project for markers creation

								setCurrentProject(project);
								builder.getBuildRunner().invokeBuild(CLEAN_BUILD, autoData,  CommonBuilder.this,
										CommonBuilder.this, console, monitor3);
							}
						}, rule, IWorkspace.AVOID_UPDATE, monitor2);
					} catch (CoreException e) {
						return e.getStatus();
					}
					IStatus returnStatus = Status.OK_STATUS;
					return returnStatus;
				}

			};

			backgroundJob.setRule(rule);
			backgroundJob.schedule();
		} else {
			// Set the current project for markers creation
			setCurrentProject(project);
			builder.getBuildRunner().invokeBuild(CLEAN_BUILD, autoData,  this, this, console, monitor);
		}

	}

	//
	/**
	 * Check whether the build has been canceled.
	 */
	private static void checkCancel(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled())
			throw new OperationCanceledException();
	}

	/**
	 * Only lock the workspace is this is a ManagedBuild, or this project references
	 * others.
	 */
	@Override
	public ISchedulingRule getRule(int trigger, Map args) {
		IResource WR_rule = ResourcesPlugin.getWorkspace().getRoot();
		if (needAllConfigBuild() || !isCdtProjectCreated(getProject()))
			return WR_rule;

		IProject buildProject = getProject();
		ICProjectDescription cdtProjectDescription = CCorePlugin.getDefault().getProjectDescription(buildProject,
				false);
		ICConfigurationDescription cdtConfigurationDescription = cdtProjectDescription.getActiveConfiguration();
		// Get the builders to run
		// IBuilder builders[] = createBuilders(buildProject,
		// cdtConfigurationDescription, args);
		// Be pessimistic if we referenced other configs
		if (CoreModelUtil.getReferencedConfigurationDescriptions(cdtConfigurationDescription, false).length > 0)
			return WR_rule;
		// // If any builder isManaged => pessimistic
		// for (IBuilder builder : builders) {
		// if (builder.isManagedBuildOn())
		// return WR_rule;
		// }

		// Success!
		return null;
	}


}

//private IProject[] build(int kind, IProject project, IBuilder[] builders, boolean isForeground,
//IProgressMonitor monitor) throws CoreException {
//return build(kind, project, builders, isForeground, monitor, new MyBoolean(false));
//}

//private MultiStatus performMakefileGeneration(AutoBuildConfigurationData autoData, IMakefileGenerator generator,
//BuildStatus buildStatus, IProgressMonitor inMonitor) throws CoreException {
//// Need to report status to the user
//IProject curProject = autoData.getProject();
//IProgressMonitor monitor=inMonitor;
//if (monitor == null) {
//monitor = new NullProgressMonitor();
//}
//
//// Ask the makefile generator to generate any makefiles needed to build delta
//checkCancel(monitor);
//String statusMsg = MessageFormat.format(ManagedMakeBuilder_message_update_makefiles, curProject.getName());
//monitor.subTask(statusMsg);
//
//MultiStatus result;
//if (buildStatus.isRebuild()) {
//result = generator.regenerateMakefiles(monitor);
//} else {
//result = generator.generateMakefiles(getDelta(curProject), monitor);
//}
//
//return result;
//}

//  private MultiStatus createMultiStatus(int severity){
//      return new MultiStatus(
//              Activator.getId(),
//              severity,
//              "", //$NON-NLS-1$
//              null);
//  }

//private static void performPostbuildGeneration(int kind, IMakefileGenerator makeFileGenerator,
//IProgressMonitor monitor) throws CoreException {
//
//boolean isRebuild = true;
//if (isRebuild) {
//makeFileGenerator.regenerateDependencies(false, monitor);
//} else {
//makeFileGenerator.generateDependencies(monitor);
//}
//
//}

//    @Override
//    public void addMarker(IResource file, int lineNumber, String errorDesc, int severity, String errorVar) {
//        super.addMarker(file, lineNumber, errorDesc, severity, errorVar);
//    }
//
//    @Override
//    public void addMarker(ProblemMarkerInfo problemMarkerInfo) {
//        super.addMarker(problemMarkerInfo);
//    }

//private static String concatMessages(List<String> msgs) {
//int size = msgs.size();
//if (size == 0) {
//  return ""; //$NON-NLS-1$
//} else if (size == 1) {
//  return msgs.get(0);
//}
//
//StringBuilder buf = new StringBuilder();
//buf.append(msgs.get(0));
//for (int i = 1; i < size; i++) {
//  buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$ //$NON-NLS-2$
//  buf.append(msgs.get(i));
//}
//return buf.toString();
//}

//private class MyBoolean {
//private boolean value;
//
//public MyBoolean(boolean value) {
//  this.value = value;
//}
//
//public boolean getValue() {
//  return value;
//}
//
//public void setValue(boolean value) {
//  this.value = value;
//}
//
//}

//private boolean performCleanning(int kind, AutoBuildConfigurationData autoData, IProgressMonitor monitor)
//throws CoreException {
//return true;
////        status.setRebuild();
////        return status;
////TOFIX decide what to do with this mess
////                IConfiguration cfg = bInfo.getConfiguration();
////                IProject curProject = bInfo.getProject();
////                //      IBuilder builder = bInfo.getBuilder();
////        
////                boolean makefileRegenerationNeeded = false;
////                //perform necessary cleaning and build type calculation
////                if (cfg.needsFullRebuild()) {
////                    //configuration rebuild state is set to true,
////                    //full rebuild is needed in any case
////                    //clean first, then make a full build
////                    outputTrace(curProject.getName(), "config rebuild state is set to true, making a full rebuild"); //$NON-NLS-1$
////                    clean(bInfo, new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
////                    makefileRegenerationNeeded = true;
////                } else {
////                    makefileRegenerationNeeded = cfg.needsRebuild();
////                    IBuildDescription des = null;
////        
////                    IResourceDelta delta = kind == FULL_BUILD ? null : getDelta(curProject);
////                    if (delta == null)
////                        makefileRegenerationNeeded = true;
////                    if (cfg.needsRebuild() || delta != null) {
////                        //use a build desacription model to calculate the resources to be cleaned
////                        //only in case there are some changes to the project sources or build information
////                        try {
////                            int flags = BuildDescriptionManager.REBUILD | BuildDescriptionManager.DEPFILES
////                                    | BuildDescriptionManager.DEPS;
////                            if (delta != null)
////                                flags |= BuildDescriptionManager.REMOVED;
////        
////                            outputTrace(curProject.getName(), "using a build description.."); //$NON-NLS-1$
////        
////                            des = BuildDescriptionManager.createBuildDescription(cfg, getDelta(curProject), flags);
////        
////                            BuildDescriptionManager.cleanGeneratedRebuildResources(des);
////                        } catch (Throwable e) {
////                            //TODO: log error
////                            outputError(curProject.getName(),
////                                    "error occured while build description calculation: " + e.getLocalizedMessage()); //$NON-NLS-1$
////                            //in case an error occured, make it behave in the old stile:
////                            if (cfg.needsRebuild()) {
////                                //make a full clean if an info needs a rebuild
////                                clean((Map<String, String>) null, new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
////                                makefileRegenerationNeeded = true;
////                            } else if (delta != null && !makefileRegenerationNeeded) {
////                                // Create a delta visitor to detect the build type
////                                ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(cfg,
////                                        bInfo.getBuildInfo().getManagedProject().getConfigurations());
////                                delta.accept(visitor);
////                                if (visitor.shouldBuildFull()) {
////                                    clean((Map<String, String>) null,
////                                            new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
////                                    makefileRegenerationNeeded = true;
////                                }
////                            }
////                        }
////                    }
////                }
////        
////                if (makefileRegenerationNeeded) {
////                    status.setRebuild();
////                }
////                return status;
//}

//private static Set<IProject> getProjectsSet(ICConfigurationDescription[] cfgs) {
//if (cfgs.length == 0)
//  return new HashSet<>(0);
//
//Set<IProject> set = new HashSet<>();
//for (ICConfigurationDescription cfg : cfgs) {
//  set.add(cfg.getProjectDescription().getProject());
//}
//
//return set;
//}
//private Set<IProject> buildReferencedConfigs(ICConfigurationDescription[] cfgs, IProgressMonitor monitor) {
//Set<IProject> buildProjects = new HashSet<>();
//ICConfigurationDescription[] filteredCfgs = filterConfigsToBuild(cfgs);
//
//if (filteredCfgs.length == 0) {
//	return buildProjects;
//}
//monitor.beginTask(CommonBuilder_22, filteredCfgs.length);
//for (ICConfigurationDescription cfg : filteredCfgs) {
//	if (builtRefConfigIds.contains(cfg.getId())) {
//		continue;
//	}
//
//	AutoBuildConfigurationDescription autoBuildConfData = AutoBuildConfigurationDescription.getFromConfig(cfg);
//	IProject project = autoBuildConfData.getProject();
//	try {
//
//		outputTrace(project.getName(), ">>>>building reference cfg " + cfg.getName()); //$NON-NLS-1$
//
//		buildProjects.addAll(buildProjectAndReferences(INCREMENTAL_BUILD, autoBuildConfData, monitor));
//
//		outputTrace(project.getName(), "<<<<done building reference cfg " + cfg.getName()); //$NON-NLS-1$
//
//	} catch (CoreException e) {
//		Activator.log(e);
//	} finally {
//		builtRefConfigIds.add(cfg.getId());
//	}
//}
//
//return buildProjects;
//}

//private ICConfigurationDescription[] filterConfigsToBuild(ICConfigurationDescription[] cfgs) {
//List<ICConfigurationDescription> cfgList = new ArrayList<>(cfgs.length);
//for (ICConfigurationDescription cfg : cfgs) {
//	IProject project = cfg.getProjectDescription().getProject();
//
//	if (scheduledConfigIds.contains(cfg.getId())) {
//		Activator.log(new Status(IStatus.WARNING, Activator.getId(),
//				MessageFormat.format(CommonBuilder_circular_dependency, project.getName(), cfg.getName())));
//		continue;
//	}
//
//	if (!builtRefConfigIds.contains(cfg.getId())) {
//		outputTrace(project.getName(), "set: adding cfg " + cfg.getName() + " ( id=" + cfg.getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//		outputTrace(project.getName(),
//				"filtering regs: adding cfg " + cfg.getName() + " ( id=" + cfg.getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//
//		cfgList.add(cfg);
//	} else {
//		outputTrace(project.getName(),
//				"filtering regs: excluding cfg " + cfg.getName() + " ( id=" + cfg.getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
//	}
//
//}
//return cfgList.toArray(new ICConfigurationDescription[cfgList.size()]);
//}