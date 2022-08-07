<?php
include 'connectDB.php';
if(!isset($_POST['dev_id']))
  exit;

$devid = $_POST['dev_id'];
$devid = mysqli_real_escape_string($link, $devid);

$status = $_POST['status'];
$status = mysqli_real_escape_string($link, $status);

$ts = $_POST['ts'];
$ts = mysqli_real_escape_string($link, $ts);

$query = "SELECT pid, name from teeninfo where devicehash = '$devid';";
$result = mysqli_query($link, $query);
if(mysqli_num_rows($result) == 0) {
  exit;
}
$row = mysqli_fetch_array($result, MYSQLI_NUM);
$pid = $row[0];
$teenName = $row[1];

//send notification
$query = "SELECT fcm_token FROM userinfo WHERE id = '$pid';";
$result = mysqli_query($link, $query);
if(mysqli_num_rows($result) == 1) {
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $fcm_token = $row[0];
}
else {
  exit;
}
//update status in database
$query = "UPDATE teeninfo SET `dev_admin`='$status', `da_ts`='$ts' WHERE devicehash = '$devid';";
$result = mysqli_query($link, $query);

//send notification to parent
$url = 'https://fcm.googleapis.com/fcm/send';
$message = "";
if($status=="0"){
  $title = "Teen Monitor App Possibly Uninstalled!";
  $message = "Device Admin has been disabled by ".$teenName .". Make sure the Teen Monitor is installed and configured correctly.";
}
else{
  $title = "Teen Monitor App Re-configured Correctly";
  $message = "Device Admin has been re-enabled on ".$teenName ."'s device.";
}

$fields = array (
        'to' => $fcm_token,
        'notification' => array (
          'sound' => 'default',
          'title' => $title,
          'body' => $message,
          'tag' => 'dev_admin'
        )
);
$fields = json_encode ( $fields );
$headers = array (
        'Authorization: key=' . "AAAAz1-tpsM:APA91bEMOObtajHFwl7mNDJ16omYii70Jn67SWM8DstGw_GmM2v_-mvm-PBsH1SlgFR-cJLCCM5OpDk8eNkw7KzzKXHONsgoDTv0PUNcwmeOJxEM5X2dZZCy-qNtgFpvfPTNlcv_HTOz", 
        'Content-Type: application/json'
);

$ch = curl_init ();
curl_setopt ( $ch, CURLOPT_URL, $url );
curl_setopt ( $ch, CURLOPT_CUSTOMREQUEST,"POST");
curl_setopt ( $ch, CURLOPT_HTTPHEADER, $headers );
curl_setopt ( $ch, CURLOPT_POSTFIELDS, $fields );
curl_setopt ( $ch, CURLOPT_RETURNTRANSFER, true);

$result = curl_exec ( $ch );
curl_close ( $ch );
?>
