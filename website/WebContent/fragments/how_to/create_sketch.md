How to create a sketch - Arduino Eclipse Plugin!
=====
This page contains detailed instructions on how to create a sketch with Sloeber.  
Note that a sketch is a Arduino term and in Eclipse it is called a project. So when I say project I mean sketch and visa versa.  

**What you should have done before you can do this**
Before you can create a sketch you must install and configure Sloeber.

**Multiple ways to create a sketch**
	
The plugin contains a wizard that allows to create a project. The wizard can be (depending on the version you are running) activated in multiple ways.  
Here are some ways  

 1.	Menu->File->new -> project Select the "create Arduino sketch" and then next  
 2. Menu->File->new->create Arduino sketch
 3. Menu->Arduino->create new sketch
 4. Toolbar->new sketch (same icon as in the Arduino IDE)

**Steps to create a sketch**  
Once in the wizard you'll go through these steps to make a sketch.
Note the described steps are the latest and greatest at the time of writing.  

Provide a name for the sketch and press next (note that in the image below the finish is enabled because I already created a sketch)  
![Name the Sketch](http://eclipse.baeyens.it/img/new-shetch-name.png)  
Do not unchek "use default location" unless you know what you are doing.

Provide the Arduino information and select next  
![multiboard Teensy selection](http://eclipse.baeyens.it/img/new-shetch-board.png)  

Next you can select the code you want to start from  
![From 1.3 onwards only](http://eclipse.baeyens.it/img/new-shetch-code.png)  
There are 4 main options:

 1. Default ino file
 2. Default cpp file
 3. Custom template
 4. Sample sketch
 
 Default ino will create a ino file with a setup() and loop().  
 Default cpp will create a cpp and header file with setup() and loop().  
 With custom template you can select the location where your template is located.  
 Sample sketch lets you select (multiple) sketches delivered by Arduino IDE or by any installed library.  
 For each sample you select the wizard will import the ino file. This means that if you select more than one example your project will not compile.  

**Link to sample code** will not make a copy of the sample code but creates a link to the ino file in the library. This allows you to update example code without having to move around code.  
see [my blog for more details on this feature](http://blog.baeyens.it/#post15)    
 
 

 **Party success**
 
 1. drink a beer
 2. [Support jantje](http://eclipse.baeyens.it/donate.html "thanks")