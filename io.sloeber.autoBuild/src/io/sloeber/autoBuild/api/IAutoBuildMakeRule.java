package io.sloeber.autoBuild.api;

import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

public interface IAutoBuildMakeRule {

	ITool getTool();

	Set<IFile> getPrerequisiteFiles();

	Map<IInputType, Set<IFile>> getPrerequisites();

	Set<IFile> getTargetFiles();

	Set<IFile> getDependencyFiles();

	Map<IOutputType, Set<IFile>> getTargets();

	String[] getRecipes(IFolder buildFolder, AutoBuildConfigurationDescription autoBuildConfData);

	/**
	 * A simple rule is a rule that takes exactly 1 input type
	 * and exactly 1 output type containing exactly 1 file
	 * 
	 * @return true if this rule is a simple rule
	 *         otherwise false
	 */

	boolean isSimpleRule();

	int getSequenceGroupID();

	String getAnnouncement();

	boolean needsExecuting(IFolder buildfolder);

	boolean isTool(ITool targetTool);

	boolean isForContainer(IContainer folder);

}