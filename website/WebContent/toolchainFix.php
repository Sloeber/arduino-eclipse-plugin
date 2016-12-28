
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="Roberto Lo Giacco">
    <link rel="shortcut icon" href="http://eclipse.baeyens.it/favicon.ico">

    <title>Fixing a broken toolchain</title>

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
        <h1>Fixing the toolchain after a upgrade of a project</h1>
      </div>
      		<p>When the toolchain id changed (for instance from baeyens.it to Sloeber.io) your project/sketch will no longer compile</p>
		<p>To fix this you go to project properties <br>
		Select "C/C++ build"<br>
		select "toolchain editor"<br>
		You should see errors (if not this is no fix for you)<br>
		deselect "Display compatible toolchains only"<br>
		select a toolchain different from "Arduino Toolchain"<br>
		select "Arduino Toolchain" as toolchain<br>
		select "C/C++ General"<br>
		select "Preprocessor, include paths,Macro etc"<br>
		select "Providers"<br>
		Make sure "Arduino Copiler Settings" is active<br>
		If you can preSs the reset button pres it<br>
		you should se following command <br>
		${COMMAND} -E -P -v -dD -D__IN_ECLIPSE__ "${INPUTS}"<br>
		note the -D__IN_ECLISPE__<br>
		select "OK" <br>
		Verify the project.</p>
			<P>Note: You need to do this for all your projects and for all the configurations in these projects.</P>


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
