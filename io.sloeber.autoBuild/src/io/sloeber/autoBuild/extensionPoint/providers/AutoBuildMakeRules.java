package io.sloeber.autoBuild.extensionPoint.providers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;

import io.sloeber.autoBuild.api.AutoBuildBuilderExtension;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IAutoBuildMakeRule;
import io.sloeber.autoBuild.api.IAutoBuildMakeRules;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;

public class AutoBuildMakeRules implements IAutoBuildMakeRules {
    static private boolean VERBOSE = false;
    private Set<IContainer> myContainersToBuild = new HashSet<>();
    private  ICSourceEntry[] mySrcEntries;
    private IAutoBuildConfigurationDescription myAutoBuildCfgDesc;
    private List<IFile> mySourceFiles=new LinkedList<>();

    @SuppressWarnings("nls")
    static private final List<String> InputFileIgnoreList = new LinkedList<>(
            List.of(".settings", ".project", ".cproject", ".autoBuildProject"));

    private Set<IAutoBuildMakeRule> myMakeRules = new LinkedHashSet<>();

    public AutoBuildMakeRules() {
        // default constructor is fine
    }

    @Override 
    public Set<IContainer> getFoldersThatContainSourceFiles(){
    	return myContainersToBuild;
    }
    
    @Override
	public void addRule(IAutoBuildMakeRule newMakeRule) {
        if (newMakeRule.isSimpleRule()) {
            Map<IOutputType, Set<IFile>> targets = newMakeRule.getTargets();

            IOutputType outputType = null;
            IFile correctOutputPath = null;
            for (Entry<IOutputType, Set<IFile>> curTarget : targets.entrySet()) {
                outputType = curTarget.getKey();
                correctOutputPath = curTarget.getValue().toArray(new IFile[1])[0];
            }
            AutoBuildMakeRule makerule = (AutoBuildMakeRule)findTarget(outputType, correctOutputPath);
            if (makerule != null) {
                Map<IInputType, Set<IFile>> prerequisites = newMakeRule.getPrerequisites();

                IInputType inputType = null;
                Set<IFile> files = null;

                for (Entry<IInputType, Set<IFile>> curTarget : prerequisites.entrySet()) {
                    inputType = curTarget.getKey();
                    files = curTarget.getValue();
                    makerule.addPrerequisites(inputType, files);
                }

                makerule.setSequenceGroupID(newMakeRule.getSequenceGroupID());
            } else {
                myMakeRules.add(newMakeRule);
            }
        }

    }

    public IAutoBuildMakeRule findTarget(IOutputType outputType, IFile correctOutputPath) {
        for (IAutoBuildMakeRule makeRule : myMakeRules) {
            for (Entry<IOutputType, Set<IFile>> target : makeRule.getTargets().entrySet()) {
                if ((target.getKey() == outputType) && (target.getValue().contains(correctOutputPath))) {
                    return makeRule;
                }
            }
        }
        return null;
    }

    @Override
	public void addRule(ITool tool, IInputType inputType, IFile InputFile, IOutputType outputType,
            IFile correctOutputFile, int sequenceID) {
    	IAutoBuildMakeRule newMakeRule = findTarget(outputType, correctOutputFile);
        if (newMakeRule == null) {
            newMakeRule = new AutoBuildMakeRule(tool, inputType, InputFile, outputType, correctOutputFile, sequenceID);
        }
        addRule(newMakeRule);
    }

    public int size() {
        return myMakeRules.size();
    }

    @Override
	public void addRules(IAutoBuildMakeRules makeRules) {
        for (IAutoBuildMakeRule makeRule : makeRules) {
            addRule(makeRule);
        }
    }

    public Map<IOutputType, Set<IFile>> getTargets() {
        Map<IOutputType, Set<IFile>> ret = new HashMap<>();
        for (IAutoBuildMakeRule makeRule : myMakeRules) {
            Map<IOutputType, Set<IFile>> toAdd = makeRule.getTargets();
            for (Entry<IOutputType, Set<IFile>> addEntry : toAdd.entrySet()) {
                IOutputType toAddKey = addEntry.getKey();
                Set<IFile> toAddValue = addEntry.getValue();
                Set<IFile> files = ret.get(toAddKey);
                if (files == null) {
                    Set<IFile> addCopy = new HashSet<>(toAddValue);
                    ret.put(toAddKey, addCopy);
                } else {
                    files.addAll(toAddValue);
                }
            }
        }
        return ret;
    }

