Mac
==
**Support status**  
There are some mac fans using the plugin. Mac support has grown over the years with input of mac users. The latest version should be as Mac like as eclipse can be.

**Here are some mac specific issues:**  
[the error "serial port is in use" when uploading](http://eclipse.baeyens.it/rlogiacco/faq.shtml#/troubleshooting) should no longer exists from version 2.4 (not yet released at the time of writing) onwards.

In the 1.5 series there has been a change in the folder structure on mac from arduino IDE 1.5.7 onwards. See [this issue](https://github.com/jantje/arduino-eclipse-plugin/pull/180) for more detail. 

If you system does not have make in its path the most appropriate solution is to get make in the path. 
If you do not have the Make utility on your system: Make comes with xcode and with pre-1.5.7 arduino IDE versions.  
Possible fixes:
1) If you copy the make utility (if it is called gnumake rename it to make) to the /usr/bin folder you fix this problem once and for all. (You may need to provide permissions)
2) If you put the xcode folder in your path you fix this problem once and for all. (you may need to copy gnumake to make as the link file make may not work in eclipse -tell me if you know-) 
3) A hardware by hardware solution (SAM, AVR, teensy,...) is to -for AVR- simply copy the 'gnumake' executable from Arduino-1.5.6-r2 '/avr/bin' directory

-for sam- copied this file to the latest Arduino-1.6.x 'avr/bin' directory and renamed it from 'gnumake' to 'make'.
Simply copy the same 'gnumake' file and place it in the '/gcc-arm-none-eabi-4.8.3-2014q1/bin' directory, again rename it to 'make'.

-for XXX- You got the point