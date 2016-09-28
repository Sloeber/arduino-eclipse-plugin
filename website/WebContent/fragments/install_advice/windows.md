Windows
===
**unzip**
The down-loaded file for the product is a tar file. On windows you can uncompress and unzip it with 7Zip. You need to use 7zip two times.  

**spaces**
Windows users keep in mind that spaces cause problems. So do not install the arduino IDE eclipse or Sloeber in "program files". Don't create the workspace in "my documents".  
  

**V2: make no longer part of arduino ide**
from Arduino IDE 1.5.7 onwards  
The first problem is consistent and is that the make file is no longer part of the Arduino IDE delivery. To fix this you can install make or copy make from an older version.  

Please use one of the 2 options below:
ftp://ftp.equation.com/make/32/make.exe
http://mingw.org/
[Full explanation can be found here.](https://www.youtube.com/watch?v=cspLbTqBi7k&feature=youtu.be)


**V2: Path issues**
From arduino IDE 1.5.7 until and including 1.6.0
This problem is not consistent and may or may not hit you. It deals wit the new toolchain delivered by arduino not 100% supporting a feature in windows that is used by the arduino eclipse plugin.  
If the issue hits you, the indexer will report bugs which do not cause issues during the compile. Even though it can be called "cosmetic" it does seriously reduce the benefit of using eclipse instead of the Arduino IDE.  
See [this arduino issues](https://github.com/arduino/Arduino/issues/2422) for more detail.  
