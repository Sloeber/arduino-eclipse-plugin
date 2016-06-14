<?php
require 'fragments/file-buttons.php';
$version = "3.0";

if (isset ( $_GET ["OS"] ))
	$OS = $_GET ["OS"];
	$os =strtolower ( $OS );
	if($os!="linux")
	{
    $os =  substr ( $os, 0, 3 );
	}
?>

<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="Roberto Lo Giacco">
    <link rel="shortcut icon" href="../../assets/ico/favicon.ico">

    <?php	echo "<title>Arduino Eclipse IDE - $OS Nightly Builds</title>"; ?>

    <!-- Bootstrap core CSS and theme -->
    <link rel="stylesheet" href="https://netdna.bootstrapcdn.com/bootstrap/3.1.0/css/bootstrap.min.css">
    <link rel="stylesheet" href="https://netdna.bootstrapcdn.com/bootstrap/3.1.0/css/bootstrap-theme.min.css">

    <!-- Custom styles  -->
    <link href='http://fonts.googleapis.com/css?family=Open+Sans:300italic,600italic,600,300|Open+Sans+Condensed:300,700|Ubuntu+Mono:400,700,400italic,700italic' rel='stylesheet' type='text/css'>
    <link href="css/theme.css" rel="stylesheet">

    <!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
      <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->
  </head>

  <body role="document" ng-app='arduinoEclipse'>

    <!-- Fixed navbar -->
    <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <?php include 'fragments/navbar.html';?>
    </nav>

    <div class="container" role="main">
      <div class="page-header">
        <?php	echo "<h1>$OS Nightly Builds <small>use only if you feel confident</small></h1>"; ?>
      </div>
      		<p>Every night, a little gnome called Jenkins walks around our servers looking for
			changes occurred during the day and, when he finds any, he feels so
			happy he starts the build process!</p>
		<p>
			Beware though, as the nightly build represents the latest and
			greatest version off the code, but
			<mark>there is no guarantee that these versions do not contain any
				issue</mark>
			(they probably do) as the little gnome is not renowned for his
			patience and he doesn't test the build extensively: actually he runs
			away right after launching it!</p>
			<P>And probably worse: The nightly is the playground of the contributors. 
			No testing has been done, no validation has been done.</P>
      

              <?php 
        if ($os==linux)
        	echo "<p><strong>The product uses 100% CPU on Ubunto 16.04 uses 100%. If you need to run on this OS installneon and the plugin version.</strong></p>";
        ?>
      <?php 
      echo "<h3>These are built for $OS 64 bits</h3>";
        $filetoRead="http://eclipse.baeyens.it/fragments/file-list.php?arch=${os}64.";
        readfile ($filetoRead);
        if($os!="mac"){
      echo "<h3>These are built for $OS 32 bits</h3>";
        $filetoRead="http://eclipse.baeyens.it/fragments/file-list.php?arch=${os}32.";
        readfile ($filetoRead);  
        }
        ?>


      <h3>Eclipse Update Site</h3>
      <p>Alternatively you can get the latest nightly build by setting the following update site within an existing Eclipse CDT installation</p>
      <div class="row">
        <div class="col-md-4 col-md-offset-4">
          <div class="well text-center">http://eclipse.baeyens.it/nightly</div>
        </div>
      </div>
       <p>Watch these movies to get a quick start:</p>
		<iframe width="560" height="315" src="https://www.youtube.com/embed/HE5iYxv-B-o" frameborder="0" allowfullscreen></iframe>
        <iframe width="560" height="315" src="https://www.youtube.com/embed/x_JKcvqpxq8" frameborder="0" allowfullscreen></iframe>
        <iframe width="560" height="315" src="https://www.youtube.com/embed/quT-5SSj-Gg" frameborder="0" allowfullscreen></iframe>
        <iframe width="560" height="315" src="https://www.youtube.com/embed/h-rz2FR3H6Y" frameborder="0" allowfullscreen></iframe>
		
    </div><!-- /container -->

    <div id="footer">
      <?php include 'fragments/footer.html';?>
    </div>
  </body>

  <script src="https://ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
  <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.1.0/js/bootstrap.min.js"></script>
  <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.26/angular.min.js"></script>
  <script src="js/marked.min.js"></script>
  <script src="js/angular-marked.min.js"></script>
  <script src="js/ui-bootstrap-tpls.js"></script>
  <script src="js/app.js"></script>
</html>
