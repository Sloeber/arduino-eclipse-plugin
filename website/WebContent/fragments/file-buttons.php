<?php
/*include '/eclipse/globals.txt';
$version = $STABLE_VERSION_MAJOR . "." . $STABLE_VERSION_MINOR;
$fullVersion = $version . "." . $STABLE_VERSION_PATCH;
$version = "4.4";
$fullVersion = "4.4.1";*/


function listFiles($os,$version,$fullVersion){
    $listFilesOutput= internalListFiles("V" . $version . '_' . $os);
    $listFilesOutput= $listFilesOutput . internalListFiles("V" . $fullVersion . '_' . $os);
    $listFilesOutput= $listFilesOutput . internalListFiles("sloeber-ide-V" . $fullVersion . '-' . $os);
    return $listFilesOutput;
}
function listVersionFiles($version){
    $listFilesOutput= internalListFiles("V" . $version );
    $listFilesOutput= $listFilesOutput . internalListFiles("sloeber-ide-V" . $version);
    return $listFilesOutput;
}

function internalListFiles($prefix) {
    include "files.php";
	global $filter;
	$filter = $prefix;
	$lines=$lines." ".$prefix." ";
	date_default_timezone_set ( 'UTC' );

	$files = array_filter ( $files, "filter" );
	sort ( $files );
	foreach ( $files as &$file ) {
		$line= '<div class="text-center col-md-4 col-md-offset-4">';
		$line= $line . '  <a href="' . $file . '" class="btn btn-success btn-lg text-center">Download <b>' .  basename ( $file ) . '</b><i class="glyphicon glyphicon-cloud-download"></i></a>';
		$line= $line . '</div>';
		$lines=$lines.$line;
	}
	return $lines ;
}
function filter($file) {
	global $filter;
	return (substr ( basename ( $file ), 0, strlen ( $filter ) ) == $filter);
}
?>