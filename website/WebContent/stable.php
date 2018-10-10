<?php
require 'fragments/file-buttons.php';
include 'globals.txt';
$version = $STABLE_VERSION_MAJOR.".".$STABLE_VERSION_MINOR;
if( $STABLE_VERSION_PATCH!=0){
    $version=$version.".".$STABLE_VERSION_PATCH;
}

if (isset ( $_GET ["OS"] ))
    $OS = $_GET ["OS"];
    $os = strtolower ( substr ( $OS, 0, 3 ) );
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


    <?php	echo "<title>Arduino Eclipse IDE - $OS Latest Stable</title>"; ?>

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

</head>

<body role="document">

	<!-- Fixed navbar -->
	<nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <?php include 'fragments/navbar.html';?>
    </nav>

	<div class="container" role="main">
		<div class="page-header">
      <?php	echo "<h1>$OS $version Latest Stable <small>runs smoothly and nicely</small></h1>"; ?>
        <a name="top">&nbsp;</a>
		</div>
		<p>This is the latest stable build of Arduino Eclipse IDE and it's
			available as a product and a plugin: pick the one you prefer!</p>
		<h3>Product bundle</h3>
		<a name="product">&nbsp;</a>
		<p><strong>Strongly advised when you are new to Sloeber.</strong></p>
		<p>This is the simplest way to start using Sloeber as it
			bundles in one single download a complete setup including a stable
			version of the Eclipse CDT and the latest stable build of our great
			Arduino eclipse IDE!</p>
		<p>This is also the only supported setup!</p>
		<?php	if($os=="win"){echo "<p>It even includes java.</p>";}; ?>

        If you have download issues try another browser. Firefox seems to work fine on all oses. chrome seems to have issues.
        <div class="row">
         <?php
        listFiles("V" . $version . '_' . $os);?>
        </div>


		<a href="#top" scroll-to="top">Back to top</a>
		<h3>Plugin update site</h3>
		<a name="plugin">&nbsp;</a>
		<p>If you want to pick a different version of the Eclipse CDT than the
			one included in the product bundle or you want to add the Arduino
			Eclipse plugin to an existing installation this is the way to go.</p>
		<p>Though this setup works this setup is not supported because setting it up is not so easy and the number of combinations is ... kind of endless.</p>
		<div class="row">
			<div class="col-md-4 col-md-offset-4">
			<?php
				echo '<div class="well text-center">http://eclipse.baeyens.it/update/V'.$STABLE_VERSION_MAJOR.'/stable</div>';
				?>
			</div>
		</div>
		        <div class="row">
        <p>Watch these V3.x new and noteworthy video's to get a quick start: (V4 video's are still not made)</p>
        <iframe width="560" height="315" src="https://www.youtube.com/embed/MGAyIOC24lU" frameborder="0" allowfullscreen></iframe>

		<iframe width="560" height="315" src="https://www.youtube.com/embed/HE5iYxv-B-o" frameborder="0" allowfullscreen></iframe>
        <iframe width="560" height="315" src="https://www.youtube.com/embed/x_JKcvqpxq8" frameborder="0" allowfullscreen></iframe>
        <iframe width="560" height="315" src="https://www.youtube.com/embed/quT-5SSj-Gg" frameborder="0" allowfullscreen></iframe>
        <iframe width="560" height="315" src="https://www.youtube.com/embed/h-rz2FR3H6Y" frameborder="0" allowfullscreen></iframe>

      </div>
	</div>
	<!-- /container -->

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
