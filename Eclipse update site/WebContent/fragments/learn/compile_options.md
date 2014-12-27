Compile Options
========
If you say: 

>Hey I can change any compile option with the Arduino IDE 1.5 by simply changing the platform.txt file.

Then I can only fully agree. However most people will find it very hard to change the platform.txt file. Moreover these settings are system wide where for most options (like defines) you want them at the level of the project.

The Arduino eclipse plugin allows you to add content to the command line at the project level, without modifying the platform.txt file.
![image](http://iloapp.baeyens.it/data/_gallery/public/6/141807692324070200_resized.png)
Moreover it also allows to set the warning level to full overwriting the Arduino default.
