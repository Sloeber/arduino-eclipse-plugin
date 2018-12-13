# Sloeber, the Arduino IDE for Eclipse                   <img style="float: right" src="https://avatars2.githubusercontent.com/u/25158881?v=3&s=200" height="50"/>

The Eclipse IDE (Integrated Developement Environment) is a full featured programming editor with many fantastic features to help you code more quickly and easily. The Arduino IDE is great for what it does – but it doesn't do much to help writing, navigating and understanding your (and other people's) code.

The Sloeber IDE bridges that gap and helps move you to a more powerful development environment whilst keeping the Arduino hardware and libraries that you love (and make life simple).

    Software Logic to program Open Electronic Boards in an Eclipse Runtime (SLOEBER)

It works on MS Windows, Mac OSX and Linux.

# Support us
We need your support to continue developing this software. This is the truth.

If you use this software professionally then we request your help. [Send some of the cash you make our way](http://eclipse.baeyens.it/donate.shtml).

If you use this software as a hobbiest [then how about $5 monthly?  :kissing_heart:](http://eclipse.baeyens.it/donate.shtml) 

# Downloads
If you are not a developer and don't want to build from sources, then there are precompiled product packages and update sites available. See the details at http://sloeber.io/.

# Build from source
Below are instructions on how to download and compile the source code from the command line and from eclipse.
You only need to do one.

Contributors (people that help us build Sloeber) may join [our Slack Channel](https://sloeber.slack.com/).

## Prerequisites
Please install [git] (http://git-scm.com/downloads) and [maven] (http://maven.apache.org/download.cgi).

## Build from the command line from source for your os and the default eclipse instance
```bash
git clone https://github.com/jantje/arduino-eclipse-plugin
cd arduino-eclipse-plugin
mvn clean verify -DskipTests=true
```

## Running the Sloeber you just build

Windows

 * win32x64.cmd (if you are on 64 bits windows)
 * win32x32.cmd (if you are on 32 bits windows)

Mac OSX and Linux

 * ./build_then_launch_plugin.sh


## Build Options

You can control the maven build with the following profiles:

* latest (default, builds against the latest versions)
* 2018-09 (builds against the 2018-09 release. Eclipse stopped naming their releases)
* photon (builds against the photon (4.8) repositories) 
* oxygen (builds against the oxygen (4.7) repositories)
* SDK (builds a Sloeber you can program Sloeber in. With Java.)
* win32 (builds for 32 bit windows)
* win64
* linux64
* mac64

### Examples
    mvn clean verify -Plinux32,latest -DskipTests=true (builds for neon and linux 32 bits)
    mvn clean verify -PSDK,latest -DskipTests=true (builds the Sloeber SDK. For Sloeber programmers.)
    mvn clean verify -P2018-09,linux64 -DskipTests=true (builds against 2018-09 and produces linux64 product) 
    
To build for latest and the platform you are running on:

    mvn clean verify -DskipTests=true

# Importing your build into another Eclipse
If you want to import the latest code based plugin to another Eclipse setup you have then it is possible to setup a local repository to install the plugin you have just built. Just add a local repository with location ```arduino-eclipse-plugin/io.sloeber.product/target/repository```

![alt text](images_plugin_dev_setup/add_local_repository.png "Adding a local repository")

# Developing (Improving) the Plugin
 * Fork the repository on GitHub (https://help.github.com/articles/fork-a-repo) for your changes. Note that your git link should look like this: https://github.com/YOUR_FORK/arduino-eclipse-plugin.git –– we will use it later.
 * Checkout locally
 * Run ```mvn clean verify -PSDK,latest -DskipTests=true``` to build

After the build, find the Sloeber SDK product in the io.sloeber.product.sdk target directory. Unzip it somewhere in your home directory (mind you we cannot handle very long path names)

    Note that Sloeber itself is NOT included in the Sloeber SDK. 


## Install the projects into the SDK via the EGit interface.

> File → Import → Git → Projects from Git → Existing local repository

* Now add the repository you just cloned 
* Press "Next".
* Next
* Select a wizard: Import Existing Projects
* Next
* Import Projects
* Select all the projects
* Finish

After all it should look like this:

![alt text](images_plugin_dev_setup/Imported_projects.png "Projects imported")

## Set the Warning Level

We want to keep the chance of missing a problem in the code to a minimum and to keep clean and tidy code. Development is
aiming to keep compiler warnings to a minimum (items that show up in the Problems tab under Warnings) with specific settings.
Please change your settings from default as follows:

Go to

> Window → Preferences → Java → Compiler → Errors/Warnings

and change the following from their defaults.

My current settings are as follows:

![alt text](images_plugin_dev_setup/Screenshot-Preferences1.png "screen capture")
![alt text](images_plugin_dev_setup/Screenshot-Preferences 2.png "screen capture")
![alt text](images_plugin_dev_setup/Screenshot-Preferences 3.png "screen capture")
![alt text](images_plugin_dev_setup/Screenshot-Preferences 4.png "screen capture")
![alt text](images_plugin_dev_setup/Screenshot-Preferences 5.png "screen capture")

 * Name shadowing and conflicts: Set all to Warning.
 * Unnecessary code: Set all to Warning.
 * Null analysis: Set all active (not greyed out) to Warning.
 * Potential programming problems: Set all to Warning.

### Running Sloeber with local changes (Testing your stuff)
Running is very simple - just right click io.sloeber.core and select:

> Run as → Eclipse Application

OR, if you'd like to debug,

> Debug as → Eclipse Application

Eclipse will launch a new workbench disabling the installed version if any, and updating it with the plugin version loaded in the current workspace.

![](images_plugin_dev_setup/running_check_versions.png "screen capture")

Now, just set up fresh again with your project settings, Preferences/Arduino, to point to IDE and private libs.

> New Project → Arduino → New Arduino Sketch

All should work. You can set breakpoints in the launching Eclipse if you ran as debug. Happy developing!

[<img border="0" style="border-width: 0px" src="http://with-eclipse.github.io/with-eclipse-1.jpg">](http://with-eclipse.github.io/)

# FAQ
## There are lots of issues in the release that seem fixed.
We close issues when they have been validated as part of the nightly. Therefore the open list no longer contains items fixed in the nightly. Known issue fixed in the last nightly can be found with this query:
is:issue is:closed -label:"status: fixed in nightly"
