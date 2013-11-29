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
<h1>here you can find product versions build by the build machine.</h1>
product versions can be run without having to install eclipse as the product versions contain eclipse.<br>
Yust download the correct version for your os unpack and start eclipse.<br>
You still need to configure (step 4 of the installation instructions)<br>
<br>
Note1: All versions before 213-11-29 are only tarred even though they are called tar.gz. <br>
Note2: All this is still pre beta (but working) so there are still some things to do (like spash screen, icons ..). <br>
<?php


function ListFileNames( $prefix)
{

  $location="download/product";
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

