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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;

import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

public class MakeRules implements Iterable<MakeRule> {
    static private boolean VERBOSE = false;

    @SuppressWarnings("nls")
    static private final List<String> InputFileIgnoreList = new LinkedList<>(
            List.of(".settings", ".project", ".cproject", ".autoBuildProject"));

    private Set<MakeRule> myMakeRules = new LinkedHashSet<>();

    public MakeRules() {
        // default constructor is fine
    }

    public void addRule(MakeRule newMakeRule) {
        if (newMakeRule.isSimpleRule()) {
            Map<IOutputType, Set<IFile>> targets = newMakeRule.getTargets();

            IOutputType outputType = null;
            IFile correctOutputPath = null;
            for (Entry<IOutputType, Set<IFile>> curTarget : targets.entrySet()) {
                outputType = curTarget.getKey();
                correctOutputPath = curTarget.getValue().toArray(new IFile[1])[0];
            }
            MakeRule makerule = findTarget(outputType, correctOutputPath);
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

    public MakeRule findTarget(IOutputType outputType, IFile correctOutputPath) {
        for (MakeRule makeRule : myMakeRules) {
            for (Entry<IOutputType, Set<IFile>> target : makeRule.getTargets().entrySet()) {
                if ((target.getKey() == outputType) && (target.getValue().contains(correctOutputPath))) {
                    return makeRule;
                }
            }
        }
        return null;
    }

    public void addRule(ITool tool, IInputType inputType, IFile InputFile, IOutputType outputType,
            IFile correctOutputFile, int sequenceID) {
        MakeRule newMakeRule = findTarget(outputType, correctOutputFile);
        if (newMakeRule == null) {
            newMakeRule = new MakeRule(tool, inputType, InputFile, outputType, correctOutputFile, sequenceID);
        }
        //		newMakeRule.addPrerequisite(inputType, InputFile);
        addRule(newMakeRule);

    }

    public int size() {
        return myMakeRules.size();
    }

    public void addRules(MakeRules makeRules) {
        for (MakeRule makeRule : makeRules.getMakeRules()) {
            addRule(makeRule);
        }
    }

    public Map<IOutputType, Set<IFile>> getTargets() {
        Map<IOutputType, Set<IFile>> ret = new HashMap<>();
        for (MakeRule makeRule : myMakeRules) {
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
        for (MakeRule makeRule : myMakeRules) {
            ret.addAll(makeRule.getAllMacros());
        }
        return ret;
    }

    public Set<String> getPrerequisiteMacros() {
        Set<String> ret = new HashSet<>();
        for (MakeRule makeRule : myMakeRules) {
            ret.addAll(makeRule.getPrerequisiteMacros());
        }
        return ret;
    }

    public Set<String> getTargetMacros() {
        Set<String> ret = new HashSet<>();
        for (MakeRule makeRule : myMakeRules) {
            ret.addAll(makeRule.getTargetMacros());
        }
        return ret;
    }

    public Set<IFile> getMacroElements(String macroName) {
        Set<IFile> ret = new HashSet<>();
        for (MakeRule makeRule : myMakeRules) {
            ret.addAll(makeRule.getMacroElements(macroName));
        }
        return ret;
    }

    public Set<MakeRule> getMakeRules() {
        return myMakeRules;
    }

    public Set<IFile> getTargetsForTool(ITool targetTool) {
        Set<IFile> ret = new HashSet<>();
        for (MakeRule curMakeRule : myMakeRules) {
            if (curMakeRule.isTool(targetTool)) {
                ret.addAll(curMakeRule.getTargetFiles());
            }
        }
        return ret;
    }

    @Override
    public Iterator<MakeRule> iterator() {
        return myMakeRules.iterator();
    }

    public MakeRules getRulesForContainer(IContainer container) {
        MakeRules rulesForContainer = new MakeRules();
        for (MakeRule curMakeRule : myMakeRules) {
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
    public MakeRules(AutoBuildConfigurationDescription autoBuildConfData, IFolder buildfolder,
            Set<IContainer> containersToBuild) throws CoreException {

        SourceLevelMakeRuleGenerator subDirVisitor = new SourceLevelMakeRuleGenerator();
        subDirVisitor.myBuildfolder = buildfolder;
        subDirVisitor.myAutoBuildConfData = autoBuildConfData;
        subDirVisitor.myConfig = autoBuildConfData.getConfiguration();
        subDirVisitor.myContainersToBuild = containersToBuild;
        subDirVisitor.mySrcEntries = autoBuildConfData.getSourceEntries();
        autoBuildConfData.getProject().accept(subDirVisitor, IResource.NONE);

        // Now we have the makeRules for the source files generate the MakeRules for the
        // created files
        generateHigherLevelMakeRules(autoBuildConfData, buildfolder);
    }

    /**
     * This class is used to recursively walk the project and create make rules for
     * all appropriate files found
     */
    class SourceLevelMakeRuleGenerator implements IResourceProxyVisitor {
        IFolder myBuildfolder;
        IConfiguration myConfig;
        Set<IContainer> myContainersToBuild;
        ICSourceEntry[] mySrcEntries;
        AutoBuildConfigurationDescription myAutoBuildConfData;

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
                if (getMakeRulesFromSourceFile(myAutoBuildConfData, (IFile) resource)) {
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
        protected boolean getMakeRulesFromSourceFile(AutoBuildConfigurationDescription autoBuildConfData,
                IFile inputFile) {

            IConfiguration config = autoBuildConfData.getConfiguration();
            String ext = inputFile.getFileExtension();
            if (ext == null || ext.isBlank()) {
                return false;
            }
            int numRulesAtStart = myMakeRules.size();
            for (ITool tool : config.getToolChain().getTools()) {
                addRules(tool.getMakeRules(autoBuildConfData, null, inputFile, 0, VERBOSE));
                //                if (!tool.isEnabled(autoBuildConfData)) {
                //                    continue;
                //                }
                //                for (IInputType inputType : tool.getInputTypes()) {
                //                    if (inputType.isAssociatedWith(inputFile)) {
                //                        for (IOutputType outputType : tool.getOutputTypes()) {
                //                            IFile outputFile = outputType.getOutputName(buildfolder, inputFile, cConfDes, inputType);
                //                            if (outputFile == null) {
                //                                if (VERBOSE) {
                //                                    System.out.println(inputFile + BLANK + tool.getName() + ACCEPTED_BY
                //                                            + inputType.getName() + IGNORED_BY + outputType.getName());
                //                                }
                //                                continue;
                //                            }
                //                            if (VERBOSE) {
                //                                System.out.println(inputFile + BLANK + tool.getName() + ACCEPTED_BY
                //                                        + inputType.getName() + ACCEPTED_BY + outputType.getName());
                //                            }
                //                            MakeRule newMakeRule = new MakeRule(tool, inputType, inputFile, outputType, outputFile, 0);
                //
                //                            addRule(newMakeRule);
                //                            ret = true;
                //                        }
                //                    } else {
                //                        if (VERBOSE) {
                //                            System.out.println(inputFile + BLANK + tool.getName() + IGNORED_BY + inputType.getName());
                //                        }
                //                    }
                //                }
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
    protected static MakeRules getMakeRulesFromGeneratedFiles(AutoBuildConfigurationDescription autoBuildConfData,
            Map<IOutputType, Set<IFile>> generatedFiles, int makeRuleSequenceID) {
        MakeRules newMakeRules = new MakeRules();
        IConfiguration config = autoBuildConfData.getConfiguration();

        for (Entry<IOutputType, Set<IFile>> entry : generatedFiles.entrySet()) {
            IOutputType outputTypeIn = entry.getKey();
            Set<IFile> files = entry.getValue();
            for (IFile file : files) {
                for (ITool tool : config.getToolChain().getTools()) {
                    newMakeRules.addRules(
                            tool.getMakeRules(autoBuildConfData, outputTypeIn, file, makeRuleSequenceID, VERBOSE));

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
    private void generateHigherLevelMakeRules(AutoBuildConfigurationDescription autoBuildConfData,
            IFolder buildfolder) {
        int makeRuleSequenceID = 1;
        Map<IOutputType, Set<IFile>> generatedFiles = getTargets();
        if (VERBOSE) {
            System.out.println("Trying to resolve generated files level 1 (that is from source files"); //$NON-NLS-1$
        }
        MakeRules newMakeRules = getMakeRulesFromGeneratedFiles(autoBuildConfData, generatedFiles, makeRuleSequenceID);
        while (makeRuleSequenceID < 20 && newMakeRules.size() > 0) {
            generatedFiles.clear();
            generatedFiles.putAll(newMakeRules.getTargets());
            addRules(newMakeRules);
            makeRuleSequenceID++;
            if (VERBOSE) {
                System.out.println("Trying to resolve generated files level " + String.valueOf(makeRuleSequenceID)); //$NON-NLS-1$
            }
            newMakeRules = getMakeRulesFromGeneratedFiles(autoBuildConfData, generatedFiles, makeRuleSequenceID);
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
        for (MakeRule curMakeRule : myMakeRules) {
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
        for (MakeRule makeRule : myMakeRules) {
            ret.addAll(makeRule.getDependencyMacros());
        }
        return ret;
    }

    public Set<IFile> getBuildFiles() {
        Set<IFile> targetFiles = new HashSet<>();
        for (MakeRule makeRule : myMakeRules) {
            targetFiles.addAll(makeRule.getTargetFiles());
            targetFiles.addAll(makeRule.getDependencyFiles());
        }
        return targetFiles;
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
