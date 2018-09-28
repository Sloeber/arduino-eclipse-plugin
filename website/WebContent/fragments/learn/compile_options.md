Compile Options
========
If you say: 

>Hey I can change any compile option with the Arduino IDE 1.8 by simply changing the platform.txt file.

Then I can only fully agree, and this works in Sloeber as well.  
However most people will find it very hard to change the platform.txt file. Moreover these settings are system wide, where for most options (like defines) you want them at the level of the project.

Sloeber allows you to add content to the command line at the project level, without modifying the platform.txt file.  
As to my experience ([and as to others](https://stackoverflow.com/questions/15909788/how-does-gcc-behave-if-passed-conflicting-compiler-flags)) gcc takes the last option if there are conflicting options.  
This means you are likely able to "overwrite any option given by Arduino by simply adding it to the end of the command line".  

![Compile options](http://eclipse.baeyens.it/img/compile_settings.png)  

