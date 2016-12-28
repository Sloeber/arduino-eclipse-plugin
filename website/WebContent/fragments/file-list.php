<?php

function listFiles($prefix) {

echo '<table class="table table-striped table-hover">';
	echo "<thead>";
		echo "<tr>";
echo '<th class="text-center col-md-2">Date</th>';
			echo "<th>Filename</th>";
			echo '<th class="text-right col-md-4">Size</th>';
		echo "</tr>";
	echo "</thead>";
	echo "<tbody>";
	  include "files.php";

	date_default_timezone_set ( 'UTC' );

	rsort ( $files );
	$count = 0;
	foreach ( $files as $file ) {
		if ($count < 5) {
			$refname = basename ( $file );
			if (substr ( $refname, 0, strlen ( $prefix ) ) == $prefix) {
				$count = $count + 1;
				$stat = stat ( $file );
				$date = date ( 'Y-m-d', $stat ['mtime'] );
				$size = formatBytes ( $stat ['size'] );
				$date = "unknown";
				$size = "unknown";
				echo "<tr class='clickable'>";
				echo "<td class='text-center'>$date</td>";
				echo "<td><a href='$file' target='_blank'><i class='glyphicon glyphicon-cloud-download'></i> $refname</a></td>";
				echo "<td class='text-right'>$size</td>";
				echo "</tr>";
				}

		}
	}
 echo "</tbody>";
echo "</table>";
if($count==0){
    echo "No files found with prefix ".$prefix;
}


}
function formatBytes($bytes, $precision = 2) {
	$units = array (
			'B',
			'KB',
			'MB',
			'GB',
			'TB'
	);
	$bytes = max ( $bytes, 0 );
	$pow = floor ( ($bytes ? log ( $bytes ) : 0) / log ( 1024 ) );
	$pow = min ( $pow, count ( $units ) - 1 );
	$bytes /= pow ( 1024, $pow );
	return round ( $bytes, $precision ) . ' ' . $units [$pow];
}


?>


