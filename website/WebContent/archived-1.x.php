<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="Roberto Lo Giacco">
    <link rel="shortcut icon" href="http://eclipse.baeyens.it/favicon.ico">

    <title>Arduino Eclipse IDE - Archive ver 1.x</title>

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
        <h1>Archive version 1.x <small>in case you want to travel back in time</small></h1>
        <a name="top">&nbsp;</a>
      </div>
      <p  class="solid"><small>One of the major problems encountered in time travel is not that of becoming your own father or mother. There is no problem in becoming your own father or mother that a broad-minded and well-adjusted family can't cope with. There is no problem with changing the course of history—the course of history does not change because it all fits together like a jigsaw. All the important changes have happened before the things they were supposed to change and it all sorts itself out in the end.<br>
      The hitchhikers guide to the galaxy.</small></p>
      <h3>Before you start <small>don't skip this section, you will regret it!</small></h3>
      <p>V1 Arduino eclipse IDE is only available as plugin.</p>

      <h3>Eclipse Update Site</h3>
      <p>Install V1 of the plugin by setting the following update site within an existing Eclipse CDT installation.</p>
      <div class="row">
        <div class="col-md-4 col-md-offset-4">
          <div class="well text-center">http://www.baeyens.it/eclipse/update site</div>
        </div>
      </div>
      <a href="#top" scroll-to="top">Back to top</a>
      <h3>Troubleshooting <small>known solutions to common problems</small></h3>
      <p>If you do not need this version: stay away.</p>
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