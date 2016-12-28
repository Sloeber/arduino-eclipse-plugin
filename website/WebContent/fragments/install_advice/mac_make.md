

If you system does not have make in its path the most appropriate solution is to get make in the path. 
If you do not have the Make utility on your system: Make comes with xcode and with pre-1.5.7 arduino IDE versions.  
Possible fixes: 
1) If you copy the make utility (if it is called gnumake rename a copy to make) to the /usr/bin folder you fix this problem once and for all. (You may need to provide permissions) 
2) If you put the xcode folder in your path you fix this problem once and for all. (you may need to copy gnumake and rename it to make as the link file make may not work in eclipse -tell me if you know-) 
3) for me a bad solution is : A hardware by hardware solution (SAM, AVR, teensy,...) is to -for AVR- simply copy the 'gnumake' executable from Arduino-1.5.6-r2 '/avr/bin' directory 

-for sam- copied this file to the latest Arduino-1.6.x 'avr/bin' directory and renamed it from 'gnumake' to 'make'. 
Simply copy the same 'gnumake' file and place it in the '/gcc-arm-none-eabi-4.8.3-2014q1/bin' directory, again rename it to 'make'. 

-for XXX- You got the point 