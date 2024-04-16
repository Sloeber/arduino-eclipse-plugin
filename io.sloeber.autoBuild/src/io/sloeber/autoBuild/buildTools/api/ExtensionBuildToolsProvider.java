package io.sloeber.autoBuild.buildTools.api;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.schema.api.IProjectType;

public abstract class ExtensionBuildToolsProvider implements IBuildToolsProvider{
	private String myID=null;
	private String myName=null;
	private String myDescription=null;
	private boolean myIsTest=false;
	private Set<String> mySupportedProjectTypeIDs=new HashSet<>();
	@Override
	public String getID() {
		return myID;
	}

	@Override
	public String getName() {
		return myName;
	}

	@Override
	public String getDescription() {
		return myDescription;
	}

	@Override
	public boolean isTest() {
		return myIsTest;
	}


	@Override
	public boolean supports(IProjectType projectType) {
		if(mySupportedProjectTypeIDs.size()==0) {
			return true;
		}
		return mySupportedProjectTypeIDs.contains(projectType.getId());
	}

	public void initialize(IConfigurationElement element) {
		myID=element.getAttribute(ID);
		myName=element.getAttribute(NAME);
		myDescription=element.getAttribute(DESCRIPTION);
		myIsTest=Boolean.valueOf( element.getAttribute(IS_TEST)).booleanValue();
		String supportedProjectTypes=element.getAttribute(SUPPORTED_PROJECT_TYPES);
		if(myID==null) {
			System.err.println("No id found for xml element "+element.getNamespaceIdentifier()); //$NON-NLS-1$
		}
		if(myName==null) {
			System.err.println("No name found for xml element "+element.getNamespaceIdentifier()); //$NON-NLS-1$
		}
		if(supportedProjectTypes!=null) {
			mySupportedProjectTypeIDs.addAll( Arrays.asList( supportedProjectTypes.split(SEMICOLON)));
		}
	}

}
