
Add stuff here on normal usage



-------------------------- Developing (Improving) the Plugin --------------------------

Add Eclipse Plugin development environment into your Eclipse environment



Add Software:

Add in PDE

Search for Plug-in
Under general tools

images_plugin_dev_setup/adding_pde.png

For completeness
Add in the JDT
Add in EGit



go to your eclipse workspaces directory and get the plugin code from github:

git clone  https://github.com/YOUR_FORK/arduino-eclipse-plugin.git
git remote add upstream https://github.com/jantje/arduino-eclipse-plugin.git


Start up Eclipse:

import, select, plugins, Import Plug-ins and Fragments

images_plugin_dev_setup/import_select.png

Import Plug-ins and Fragments

Select workspace/arduino----- directory

Select from all -- unchanged

projects with source folders

images_plugin_dev_setup/plugins_import_config.png


import 3 newer projects -- all except core.nl1(OLD) version 

images_plugin_dev_setup/plugins_select.png

looks like this after it all works:
images_plugin_dev_setup/imported_projects.png


TODO: Work out how to clear up this structure and git -- the project files are copied by default....


Then running is very simple - just right clock any of the projects and select run as an Eclipse application (or debug as Eclipse application -- letting you set breakpoints)

Eclipse will launch a new workbench disabling the installed version if any of the plugin and updating with the plugins in the current workspace.


images_plugin_dev_setup/running_check_versions.png


Now, just set up fresh again with your project settings:
Preferences/Arduino to point to IDE and private libs

New Project, Arduino, New Arduino Sketch

TODO: Issues

