Windows
===

Windows users keep in mind that spaces cause problems. So do not install the arduino IDE eclipse or the product in "program files". Don't create the workspace in "my documents".  

The biggest problem right now with windows is in the 1.5.x arduino IDE.  
Arduino IDE 1.5.7 came with a new toolchain which is known to cause 2 problems in windows.  

The first problem is consistent and is that the make file is no longer part of the Arduino IDE delivery. To fix this you can install make or copy make from an older version.  

The second problem is not consistent and may or may not hit you. It deals wit the new toolchain delivered by arduino not 100% supporting a feature in windows that is used by the arduino eclipse plugin.  
If the issue hit you the indexer will report bugs which do not cause issues during the compile. Even though it can be called "cosmetic" it does seriously reduce the benefit of using eclipse instead of the Arduino IDE.  
See [the arduino issues](https://github.com/arduino/Arduino/issues/2422) for more detail and progress.  
