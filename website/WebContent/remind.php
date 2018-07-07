 <?php
    include 'globals.txt';
	$default = 1;
	if (isset ( $_GET ["systemhash"] )) {
		$KEY = $_GET ["systemhash"];
		include 'secret.txt';

		// Create connection
		$conn = new mysqli ( $servername, $username, $password, $dbname );

		// Check connection
		if (! $conn->connect_error) {
			$sql = 'SELECT  patronIDE FROM Patrons WHERE hascode="' . $KEY . '"';
			$result = $conn->query ( $sql );

			if ($result->num_rows > 0) {
				$default = 0;
				echo $secretMessage;
				// output data of each row
				while ( $row = $result->fetch_assoc () ) {
					echo "Patron: " . $row ["patronIDE"] . " key:" . $KEY . "<br>";
				}
			}
			$conn->close ();
		}
	}
	if ($default == 1) {
	    $OS = $_GET ["os"];
	    $ARCH = $_GET ["arch"];
	    $VERSION_MAJOR = $_GET ["majorVersion"];
	    $VERSION_MINOR = $_GET ["minorVersion"];
	    $VERSION_MICRO = $_GET ["microVersion"];
	    $VERSION_QUAL = $_GET ["qualifierVersion"];
	    $ADVICE_UPGRADE =($VERSION_MAJOR < $STABLE_VERSION_MAJOR)  || (($VERSION_MAJOR == $STABLE_VERSION_MAJOR) && ($VERSION_MINOR<$STABLE_VERSION_MINOR ) );
	    $ADVERTISE_HIDE_THIS_PAGE =($VERSION_MAJOR > 3);
	    ?>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Thanks for using our plugin.</title>
<link href="MyStyle.css" rel="Stylesheet" type="text/css">
</head>
<body>
<div id="header">
  <a href="http://www.eclipse.org/"><img src="https://raw.githubusercontent.com/jantje/arduino-eclipse-plugin/master/io.sloeber.application/splash.bmp" width="300" height="150" border="0" alt="Eclipse Logo" class="logo" /></a>
</div>
<div style="float:left; width:80%;">
<div>
<h1>So, you think Jantje and the contributors did a great job?</h1>
<h2><a href="http://eclipse.baeyens.it/donate.shtml">Why don't you show it?</a></h2>
<p>Note that 1 dollar a month makes a world of difference to us.</p>
<a href="http://eclipse.baeyens.it/donate.shtml">
    <h1>Do you want a Sloeber T-shirt</h1>
<img alt="Buy a t-shirt" border="0" src="http://baeyens.it/Media/vaderendochter.jpg" width=400 height=300>
</a>


<?php if($ADVERTISE_HIDE_THIS_PAGE){ ?>
<h2><a href="https://www.patreon.com/bePatron?rid=228464&u=798640"> for 5 dollar a month (exclusive VAT) you can get rid of this reminder.</a> </h2>
<?php } ?>


<?php if($ADVICE_UPGRADE){
    echo "<h1>You are running Sloeber V".$VERSION_MAJOR.".".$VERSION_MINOR." and the Latest stable version is V".$STABLE_VERSION_MAJOR.".".$STABLE_VERSION_MINOR."!</h1>" ?>
Please consider upgrading to the latest Stable.<br>
Click <a href="http://eclipse.baeyens.it/how_to.shtml#/n">here to see how to upgrade.</a>
<?php } ?>


</div>

<div >
  <table width="100%">
    <tr>
      <th colspan=2 align=left>Jantje thanks all code contributors (in no particular order)</th>
    </tr>
    <tr>
      <td>
      <img src="https://2.gravatar.com/avatar/54a671636f1178fd1ce6ff80cff21a23?d=https%3A%2F%2Fidenticons.github.com%2F682ec544d44cf6a3760a9d07f9c989a6.png&r=x&s=60" width=30 height=30><a href="https://github.com/wimjongman">wimjongman</a><br>
      <img src="https://avatars1.githubusercontent.com/u/199473?v=3&s=60" width=30 height=30><a href="https://github.com/rlogiacco">rlogiacco</a><br>
      <img src="https://avatars3.githubusercontent.com/u/19333281?v=3&s=460" width=30 height=30><a href="https://github.com/MarceloLimori">Marcelo Limori</a><br>
      <img src="https://2.gravatar.com/avatar/4416a893377f317cb23a0b0b72e18a3e?d=https%3A%2F%2Fidenticons.github.com%2F00b6b0fffe8118d18c541731d6a094c0.png&amp;r=x&amp;s=60" width=30 height=30><a href="https://github.com/brodykenrick" >brodykenrick</a><br>
    </td>
  </tr>
  </table>
</div>
</div>
<div style="float:left; width:20%;">
  <table width="100%">
    <tr>
      <th colspan=2 align=left><h2>Following boards are fully supported thanks to board patrons</h2></th>
    </tr>
    <tr>
      <td>
      <h3>Mini Ultra Pro V3</h3>
      <a href="http://www.rocketscream.com/blog/product/mini-ultra-pro-v3-with-radio/"><img src="http://www.rocketscream.com/blog/wp-content/uploads/2018/02/DEV-00067-FRONT.jpg" width=120 height=120 ></a><br>
      Thanks to <br>
      <img src="https://www.rocketscream.com/blog/wp-content/uploads/2016/02/logo.png"  width=200 height=120><br>
    </td>
  </tr>
  </table>
</div>
</body>
</html>

 <?php } ?>