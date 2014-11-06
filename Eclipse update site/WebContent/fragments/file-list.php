      <?php
        function listFiles($prefix) {
          date_default_timezone_set('UTC');
          $location="download/product";
          //$location=".";
          $dir = opendir($location);
          while(false != ($file = readdir($dir))) {
            if(($file != ".") and ($file != "..") and ($file != "index.php")) {
              $files[] = $location."/".$file; // put in array.
            }
          }
          closedir($dir);
          rsort($files); // sort.
          foreach($files as $file) {
            $refname=basename($file);
            if (substr($refname,0,strlen($prefix))==$prefix) {
              $stat = stat($file);
              $date = date('Y-m-d',$stat['mtime']);
              $size = formatBytes($stat['size']);
              echo "<tr>";
              echo "<td class='text-center'>$date</td>";
              echo "<td><a href='$file' target='_blank'><i class='glyphicon glyphicon-cloud-download'></i> $refname</a></td>";
              echo "<td class='text-right'>$size</td>";
              echo "</tr>";
            }
          }
        }
        function formatBytes($bytes, $precision = 2) {
          $units = array('B', 'KB', 'MB', 'GB', 'TB');
          $bytes = max($bytes, 0);
          $pow = floor(($bytes ? log($bytes) : 0) / log(1024));
          $pow = min($pow, count($units) - 1);
          $bytes /= pow(1024, $pow);
          return round($bytes, $precision) . ' ' . $units[$pow];
        }
      ?>

      <div class="panel panel-default">
        <table class="table table-striped table-hover">
          <tr>
            <th class="text-center col-md-2">Date</th>
            <th>Filename</th>
            <th class="text-right col-md-4">Size</th>
          </tr>
          <?php listFiles($_GET["arch"]); ?>
        </table>
      </div>