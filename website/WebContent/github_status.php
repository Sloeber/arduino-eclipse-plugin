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
<style>
table, th, td {
    border: 1px solid black;
        padding: 2px;
}

th {
    padding: 5px;
}

</style>
</head>

<body>
    <!-- Fixed navbar -->
    <nav class="navbar navbar-inverse navbar-fixed-top" role="navigation">
      <?php include 'fragments/navbar.html';?>
    </nav>



<?php
$MAJOR_VERSION = "0";
$MINOR_VERSION = "0";
$MICRO_VERSION = "0";
$QUALIFIER_VERSION = "0";
$debuginfo = "<strong>Debug output </strong><br>\n";
$SHOW_DEBUG_INFO = false;
$enabledump = false;


if (isset ( $_GET ["majorVersion"] ))
    $MAJOR_VERSION = filter_var(trim($_GET ["majorVersion"]), FILTER_SANITIZE_NUMBER_INT);
if (isset ( $_GET ["minorVersion"] ))
    $MINOR_VERSION = filter_var(trim($_GET ["minorVersion"]), FILTER_SANITIZE_NUMBER_INT);
if (isset ( $_GET ["microVersion"] ))
    $MICRO_VERSION = filter_var(trim($_GET ["microVersion"]), FILTER_SANITIZE_NUMBER_INT);
if (isset ( $_GET ["qualifierVersion"] ))
    $QUALIFIER_VERSION = filter_var(trim($_GET ["qualifierVersion"]), FILTER_SANITIZE_NUMBER_INT);

function onlyhitgithubEveryFiveMinutes($url){
    global $debuginfo;

    $delayTime =(60 * 10 );
    $githubFile="./cache/githubhit.tmp";
      if (file_exists ( $githubFile )){
            $GithubHitDate = filectime ( $githubFile );
        if (time () <= ($GithubHitDate + $delayTime )) {
            $debuginfo= $debuginfo .  $cachefile . "<strong>github url request is ignored</strong>. " . $url ."<br>\n";
            $waittime=$GithubHitDate + $delayTime- time ();
             $debuginfo= $debuginfo .  "<strong>Please wait. " . $waittime . " seconds.</strong><br>\n";
            return false;
        }
      }
      $debuginfo= $debuginfo .  $cachefile . "github url rquest is actioned. ". $url ."<br>\n";
            // create a context to use, so github won't complain about missing user-agent
        $opts = [
                'http' => [
                        'method' => 'GET',
                        'header' => [
                                'User-Agent: PHP'
                        ]
                ]
        ];
        $context = stream_context_create($opts);
        file_put_contents($githubFile,time ());
        $ret = file_get_contents ( $url, false, $context);
        if ($ret === false) {

            $debuginfo= $debuginfo . "<strong>WARNING: Download of ".$url." failed - using cached data instead.</strong><br>\n";
        }
        return $ret;


}
function cachedGithubJson($cachefile, $url) {
    global  $debuginfo ,$enabledump;
    $debuginfo= $debuginfo . "get data from url: <a href=\"" . $url . "\">" . $url . "</a><br>\n";
    $cachefile = "./cache/".$cachefile;

    $ret="";
    if (file_exists ( $cachefile )) {
        $debuginfo= $debuginfo .  $cachefile . " exists<br/>\n";
        $CacheFileDate = filectime ( $cachefile );
        if (time () <= $CacheFileDate + (60 * 60 * 10)) {
            $ret = file_get_contents ( $cachefile );
            if (strlen ( $ret ) > 10) {
                $debuginfo= $debuginfo .  "    using cache<br/>\n";
            } else {
                $debuginfo= $debuginfo .  "    Local cache is empty<br/>\n";
                $ret = "";
            }
        } else {
            $debuginfo= $debuginfo .  $cachefile . " Cache is out of date<br/>\n";
        }
    } else {
        $debuginfo= $debuginfo .  $cachefile . " does not exist<br/>\n";
    }
    if ($ret == "") {

        $ret=onlyhitgithubEveryFiveMinutes($url);
        if ($ret === false) {

            $ret = file_get_contents ( $cachefile );
            $enabledump = true;
        }
        else{
            file_put_contents($cachefile,$ret);
        }
    }
      $debuginfo= $debuginfo .  "<br/>\n";
    return $ret;
}

