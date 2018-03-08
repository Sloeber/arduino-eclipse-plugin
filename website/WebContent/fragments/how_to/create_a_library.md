How to create a library with the Arduino Eclipse plugin.
==

NOTE: This guideline assumes you have basic knowledge of sloeber and CDT. This means we do not go intodetails of "commonly used functionality". We stick to the "library creation". Also allow for differences in the images when verifying your steps. These images have been taken over versions.

As you have a real development environment now which allows you to modify libraries as if they are part of your code you may wonder: "how can I make my own libraries?"

The way I do it is a multiple step process as you can see below.

**start by having a project**
For sake of this demo I created a empty workspace and created a sketch in there. Here is what it looks like:
![freshly created sketch in empty workspace](http://iloapp.baeyens.it/data/_gallery/public/6/141814238387984300_resized.png)

**write your first version of the library code**
In most cases you will start by simply creating a class (right click the project and select new class) inside a existing project.
Later you will decide that this class/code is worth to be upgraded to a library.
So we create a class in our project with the name of the library we want to create.

**Create a subfolder with the library name in you private library folder**
Outside of eclipse
Create a subfolder with the library name in you private library folder.
Don't put any files there, just create the folder.
End of outside eclipse

On my system I created a folder in /home/jan/Arduino/libraries with the name Libcodeclass. Note that it is a good practise to have the same name for the folder as for the header file (that is even with casing).

**Import the library in your project**
Because we created a "folder" on disk the plugin can add the "library" (with the foldername) to the project.
Click Arduino in the menu bar, select "Add a library to the selected project" and select the "library" (folder) you created above (LibCodeDemo).
![Add the library to your project](http://iloapp.baeyens.it/data/_gallery/public/6/141814162245895400_resized.png)
now your project should look something like:
![a empty library folder](http://iloapp.baeyens.it/data/_gallery/public/6/141814162454370900_resized.png)
Note that the Libcodeclass folder under libraries is empty.
Also not the small arrow in the folder icon which indicates this is a linked folder.

**Drag and drop the class to the library folder**
Now simply drag and drop the header file and source code to the library folder.
![we are done](http://iloapp.baeyens.it/data/_gallery/public/6/141814162408665500_resized.png)
That is all there is to it.

Note that you can just as well create the class in the Libcodeclass folder in eclipse. However in most cases you will promote existing code to a library.


 **Party success**

 1. drink a beer
 2. [Become a patron of jantje](http://eclipse.baeyens.it/donate.html "thanks")