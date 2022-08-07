<?php
echo "This is a test";
date_default_timezone_set('America/New_York');
$timezone = date_default_timezone_get();
echo "\nThis is the server timezone: " . $timezone;
$hour = date('H');
$title = "";
$name = 'Anh';
if($hour >= 0 && $hour <= 5)
	$title = 'After Hour from ' . $name . '\'s Device';
else
	$title = 'Normal';

date_default_timezone_set('America/New_York');
    $timezone = date_default_timezone_get();
    $hour = date('H');
    $title = "";
    if($hour >= 0 && $hour <= 5)
        $title = 'After Hour Texting Activity on ' . $name . '\'s Device';
    else
	    $title = 'Texting Activity on ' . $name . '\'s Device';
$appName = "Twitter";
$installoruninstall = "installed";
$teenName = "Anh";
$message = "App '" . $appName . "' has been " . $installoruninstall . " by ". $teenName . ".";
echo $title;
echo $message;
?>
