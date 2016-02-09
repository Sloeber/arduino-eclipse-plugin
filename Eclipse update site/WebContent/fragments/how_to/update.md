How to update your plugin  
=========================

This update procedure works for all versions of the product.  

When you are in Eclipse select `Help` -> `Check for updates`: the procedure will check for any available update for your Eclipse installation, including eclipse, egit, CDT, the Arduino Eclipse plugin and many more. 
After that is done do the after update actions as described below. 

Please **do not** download the new product version but use the update process via the update sites.

**V1 users:** note there will never be an update.

**V2 users:** Note that the nightly is V3 so a update with the nightly in your update sites will result in a upgrade to V3.

**V2 Product users:** note that with recent versions you will update to the nightly build. If you do not want that remove the nightly build from your update sites.

After this do the **after update actions**

 How to update the arduino ide version (not applicable for V3)  
========================= 
Install the arduino IDE as you would do usually.  
After this do the **after update actions**

after update actions
==================
**V1 to V2 actions** Open 2 eclipse instances 1 pointing to your old workspace one pointing to your new workspace.
Setup the new workspace so it knows all hardware and libraries you need.
For each project in the old workspace; create a new projects and drag and drop your code from the old workspace/project to the new workspace/project. Do not copy core or library code.

**V2 to V3 actions** do the same as V1 to V2.

I'm not saying these steps should be done after each and every upgrade. However if something does not seem to work these are the first steps to do.

In V1 and V2 look at the general preferences and make sure it points to the "correct" arduino IDE.  
**Select apply and close.**

All versions: For each and every project go to the project properties->arduino. Verify your settings.  **Select apply and close.**

Note that it is not "abnormal" that after a update -of the plugin or the arduino IDE- the data in the preferences->arduino or project properties-arduino is completely wrong. 

