Linux
===
**Support status**  
Jantjes development environment used to be a Linux. He still has the machine so it is a good supported OS.

**However some tips may come in handy:**

If things are not working out for you;  
When using V1 or V2: install the Arduino IDE from arduino.cc and not from your package provider. In the past some package providers have not strictly adhered to the arduino folder layout which is important for the plugin.

V1 suffers from "serial port blindness". See the FAQ [On Linux I have no serial ports available.](http://eclipse.baeyens.it/rlogiacco/faq.shtml#/troubleshooting) on the faq to see how to fix this. This issue has been solved in V2.

My Linux comes with make in the path. I have not yet heard of someone having the make issue on Linux. If you do experience the make issue on a Linux flavor please contact me.

I never got the Leonardo nor the yun to be recognized on my Linux system. As a result upload issues with these boards have been reported on Linux. The problem seems to be a timing issues in the reset. It seems to work for most people.  
If it doesn't you are back to pressing and releasing the reset button for the leonardo.  
For the yun use the web upload.
 