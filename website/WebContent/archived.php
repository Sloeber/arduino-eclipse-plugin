<?php
require 'fragments/file-buttons.php';
$version = "3.0";

if (isset ( $_GET ["ver"] ))
	$version = $_GET ["ver"];
?>

<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="Roberto Lo Giacco">
    <link rel="shortcut icon" href="http://eclipse.baeyens.it/favicon.ico">

    <title>Arduino Eclipse IDE - Archive ver <?php echo $version;?></title>

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
        <h1>Archive version <?php echo $version;?> <small>in case you want to travel back in time</small></h1>
        <a name="top">&nbsp;</a>
      </div>
            <p  class="solid"><small>One of the major problems encountered in time travel is not that of becoming your own father or mother. There is no problem in becoming your own father or mother that a broad-minded and well-adjusted family can't cope with. There is no problem with changing the course of history—the course of history does not change because it all fits together like a jigsaw. All the important changes have happened before the things they were supposed to change and it all sorts itself out in the end.<br>
      The hitchhikers guide to the galaxy.</small></p>
      <h3>Before you start <small>don't skip this section, you will regret it!</small></h3>
      <?php
            if ($version=="2.2"){
          echo "<p>This is so old I can't recall.</p>";
      }
            if ($version=="2.3" || $version=="2.1"){
          echo "<p>$version got lost in the myst of time.</p>";
      }
      if ($version=="2.4"){
          echo "<p>Only for ArduinoIDE 1.6.5 and earlier</p>";
      }
    if ($version=="3.0"){
          echo "<p>The first version for which no Arduino IDE is needed</p>";
      }
      ?>

      <a href="#top" scroll-to="top">Back to top</a>
      <h3>Pick your OS architecture in the following list</h3>
      			<?php echo listVersionFiles($version);
		?>

      <a href="#top" scroll-to="top">Back to top</a>
      <h3>Troubleshooting <small>known solutions to common problems</small></h3>
      <p>...known solutions to know problems...</p>
      <a href="#top" scroll-to="top">Back to top</a>
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