package io.sloeber.autoBuild.api;

import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;

import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

public interface IAutoBuildMakeRules extends  Iterable<IAutoBuildMakeRule> {

	void addRule(IAutoBuildMakeRule newMakeRule);

	void addRule(ITool tool, IInputType inputType, IFile InputFile, IOutputType outputType, IFile correctOutputFile,
			int sequenceID);

	void addRules(IAutoBuildMakeRules makeRules);
	
	List<IFile> getSourceFilesToBuild();

	Set<IContainer> getFoldersThatContainSourceFiles();

}