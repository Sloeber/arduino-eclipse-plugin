Linux
===
**Support status**
Jantjes development environment used to be a Linux. He still has the machine so it is a good supported OS.
Also the build servers we use (jekins and travis) are linux servers. As part of the build projects are created and compiled.

**However some tips may come in handy:**

If Sloeber does not find *make* install make using your package manager.
For instance in ubuntu I had to run  
>sudo apt install make.

Some linux flavors do not come with a 32 compatibility runtime. Many tools in the toolchain are 32 bit. So if you run a 64 bit linux (which is very likely) you may have to install the *32 bit architecture*.  
If Sloeber fails to find the compiler but it is on your system ... this is probably the case.  
In ubuntu run following commands  :  

>sudo dpkg --add-architecture i386  
sudo apt-get update  
sudo apt-get install libc6:i386 libncurses5:i386 libstdc++6:i386 `  

I never got the Leonardo nor the yun to be recognized on my Linux system. As a result upload issues with these boards have been reported on Linux. The problem seems to be a timing issues in the reset. It seems to work for most people.
If it doesn't you are back to pressing and releasing the reset button for the leonardo.
For the yun use the web upload.

**for older versions**

If things are not working out for you;
When using V1 or V2: install the Arduino IDE from arduino.cc and not from your package provider. In the past some package providers have not strictly adhered to the arduino folder layout which is important for the plugin.

V1 suffers from "serial port blindness". See the FAQ [On Linux I have no serial ports available.](http://eclipse.baeyens.it/rlogiacco/faq.shtml#/troubleshooting) on the faq to see how to fix this. This issue has been solved in V2.