function dumpIssues($version, $issues){
        // output the td with the version
        $numIssues = count($issues);
        $firstRowOnly= "<td rowspan=".$numIssues." >".$version."</td>";

        foreach ( $issues as $issue ) {
                $title = "<a href=" . $issue ["html_url"] . ">" . htmlspecialchars($issue ["title"]) . "</a> ";
                $domain_label = "";
                $importance_label = "";
                $os_label = "";
                $rest_label = "";
                $importance_inbetween = "";
                $domain_inbetween = "";
                $os_inbetween = "";
                $rest_inbetween = "";

                foreach ( $issue ["labels"] as $label ) {
                        if (0 === strpos ( $label ["name"], "domain: " )) {
                                $domain_label = $domain_label . $domain_inbetween . '<SPAN style="background-color: #' . $label ["color"] . ';"> <b>' . htmlspecialchars(substr ( $label ["name"], 8 ))                                      . " </b></SPAN>&ensp;";
                                $domain_inbetween = "<br>";
                        } elseif (0 === strpos ( $label ["name"], "importance: " )) {
                                $importance_label = $importance_label . $importance_inbetween . '<SPAN style="background-color: #' . $label ["color"] . ';"> <b>' . htmlspecialchars(substr ( $label ["n                                     ame"], 12 )) . " </b></SPAN>&ensp;";
                                $importance_inbetween = "<br>";
                        } elseif (0 === strpos ( $label ["name"], "OS: " )) {
                                $os_label = $os_label . $os_inbetween .'<SPAN style="background-color: #' . $label ["color"] . ';"> <b>' . htmlspecialchars(substr ( $label ["name"], 4 )) . " </b></SPA                                     N>&ensp;";
                                $os_inbetween = "<br>";
                        } elseif (0 === strpos($label["name"], "status: fixed in")){
                          // ignore fixed status as this is in first column already
                        } else {
                                $rest_label = $rest_label . $rest_inbetween . '<SPAN style="background-color: #' . $label ["color"] . ';"> <b>' . htmlspecialchars($label ["name"]) . " </b></SPAN>&ensp                                     ;";
                                $rest_inbetween = "<br>";
                        }
                }
                $ret=$ret .  "<tr>";
                $ret=$ret .  $firstRowOnly;
                $firstRowOnly="";
                $ret=$ret .  "<td>" . $title . "</td>";
                $ret=$ret .  "<td>" . $domain_label . "</td>";
                $ret=$ret .  "<td>" . $importance_label . "</td>";
                $ret=$ret .  "<td>" . $os_label . "</td>";
                $ret=$ret .  "<td>" . $rest_label . "</td>";
                $ret=$ret .  "</tr>\n";
        }
        return $ret;
}




// read data from local file
$labelsJson = cachedGithubJson ( "githubdata_labels_cache.tmp", "https://api.github.com/repos/Sloeber/arduino-eclipse-plugin/labels?per_page=100" );
$labels = json_decode ( $labelsJson, true );
$fixedLabels="";

// work through all labels, collect the versions
$versions = array();
foreach ( $labels as $label ) {
    // select labels starting with "status: fixed in "
    if (0 === strpos ( $label ["name"], "status: fixed in " )) {
        $curLabelversion=substr ( $label ["name"], 17 ) ;

        // preset with version 9999 for the nightlies
        $major=9999;
        $minor=0;

        if ($curLabelversion != "nightly" ) {
            $labelVerArr=explode('.', $curLabelversion);
            $major=$labelVerArr[0];
            $minor=$labelVerArr[1];
        }

        // this assumes we will never reach minor version 1000 :)
        $sortVersion = ($major*1000)+$minor;

                //store in versions array
        $versions[$sortVersion] = $curLabelversion;
    }
}

// calculate comparison value for user's current build given by parameters
$myversion=($MAJOR_VERSION*1000)+$MINOR_VERSION;




// start the table
$table= "<table>\n";
$table= $table . "<tr><th>fixed in</th><th>title</th><th>domain</th><th>importance</th><th>os</th><th>other</th></tr>\n";

$totalIssues =0;
// loop through collected versions
foreach ($versions as $sortVersion => $strVersion) {
    // filter versions below and including the one we run
    if ($myversion < $sortVersion) {
        // sanitize because we will use this in filename - must not contain / or \ or such...
                $cacheFile = "githubdata_".preg_replace("/[^0-9a-zA-Z\.]/", "_", $strVersion)."_cache.tmp";

        $CurIssuesJson = cachedGithubJson ( $cacheFile, "https://api.github.com/repos/Sloeber/arduino-eclipse-plugin/issues?state=all&labels=" . urlencode("status: fixed in ".$strVersion));
        $CurIssues = json_decode ( $CurIssuesJson, true );

                $totalIssues = $totalIssues + count($CurIssues);
        $table= $table . dumpIssues($strVersion,$CurIssues);
    }
}

if (0 === $totalIssues) {
        $table= $table .  "<tr><td colspan=6>There are no issues fixed in later builds </td></tr>\n";
}

$table= $table .  "</table>";

//report if data is outdated or incomplete
if( $enabledump === true){
    echo "<strong>WARNING: The data below may contain outdated data or may be incomplete.<br> See  the bottom of this page for more details in the debug info.</strong><br>\n";
    $SHOW_DEBUG_INFO=true;
}
echo $table;

//dump debug info if needed
if($SHOW_DEBUG_INFO){
echo    "<br>\n";
echo $debuginfo;
echo "<strong>end of debug output </strong><br>\n";
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