    public Set<String> getAllMacroNames() {
        Set<String> ret = new HashSet<>();
        for (IAutoBuildMakeRule makeRule : myMakeRules) {
            ret.addAll(((AutoBuildMakeRule)makeRule).getAllMacros());
        }
        return ret;
    }

    public Set<String> getPrerequisiteMacros() {
        Set<String> ret = new HashSet<>();
        for (IAutoBuildMakeRule makeRule : myMakeRules) {
            ret.addAll(((AutoBuildMakeRule)makeRule).getPrerequisiteMacros());
        }
        return ret;
    }

    public Set<String> getTargetMacros() {
        Set<String> ret = new HashSet<>();
        for (IAutoBuildMakeRule makeRule : myMakeRules) {
            ret.addAll(((AutoBuildMakeRule)makeRule).getTargetMacros());
        }
        return ret;
    }

    public Set<IFile> getMacroElements(String macroName) {
        Set<IFile> ret = new HashSet<>();
        for (IAutoBuildMakeRule makeRule : myMakeRules) {
            ret.addAll(((AutoBuildMakeRule)makeRule).getMacroElements(macroName));
        }
        return ret;
    }

    public Set<IAutoBuildMakeRule> getMakeRules() {
        return myMakeRules;
    }

    public Set<IFile> getTargetsForTool(ITool targetTool) {
        Set<IFile> ret = new HashSet<>();
        for (IAutoBuildMakeRule curMakeRule : myMakeRules) {
            if (curMakeRule.isTool(targetTool)) {
                ret.addAll(curMakeRule.getTargetFiles());
            }
        }
        return ret;
    }

    @Override
    public Iterator<IAutoBuildMakeRule> iterator() {
        return myMakeRules.iterator();
    }

    public AutoBuildMakeRules getRulesForContainer(IContainer container) {
        AutoBuildMakeRules rulesForContainer = new AutoBuildMakeRules();
        for (IAutoBuildMakeRule curMakeRule : myMakeRules) {
            if ((curMakeRule.getSequenceGroupID() == 0) && curMakeRule.isForContainer(container)) {
                rulesForContainer.addRule(curMakeRule);
            }
        }
        return rulesForContainer;
    }

    /**
     * generate the makeRules for the source files of this configuration
     * This method also calculates the list of containers that
     * contain source code leading to make rules
     * Those are stored in containersToBuild parameter
     * The generated Make rules will have a MakeRuleSequenceID
     * starting from 0 and going up (there may be gaps)
     * Rules with the same MakeRuleSequenceID can be run in parallel
     * A rule is always safe to run if all rules with a lower MakeRuleSequenceID
     * have finished (but it might be safe earlier)
     * 
     * @param autoBuildConfData
     *            the configuration to make rules for
     * @param buildfolder
     *            the folder the build is going to be executed in
     * @param containersToBuild
     *            returns the containers (=folders and project) that contain make
     *            rules are added to this list
     * 
     * @return The MakeRules needed to build this autobuild configuration
     * @throws CoreException
     */
    public AutoBuildMakeRules(IAutoBuildConfigurationDescription autoBuildConfData) throws CoreException {
        SourceLevelMakeRuleGenerator subDirVisitor = new SourceLevelMakeRuleGenerator();
        myAutoBuildCfgDesc = autoBuildConfData;
        mySrcEntries = IAutoBuildConfigurationDescription.getResolvedSourceEntries(autoBuildConfData);
        AutoBuildBuilderExtension.beforeAddingSourceRules(this,autoBuildConfData);
        autoBuildConfData.getProject().accept(subDirVisitor, IResource.NONE);

        AutoBuildBuilderExtension.beforeAddingSecondaryRules(this,autoBuildConfData);
        // Now we have the makeRules for the source files generate the MakeRules for the
        // created files
        generateHigherLevelMakeRules();
        AutoBuildBuilderExtension.endOfRuleCreation(this,autoBuildConfData);
    }


    
    
