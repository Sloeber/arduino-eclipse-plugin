package it.baeyens.arduino.common;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPFunction;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexName;
import org.eclipse.cdt.core.index.IndexFilter;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class IndexHelper {
    /**
     * given a list of names from the indexer. Find a function call and return
     * the parameter I assume there is only one parameter and it is a string so
     * there are quotes
     * 
     * @param names
     *            the names provided by the indexer
     * @param function
     *            the name of the function for which we are looking for a
     *            parameter
     * @return the string or defaultValue if no string is found
     */
    private static String findParameterInFunction(IIndexName[] names, String function, String defaultValue) {
	for (IIndexName name : names) {
	    String SetupFileName = name.getFileLocation().getFileName();
	    String SetupFileContent;
	    try {
		SetupFileContent = FileUtils.readFileToString(new File(SetupFileName));
	    } catch (IOException e) {
		return defaultValue;
	    }
	    SetupFileContent = SetupFileContent.replaceAll("//.*|/\\*((.|\\n)(?!=*/))+\\*/", ""); //$NON-NLS-1$ //$NON-NLS-2$
	    int serialBeginStart = SetupFileContent.indexOf(function);
	    if (serialBeginStart != -1) {
		int serialBeginStartbraket = SetupFileContent.indexOf("(", serialBeginStart); //$NON-NLS-1$
		if (serialBeginStartbraket != -1) {
		    int serialBeginCloseBraket = SetupFileContent.indexOf(")", serialBeginStartbraket); //$NON-NLS-1$
		    if (serialBeginCloseBraket != -1) {
			return SetupFileContent.substring(serialBeginStartbraket + 1, serialBeginCloseBraket).trim();

		    }
		}
	    }
	}
	return defaultValue;

    }

    /**
     * given a project look in the source code for the line of code that sets
     * the password;
     * 
     * 
     * 
     * return the password string of no_pwd_found_in_code
     * 
     * @param iProject
     * @return
     */
    public static String findParameterInFunction(IProject project, String parentFunctionName, String childFunctionName,
	    String defaultValue) {

	ICProject curProject = CoreModel.getDefault().getCModel().getCProject(project.getName());

	IIndex index = null;
	try {
	    index = CCorePlugin.getIndexManager().getIndex(curProject);
	    index.acquireReadLock();
	    IIndexBinding[] bindings = index.findBindings(parentFunctionName.toCharArray(), IndexFilter.ALL_DECLARED,
		    new NullProgressMonitor());
	    ICPPFunction parentFunction = null;
	    for (IIndexBinding curbinding : bindings) {
		if (curbinding instanceof ICPPFunction) {
		    parentFunction = (ICPPFunction) curbinding;
		}
	    }

	    if (parentFunction == null) {
		return defaultValue;// that on found binding must be a function
	    }

	    IIndexName[] names = index.findNames(parentFunction, org.eclipse.cdt.core.index.IIndex.FIND_DEFINITIONS);

	    return findParameterInFunction(names, childFunctionName, defaultValue);

	} catch (CoreException | InterruptedException e) {
	    e.printStackTrace();
	} finally {
	    if (index != null) {
		index.releaseReadLock();
	    }
	}

	return defaultValue;
    }

}
