package io.sloeber.autoBuild.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.internal.core.BuildRunnerHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This class purely exists to avoid warnings because AutoBuild code is outside
 * of CDT
 * and BuildRunnerHelper is not API
 *
 * @author jan
 *
 */
public class AutoBuildRunnerHelper extends BuildRunnerHelper {

    @Override
    public synchronized  OutputStream getOutputStream() {
        // TODO Auto-generated method stub
        return super.getOutputStream();
    }

    @Override
    public synchronized  OutputStream getErrorStream() {
        // TODO Auto-generated method stub
        return super.getErrorStream();
    }

    @Override
    public void refreshProject(String configName, IProgressMonitor monitor) {
        // TODO Auto-generated method stub
        super.refreshProject(configName, monitor);
    }

    @Override
    public void greeting(int kind) {
        // TODO Auto-generated method stub
        super.greeting(kind);
    }

    @Override
    public void greeting(String kind, String cfgName, String toolchainName, boolean isSupported) {
        // TODO Auto-generated method stub
        super.greeting(kind, cfgName, toolchainName, isSupported);
    }

    @Override
    public void greeting(String msg) {
        // TODO Auto-generated method stub
        super.greeting(msg);
    }

    @Override
    public void printLine(String msg) {
        // TODO Auto-generated method stub
        super.printLine(msg);
    }

    @Override
    public void setLaunchParameters(ICommandLauncher launcher, IPath buildCommand, String[] args,
            URI workingDirectoryURI,String[] envp) {
        super.setLaunchParameters(launcher, buildCommand, args, workingDirectoryURI, envp);
    }

    @Override
    public void prepareStreams(ErrorParserManager epm, List<IConsoleParser> buildOutputParsers, IConsole con,
            IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        super.prepareStreams(epm, buildOutputParsers, con, monitor);
    }

    @Override
    public void greeting(int kind, String cfgName, String toolchainName, boolean isSupported) {
        // TODO Auto-generated method stub
        super.greeting(kind, cfgName, toolchainName, isSupported);
    }

    @Override
    public void removeOldMarkers(IResource rc, IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        super.removeOldMarkers(rc, monitor);
    }

    @Override
    public void goodbye() {
        // TODO Auto-generated method stub
        super.goodbye();
    }

    @Override
    public int build(IProgressMonitor monitor) throws CoreException, IOException {
        // TODO Auto-generated method stub
        return super.build(monitor);
    }

    @Override
    public void close() throws IOException {
        // TODO Auto-generated method stub
        super.close();
    }

    public AutoBuildRunnerHelper(IProject project) {
        super(project);
        // TODO Auto-generated constructor stub
    }

    public static String[] envMapToEnvp(Map<String, String> envMap) {
        return BuildRunnerHelper.envMapToEnvp(envMap);
    }

	/**
	 * Print a message to the console info output. Note that this message is colored
	 * with the color assigned to "Info" stream.
	 * @param msg - message to print.
	 */
	public synchronized  void toConsole(String msg) {
			try(OutputStream out=getOutputStream();) {
				out.write(msg.getBytes());
				out.write('\n');
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}
