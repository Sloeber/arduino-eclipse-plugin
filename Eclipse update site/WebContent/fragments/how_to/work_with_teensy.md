How to configure for Teensy
===========================

*trippylighting* (a very nice guy) has done some very good documentation on how to use the Teensy boards with the Arduino Eclipse plugin.

Please see [his website](http://trippylighting.com/teensy-arduino-ect/arduino-eclipse-plugin/) for detailed Teensy related stuff.

**Users of V3**  
Teensyduino 1.27 needs some modifications before you can use it.  
TeensyDuino 1.28 works without modifications.  
To get this to work add [teensyduino]/hardware/teensy to the additional hardware paths.  

Due to a bug in eclipse The file open dialogs on Mac don't let you access the package contents as you can do in Finder ([please vote for his bug](https://bugs.eclipse.org/bugs/show_bug.cgi?id=487534)).  
The work around is is a 2 step activity.  
   1) In the finder: Alt-Cmd-Drag the folder [teensyduino]/hardware/teensy out of the .app package into a normal, accessible folder on your disk. This will create a symbolic link that is accessible also via the folder chooser dialogs.  
   2) Add the symbolic link to the hardware paths.  

You can delete the symbolic link folder after adding the folder to the hardware path.
   

**Users of the Arduino eclipse plugin version 2.4 **  
The arduino eclipse plugin support teensyduino 1.21 (and later) like arduino IDE.
So basically there is no longer a difference between using teensyduino and Arduino IDE.



**The old answer version 2.3 &nd earlier**  
Working with Teensy and the Eclipse plugin is possible but asks some work.


 
 **Party success**
 
 1. drink a beer
 2. [Become a patron of jantje](http://eclipse.baeyens.it/donate.html "thanks")