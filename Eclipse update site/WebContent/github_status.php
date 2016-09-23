<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="">
<meta name="author" content="Jan Baeyens">
<link rel="shortcut icon" href="http://eclipse.baeyens.it/favicon.ico">

<title>Sloeber version status page</title>

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

<body>
	<!-- Fixed navbar -->
	<nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <?php include 'fragments/navbar.html';?>
    </nav>

<?php
// read data from local file
$filter = "nightly";
$githubCacheFile = "githubdata_" . $filter . "_cache.tmp";
$githubUrl = "https://api.github.com/repos/jantje/arduino-eclipse-plugin/issues?labels=status:%20fixed%20in%20" . $filter;
$issuesJson = "";
// echo "Testing existance of ".$githubCacheFile."<br/>\n";
if (file_exists ( $githubCacheFile )) {
	// echo $githubCacheFile." exists<br/>\n";
	$GithubCacheFileDate = filectime ( $githubCacheFile );
	if (time () <= $GithubCacheFileDate + (60 * 2)) {
		$issuesJson = file_get_contents ( $githubCacheFile );
		// echo "using cache<br/>\n";
	}
	// else{
	// echo $githubCacheFile." Is old<br/>\n";
	// }
}
// else{
// echo $githubCacheFile." does not exists<br/>\n";
// }
if ($issuesJson == "") {
	$issuesJson = file_get_contents ( $githubUrl );
	file_put_contents ( $githubCacheFile, $issuesJson );
	// echo "refreshing cache<br/>\n";
}
$issues = json_decode ( $issuesJson, true );
// var_dump($issues);
foreach ( $issues as $issue ) {
	echo "<div><a href=" . $issue ["html_url"] . ">" . $issue ["title"] . "</a> ";
	foreach ( $issue ["labels"] as $label ) {
		echo '<SPAN style="background-color: #' . $label ["color"] . ';"> ' . $label ["name"] . " </SPAN>&ensp;";
	}
	echo "</div><br />\n";
}

?>

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
