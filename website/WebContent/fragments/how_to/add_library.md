How to add a library - Arduino Eclipse Plugin!
=====
Part of the power of arduino is the multitude of libraries available to the users.
Therefore this is a basic functionality of Sloeber.
Just like in the Arduino IDE there is a library manager to install libraries and a "add library to sketch" functionality.
The Arduino core team has done a good job in hiding the complexity of using libraries. In a real IDE you do not want to hide this complexity to the full extend that the Arduino IDE does. As such there is some more visibility (read work and possibilities) in regards to libraries.
In your project there is a subfolder called libraries. This folder contains the libraries that have been imported into your project. This list should only contain libraries you really use.
Adding a library to this folder is what we call importing a library.
It is very easy to import a library into your project.

Note that in all versions the libraries are linked; so changing library code changes the code for all your projects.

V4
-----
Simply add the include statement to your code.
Save your file.
The automatic library includer will import the library into your project and install the library if not yet installed (only if the library folder name is header - ".h".)
You may have to wait a while before the indexer has indexed the source code and the library includer can kick in.
The auto import and auto install can be disabled in windows->preferences->arduino settings.

V3
-----
Simply add the include statement to your code.
Save your file.
The automatic library includer will import the library into your project if the library is installed.
You may have to wait a while before the indexer has indexed the source code and the library includer can kick in.
The auto import can be disabled in windows->preferences->arduino settings.
From V3 onwards all libraries are shown in one list. There is no longer the grouping of "arduino libs" "Arduino hardware libs"...

V2 and V3
-----

Select the project you want the library to add to. In the main menu select "Arduino->add a library to the selected project".
A dialog box will pop up showing all available libraries.
![import library](http://iloapp.baeyens.it/data/_gallery/public/1/137950377839940200_resized.png)


Note: that in contrast to the arduino IDE you will still need to add the include directives to your ino file.
Note: from march 2015 there is no more subsections (hardware/arduino/library) Al libraries are sorted alphabetically.


 **Party success**

 1. drink a beer
 2. [Become a patron of jantje](http://eclipse.baeyens.it/donate.html "thanks")