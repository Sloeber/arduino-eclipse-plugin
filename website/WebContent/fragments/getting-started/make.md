Make
====
Make is an old tool that is used to launch a sequence of commands where there are file dependencies on each other. So perfect to launch a C/C++ build.
These commands are the tools in the toolchain.

Simplified Eclipse CDT does not Build the code but it makes a make file and launches make.  

The Arduino IDE does not use make but make came with the Arduino IDE until Arduino IDE 1.5.6. 

V2: Warning for windows users
======
Windows users who are using an Arduino IDE 1.5.5 or later need to install make.
You can copy it from an older Arduino IDE installation 

[pre 1.5.6 Arduino]/hardware/tools/avr/utils/bin

to

[post 1.5.5 Arduino]/hardware/tools/avr/utils/bin


V3: Warning for windows users
======
V3 will download the make.exe in the ArduinoPlugin folder of your eclipse installation. There have been reports of virus scanners removing the make.exe.  

Linux and Mac
======
On most Linux and MAC systems make is already available.  
On Ubuntu I had to install make using sudo apt install make    
We never got a bug report on this. 

