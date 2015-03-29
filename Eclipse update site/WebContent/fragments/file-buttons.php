<?php
  if(isset($_GET["ver"]) && isset($_GET["arch"])) 
    listFiles("V" . $_GET["ver"] . '_' . $_GET["arch"]);

  function listFiles($prefix) {
    global $filter;
    $filter = $prefix;
    date_default_timezone_set('UTC');
    $location="../download/product";
    echo "<!-- listing files in $location with prefix $prefix -->";
    $dir = opendir($location);
    while(false != ($file = readdir($dir))) {
      if(($file != ".") and ($file != "..") and ($file != "index.php")) {
        $files[] = $location."/".$file; // put in array.
      }
    }
    closedir($dir);
    $location="../../download/product";
    $dir = opendir($location);
    while(false != ($file = readdir($dir))) {
      if(($file != ".") and ($file != "..") and ($file != "index.php")) {
        $files[] = $location."/".$file; // put in array.
      }
    }
    closedir($dir);
    $files = array_filter($files, "filter");
    sort($files);
    $count = count($files);
    if ($count == 1) {
    	$curfile=basename($files[0]);
      echo '<div class="text-center col-md-4 col-md-offset-4">';
      echo '  <a href="http://eclipse.baeyens.it/download/product/' . $curfile . '?forcedownload" class="btn btn-success btn-lg text-center">Download <b>' . substr($curfile, strlen($prefix), 2) . ' bits</b> Bundle <i class="glyphicon glyphicon-cloud-download"></i></a>';
      echo '</div>';
    } else if ($count == 2) {
    	$curfile=basename($files[0]);
      echo '<div class="text-center col-md-3 col-md-offset-3">';
      echo '  <a href="http://eclipse.baeyens.it/download/product/' . $curfile . '?forcedownload" class="btn btn-success btn-lg text-center">Download <b>' . substr($curfile, strlen($prefix), 2) . ' bits</b> Bundle <i class="glyphicon glyphicon-cloud-download"></i></a>';
      echo '</div>';
      $curfile=basename($files[1]);
      echo '<div class="text-center col-md-3">';
      echo '  <a href="http://eclipse.baeyens.it/download/product/' . $curfile . '?forcedownload" class="btn btn-success btn-lg text-center">Download <b>' . substr($curfile, strlen($prefix), 2) . ' bits</b> Bundle <i class="glyphicon glyphicon-cloud-download"></i></a>';
      echo '</div>';
    } else {
      echo "<!-- there are $count files in $location with prefix $prefix -->";
    }
  }
  function filter($file) {
    global $filter;
    return(substr(basename($file), 0, strlen($filter)) == $filter);
  }
?>