Keeping the Eclipse project compatible with the IDE (post .ino)  
===============================================================

Now that the plugin supports `.ino` files the plugin is 95% compatible with Arduino sketches created with the Arduino IDE.  
But how do you make sure the sketches build in the plugin can also be build with the Arduino IDE?  
There are a couple of rules you should keep in mind:

 1. The Arduino IDE has no *imported libraries* as in the plugin does: the Arduino IDE decides on the *imported libraries* by looking at the includes in the `.ino` files only. 
 2. Do not use the *compilation settings* functionality in the Eclipse plugin as the Arduino IDE doesn't provide support for those *advanced* settings. In other words, make sure all compiles fine with default defines.
 
Please read [this blog post](http://blog.baeyens.it/#post13) if you want to know more about the implementation of `.ino` files in the plugin.
 
