Mac
==
**Support status**
There are some mac fans using Sloeber. Mac support has grown over the years with input of mac users. The latest version should be as Mac like as eclipse can be.  


**Here are some mac specific issues:**  

Mac has implemented some rules for download safety that will affect you. [Do not launch from the download location.](http://lapcatsoftware.com/articles/app-translocation.html)  

If you system does not have make in its path the most appropriate solution is to get make in the path.   
If you do not have the Make utility on your system: Make comes with xcode and with pre-1.5.7 arduino IDE versions.  
Possible fixes:  
1) If you copy the make utility (if it is called gnumake rename a copy to make) to the /usr/bin folder you fix this problem once and for all. (You may need to provide permissions)   
2) If you put the xcode folder in your path you fix this problem once and for all. (you may need to copy gnumake and rename it to make as the link file make may not work in eclipse -tell me if you know-)   



**some issues in older versions**  

[the error "serial port is in use" when uploading](http://eclipse.baeyens.it/rlogiacco/faq.shtml#/troubleshooting) should no longer exists from version 2.4 onwards.  

In the 1.5 series there has been a change in the folder structure on mac from arduino IDE 1.5.7 onwards. See [this issue](https://github.com/jantje/arduino-eclipse-plugin/pull/180) for more detail.  

