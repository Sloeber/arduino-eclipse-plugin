<?php
require 'fragments/file-buttons.php';
include 'globals.txt';
$OS = "unknown";
$ARCH = "unknown";
$MAJOR_VERSION = "unknown";
$MINOR_VERSION = "unknown";
$MICRO_VERSION = "unknown";
$QUALIFIER_VERSION = "unknown";

if (isset ( $_GET ["os"] ))
    $OS = $_GET ["os"];
    if (isset ( $_GET ["arch"] ))
        $ARCH = $_GET ["arch"];
        if (isset ( $_GET ["majorVersion"] ))
            $MAJOR_VERSION = $_GET ["majorVersion"];
            if (isset ( $_GET ["minorVersion"] ))
                $MINOR_VERSION = $_GET ["minorVersion"];
                if (isset ( $_GET ["microVersion"] ))
                    $MICRO_VERSION = $_GET ["microVersion"];
                    if (isset ( $_GET ["qualifierVersion"] ))
                        $QUALIFIER_VERSION = $_GET ["qualifierVersion"];
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

    <?php
				echo "<title>Should I upgrade from - $MAJOR_VERSION.$MINOR_VERSION.$MICRO_VERSION.$QUALIFIER_VERSION on $OS</title>";
				?>
    <!-- Bootstrap core CSS and theme -->
<link rel="stylesheet"
	href="https://netdna.bootstrapcdn.com/bootstrap/3.1.0/css/bootstrap.min.css">
<link rel="stylesheet"
	href="https://netdna.bootstrapcdn.com/bootstrap/3.1.0/css/bootstrap-theme.min.css">

<!-- Custom styles  -->
<link
	href='http://fonts.googleapis.com/css?family=Open+Sans:300italic,600italic,600,300|Open+Sans+Condensed:300,700|Ubuntu+Mono:400,700,400italic,700italic'
	rel='stylesheet' type='text/css'>
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
        <?php	echo "<h1>You are running version $MAJOR_VERSION.$MINOR_VERSION.$MICRO_VERSION.$QUALIFIER_VERSION on $OS</h1>"; ?>
</div>
   <?php
			if (($MAJOR_VERSION == $STABLE_VERSION_MAJOR) and ($MINOR_VERSION == $STABLE_VERSION_MINOR)) {
				echo "you are on the latest stable version $STABLE_VERSION_MAJOR.$STABLE_VERSION_MINOR. There is no need to upgrade.<br>";
			} else {
				echo "Please consider upgrading to the latest stable  $STABLE_VERSION_MAJOR.$STABLE_VERSION_MINOR.<br>";
				if ($MAJOR_VERSION == "1") {
					echo "<strong>Are you really still running V1??? Why???</strong><br>";
				} elseif ($MAJOR_VERSION == "2") {
					if ($MINOR_VERSION < "4") {
						echo '<a href="https://github.com/jantje/arduino-eclipse-plugin/issues?utf8=%E2%9C%93&q=%20label%3A%22status%3A%20fixed%20in%202.4%22%20"> Fixed in 2.4</a><br>';
					}
					echo '<a href="https://github.com/jantje/arduino-eclipse-plugin/issues?utf8=%E2%9C%93&q=%20label%3A%22status%3A%20fixed%20in%203.0%22%20"> Fixed in 3.0</a><br>';
				} elseif ($MAJOR_VERSION == "3") {
					if ($MINOR_VERSION == "0") {
						echo '<a href="https://github.com/jantje/arduino-eclipse-plugin/issues?utf8=%E2%9C%93&q=%20label%3A%22status%3A%20fixed%20in%203.1%22%20"> Fixed in 3.1</a><br>';
					}
				}
			}
			echo '<a href="https://github.com/jantje/arduino-eclipse-plugin/issues?q=is%3Aissue+label%3A%22status%3A+fixed+in+nightly%22"> Fixed in nightly</a><br>';
			?>
	</div>
	<div id="footer">
      <?php include 'fragments/footer.html';?>
    </div>
</body>

<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
<script
	src="https://maxcdn.bootstrapcdn.com/bootstrap/3.1.0/js/bootstrap.min.js"></script>
<script
	src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.26/angular.min.js"></script>
<script src="js/marked.min.js"></script>
<script src="js/angular-marked.min.js"></script>
<script src="js/ui-bootstrap-tpls.js"></script>
<script src="js/app.js"></script>
</html>
