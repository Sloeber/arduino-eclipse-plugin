How to add a library - Arduino Eclipse Plugin!
=====
Part of the power of arduino is the multitude of libraries available to the users.
Therefore this is a basic functionality of Sloeber.
Just like in the Arduino IDE there is a library manager to install libraries and a "add library to sketch" functionality.
The Arduino core team has done a good job in hiding the complexity of using libraries. In a real IDE you do not want to hide this complexity to the full extend that the Arduino IDE does. As such there is some more visibility (read work and possibilities) in regards to libraries.
In your project there is a subfolder called libraries. This folder contains the libraries that have been imported into your project. This list should only contain libraries you really use.
Adding a library to this folder is what we call importing a library.
It is very easy to import a library into your project.

Note that in all versions the libraries are linked; so **changing library code changes the code for all your projects**.

Use the include statement
-----
Simply add the include statement to your code.
Save your file.
The automatic library includer will import the library into your project and install the library if not yet installed (only if the library folder name is header - ".h".)
You may have to wait a while before the indexer has indexed the source code and the library includer can kick in.
The auto import and auto install can be disabled in windows->preferences->arduino settings.  
Sometimes the includer imports unneeded libraries or libraries may no longer be needed. In this case you can delete the libraries folder or individual libraries. As it are all links the libraries are not deleted, only the links.



Import manually
-----

If the includer does not find the library or you turned off "auto import libraries" in the preferences, you can add a library to a project using "import library".  
Select the project you want the library to add to. In the main menu select "Arduino->add a library to the selected project".
A dialog box will pop up showing all available libraries.
![import library](http://eclipse.baeyens.it/img/import_libraries.png)


Note: that in contrast to the arduino IDE you will still need to add the include directives to your ino file.



 **Party success**

 1. drink a beer
 2. [Support jantje](http://eclipse.baeyens.it/donate.html "thanks")