    /**
     * This class is used to recursively walk the project and create make rules for
     * all appropriate files found
     */
    class SourceLevelMakeRuleGenerator implements IResourceProxyVisitor {


        @Override
        public boolean visit(IResourceProxy proxy) throws CoreException {
            IResource resource = proxy.requestResource();
            if (resource.isDerived()) {
                //if (myBuildfolder.getFullPath().isPrefixOf(resource.getFullPath())) {
                //Ignore build folder content as that may cause loops
                return false;
            }
            if (InputFileIgnoreList.contains(resource.getName())) {
                return false;
            }
            boolean isExcluded = mySrcEntries == null ? false
                    : CDataUtil.isExcluded(resource.getProjectRelativePath(), mySrcEntries);
            if (isExcluded) {
                return false;
            }
            if (proxy.getType() == IResource.FILE) {
                if (addMakeRulesFromSourceFile( (IFile) resource)) {
                    IContainer parent = ((IFile) resource).getParent();
                    myContainersToBuild.add(parent);
                }
                return false;
            }
            return true;
        }

        /**
         * For the found source file give the makerules that need to be executed
         * to build the project
         * 
         * @param inputFile
         * @return true if a makerule has been created
         */
        protected boolean addMakeRulesFromSourceFile( IFile inputFile) {

            IToolChain toolchain=myAutoBuildCfgDesc.getProjectType().getToolChain();
            String ext = inputFile.getFileExtension();
            if (ext == null || ext.isBlank()) {
                return false;
            }
            int numRulesAtStart = myMakeRules.size();
            for (ITool tool : toolchain.getTools()) {
                addRules(tool.getMakeRules(myAutoBuildCfgDesc, null, inputFile, 0, VERBOSE));
            }
            return numRulesAtStart != myMakeRules.size();
        }
    }

    /**
     * Helper method to generateHigherLevelMakeRules Generate the makerules for the
     * generated files
     * 
     * @param config
     * @param buildfolder
     * @param cConfDes
     * 
     * @param generatedFiles
     *            The files generated by a rule that may generate make
     *            rules
     * @param makeRuleSequenceID
     *            The makeRuleSequenceID to assign to the created MakeRules
     * 
     * @return The MakeRules that have been created
     */
    protected AutoBuildMakeRules getMakeRulesFromGeneratedFiles( Map<IOutputType, Set<IFile>> generatedFiles, int makeRuleSequenceID) {
        AutoBuildMakeRules newMakeRules = new AutoBuildMakeRules();
        IToolChain toolchain=myAutoBuildCfgDesc.getProjectType().getToolChain();

        for (Entry<IOutputType, Set<IFile>> entry : generatedFiles.entrySet()) {
            IOutputType outputTypeIn = entry.getKey();
            Set<IFile> files = entry.getValue();
            for (IFile file : files) {
                for (ITool tool : toolchain.getTools()) {
                    newMakeRules.addRules(
                            tool.getMakeRules(myAutoBuildCfgDesc, outputTypeIn, file, makeRuleSequenceID, VERBOSE));

                    //                    for (IInputType inputType : tool.getInputTypes()) {
                    //                        if (inputType.isAssociatedWith(file, outputTypeIn)) {
                    //                            for (IOutputType outputType : tool.getOutputTypes()) {
                    //                                IFile outputFile = outputType.getOutputName(buildfolder, file, cConfDes, inputType);
                    //
                    //                                if (outputFile == null) {
                    //                                    if (VERBOSE) {
                    //                                        System.out.println(file + BLANK + tool.getName() + ACCEPTED_BY
                    //                                                + inputType.getName() + IGNORED_BY + outputType.getName());
                    //                                    }
                    //                                    continue;
                    //                                }
                    //                                if (VERBOSE) {
                    //                                    System.out.println(file + BLANK + tool.getName() + ACCEPTED_BY + inputType.getName()
                    //                                            + ACCEPTED_BY + outputType.getName());
                    //                                }
                    //                                newMakeRules.addRule(tool, inputType, file, outputType, outputFile, makeRuleSequenceID);
                    //
                    //                            }
                    //                        } else {
                    //                            if (VERBOSE) {
                    //                                System.out.println(file + BLANK + tool.getName() + IGNORED_BY + inputType.getName());
                    //                            }
                    //                        }
                    //                    }
                }
            }
        }
        return newMakeRules;
    }

