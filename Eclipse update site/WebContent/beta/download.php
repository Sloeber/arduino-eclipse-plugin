<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Arduino Eclipse plugin product download page</title>
<link href="MyStyle.css" rel="Stylesheet" type="text/css">
<body>

        <div id="header">
            <a href="http://www.eclipse.org/"><img src="http://download.eclipse.org/eclipse/eclipse.org-common/stylesheets/header_logo.gif" width="163" height="68" border="0" alt="Eclipse Logo" class="logo" /></a>
            <SCRIPT LANGUAGE="JavaScript" type="text/javascript" src="news.js"></SCRIPT> 
            </div>
<h1>Thank you for volunteering to do beta testing</h1>
May I ask you :
<ul><li>to keep this location secret.</li><li>
report things you think need fixing on github.</li><li>
Fill in and send test reports to eclipse@baeyens.it.</li><li>
Report when you stop testing to eclipse@baeyens.it.</li>
</ul>
<br>

<h2>What is to test?</h2>
<h3>Arduino eclipse plugin (short plugin) and arduino eclipse IDE (also called product)</h3>
There is a big difference between this test and the previous tests. Here each and every tester can test the Arduino eclipse IDE and the Arduino Eclipse plugin.<br>
The Arduino eclipse plugin is the old well known "animal". It required downloading and installing eclipse C/C++ and installing the Arduino eclipse plugin before you can get started.<br>
The Arduino eclipse IDE is -kind of like- eclipse C/C++ packed together with the arduino eclipse plugin in one download.<br>
Even though this seems like little difference it does mean a big change. To name some:<br>
One less installation.<br>
Far more test versions. <br>
The product comes with kepler (no juno or indigo ...)<br>
The plugin can be installed and works in juno and kepler. Probably the same counts for luna.<br>
The product has a startup window that gives initial instructions. (I can really use a web designer to improve on the looks and feel of this page).<br>
The product has the perspective "Arduino" and also knows the C/C++ perspective.<br>
The plugin does not know the perspective "arduino" and should be used with the C/C++ perspective.<br>

<h3>All normal behavior</h3>
Use this version as you use the plugin normally.

<h3>The new features</h3>
The scope is brand new and needs some special attention as I hope this will be used intensively.
The new debug configuration.
And custom templates.

<h2>Platform specific info</h2>
<h3>Linux specific</h3>
There is no longer a need to specify the ports. <a href="http://eclipse.baeyens.it/Arduino%20eclipse%20plugin%20FAQ.html#LinuxNoSerialPorts">faq</a>

<h3>Mac specific</h3>
The configuration now asks for a file instead of a folder. This means that you can select arduino.app directly.
 
<h3>Windows specific</h3>
 use 7Zip to uncompress and the use 7Zip to unpak the tar file of the product.


<h2>Teensy</h2>
Download this <a href="http://www.baeyens.it/eclipse/download/teensy_1.15_mac_1.14.tar.gz">tar</a>. unzip ( and untar in windows using 7zip) in your home/arduino/hardware folder so you get home/arduino/hardware/teensy/.<br>
Replace the <a href="boards.txt">boards.txt</a> and <a href="platform.txt">platform.txt</a> with the these.<br>
I contacted paul to have newer versions from the teensy library directly from him. We didn't make a schedule, so don't ask me when it will be ready.<br>

<h2>The report</h2>
I'll try to make a form so we have some overview of what has been tested.<br>

<h1>locations</h1>
The download site is at http://www.baeyens.it/eclipse/beta/update

<?php


function ListFileNames( $prefix)
{

  $location="product";
  $dir = opendir($location);
  while(false != ($file = readdir($dir))) 
    {
      if(($file != ".") and ($file != "..") and ($file != "index.php")) 
        {
          $files[] = $file; // put in array.
        }   
    }
  rsort($files); // sort.
  closedir($dir);

  foreach($files as $file) 
    {
        $fullpath = $location."/".$file;
        if (substr($file,0,strlen($prefix))==$prefix)
        echo "<a href=\"$fullpath\" target=\"_blank\">$file</a>\n<br>";
    }
}
?>
<h2>Linux 64 bit</h2>
<?php
ListFileNames("linux64.");
?>

<h2>Linux 32 bit</h2>
<?php
ListFileNames("linux32.");
?>

<h2>win 64 bit</h2>
<?php
ListFileNames("win64.");
?>
<h2>win 32 bit</h2>
<?php
ListFileNames("win32.");
?>

<h2>Mac 64 bit</h2>
<?php
ListFileNames("mac64.");
?>
<br>
</body>
</html>

