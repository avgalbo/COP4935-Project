<?php
  if(!isset($_POST['fcm_token'])){
    exit;
  }
  $fcm_token = $_POST['fcm_token'];
  $fcm_token = mysqli_real_escape_string($link, $fcm_token);

  if(isset($_POST['auth_token'])){
    $pid = "";
    include 'gtoken-signin.php';
    if($pid == "")
      exit;

    $query = "INSERT INTO userinfo (fcm_token) VALUES ('$fcm_token');";
    $result = mysqli_query($link, $query);
    echo "success";

  } else if(isset($_POST['dev_id'])){
    include 'connectDB.php';
    $devid = $_POST['dev_id'];
    $devid = mysqli_real_escape_string($link, $devid);

    $query = "SELECT pid, id from teeninfo where devicehash = '$devid';";
    $result = mysqli_query($link, $query);
    if(mysqli_num_rows($result) == 0) {
      exit;
    }
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $pid = $row[0];
    $id = $row[1];

    $query = "INSERT INTO teeninfo (fcm_token) VALUES ('$fcm_token');";
    $result = mysqli_query($link, $query);
    echo "success";
  }

?>
