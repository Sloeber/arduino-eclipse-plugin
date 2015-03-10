Keeping the Eclipse project compatible with the IDE (pre .ino)
=============================================================

**Below is the old description. But as the newer version of the plugin support ino files you're way better off upgrading.**

The compatibility between the Arduino IDE and the Arduino Eclipse plugin can only be guaranteed from the Arduino Eclipse Plugin to the Arduino IDE and not the other way around. Currently the Arduino Eclipse plugin is out of the box not compatible with the Arduino IDE. The incompatibility is caused by the fact that the Arduino Eclipse plugin works with C++ files only. The Arduino IDE can handle C++ files but needs at least one ino/PDE file (for amongst others) to identify the classpath.  
To be able to compile your code directly in the Arduino IDE you need to do something extra at three common actions during Arduino code development  

 1. After installation tell Eclipse to process ino(and/or pde) files like C++ file
 2. After creation of a new sketch project rename the created cpp file to ino (or pde if you are using a Arduino IDE version below 1.0)
 3. After inclusion of a library add an include of the library to the ino file (Do not include it in a header file but in the ino directly.).
 
That is all which is needed to be done to create a project that can be Arduino IDE compatible.  
When you want to compile the project in the Arduino IDE select file open. Goto the Eclipse workspace folder. In the workspace folder there is a subfolder with your project name. In that subfolder you will find the .cpp file you renamed to .pde or .ino. Select that file and compile.  

**And here are detailed instructions on how to do this**
  
After installation tell Eclipse to process ino like C++ file  
In the menu select Windows ->preferences  
In the section C/C++ -> file types press new  
enter *.pde and or *.ino as filename and C++Source file as type.  
After creation of a new sketch rename cpp file to pde  
Right click the .cpp file created by the new Arduino sketch wizard  
select rename  
change the .cpp to .ino or .pde  
After inclusion of a library add an include reference to the library to the ino or pde file.  
In the ino file at the top add a line that looks like  
    #include < [the imported library].h >   
