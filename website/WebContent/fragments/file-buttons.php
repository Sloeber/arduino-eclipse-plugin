<?php
if (isset ( $_GET ["ver"] ) && isset ( $_GET ["arch"] ))
	listFiles ( "V" . $_GET ["ver"] . '_' . $_GET ["arch"] );
function my_offset($curfile, $prefix) {
	preg_match ( '/\d/', $curfile, $m, PREG_OFFSET_CAPTURE, strlen ( $prefix ) );
	if (sizeof ( $m ))
		return $m [0] [1];
	return strlen ( $prefix );
}
function my_bitness($curfile, $prefix) {
	return substr ( $curfile, my_offset ( $curfile, $prefix ), 2 );
}
function listFiles($prefix) {
	global $filter;
	$filter = $prefix;
	date_default_timezone_set ( 'UTC' );
	$location = "/customers/0/9/d/baeyens.it/httpd.www/eclipse/download/product";
	$dir = opendir ( $location );
	while ( false != ($file = readdir ( $dir )) ) {
		if (($file != ".") and ($file != "..") and ($file != "index.php")) {
			$files [] = $location . "/" . $file; // put in array.
		}
	}
	closedir ( $dir );
	$files = array_filter ( $files, "filter" );
	sort ( $files );
	$lines = array (
			"32" => "",
			"64" => ""
	);
	foreach ( $files as &$file ) {
		$curfile = basename ( $file );
		$bitness=my_bitness ( $curfile, $prefix );
		$lines[$bitness]= '<div class="text-center col-md-4 col-md-offset-4">';
		$lines[$bitness]= $lines[$bitness] . '  <a href="http://eclipse.baeyens.it/download/product/' . $curfile . '" class="btn btn-success btn-lg text-center">Download <b>' . $bitness . ' bits</b> Bundle <i class="glyphicon glyphicon-cloud-download"></i></a>';
		$lines[$bitness]= $lines[$bitness] . '</div>';
	}
	echo $lines [ "64" ];
	echo $lines [ "32" ];
}
function filter($file) {
	global $filter;
	return (substr ( basename ( $file ), 0, strlen ( $filter ) ) == $filter);
}
?>