This section contains Detailed instructions on how to configure the Arduino Eclipse plugin.
===

This section assumes you have installed the Arduino Eclipse plugin or product.

**tell which Arduino IDE you use**  
 1. Start Eclipse that contains the plugin. c:/eclipse/Eclipse or c:/eclipse/eclipseArduinoIDE for the product
 2. Open a workspace of your choice C:/workspace
 3. If needed close the welcome page
 4. open the preferences (windows -> preferences) 
 5. in V1 open the section Arduino->Arduino 
 6. In V2 open Arduino 
 7. Fill it in like below (using the location of the Arduino IDE c:/arduino)
 8.	Add the location of your private libraries.
 in V1 this looks like
	![V1](http://iloapp.baeyens.it/data/_gallery/public/1/134998877869708900_resized.png)  
 in V2 this looks like  
 ![V2](http://iloapp.baeyens.it/data/_gallery/public/6/141798497248313500_resized.png)  
	The newer version no longer uses RXTX so there is no longer a "test serial dll" button. If the button is there click it.  
	V2 does not contain the option "use Arduino IDE tools in Eclipse".  
	V2.2 fills in mosts fields automatically and adds a "private hardware path" folder.
 9. select OK to save the preferences

**tell cdt you are using ino file**  
  ino and pde files are a file extension created by Arduino and as such is not recognized as a C/C++ file.  
  We need to tell Eclipse to treat ino and pde files as C/C++ file.  
  To do so goto menu:window->preferences->C/C++->file types    
  and add *.ino and *.pde as c/c++ files one by one using the new button.  
  ![file types](http://iloapp.baeyens.it/data/_gallery/public/1/1320784770_resized.png)  
  
 
 **Party success**
 
 1. drink a beer
 2. [Buy Jantje a beer](http://eclipse.baeyens.it/donate.html "thanks")