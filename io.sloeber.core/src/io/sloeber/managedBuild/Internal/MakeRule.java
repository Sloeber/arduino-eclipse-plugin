package io.sloeber.managedBuild.Internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

public class MakeRule {

    Map<String, List<IPath>> targets = new HashMap<>(); //Macro file target map
    Map<String, List<IPath>> prerequisites = new HashMap<>();//Macro file prerequisites map
    List<String> rules = new LinkedList<>();

    public HashSet<String> getMacros() {
        HashSet<String> ret = new HashSet<>();
        ret.addAll(targets.keySet());
        ret.addAll(prerequisites.keySet());
        return ret;
    }

    public HashSet<IPath> getMacroElements(String macroName) {
        HashSet<IPath> ret = new HashSet<>();
        List<IPath> list = targets.get(macroName);
        if (list != null) {
            ret.addAll(list);
        }
        list = prerequisites.get(macroName);
        if (list != null) {
            ret.addAll(list);
        }
        return ret;
    }

    public void addTarget(String Macro, IPath file) {
        List<IPath> files = targets.get(Macro);
        if (files == null) {
            files = new LinkedList<>();
            files.add(file);
            targets.put(Macro, files);
        } else {
            files.add(file);
        }
    }

    public void addPrerequisite(String Macro, IPath file) {
        List<IPath> files = prerequisites.get(Macro);
        if (files == null) {
            files = new LinkedList<>();
            files.add(file);
            prerequisites.put(Macro, files);
        } else {
            files.add(file);
        }
    }
}
