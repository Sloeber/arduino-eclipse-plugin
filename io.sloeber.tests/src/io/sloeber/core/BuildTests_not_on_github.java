package io.sloeber.core;




import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import io.sloeber.arduinoFramework.api.LibraryManager;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({ "nls", "static-method"})
public class BuildTests_not_on_github {

    /*
     * In new new installations (of the Sloeber development environment) the
     * installer job will trigger downloads These must have finished before we can
     * start testing
     */
    @BeforeAll
    public static void beforeClass() throws Exception {
        Shared.waitForBoardsManager();
        Shared.setDeleteProjects(false);
        ConfigurationPreferences.setUseBonjour(false);
    }


    @Test
    public void onlyInstallLibraryWhenAllowed() throws Exception {
    	String libName="SD";

        //set option not to install lib
    	ConfigurationPreferences.setInstallLibraries(false);

        //uninstall lib
    	LibraryManager.uninstallLibrary( libName);

    	// create a project that uses a the lib
        String testName = "onlyInstallLibraryWhenAllowed";
        IProgressMonitor  monitor=new NullProgressMonitor();
        IPath templateFolder = Shared.getTemplateFolder(testName);
        CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);
        MCUBoard unoboard = Arduino.uno();
        IProject theTestProject = SloeberProject.createArduinoProject(testName, null, unoboard.getBoardDescriptor(), codeDescriptor,
        		 new CompileDescription(), monitor);
        //wait for indexer and so on
		Shared.waitForAllJobsToFinish();


        //Building the project should fail
		assertNotNull( Shared.buildAndVerify(theTestProject,3,IncrementalProjectBuilder.FULL_BUILD ,monitor),"Sloeber wrongly installed lib "+libName);

        //set option to install libs
		ConfigurationPreferences.setInstallLibraries(true);

		//trigger the indexer
		ICProject cTestProject = CoreModel.getDefault().getCModel().getCProject(theTestProject.getName());
		CCorePlugin.getIndexManager().reindex(cTestProject);
		Thread.sleep(5000);
		Shared.waitForIndexer(theTestProject);

        //build should not fail
		assertNull( Shared.buildAndVerify(theTestProject,3,IncrementalProjectBuilder.FULL_BUILD ,monitor),"Sloeber dit not install lib "+libName);



    }
}
