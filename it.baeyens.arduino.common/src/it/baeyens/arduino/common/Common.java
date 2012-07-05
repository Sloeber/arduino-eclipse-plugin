package it.baeyens.arduino.common;

import it.baeyens.arduino.arduino.Serial;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

public class Common extends ArduinoInstancePreferences
	{
		private static boolean RXTXLibraryLoaded =false;
		// /**
		// * GetAvrDudeComPortPrefix is used to determine the prefix to be used with
		// * the com port.<br/>
		// * Avr dude delivered in WinAVR does not need a prefix.<br/>
		// * Avr dude delivered with Arduino needs a prefix <br/>
		// * This code checks the flag "use ide settings" that can be set in the
		// * preferences and when set this method will return a prefix.<br/>
		// * In all other cases a empty string will be returned.
		// *
		// * @return The prefix needed for the com port for AvrDude
		// */
		// public static String GetAvrDudeComPortPrefix() {
		// if (ArduinoPreferences.getUseIDESettings())
		// return "";
		// return "";
		// }

		static ISerialUser OtherSerialUser = null; // If someone else uses the
																								// serial port he can register
																								// here so we can
																								// request him to disconnect
																								// when we need the serial port

		public static void registerSerialUser(ISerialUser SerialUser)
			{
				OtherSerialUser = SerialUser;
			}

		public static void UnRegisterSerialUser()
			{
				OtherSerialUser = null;
			}

		/**
		 * This method makes sure that a string can be used as a file or folder name<br/>
		 * To do this it replaces all unacceptable characters with underscores.<br/>
		 * Currently it replaces (based on http://en.wikipedia.org/wiki/Filename ) /
		 * slash used as a path name component separator in Unix-like, Windows, and
		 * Amiga systems. (The MS-DOS command.com shell would consume it as a switch
		 * character, but Windows itself always accepts it as a
		 * separator.[6][vague]) \ backslash Also used as a path name component
		 * separator in MS-DOS, OS/2 and Windows (where there are few differences
		 * between slash and backslash); allowed in Unix filenames, see Note 1 ?
		 * question mark used as a wildcard in Unix, Windows and AmigaOS; marks a
		 * single character. Allowed in Unix filenames, see Note 1 % percent used as
		 * a wildcard in RT-11; marks a single character. asterisk or star used as a
		 * wildcard in Unix, MS-DOS, RT-11, VMS and Windows. Marks any sequence of
		 * characters (Unix, Windows, later versions of MS-DOS) or any sequence of
		 * characters in either the basename or extension (thus "*.*" in early
		 * versions of MS-DOS means "all files". Allowed in Unix filenames, see note
		 * 1 : colon used to determine the mount point / drive on Windows; used to
		 * determine the virtual device or physical device such as a drive on
		 * AmigaOS, RT-11 and VMS; used as a pathname separator in classic Mac OS.
		 * Doubled after a name on VMS, indicates the DECnet nodename (equivalent to
		 * a NetBIOS (Windows networking) hostname preceded by "\\".) | vertical bar
		 * or pipe designates software pipelining in Unix and Windows; allowed in
		 * Unix filenames, see Note 1 " quote used to mark beginning and end of
		 * filenames containing spaces in Windows, see Note 1 < less than used to
		 * redirect input, allowed in Unix filenames, see Note 1 > greater than used
		 * to redirect output, allowed in Unix filenames, see Note 1 . period or dot
		 * 
		 * @param Name
		 *          the string that needs to be checked
		 * @return a name safe to create files or folders
		 */
		public static String MakeNameCompileSafe(String Name)
			{
				return Name.trim().replace(" ", "_").replace("/", "_").replace("\\", "_").replace("(", "_").replace(")", "_").replace("*", "_").replace("?", "_").replace("%", "_").replace(".", "_")
						.replace(":", "_").replace("|", "_").replace("<", "_").replace(">", "_").replace(",", "_").replace("\"", "_");
			}

		// public static String getPersistentProperty(String Tag)
		// {
		// return getPersistentProperty(getProject(), Tag);
		// }
		/**
		 * Gets a persistent project property
		 * 
		 * @param project
		 *          The project for which the property is needed
		 * 
		 * @param Tag
		 *          The tag identifying the property to read
		 * @return returns the property when found. When not found returns an empty
		 *         string
		 */
		public static String getPersistentProperty(IProject project, String Tag)
			{
				try
					{
						String sret = project.getPersistentProperty(new QualifiedName("", Tag));
						if (sret == null)
							sret = "";
						return sret;
					} catch (CoreException e)
					{
						log(new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to read persistent setting " + Tag, e));
						// e.printStackTrace();
						return "";
					}
			}

		/**
		 * Logs the status information
		 * 
		 * @param status
		 *          the status information to log
		 */
		public static void log(IStatus status)
			{
				int style = StatusManager.LOG;

				if (status.getSeverity() == IStatus.ERROR)
					{
						style = StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK;
						StatusManager stMan = StatusManager.getManager();
						stMan.handle(status, style);
					} else
					{
						Activator.getDefault().getLog().log(status);
					}

			}

		/**
		 * Given a project name give back the project itself
		 * 
		 * @param Projectname
		 *          The name of the project you want to find
		 * @return The project if found. Else null
		 */
		public static IProject findProjectByName(String Projectname)
			{
				IProject AllProjects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				for (int CurProject = AllProjects.length - 1; CurProject >= 0; --CurProject)
					{
						if (AllProjects[CurProject].getName().equals(Projectname))
							return AllProjects[CurProject];
					}
				return null;
			}

		/**
		 * This method returns the dude config suffix which is dependent on the
		 * platform
		 * 
		 * @return dude config suffix
		 */
		public static String DUDE_CONFIG_SUFFIX()
			{
				if (Platform.getOS().equals(Platform.OS_WIN32))
					return DUDE_CONFIG_SUFFIX_WIN;
				if (Platform.getOS().equals(Platform.OS_LINUX))
					return DUDE_CONFIG_SUFFIX_LINUX;
				if (Platform.getOS().equals(Platform.OS_MACOSX))
					return DUDE_CONFIG_SUFFIX_MACOSX;
				Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null));
				return DUDE_CONFIG_SUFFIX_WIN;
			}

		/**
		 * This method returns the GNU Path suffix which is dependent on the
		 * platform
		 * 
		 * @return dude config suffix
		 */
		public static String GNU_PATH_SUFFIX()
			{
				if (Platform.getOS().equals(Platform.OS_WIN32))
					return GNU_PATH_SUFFIX_WIN;
				if (Platform.getOS().equals(Platform.OS_LINUX))
					return GNU_PATH_SUFFIX_LINUX;
				if (Platform.getOS().equals(Platform.OS_MACOSX))
					return GNU_PATH_SUFFIX_MACOSX;
				Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null));
				return GNU_PATH_SUFFIX_WIN;
			}

		/**
		 * This method returns the avrdude path suffix which is dependent on the
		 * platform
		 * 
		 * @return avrdude path suffix
		 */
		public static String AVRDUDE_PATH_SUFFIX()
			{
				if (Platform.getOS().equals(Platform.OS_WIN32))
					return AVRDUDE_PATH_SUFFIX_WIN;
				if (Platform.getOS().equals(Platform.OS_LINUX))
					return AVRDUDE_PATH_SUFFIX_LINUX;
				if (Platform.getOS().equals(Platform.OS_MACOSX))
					return AVRDUDE_PATH_SUFFIX_MAC;
				Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null));
				return AVRDUDE_PATH_SUFFIX_WIN;
			}

		/**
		 * This method returns the avrdude upload port prefix which is dependent on
		 * the platform
		 * 
		 * @return avrdude upload port prefix
		 */
		public static String UploadPortPrefix()
			{
				if (Platform.getOS().equals(Platform.OS_WIN32))
					return UploadPortPrefix_WIN;
				if (Platform.getOS().equals(Platform.OS_LINUX))
					return UploadPortPrefix_LINUX;
				if (Platform.getOS().equals(Platform.OS_MACOSX))
					return UploadPortPrefix_MAC;
				Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null));
				return UploadPortPrefix_WIN;
			}

		// /**
		// * Returns the default programmer name
		// *
		// * This is called the upload.protocol in the board.txt
		// *
		// * @return the default programmer
		// */
		// public static String DefaultProgrammerName() {
		// if (isArduinoIdeOne())
		// {
		// return ProgrammerNameOne;
		// }
		// return ProgrammerName;
		// }
		//

		/**
		 * ToInt converts a string to a integer in a save way
		 * 
		 * @param Number
		 *          is a String that will be converted to an integer. Number can be
		 *          null or empty and can contain leading and trailing white space
		 * @return The integer value represented in the string based on parseInt
		 * @see parseInt. After error checking and modifications parseInt is used
		 *      for the conversion
		 **/
		public static int ToInt(String Number)
			{
				if (Number == null)
					return 0;
				if (Number.equals(""))
					return 0;
				return Integer.parseInt(Number.trim());
			}

		/**
		 * Converts a selection object to a IProject object.
		 * 
		 * @param selection
		 * @return the first project in the selection or NULL if failed
		 */
		public static IProject getProject(ISelection selection)
			{

				// The user has selected a different Workbench object.
				// If it is an IProject we keep it.

				Object item;

				if (selection instanceof IStructuredSelection)
					{
						item = ((IStructuredSelection) selection).getFirstElement();
					} else
					{
						return null;
					}
				if (item == null)
					{
						return null;
					}
				return getProject(item);
			}

		/**
		 * Converts a object to a project if it is related to a project
		 * 
		 * @param item
		 *          an object that one way or anther refers to a IProject
		 * @return the referred project or an iproject
		 */
		public static IProject getProject(Object item)
			{
				// See if the given is an IProject (directly or via IAdaptable)
				if (item instanceof IProject)
					{
						return (IProject) item;
					} else if (item instanceof IResource)
					{
						return ((IResource) item).getProject();
					} else if (item instanceof IAdaptable)
					{
						IAdaptable adaptable = (IAdaptable) item;
						IProject project = (IProject) adaptable.getAdapter(IProject.class);
						if (project != null)
							{
								return project;
							}
						// Try ICProject -> IProject
						ICProject cproject = (ICProject) adaptable.getAdapter(ICProject.class);
						if (cproject == null)
							{
								// Try ICElement -> ICProject -> IProject
								ICElement celement = (ICElement) adaptable.getAdapter(ICElement.class);
								if (celement != null)
									{
										cproject = celement.getCProject();
									}
							}
						if (cproject != null)
							{
								return cproject.getProject();
							}
					}
				return null;
			}

		public static void ResetArduino(String ComPort, int UploadSpeed)
			{
				if (RXTXDisabled())
					return;

				// Cleanup the serial buffer
				Serial serialPort;
				try
					{
						serialPort = new Serial(ComPort, UploadSpeed);
					} catch (Exception e)
					{
						e.printStackTrace();
						Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unable to open Serial port " + ComPort, e));
						return;
						// throw new RunnerException(e.getMessage());
					}
				@SuppressWarnings("unused")
				byte[] readBuffer;
				while (serialPort.available() > 0)
					{
						readBuffer = serialPort.readBytes();
						try
							{
								Thread.sleep(100);
							} catch (InterruptedException e)
							{
							}
					}

				serialPort.setDTR(false);
				serialPort.setRTS(false);

				try
					{
						Thread.sleep(100);
					} catch (InterruptedException e)
					{
					}

				serialPort.setDTR(true);
				serialPort.setRTS(true);

				serialPort.dispose();

			}

		public static boolean RXTXDisabled()
			{
				return getGlobalBoolean(KEY_RXTXDISABLED);
			}

		public static Boolean StopSerialMonitor(String mComPort)
			{
				if (RXTXDisabled())
					return false;
				if (OtherSerialUser != null)
					{
						return OtherSerialUser.PauzePort(mComPort);
					}
				return false;
			}

		public static void StartSerialMonitor(String mComPort)
			{
				if (RXTXDisabled())
					return;
				if (OtherSerialUser != null)
					{
						OtherSerialUser.ResumePort(mComPort);
					}
				// TODO Auto-generated method stub

			}

		public static String[] listComPorts()
			{
				if (RXTXDisabled())
					return new String[0];
				return Serial.list();
			}

		public static boolean LoadRXTX()
			{
				if (RXTXLibraryLoaded) return true;
				return RXTXLibraryLoaded = LoadRXTX(null);
			}

		public static boolean LoadRXTX(Shell parent)
			{
				String OsInfo = "\nOs =" +Platform.getOS()+ " Os Architecture =" +Platform.getOSArch();
				try
					{
						// try to load the arduino DLL
						System.load(GetSerialFullDllName());
					} catch (Error e1)
					{
						try
							{
								// Log the Stack trace
								e1.printStackTrace();
								// Try to load the dll delivered with the plugin
								System.loadLibrary(GetSerialDllName());
							} catch (Error e2)
							{
								String FailMessage = "Failed to load Arduino IDE delivered rxtx library (" + GetSerialFullDllName() + ") " + e1.getMessage(); 
								FailMessage		+= "\nFailed to load Eclipse plugin delivered rxtx library (" + GetSerialDllName()	+ ") " +  e2.getMessage();
								FailMessage		+= OsInfo;
								FailMessage		+= "\nSee Error view for more info.";
								int FailErrorCode = IStatus.WARNING;
								if (parent != null)
									{
										MessageBox Failed = new MessageBox(parent, SWT.ICON_ERROR | SWT.OK);
										Failed.setText("Failed to load rxtx dll!");
										Failed.setMessage(FailMessage);
										Failed.open();
									} else
									{
										if (!RXTXDisabled())
											{
												FailErrorCode = IStatus.ERROR;
											}
									}
								Common.log(new Status(FailErrorCode, Common.CORE_PLUGIN_ID, FailMessage, e2));
								e2.printStackTrace();
								return false;
							}
						if (parent != null)
							{
								MessageBox SuccessBox = new MessageBox(parent, SWT.ICON_WORKING | SWT.OK);
								SuccessBox.setText("Succesfully loaded rxtx dll!");
								SuccessBox.setMessage("Eclipse plugin delivered rxtx library (" + GetSerialDllName() + ") has been loaded successfully" +OsInfo);
								SuccessBox.open();
							}
						return true; // Succeeded in loading the eclipse delivered dll
					}
				if (parent != null)
					{
						MessageBox SuccessBox = new MessageBox(parent, SWT.ICON_WORKING | SWT.OK);
						SuccessBox.setText("Succesfully loaded rxtx dll!");
						SuccessBox.setMessage("Arduino IDE delivered rxtx library (" + GetSerialFullDllName() + ") has been loaded successfully" + OsInfo);
						SuccessBox.open();
					}
				return true; // Succeeded in loading the Arduino IDE delivered dll
			}

		/**
		 * This method reads the full dll name of the rxtxSerial.dll delivered with
		 * the Arduino IDE
		 * 
		 * @return the arduino path
		 * @author Jan Baeyens
		 */
		private static String GetSerialFullDllName()
			{
				if (Platform.getOS().equals(Platform.OS_WIN32))
					{
						return getArduinoPath() + "/rxtxSerial.dll";
					}
				if (Platform.getOS().equals(Platform.OS_LINUX))
					{
						if (Platform.getOSArch().equals(Platform.ARCH_IA64) || Platform.getOSArch().equals(Platform.ARCH_X86_64))
							return getArduinoPath() +  "/lib/librxtxSerial64.so";
						return getArduinoPath() + "/lib/librxtxSerial.so";						
					}
				if (Platform.getOS().equals(Platform.OS_MACOSX))
					return getArduinoPath() + "/contents/Resources/java/LibrtxSerial.jnilib";
				Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system for serial functionality", null));
				return getArduinoPath() + "/rxtxSerial.dll";
			}

		private static String GetSerialDllName()
			{
				// TODO Auto-generated method stub
				if (Platform.getOS().equals(Platform.OS_WIN32))
					{
						if (Platform.getOSArch().equals(Platform.ARCH_X86_64))
							{
								return "rxtxSerial64";
							}
						return "rxtxSerial";
					}
				if (Platform.getOS().equals(Platform.OS_LINUX))
					{
						if (Platform.getOSArch().equals(Platform.ARCH_IA64))
							return "rxtxSerial";
						if (Platform.getOSArch().equals(Platform.ARCH_X86))
							return "rxtxSerial";
						if (Platform.getOSArch().equals(Platform.ARCH_X86_64))
							return "rxtxSerialx86_64";
					}
				if (Platform.getOS().equals(Platform.OS_MACOSX))
					return "rxtxSerial";
				Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system for serial functionality", null));
				return "rxtxSerial";
			}

		public static String[] listBaudRates()
			{
				String outgoing[] =
					{ "115200", "57600", "38400", "31250", "28800", "19200", "14400", "9600", "4800", "2400", "1200", "300" };
				return outgoing;
			}

		public static Object listLineEndings()
			{
				String outgoing[] =
					{ "none", "CR", "NL", "NL/CR" };
				return outgoing;
			}

		public static String getLineEnding(int selectionIndex)
			{
				switch (selectionIndex)
					{
					case 0:
						return "";
					case 1:
						return "\r";
					case 2:
						return "\n";
					case 3:
						return "\r\n";
					}
				return "";
			}

	}
