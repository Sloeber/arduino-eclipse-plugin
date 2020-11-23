Linux
===
**Support status**  
Jantje has a Linux development environment (ubuntu) he uses to run tests so it is a good supported OS.  
Also the build servers we use (jekins and travis) are linux servers. As part of the build projects are created and compiled.  
That being said there are less boards working "out of the box" on Linux than windows.  
That is still more than 600 boards though.  

**However you system probably does not have all needed packages installed:**  
Arduino IDE Linux version comes with an install script that amongst others set udev rules. I have run it on my Linux system.  
I know that running the arduino IDE install script did helpfull stuff and is required for Sloeber as well.     

If Sloeber does not find **make** install make using your package manager.
For instance in ubuntu I had to run  
>sudo apt install make.

Some Linux flavors do not come with a 32 compatibility runtime. Many tools in the toolchain are 32 bit. So if you run a 64 bit Linux (which is very likely) you may have to install the **32 bit architecture**.  
If Sloeber fails to find the compiler but it is on your system ... this is probably the case.  
In Ubuntu run following commands  :  

>sudo dpkg --add-architecture i386  
sudo apt-get update  
sudo apt-get install libc6:i386 libncurses5:i386 libstdc++6:i386 `  

I never got the Leonardo nor the yun to be recognized on my Linux system. As a result upload issues with these boards have been reported on Linux (and due has upload problems on linux). The problem seems to be a timing issues in the reset. It seems to work for most people.  
If it doesn't you are back to pressing and releasing the reset button for the Leonardo.
For the yun yiou can use the web upload.

Some boards need **pyton** to work properly   
>on ubuntu  
sudo apt install python  
sudo apt install python-serial  
  
If you want to do **local debug** and you do not have gcc installed   
>sudo apt install gcc  
sudo apt install g++  


