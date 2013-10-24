#Arduino-Eclipse-Plugin

This plugin helps ease you into the world of Eclipse from Arduino. The Eclipse IDE (integrated developement Environment) is a full featured programming editor with many fantastic features to help you code more quickly and easily. The Arduino IDE is great for what it does -- but it doesn't do much to make writing your code easier.

It works on MS Windows, Mac OSX and Linux.

TODO: More on the plugin.

##Installing the Plugin

See http://www.baeyens.it/eclipse/Install.html

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
Go to window->preferences->java->Code style->Formatter import codeformat.xml in the root of the repo.

###Running the plugin

Then running is very simple - just right click it.bayaens.arduino.core and select *run as -> Eclipse Application* (or *debug as -> Eclipse Application* -- letting you set breakpoints)

Eclipse will launch a new workbench disabling the installed version if any of the plugin and updating with the plugins in the current workspace.

images_plugin_dev_setup/running_check_versions.png


Now, just set up fresh again with your project settings:
Preferences/Arduino to point to IDE and private libs

New Project, Arduino, New Arduino Sketch

All should work. You can set breakpoints in the launching Eclipse if you ran as debug. Happy developing!

