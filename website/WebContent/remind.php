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
				header ( $_SERVER ["SERVER_PROTOCOL"] . " 404 Not Found", true, 404 );
				// output data of each row
				while ( $row = $result->fetch_assoc () ) {
					echo "Patron: " . $row ["patronIDE"] . " key:" . $KEY . "<br>";
				}
			}
			$conn->close ();
		}
	}
	if ($default == 1) {
		include 'remind3_0.html';
	}
	?> 