<?php
// Use your Heroku Postgres connection string here
// Example: postgres://username:password@host:5432/dbname
$dbUrl = getenv('DATABASE_URL') ?: 'postgres://u9o7tguuile3km:pdbc0711015539daf265db61a087a4fc6d094ef8f1db4b7a7b763f88062108034@c3cj4hehegopde.cluster-czrs8kj4isg7.us-east-1.rds.amazonaws.com:5432/d1ho4vshg30ue9';

// Parse the URL to get the connection details
$dbopts = parse_url($dbUrl);
$host = $dbopts["host"];
$port = $dbopts["port"];
$user = $dbopts["user"];
$password = $dbopts["pass"];
$dbname = ltrim($dbopts["path"], '/');

try {
    $conn = new PDO("pgsql:host=$host;port=$port;dbname=$dbname", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    echo "Connection failed: " . $e->getMessage();
    exit;
}
?>