    /**
     * This method generates the rules for the files generated from the source files
     * The results are added to the field makerules which already contain the source
     * make rules.
     * 
     * The generated Make rules will have a MakeRuleSequenceID >0
     * 
     * @param config
     * @param buildfolder
     * @param cConfDes
     */
    private void generateHigherLevelMakeRules() {
        int makeRuleSequenceID = 1;
        Map<IOutputType, Set<IFile>> generatedFiles = getTargets();
        if (VERBOSE) {
            System.out.println("Trying to resolve generated files level 1 (that is from source files"); //$NON-NLS-1$
        }
        AutoBuildMakeRules newMakeRules = getMakeRulesFromGeneratedFiles( generatedFiles, makeRuleSequenceID);
        while (makeRuleSequenceID < 20 && newMakeRules.size() > 0) {
            generatedFiles.clear();
            generatedFiles.putAll(newMakeRules.getTargets());
            addRules(newMakeRules);
            makeRuleSequenceID++;
            if (VERBOSE) {
                System.out.println("Trying to resolve generated files level " + String.valueOf(makeRuleSequenceID)); //$NON-NLS-1$
            }
            newMakeRules = getMakeRulesFromGeneratedFiles( generatedFiles, makeRuleSequenceID);
        }
        if (newMakeRules.size() != 0) {
            System.err.println("Makerules did not resolve to targets. Probably caused by recursion"); //$NON-NLS-1$
        }
        if (VERBOSE) {
            System.out.println("All rules are created"); //$NON-NLS-1$
        }
    }

    /**
     * Get the targets for the makefiles with the highest sequence ID
     * Though literally speaking this is not the same as the final targets
     * it should be :-S (well in my mind right now)
     * 
     * @return The targets of rules that are not used as input by another tool
     */
    public Set<IFile> getFinalTargets() {
        Set<IFile> ret = new HashSet<>();
        int highestSequenceID = 1; //(0 is for sure not the highest)
        for (IAutoBuildMakeRule curMakeRule : myMakeRules) {
            if (curMakeRule.getSequenceGroupID() > highestSequenceID) {
                highestSequenceID = curMakeRule.getSequenceGroupID();
                ret.clear();
            }
            if (curMakeRule.getSequenceGroupID() >= highestSequenceID) {
                ret.addAll(curMakeRule.getTargetFiles());
            }
        }
        return ret;
    }

    public Set<String> getDependencyMacros() {
        Set<String> ret = new HashSet<>();
        for (IAutoBuildMakeRule makeRule : myMakeRules) {
            ret.addAll(((AutoBuildMakeRule)makeRule).getDependencyMacros());
        }
        return ret;
    }

    public Set<IFile> getBuildFiles() {
        Set<IFile> targetFiles = new HashSet<>();
        for (IAutoBuildMakeRule makeRule : myMakeRules) {
            targetFiles.addAll(makeRule.getTargetFiles());
            targetFiles.addAll(makeRule.getDependencyFiles());
        }
        return targetFiles;
    }

	@Override
	public List<IFile> getSourceFilesToBuild() {
		return mySourceFiles;
	}

    //	public Map<IOutputType, Set<IFile>> getTargets() {
    //		Map<IOutputType, Set<IFile>> generatedFiles = new HashMap<>();
    //		for (MakeRule makeRule : myMakeRules) {
    //			for (Entry<IOutputType, Set<IFile>> curTarget : makeRule.getTargets().entrySet()) {
    //				IOutputType curTargetOutputType = curTarget.getKey();
    //				Set<IFile> curTargetFiles = curTarget.getValue();
    //				Set<IFile> esxistingTarget = generatedFiles.get(curTargetOutputType);
    //				if (esxistingTarget != null) {
    //					esxistingTarget.addAll(curTargetFiles);
    //				} else {
    //					Set<IFile> copySet = new HashSet<>();
    //					copySet.addAll(curTargetFiles);
    //					generatedFiles.put(curTargetOutputType, copySet);
    //				}
    //
    //			}
    //		}
    //		return generatedFiles;
    //	}

}
