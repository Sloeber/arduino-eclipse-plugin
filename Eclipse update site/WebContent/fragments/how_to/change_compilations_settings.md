Change your compilations settings
===

Somethimes it may be needed to change the compilation settings of your project.  
A common need is to add an extra define or set the warning level.  
The V2 plugin no longer allows you to change the settings on he toolchain. But you can add extra content to the compile command. Normally if a setting is 2 times in the same command the last one is dominant. As such you can overrule default Arduino settings.
But how can I add settings?"  
It is actually easy:  
Right click on the project and select properties.    
In properties select Arduino.  
No select the compile options tab
Here you can change your Arduino settings.  
The dialog looks pretty much like the one in the sketch creation wizard.  
![change Arduino board settings](http://iloapp.baeyens.it/data/_gallery/public/6/141807692324070200_resized.png) 
**Show all warnings** will set the warning level on 4. Note that this is the compiler warnings and not the indexer warnings. For indexer warnings please see the cdt documentation.  
**Use alternative size** command changes the command that is executed to calculate the size of the sketch at the end of the build. Note that this command will not make sense on all boards (FI the all sam boards)  
The other options seem pretty straightforward.  

 **Party success**
 
 1. drink a beer
 2. [Buy Jantje a beer](http://eclipse.baeyens.it/donate.html "thanks")