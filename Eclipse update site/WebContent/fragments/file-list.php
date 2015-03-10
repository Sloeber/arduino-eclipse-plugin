<?php
function listFiles($prefix) {
	date_default_timezone_set ( 'UTC' );
	$location = "../download/product";
	echo "<!-- listing files in $location with prefix $prefix -->";
	if (false != ($dir = opendir ( $location ))) {
		while ( false != ($file = readdir ( $dir )) ) {
			if (($file != ".") and ($file != "..") and ($file != "index.php")) {
				$files [] = $location . "/" . $file; // put in array.
			}
		}
		closedir ( $dir );
	} else {
		$location = "../../download/product";
		if (false != ($dir = opendir ( $location ))) {
			while ( false != ($file = readdir ( $dir )) ) {
				if (($file != ".") and ($file != "..") and ($file != "index.php")) {
					$files [] = $location . "/" . $file; // put in array.
				}
			}
			closedir ( $dir );
		}
	}
	rsort ( $files );
	foreach ( $files as $file ) {
		$refname = basename ( $file );
		if (substr ( $refname, 0, strlen ( $prefix ) ) == $prefix) {
			$stat = stat ( $file );
			$date = date ( 'Y-m-d', $stat ['mtime'] );
			$size = formatBytes ( $stat ['size'] );
			echo "<tr class='clickable'>";
			echo "<td class='text-center'>$date</td>";
			echo "<td><a href='http://eclipse.baeyens.it/download/product/$refname' target='_blank'><i class='glyphicon glyphicon-cloud-download'></i> $refname</a></td>";
			echo "<td class='text-right'>$size</td>";
			echo "</tr>";
		}
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

<table class="table table-striped table-hover">
	<thead>
		<tr>
			<th class="text-center col-md-2">Date</th>
			<th>Filename</th>
			<th class="text-right col-md-4">Size</th>
		</tr>
	</thead>
	<tbody>
  <?php if(isset($_GET["arch"])) listFiles($_GET["arch"]); ?>
  <?php if(isset($_GET["ver"])) listFiles("V" . $_GET["ver"] . '_'); ?>
  </tbody>
</table>

<script>
  $('tr.clickable').click(function() {
    $(this).find('a')[0].click();
  });
</script>