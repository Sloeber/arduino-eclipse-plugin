How to configure for Teensy
===========================

**Users of V4**

Simply install arduiono IDE 1.8 (or later) and Teensyduino 1.5 (or later) and then, inside Arduino Eclipse, go to Windows -> Arduino -> Private Hardware path. Add a new path pointing to your Teensy hardware installation directory. For windows this is something like "C:\Program Files (x86)\arduino\hardware\teensy"
You will now be able to select Teensy boards by right clicking your project -> Properties -> Arduino and then click on the Platform folder entry. A second entry showing the path you just set above will appear. Once selected, simply choose your Teensy variant. You must populate all the fields (for example with a 3.5 Teensy try 120 Mhz and USB type 'Serial') and then press apply. You will find that uploading may require you to press the button on the teensy. The log may say it cannot connect but normally it works like a charm anyway.



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
