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
Yust download the correct version for your os unpack and start eclipse.
You still need to configure (step 4 of the installation instructions)


<h2>Linux 64 bit</h2><br>
<?php
$dir = "http://eclipse.baeyens.it/download/product";
$dh = opendir("download/product");
while ($f = readdir($dh)) {
  $fullpath = $dir."/".$f;
  if ($f{0} == "." || is_dir($fullpath)) continue;
  if (substr($f,0,18)=="linux64.")
  {
    echo "<a href=\"$fullpath\" target=\"_blank\">$f</a>\n<br>";
  }
}
closedir($dh);
?>

<h2>Linux 32 bit</h2><br>
<?php
$dir = "http://eclipse.baeyens.it/download/product";
$dh = opendir("download/product");
while ($f = readdir($dh)) {
  $fullpath = $dir."/".$f;
  if ($f{0} == "." || is_dir($fullpath)) continue;
  if (substr($f,0,18)=="linux32.")
  {
    echo "<a href=\"$fullpath\" target=\"_blank\">$f</a>\n<br>";
  }
}
closedir($dh);
?>
<h2>win 64 bit</h2>
<?php
$dir = "http://eclipse.baeyens.it/download/product";
$dh = opendir("download/product");
while ($f = readdir($dh)) {
  $fullpath = $dir."/".$f;
  if ($f{0} == "." || is_dir($fullpath)) continue;
  if (substr($f,0,10)=="win64.")
  {
    echo "<a href=\"$fullpath\" target=\"_blank\">$f</a>\n<br>";
  }
}
closedir($dh);
?>
<h2>win 32 bit</h2>
<?php
$dir = "http://eclipse.baeyens.it/download/product";
$dh = opendir("download/product");
while ($f = readdir($dh)) {
  $fullpath = $dir."/".$f;
  if ($f{0} == "." || is_dir($fullpath)) continue;
  if (substr($f,0,10)=="win32")
  {
    echo "<a href=\"$fullpath\" target=\"_blank\">$f</a>\n<br>";
  }
}
closedir($dh);
?>
<h2>MAC 64 bit</h2>
<?php
$dir = "http://eclipse.baeyens.it/download/product";
$dh = opendir("download/product");
while ($f = readdir($dh)) {
  $fullpath = $dir."/".$f;
  if ($f{0} == "." || is_dir($fullpath)) continue;
  if (substr($f,0,10)=="mac64")
  {
    echo "<a href=\"$fullpath\" target=\"_blank\">$f</a>\n<br>";
  }
}
closedir($dh);
?>
<br>
</body>
</html>

