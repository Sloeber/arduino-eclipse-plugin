How to update your plugin
=========================
**When you update the plugin you also have to update the workspace.**

How to do a major update  (V2.x to V3.y)
==================
A reinstall of the product is the only supported way.
To save yourself a ton of downloads you can copy the [old eclipse install/arduioPlugin/downloads] to [new eclipse install/arduioPlugin/downloads]
Only copy the downloads folder as the folder structure has changed and probably will change again.

How to do a minor update (V4.x to V4.y with x<y)
==================
When you are in Eclipse select `Help->Check for updates`: the procedure will check for any available update for your Eclipse installation, including eclipse, egit, CDT, gnuarm, Sloeber and many more.

If no sloeber update is found see wether the Sloeber update site is known and active in `windows->preferences->available update sites`. The site must be listed and a check must be in the checkbox.

The update site should be like http://eclipse.baeyens.it/update/V[major version number]/stable for instance http://eclipse.baeyens.it/update/V4/stable


 How to update the arduino ide version (for V1 and V2 users only)
=========================
Install the arduino IDE as you would do usually.

Workspace update actions
==================
For each and every project go to the `project properties->arduino`. Verify your settings.  **Select apply and close.**
Note that it is not "abnormal" that after a update the data in  `project properties->arduino` is wrong.

**V1 to V2 actions** Open 2 eclipse instances 1 pointing to your old workspace one pointing to your new workspace.
Setup the new workspace so it knows all hardware and libraries you need.
For each project in the old workspace; create a new projects and drag and drop your code from the old workspace/project to the new workspace/project. Do not copy core or library code.

**V2 to V3 actions** do the same as V1 to V2.

I'm not saying these steps should be done after each and every upgrade. However if something does not seem to work these are the first steps to do.

In V1 and V2 look at the general preferences and make sure it points to the "correct" arduino IDE.
**Select apply and close.**


 **Party success**

 1. drink a beer
 2. [Become a patron of jantje](http://eclipse.baeyens.it/donate.html "thanks")