Keeping the eclipse project compatible with the IDE post ino  
==

Now that the plugin supports ino files the plugin is 95% compatible with arduino sketches created with the arduino IDE.  
But how do you make sure the sketches build in the plugin can also be build with the arduino IDE?  
There are a couple of rules you should keep in mind:

 1. The arduino IDE has no "imported libraries" as in the plugin. The arduino IDE decides on the "imported libraries" by looking at the includes in the .ino files only. 
 2. Do not use the "compilation settings" functionality in the eclipse plugin. In other words make sure all compiles fine with default defines.
 
 [If you want to know more about the implementation of ino files in the plugin.](http://blog.baeyens.it/#post13)
 
