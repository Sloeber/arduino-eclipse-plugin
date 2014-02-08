#Arduino-Eclipse-Plugin

The Arduino Eclipse Plugin/IDE.

The Eclipse IDE (Integrated Developement Environment) is a full featured programming editor with many fantastic features to help you code more quickly and easily. The Arduino IDE is great for what it does -- but it doesn't do much to help writing, navigating and understanding your (and other people's) code.

The Arduino Eclipse Plugin bridges that gap and helps move you to a more powerful development environment whilst keeping the Arduino hardware and libraries that you love (and make life simple).

It works on MS Windows, Mac OSX and Linux.

##Are you a regular user?
If you are a regular user and don't want to build from source then there are precompiled product packages and update sites available. See the details at http://www.baeyens.it/eclipse/.

##Quick Installation 
###Prerequisites

Install [git] (http://git-scm.com/downloads) and [maven] (http://maven.apache.org/download.cgi)

###Build from source

git clone https://github.com/jantje/arduino-eclipse-plugin

cd arduino-eclipse-plugin

mvn verify

execute eclipse; for instance on 64 bit mac with 
open it.baeyens.arduino.product/target/products/it.baeyens.arduino.product/macosx/cocoa/x86_64/eclipseArduino/Eclipse.app

for Linux you can run the verify and start eclipse in on command:

./build_then_launch_plugin.sh


###Running the IDE/Plugin

Windows:
win32x64.cmd (if you are on 64 bits windows)

win32x32.cmd (if you are on 32 bits windows)

./build_then_launch_plugin.sh (Mac OSX and Linux)


=======
##Build Options
You can control the maven build with the following profiles:
* juno (default) (builds against the juno repositories (4.2))
* kepler (builds agains the kepler repositories (4.3))
* luna (builds agains the luna repositories (4.4))
* win32 (builds for 32 bit windows)
* win64 
* linux32
* linux64
* mac32
* mac64


#####Examples:

mvn verify -Plinux32,kepler

mvn verify -Pwin32,juno,linux32

mvn verify (builds for juno and the platform you are running on)


###Setting up a repository

If you want to import the latest code based plugin to another Eclipse setup you have then it is possible to setup a local repository to install the plugin you have just built. Just add a local respoitory with location "arduino-eclipse-plugin/it.baeyens.arduino.product/target/repository"

![alt text](images_plugin_dev_setup/add_local_repository.png "Adding a local repository")

##Developing (Improving) the Plugin

Fork the repository on GitHub (https://help.github.com/articles/fork-a-repo) for your changes.

Note that your git link should look like this: https://github.com/YOUR_FORK/arduino-eclipse-plugin.git -- we will use it later.

###Add needed plugins/features into the Eclipse Environment

You should already have a supported Eclipse version installed (the CDT package makes a good start point). Let's add:

####Add Eclipse Plugin Development Environment

Help-> Install New Software -> Work with: -> All Available Sites

Now search/select the *Eclipse Plug-in Development Environment*

Note: This may take a while to download all the available packages.

![alt text](images_plugin_dev_setup/adding_pde.png "Adding the Plugin Development Environment")


####Add Eclipse Java Development Tools

If you're not using Eclipse with the JDT you'll need to install them. To do this you first need to open the Dialog for installing new Software:

*Help*-> *Install New Software*

There you select for *Work with:*  *YOUR_ECLIPSE_RELEASE - http://download.eclipse.org/releases/YOUR_ECLIPSE_RELEASE*

There you open:
*Programming Languages -> Eclipse Java Development Tools.*

####Add EGit - Eclipse Git Team Provider

To install EGit you'll need to do the following:

*Help*-> *Install New Software*

There have to enter the following URL and press ENTER.

 *http://download.eclipse.org/egit/updates*

Now you have to open up the **Eclipse Git Team Provider** Category and select **Eclipse Git Team Provider** , now you just have to press next and do what the dialog tells you to do.

<!-- Eclipse GitHub integration with task focused interface -->



###Importing the Arduino Plugin Project into Eclipse

After you installed all the plugins you'll need to restart Eclipse to use them.


Then comes time to get the plugin source code in a place you can use. You will need to import the projects into Eclipse, there are two ways to do that:

####1)Via command line


If you're using Windows you should first install [GitHub for Windows](http://windows.github.com/).

First you should open a command line, and change the directory to the directory where you want to store your Project.

Now you have to clone your Fork:

*git clone https://github.com/YOUR_FORK/arduino-eclipse-plugin.git*

After that you should import the Project to Eclipse:

*File -> Import -> Plug-in Development -> Plug-ins and Fragments*

You should select all as shown in this picture.


![alt text](images_plugin_dev_setup/plugins_import_config.png "Adding the Plugin Development Environment")

Of cource you should change the directory to the directory where you cloned the project to.

Press Next.

In the next window you have to select which Plug-in fragments you want to import.
You will select the following:

![alt text](images_plugin_dev_setup/plugins_select.png "Adding the Plugin Development Environment")


Now press Finish, and it should import the selected Projects.


####2)Via EGit interface.

*File -> Import -> Git -> Projects from Git -> Clone URI*

Now type your fork in to URI, for example:

*https://github.com/YOUR_FORK/arduino-eclipse-plugin.git*

Next

Branch Selection: master

Local destination: /home/your_name/git/arduino-eclipse-plugin
or c:\git\arduino-eclipse-plugin

Rest of page unchanged.

Next.

Select a wizard: Import Existing Projects

Next.

Import Projects

Select all the projects

FINISH

After all it should look like this: 
![alt text](images_plugin_dev_setup/Imported_projects.png "Projects imported")

###Set the code formatting
To avoid having changes all the time because of different formatting this project contains a formatting xml.
Go to Window->Preferences->Java->Code Style->Formatter and import codeformat.xml from the root of the repo.

###Set the warning level
We want to keep the chance of missing a problem in the code to a minimum and to keep clean and tidy code. Development is aiming to keep compiler warnings to a minimum (items that show up in the Problems tab under Warnings) with specific settings. Please change your settings from default as follows:

Go to Window->Preferences->Java->Compiler->Errors/Warnings and change the following from their defaults.

My current settings are as follows:
![alt text](images_plugin_dev_setup/Screenshot-Preferences 1.png "screen capture")
![alt text](images_plugin_dev_setup/Screenshot-Preferences 2.png "screen capture")
![alt text](images_plugin_dev_setup/Screenshot-Preferences 3.png "screen capture")
![alt text](images_plugin_dev_setup/Screenshot-Preferences 4.png "screen capture")
![alt text](images_plugin_dev_setup/Screenshot-Preferences 5.png "screen capture")

Name shadowing and conflicts. Set all to Warning.
Unnecessary code. Set all to Warning.
Null analysis. Set all active (not greyed out) to Warning.
Potential programming problems. Set all to Warning.



###Running the plugin

Then running is very simple - just right click it.bayaens.arduino.core and select *run as -> Eclipse Application* (or *debug as -> Eclipse Application* -- letting you set breakpoints)

Eclipse will launch a new workbench disabling the installed version if any of the plugin and updating with the plugins in the current workspace.

images_plugin_dev_setup/running_check_versions.png


Now, just set up fresh again with your project settings:
Preferences/Arduino to point to IDE and private libs

New Project, Arduino, New Arduino Sketch

All should work. You can set breakpoints in the launching Eclipse if you ran as debug. Happy developing!

<a href="http://with-eclipse.github.io/" target="_blank"><img alt="with-Eclipse logo" src="http://with-eclipse.github.io/with-eclipse-1.jpg" /></a>
