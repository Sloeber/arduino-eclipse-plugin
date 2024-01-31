<?php
require 'file-buttons.php';
$version = "4.4";
$fullVersion=$version.".2";

    ?>

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="">
<link rel="shortcut icon" href="http://eclipse.baeyens.it/favicon.ico">
<title>test fileList</title>

<body role="document">


		<p>Linux</p>
         <?php
         echo listFiles( "lin",$version,$fullVersion) . "AAAAAA";
        ?>

		<p>Windows</p>
         <?php
         echo listFiles( "win",$version,$fullVersion). "AAAAAA";?>
       
		<p>Mac</p>
         <?php
         echo listFiles( "mac",$version,$fullVersion). "AAAAAA";?>

</body>
</html>
