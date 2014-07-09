# Arduino Eclipse Plugin

The Arduino Eclipse Plugin/IDE.

The Eclipse IDE (Integrated Developement Environment) is a full featured programming editor with many fantastic features to help you code more quickly and easily. The Arduino IDE is great for what it does, but it doesn't do much to when it comes to help writing, navigating or understanding your (and other people's) code.

The Arduino Eclipse Plugin bridges this gap and helps move you to a more powerful development environment whilst keeping the same support for Arduino hardware and it's libraries that you know and love.

This plugin works on Windows, Mac OSX and Linux.

## Quick installation

If you are a regular user and don't want to build from source, there are precompiled Eclipse version and update sites available. See the details at http://www.baeyens.it/eclipse/.

## Manual installation

You can manually build the Arduino plugin. This will also build a version of Eclipse that has the required plugins pre-installed.

### Prerequisites

Install [git] (http://git-scm.com/downloads) and [maven] (http://maven.apache.org/download.cgi).

### Build from source

```
git clone https://github.com/jantje/arduino-eclipse-plugin
cd arduino-eclipse-plugin
mvn verify
```

### Running the IDE

On windows, execute `win32x64.cmd` (64-bit) or `win32x32.cmd` (32-bit).

On Mac OSX and Linux, `cd` into the folder and run the following command:

```bash
./build_then_launch_plugin.sh
```

### Build options

You can control the maven build with the following profiles:
*   `luna` - builds against the luna repository (4.4)
*   `mars` - builds against the mars repository (4.5)
*   `win32` - builds for Windows (32-bit)
*   `win64` - builds for Windows (64-bit)
*   `linux32`- builds for Linux (32-bit)
*   `linux64` - builds for Linux (64-bit)
*   `mac32` - builds for Mac OSX (32-bit)
*   `mac64` - builds for Mac OSX (64-bit)


#### Examples:

```bash
mvn verify -Plinux32 # Builds for luna and Linux (32-bit)
mvn verify -Pwin32,mars,linux32 # Builds for mars, Linux (32-bit) and Windows (32-bit)
mvn verify # Builds for luna and your current platform
```

### Setting up a repository

The easiest way to share the plugin that you just build above among multiple new (or pre-existing) Eclipse instances is to create a local code repository. The build commands above will create the repository automatically in the local path (from the root of the build path) `it.baeyens.arduino.product/target/repository`.

You can add this to Eclipse through the *Install New Software* window, as seen below:

![Adding a local repository to Eclipse](images_plugin_dev_setup/add_local_repository.png "Adding a local repository")

## Developing the plugin

Firstly, fork the repository on GitHub (https://help.github.com/articles/fork-a-repo) so that you can make changes to the code.

### Adding the required plugins to your Eclipse environment

You should already have a supported version of Eclipse installed (Eclipse CDT is the recommended package).

#### Adding the Eclipse plugin development environment

Navigate to:
*Help* -> *Install New Software* -> *Work with:* -> *All Available Sites*

Now search for and select the package named *Eclipse Plug-in Development Environment*

Note: It may take a while for all available packages to be downloaded to your computer.

![Adding the eclipse plugin development environment](images_plugin_dev_setup/adding_pde.png "Adding the Plugin Development Environment")

#### Adding the Eclipse Java Development Tools

If you're not using Eclipse with the Java development tools installed, you'll need to install them. To do this you first need to open the dialog for installing new software:

Navigate to:
*Help* -> *Install New Software*

Then select:
*Work with:* -> *YOUR_ECLIPSE_RELEASE - http://download.eclipse.org/releases/YOUR_ECLIPSE_RELEASE*

Then you open:
*Programming Languages* -> *Eclipse Java Development Tools.*

#### Adding EGit - the Eclipse Git team provider

To install EGit you'll need to do the following:

Navigate to:
*Help* -> *Install New Software*

Enter the following URL and press ENTER.

````
http://download.eclipse.org/egit/updates
````

Now open up the **Eclipse Git Team Provider** category and select **Eclipse Git Team Provider**. Press next and follow the on-screen instructions.

### Importing the Arduino plugin project into Eclipse

After you have installed all the plugins, you'll need to restart Eclipse.

Now comes the time to get the plugin source code into a place you can use it. You will need to import the project into Eclipse. There are two ways to do that:

1.  **Via the command line**
    
    First, open a command line and change into the directory that you want to store the plugin source code in.
    
    Now clone **your** fork:
    
    ```bash
    git clone https://github.com/YOUR_FORK/arduino-eclipse-plugin.git
    ```
    
    Import the Project to Eclipse:
    
    Navigate to:
    *File* -> *Import* -> *Plug-in Development* -> *Plug-ins and Fragments*
    
    You should select the same options as the ones shown in the following picture:
    
    ![Adding the plugin to the development environment](images_plugin_dev_setup/plugins_import_config.png "Adding the Plugin Development Environment")
    
    **Note:** Change the path shown in the picture to the location where you stored the plugin's source code.
    
    Press the next button. On the next window you have to select which plugin fragments you want to import.
    Select the same fragments as shown in the picture below:
    
    ![Fragment selection window](images_plugin_dev_setup/plugins_select.png "Selecting the plugin gragments")
    
    Upon clicking the finish button, Eclipse will being importing the project.

2.  **Via the EGit interface**
    
    Navigate to:
    *File* -> *Import* -> *Git* -> *Projects from Git* -> *Clone URI*
    
    Now type in the path to your fork, for example:
    
    ```
    https://github.com/YOUR_FORK/arduino-eclipse-plugin.git
    ```

    Adjust the settings on the dialog to match the ones below:

    *Branch Selection*: master
    
    *Local destination*: `/home/your_name/git/arduino-eclipse-plugin`
    or `c:\git\arduino-eclipse-plugin`
    
    Click the next button. On the next page, choose the following option: 
    *Select a wizard: Import Existing Projects*
    
    Click the next button again and finally select your projects to be imported.

Your environment should now be fully setup to develop the Arduino Eclipse plugin. Your environment should look similar to the following:

![Final Eclipse workspace](images_plugin_dev_setup/Imported_projects.png "Projects imported")

### Set the code formatting
To avoid having changes all the time because of different formatting this project contains a formatting xml.
Navigate to:
*Window* -> *Preferences* -> *Java* -> *Code Style* -> *Formatter and import code* and select the `format.xml` from the root of the repository.

### Set the warning level

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

### Running the plugin

Then running is very simple - just right click it.bayaens.arduino.core and select *run as -> Eclipse Application* (or *debug as -> Eclipse Application* -- letting you set breakpoints)

Eclipse will launch a new workbench disabling the installed version if any of the plugin and updating with the plugins in the current workspace.

images_plugin_dev_setup/running_check_versions.png

Now, just set up fresh again with your project settings:
Preferences/Arduino to point to IDE and private libs

New Project, Arduino, New Arduino Sketch

All should work. You can set breakpoints in the launching Eclipse if you ran as debug. Happy developing!

<a href="http://with-eclipse.github.io/" target="_blank"><img alt="with-Eclipse logo" src="http://with-eclipse.github.io/with-eclipse-1.jpg" /></a>
