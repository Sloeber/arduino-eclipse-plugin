REM this script starts sloeberide in windows
REM it removes git and mingw entries from the path
REM to avoid confict with bash and sh shells
@echo off
setlocal EnableDelayedExpansion
set path
set $line=%path%
set $line=%$line: =#%
set $line=%$line:;= %
REM here we replace mingw with git
set $line=%$line:Mingw=git%
set $line=%$line:)=^^)%

REM Here we filter all dirs with "Git" in the name. 
for %%a in (%$line%) do echo %%a | find /i "git" || set $newpath=!$newpath!;%%a
set $newpath=!$newpath:#= !
set $newpath=!$newpath:^^=!
set path=!$newpath:~1!

REM Run Sloeber, Run!
sloeber-ide.exe
