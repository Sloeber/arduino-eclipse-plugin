Make
====
Make is an old tool that is used to launch a sequence of commands where there are file dependencies on each other. So perfect to launch a C/C++ build.
These commands are the tools in the toolchain.

Simplified Eclipse CDT does not Build the code but it makes a make file and launches make.
The Arduino Eclipse plugin directs Eclipse CDT on how to make the makefile.

The Arduino IDE does not use make but Make came with the Arduino IDE until Arduino IDE 1.5.6. 

Warning for windows users
=======
Windows users who are using an Arduino IDE later than 1.5.5 need to install make.
You can copy it from an older Arduino IDE installation 

[pre 1.5.6 Arduino]/hardware/tools/avr/utils/bin

to

[post 1.5.5 Arduino]/hardware/tools/avr/utils/bin


On most Linux and MAC systems make is already available. 

