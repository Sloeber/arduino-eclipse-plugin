How to create a sketch - Arduino Eclipse Plugin!
=====
This page contains detailed instructions on how to create a sketch with the Arduino Eclipse plugin.  
Note that a sketch is a Arduino term and in Eclipse it is called a project. So when I say project I mean sketch and visa versa.  
Note in V1 this is not 100% true because a project is made for each board and for each sketch. But who still uses V1?

**What you should have done before you can do this**
Before you can create a sketch you must install the plugin and configure the plugin.

**Multiple ways to create a sketch**
	
The plugin contains a wizard that allows to create a project. The wizard can be (depending on the version you are running) activated in multiple ways.  
Here are some ways  

 1.	Menu->File->new -> project Select the Arduino sketch and then next  
 ![select Arduino sketch](http://iloapp.baeyens.it/data/_gallery/public/1/1320529642_resized.png)
 2. Menu->File->new->new Arduino sketch
 3. Menu->Arduino->new sketch
 4. Toolbar->new sketch (same icon as arduino)

**Steps to create a sketch**  
Once in the wizard you'll go through these steps to make a sketch.
Note the described steps are the latest and greatest. If you do not have all these steps consider upgrading.

Provide a name for the sketch and press next (note that in the image below the finish is enabled because I already created a sketch)  
![Name the Sketch](http://iloapp.baeyens.it/data/_gallery/public/1/1320529645_resized.png)  
Do not unchek "use default location". That is not supported by the tool.

Provide the Arduino information and select next  
![before 1.3 board settings](http://iloapp.baeyens.it/data/_gallery//public/1/1320529644_resized.png)  
from Version 1.3 onwards when you have multiple hardwares the screen will look a bit different.  
![with 1.3 or later with multiple hardwares board settings](http://iloapp.baeyens.it/data/_gallery//public/1/134998998101133100_resized.png)  
In the latest version (at the time of writing) it looks different again. The screen adopts wit the options available for the board.  
![multiboard Teensy selection](http://iloapp.baeyens.it/data/_gallery/public/6/141798833684427200_resized.png)  

Next you can select the code you want to start from  
![From 1.3 onwards only](http://iloapp.baeyens.it/data/_gallery/public/6/141790480561384700_resized.png)  
There are 4 main options:

 1. Default ino file
 2. Default cpp file
 3. Custom template
 4. Sample sketch
 
 Default ino will create a ino file with a setup() and loop().  
 Default cpp will create a cpp and header file with setup() and loop().  
 With custom template you can select the location where your template is located.  
 Sample sketch lets you select (multiple) sketches delivered by Arduino or by any installed library.  
 For each sample you select the wizard will import the library that delivered that sketch. It will import parent libraries.  
 **use current settings as default** will save your settings but will not save the sketch(es) you selected in case ou selected a sample sketch.
**Link to sample code** will not make a copy of the sample code but lets you modify the code directly in your project. This is extremely handy if you are developing/maintaining examples of a library. see [my blog for more detail](http://blog.baeyens.it/#post15)    
 
 
Lastly you can have a debug configuration created as well  
![From 1.3 onwards only](http://iloapp.baeyens.it/data/_gallery/public/6/141790480451146300_resized.png)  
In V1 the plugin will create 2 projects. one for the Arduino library and one for the sketch. The image below holds more projects but the new ones are highlighted.  
![pre 2](http://iloapp.baeyens.it/data/_gallery//public/1/1320529641_resized.png?width=720&height=540)  

Normally you will not open the arduino_atmegaXXX project directly yourself. You will edit the project you created.  
In the image above I opened the my sketch.cpp file by double clicking on it.  
Pressing the hammer (V1:marked in the image above) or the Arduino verify symbol will compile the project. Pressing the AVR button (V1: marked above) or the arduino upload button will upload the project to your Arduino board.
Note that the arduino upload does not verify first.    
In V1: When modifying the file later on do not delete the #include directive at the top of the file. If you do the Arduino language will not be known to your sketch.

In V1 2 projects are created. One for the board and one for the sketch.

In V2: if you have a ino file without libraries the indexer will mark errors. This means that the sketch will compile but the indexer thinks it won't. To fix this simply add #include "Arduino.h" at the top of the ino/pde file.  

In V2 only one project is created. The Arduino code is in folder Arduino in your project.  

 **Party success**
 
 1. drink a beer
 2. [Buy Jantje a beer](http://eclipse.baeyens.it/donate.html "thanks")