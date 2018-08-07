How to update your product
=========================
**!!When you update the plugin you also have to update the workspace.!!**  
**!!Take a backup before you do the update. Backup both workspaces and eclipse install!!**  


The ways to upgrade a product    
==================  
There are actually 2 ways to upgrade Sloeber.  
1. install new product next to current product  
2. upgrade current product  

In both cases once sloeber is back up and running you need to upgrade your workspace.
  

Install new product next to current product    
==================   
**What is it? **  
Basically you install 2 (or more) Sloebers on your system in a different version.  
As both can run at the same time (not with the same workspace) you can quickly check configuration settings or different behavior.  
This is the only way to do a upgrade of a major version.  
  
**Advantages**  
This is actually the safest way to work; because you can always fall back to the old situation. Even long after the upgrade you can still check what setting you used or which toolchains were installed.      
You start from a clean setup. Old "mistakes" impacting your setup are now undone for sure.    
Because uninstalling an Arduino platform version does not uninstall the tools (they may be needed for other versions); old tools are cleaned up this way.  

**Disadvantages**  
The disadvantage is that you lose your configuration settings. That is:   
1. All changes you made in windows->preferences
2. All additional plugins you installed
3. All platforms you installed

This also means that the new sloeber may have installed a newer Arduino avr platform.  
In other words: "you will lose some time configuring and installing".  

**Steps to install a new product next to the current one**  
1. Make a backup of your current sloeber install folder (just copy the folder to a safe place or make a zip)
2. Make a backup of all your workspace folders (just copy the folder to a safe place or make a zip)
3. Download the new version
4. unzip the new Sloeber version in a new folder
5. Optionally: Copy the [current sloeber folder/arduino plugin/downloads] to [new sloeber folder/arduino plugin/downloads] (this will reduce downloads of the internet) 
6. Start the new sloeber
7. Point Sloeber to your workspace
8. Install your favorite "other plugins"
9. Make changes to the Sloeber configuration if your not using the defaults.

upgrade current product    
================== 
**What is it** 
You ask eclipse to do what it thinks it needs to do to your current install to upgrade.  

**Advantages of upgrading the product**  
If all goes well this is your preferred option.  
All the toolchains and configurations remain as is.    

  
 **Disadvantages of upgrading the product**  
There are many cases this simply doesn't work. And if it doesn't **you only have your backups** to go back to.  
In other words: if it fails you will have lost time trying to upgrade; trying to fix the upgrade; do the same work as "install new product next to current project" after getting your backups out. 

A word of warning: I have spend quite a lot of frustrating days for all different reasons and errors trying to understand how to fix a broken updated eclipse instance.  
 
**Steps to upgrade a Sloeber product**  
When you are in Sloeber select `Help->Check for updates`: the procedure will check for any available update for your Eclipse installation, including eclipse, egit, CDT, gnuarm, Sloeber and many more.    
  
If no sloeber update is found check if the Sloeber update site is known and active in `windows->preferences->available update sites`. The site must be listed and a check must be in the checkbox.    
  
The update site should be like http://eclipse.baeyens.it/update/V[major version number]/stable for instance http://eclipse.baeyens.it/update/V4/stable  


Workspace update actions  
==================
For each and every project in your workspace go to the `project properties->arduino`. Verify your settings.  **Select apply and close.** (Do not use cancel!!)  
Note that it is not "abnormal" that after a update the data in `project properties->arduino` is wrong.  

Other thoughts
==================
**Upgrading the plugin**  
This document is about upgrading the Sloeber Product. If you are running the plugin please see standard eclipse documentation about upgrading plugins.  
There are simply to many options and I'm not the right person to do a write up on it.  
    
**oomph**  
Eclipse launched a product to help with the whole hassle of configuration when setting up a new eclipse install.  
This product is called oomph. Oomph consists out of 2 parts.  
The installer and the recorder.    
I'm fine with the installer but the recorder breaks the encapsulation which (I think) **is bad bad bad**.      
Oomph is part of the eclipse install so it is part of Sloeber. I tried to disarm it but it does not always work.    
My advice: If oomph asks you something select the strongest "Go away" option.  

**knowing your version**  
If you do not know your current Sloeber version, do as follows to find out.  
Start Sloeber look left bottom of the splash. It should contain the version number.  
If it does not you are or running a very old version or the plugin.   
In Sloeber you can select help->about. Click on installation details. in the installed software you should find Sloeber and version numbers.  

**Why should I upgrade**  
You are happy with your current Sloeber. You have everything setup right. Why would you upgrade?  
I agree upgrading is a hassle someone else puts on your agenda. (I'm sorry)  
You don't have to take this crap. (I agree)  

But..... if you run into a issue and ask for help, you will be asked which version you are running. If your answer is V1.0 ... (I don't have to take this crap)  
So at this point in time you have 2 options, fix the issue yourself or upgrade (to get help).    
Can you imagine an upgrade from Sloeber V1.0 to V4.3? (I can not!)  
  
My advice? You do not need to upgrade to the latest and greatest as soon as it is released. But do upgrade from time to time to avoid the extra load of a technology jump when you need to upgrade.  
Installing a new sloeber product next to your current one is a great fast way to see why you should upgrade.


 **Party success**

 1. drink a beer
 2. [Help keep Sloeber alive](http://eclipse.baeyens.it/donate.html "thanks")