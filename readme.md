
Add stuff here on normal usage



-------------------------- Developing (Improving) the Plugin --------------------------

Fork the repository on github. Get you link like: https://github.com/YOUR_FORK/arduino-eclipse-plugin.git


Add Eclipse Plugin development environment into your Eclipse environment



Add Software:

Add in PDE

Search for Plug-in
Under general tools

images_plugin_dev_setup/adding_pde.png

For completeness
Add in the JDT
Add in EGit


Start up Eclipse:

Import the projects into Eclipse via EGit interface.

Import, Git, Projects from Git

https://github.com/YOUR_FORK/arduino-eclipse-plugin.git



import, select, plugins, Import Plug-ins and Fragments

images_plugin_dev_setup/import_select.png

Import Plug-ins and Fragments

URI

https://github.com/YOUR_FORK/arduino-eclipse-plugin.git

master

import all existing projects

FINISH

looks something like this after it all works:
images_plugin_dev_setup/imported_projects.png




Then running is very simple - just right clock it.bayaens.arduino.core and select run as an Eclipse application (or debug as Eclipse application -- letting you set breakpoints)

Eclipse will launch a new workbench disabling the installed version if any of the plugin and updating with the plugins in the current workspace.


images_plugin_dev_setup/running_check_versions.png


Now, just set up fresh again with your project settings:
Preferences/Arduino to point to IDE and private libs

New Project, Arduino, New Arduino Sketch

TODO: Issues

