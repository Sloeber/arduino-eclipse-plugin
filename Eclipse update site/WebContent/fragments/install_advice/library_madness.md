Library madness
==
in 1.5.7 this problem has been solved completely.

Arduino introduced library specification 1.5 which made maintaining the arduino eclipse plugin impossible. Fortunately the Arduino core team listened to the concerns of "other IDE developpers" and undid the changes this specification created.  
The library specification 1.5 has not been completely undone in Arduino IDE 1.5.5 as such these libraries will not compile in the plugin.
Arduino IDE 1.5.6 implemented the workaround I requested and described below (Arduino did not ifdef the headers)  
The libraries that have issues are the ones that have multiple folders under the arch subfolder (normally sam and avr).
As the plugin includes all code recursively under the library folder for each arch code is included; which gives problems.
To fix this problem add #ifdef ARDUINO_ARCH_[upercase(architecture)] at the top of each file and #endif at the bottom.
So for the files in the sam folder that becomes #ifdef ARDUINO_ARCH_SAM ..... #endif

Unix script (also works for mac)
Unix users can run the code below in the hardware folder to fix the issue automatically.
# /bin/bash
arch=avr
prepend="#ifdef ARDUINO_ARCH_"${arch^^}
append="#endif ARDUINO_ARCH_"${arch^^}
find -path "*/arch/${arch}/*.cpp" -or -path "*/arch/${arch}/*.h" -or -path "*/arch/${arch}/*.c" |xargs -I % sh -c "sed -i.bak \"1i ${prepend}\" %; echo "">> %; echo \"$append\">> %;"
arch=sam
prepend="#ifdef ARDUINO_ARCH_"${arch^^}
append="#endif ARDUINO_ARCH_"${arch^^}
find -path "*/arch/${arch}/*.cpp" -or -path "*/arch/${arch}/*.h" -or -path "*/arch/${arch}/*.c" |xargs -I % sh -c "sed -i.bak \"1i ${prepend}\" %; echo "">> %;echo \"$append\">> %;"
Windows users
This user explains how you can do it in windows with cygwin.

Arvid Jedlicka send me this batch file for windows users. I havn't tried the script but it looks ok. I think you can run it in the libraries folder only so it will take less time.
@ECHO OFF

:: Recommended usage and things that can go wrong ...
::
:: This batch file does not tollerate spaces or any other strange characters in the path names.
::
:: Recommended usage scenario would be to copy the [arduino_IDE_install_directory]/hardware/arduino directory
:: to a temporary directory that you created at the root of a drive ... something like C:\_temp will do but feel
:: free to call it what ever you want as long as there are no spaces or special characters in it. This ensures
:: there will not be any spaces in any of the path names and also keeps the original files until you are sure
:: the 1200+ files that will be changed are 'right' before you overwrite the originals.
::
:: Now that you have C:\_temp\arduino, put this file in the directory. Make sure the extension is .bat as it may have
:: been changed to something else to allow it to pass thru spam and virus scanning software.
::
:: Run the batch file. It will take a while as it is modifying 1200+ files. If you want to see exactly what it is
:: doing comment out the '@ECHO OFF' ... ':: @ECHO OFF' ... before you execute the batch file.
::
:: After running the batch file and ensuring you are satisfied with the results replace the contents of the
:: [arduino_IDE_install_directory]/hardware/arduino directory with the contents of the C:\_temp\arduino directory.
::
:: This batch file does not provide any safeguards related to you doing the wrong thing ... i.e. running it a second time
:: on previously patched files, etc. ... so Stop, Pause, Think is your best course of action.
::

SET temporaryFile=%CD%\temporary_%RANDOM%.txt
SET appendFile=%CD%\textToAppend_%RANDOM%.txt
SET prependFile=%CD%\textToPrepend_%RANDOM%.txt

ECHO #endif>%appendFile%

SET architecture=AVR
ECHO #ifdef ARDUINO_ARCH_%architecture%>%prependFile%
ECHO #endif /* ARDUINO_ARCH_%architecture% */>%appendFile%

PUSHD %architecture%
FOR /F "usebackq delims=*" %%i IN (`dir /b /s *.cpp /s *.h /s *.c`) DO TYPE %prependFile%>%temporaryFile% & TYPE %%i>>%temporaryFile% & TYPE %appendFile%>>%temporaryFile% & MOVE /Y %temporaryFile% %%i
POPD

SET architecture=SAM
ECHO #ifdef ARDUINO_ARCH_%architecture%>%prependFile%
ECHO #endif /* ARDUINO_ARCH_%architecture% */>%appendFile%

PUSHD %architecture%
FOR /F "usebackq delims=*" %%i IN (`dir /b /s *.cpp /s *.h /s *.c`) DO TYPE %prependFile%>%temporaryFile% & TYPE %%i>>%temporaryFile% & TYPE %appendFile%>>%temporaryFile% & MOVE /Y %temporaryFile% %%i
POPD

DEL /F /Q %appendFile%
DEL /F /Q %prependFile%
