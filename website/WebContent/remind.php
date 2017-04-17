 <?php
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
	    $ADVICE_UPGRADE =(($VERSION_MAJOR < 3) && ($VERSION_MAJOR >0)) || (($VERSION_MAJOR == 3) && ($VERSION_MINOR < 1) );
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
<div>
<h1>So, you think Jantje and the contributors did a great job?</h1>
<h2><a href="https://www.patreon.com/jantje?ty=h">Why don't you show it?</a></h2>
<p>Or simply clik the button below!</p>
<a href="https://www.patreon.com/bePatron?u=798640&redirect_uri=http%3A%2F%2Fpatron.baeyens.it%2Fthanks.php">
    <img height="40" width="204" src="https://s3-us-west-1.amazonaws.com/widget-images/become-patron-widget-medium%402x.png">
</a>
<p>Note that 1 dollar a month makes a world of difference to us.</p>

<?php if($ADVERTISE_HIDE_THIS_PAGE){ ?>
<h2><a href="https://www.patreon.com/bePatron?rid=228464&u=798640"> for 5 dollar a month (exclusive VAT) you can get rid of this reminder.</a> </h2>
<?php } ?>


<?php if($ADVICE_UPGRADE){
echo "<h1>You are running Sloeber V".$VERSION_MAJOR.".".$VERSION_MINOR." and the Latest stable version is V3.1!</h1>" ?>
Please consider upgrading to the latest Stable.<br>
Click <a href="http://eclipse.baeyens.it/how_to.shtml#/n">here to see how to upgrade.</a>
<?php } ?>


</div>

<div>
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
      <img src="https://1.gravatar.com/avatar/05031a297051639ce921bba19024ca15?d=https%3A%2F%2Fidenticons.github.com%2Fb49b0e01582af2658c855692ea3807fc.png&r=x&s=60" width=30 height=30><a href="https://github.com/neuweiler">neuweiler</a><br>
      <img src="https://1.gravatar.com/avatar/912644f6b65460cdf7b5e3f2ff1e52ba?d=https%3A%2F%2Fidenticons.github.com%2F4dbf38d246b5e8553a14c0536b80e856.png&r=x&s=60" width=30 height=30><a href="https://github.com/Darcade">Darcade</a><br>
      <img src="https://avatars3.githubusercontent.com/u/2527331?v=3&s=60" width=30 height=30><a href="https://github.com/henols">henols</a><br>
      <img src="https://avatars1.githubusercontent.com/u/9949033?v=3&s=60" width=30 height=30><a href="https://github.com/Stefan-Code">Stefan-Code</a><br>
      <img src="https://avatars0.githubusercontent.com/u/4212876?v=3&s=60" width=30 height=30><a href="https://github.com/neuweiler">neuweiler</a><br>
      <img src="https://avatars3.githubusercontent.com/u/259982?v=3&s=60" width=30 height=30><a href="https://github.com/kigster">kigster</a><br>
      <img src="https://avatars3.githubusercontent.com/u/2698169?v=3&s=60" width=30 height=30><a href="https://github.com/amorellgarcia">amorellgarcia</a><br>
      <img src="https://avatars1.githubusercontent.com/u/10581272?v=3&s=460" width=30 height=30><a href="https://github.com/SuperOok">Hauke Fuhrmann</a><br>
      <img src="https://avatars1.githubusercontent.com/u/1100327?v=3&s=460" width=30 height=30><a href="https://github.com/alexandrezia">Alexandre Zia</a><br>
      <img src="https://avatars1.githubusercontent.com/u/2027490?v=3&s=60" width=30 height=30><a href="https://github.com/mjmeijer">mjmeijer</a><br>
      <img src="https://avatars1.githubusercontent.com/u/5672365?v=3&s=60" width=30 height=30><a href="https://github.com/AhmedObaidi">AhmedObaidi</a><br>
      <img src="https://avatars1.githubusercontent.com/u/1614482?v=3&s=60" width=30 height=30><a href="https://github.com/paulvi">paulvi</a><br>
      <img src="https://avatars0.githubusercontent.com/u/111074?v=3&s=60" width=30 height=30><a href="https://github.com/tnarik">tnarik</a><br>
      <img src="https://avatars3.githubusercontent.com/u/1475287?v=3&s=60" width=30 height=30><a href="https://github.com/evil-dog">evil-dog</a><br>
      <img src="https://avatars1.githubusercontent.com/u/1312932?v=3&s=60" width=30 height=30><a href="https://github.com/nicoverduin">nicoverduin</a><br>
      <img src="https://avatars1.githubusercontent.com/u/3158323?v=3&s=60" width=30 height=30><a href="https://github.com/riban-bw">riban-bw</a><br>
      <img src="https://avatars3.githubusercontent.com/u/12892705?v=3&s=60" width=30 height=30><a href="https://github.com/b3ndo">b3ndo</a><br>
      <img src="https://avatars1.githubusercontent.com/u/2420404?v=3&s=60" width=30 height=30><a href="https://github.com/witold-markowski-sentaca">witold-markowski-sentaca</a><br>
      <img src="https://avatars1.githubusercontent.com/u/11740256?v=3&s=60" width=30 height=30><a href="https://github.com/jipp">jipp</a><br>
      <img src="https://avatars1.githubusercontent.com/u/16732352?v=3&s=460" width=30 height=30><a href="https://github.com/georgekankava">George Kankava</a><br>
      <img src="https://avatars0.githubusercontent.com/u/4579183?v=3&s=460" width=30 height=30><a href="https://github.com/infthi">infthi</a><br>
      <img src="https://avatars0.githubusercontent.com/u/5371865?v=3&s=460" width=30 height=30><a href="https://github.com/nafep">Stefan Eppe</a><br>
    </td>
  </tr>
  </table>
</div>
</body>
</html>

 <?php } ?>