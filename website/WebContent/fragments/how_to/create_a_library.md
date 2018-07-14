How to create a library with the Arduino Eclipse plugin.
==

>This guideline assumes you have basic knowledge of sloeber and CDT. This means we do not go into details of "commonly used functionality". We stick to the "library creation". Also allow for differences in the images when verifying your steps. These images may have been taken over different versions of sloeber and different oses.

With Sloeber you have a real development environment which allows you to modify libraries as if they are part of your code, so you may wonder: "how can I make my own libraries?" There are plenty of ways; but here is one to get you going.

**start by having a project**
For sake of this demo I created a empty workspace and created a sketch in there.
Here is what it looks like on my system:
![freshly created sketch in empty workspace](http://iloapp.baeyens.it/data/_gallery/public/1/152062477075274500_resized.png?width=750&height=480)

**write your first version of the library code**
In most cases you will start by simply creating a class (right click the project and select new class) into an existing project.
Later you will decide that this class/code is worth to be upgraded to a library.
So we create a class in our project with the name of the library we want to create.
![My first lib class](http://iloapp.baeyens.it/data/_gallery/public/1/152062566341147200_resized.png?width=750&height=380)

**Outside of eclipse: create a subfolder with the library name in one of your private library folders**
As I have C:\Users\jan\Documents\Arduino\libraries listed as a private Library;  I created a folder in C:\Users\jan\Documents\Arduino\libraries with the name Libcodeclass.
Add a file with extension .h in the folder (I used findMe.h)
![Create a llibrary placeholder](http://iloapp.baeyens.it/data/_gallery/public/1/152062566836976100_resized.png?width=750&height=380)

Note that it is a good practice to have the same name for the folder as for the header file (that is even with casing).

**Import the library into your project**
Because we created a "folder containing code" on disk the plugin can add the "library" (with the foldername) to the project.
Click Arduino in the menu bar, select "Add a library to the selected project" and select the "library" (folder) you created above (LibCodeDemo).

![Add the library to your project](http://iloapp.baeyens.it/data/_gallery/public/1/152062391251108300_resized.png?width=750&height=480)
>If the library is not showing up.
Check wether the root folder is actualy a private library folder.
Check if you added the header file to your library.

Select Finish and your project should look something like:

![a empty library folder](http://iloapp.baeyens.it/data/_gallery/public/1/152062645306058800_resized.png?width=750&height=380)
Do not forget to delete the findMe.h file.
Note the small arrow in the folder icon which indicates this is a linked folder.

**Drag and drop the class to the library folder**
Now simply drag and drop the header file and source code to the library folder.
![we are done](http://iloapp.baeyens.it/data/_gallery/public/1/152062664041588100_resized.png?width=750&height=480)

That is all there is to it.

>IMPORTANT Your library code is now no longer in your workspace!!




 **Party success**

 1. drink a beer
 2. [Become a patron of jantje](http://eclipse.baeyens.it/donate.html "thanks")