package io.sloeber.managedBuild.Internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.managedbuilder.core.IInputType;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.resources.IFile;

public class MakeRules {
    private Set<MakeRule> myMakeRules = new LinkedHashSet<>();

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
                }
                makerule.addPrerequisites(inputType, files);
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

    public void addRule(ITool tool, IInputType inputType, String macroName, Set<IFile> InputFiles,
            IOutputType outputType, IFile correctOutputFile) {
        MakeRule newMakeRule = findTarget(outputType, correctOutputFile);
        if (newMakeRule == null) {
            newMakeRule = new MakeRule(tool, inputType, InputFiles, outputType, correctOutputFile);
        }
        newMakeRule.addPrerequisites(inputType, InputFiles);
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
            ret.putAll(makeRule.getTargets());
        }
        return ret;
    }

    public Set<String> getMacroNames() {
        Set<String> ret = new HashSet<>();
        for (MakeRule makeRule : myMakeRules) {
            ret.addAll(makeRule.getAllMacros());
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
		Set<IFile> ret=new HashSet<>();
		for(MakeRule curMakeRule :myMakeRules) {
			if(curMakeRule.isTool(targetTool)) {
				ret.addAll( curMakeRule.getTargetFiles());
			}
		}
		return ret;
	}

}
