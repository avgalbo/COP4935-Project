<?php
  include 'connectDB.php';
  if(!isset($_POST['dev_id']))
    exit;

  $devid = $_POST['dev_id'];
  $devid = mysqli_real_escape_string($link, $devid);

  $query = "SELECT id, pid from teeninfo where devicehash = '$devid';";
  $result = mysqli_query($link, $query);
  if(mysqli_num_rows($result) == 1){
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $tid = $row[0];
    $pid = $row[1];
    $tablename = "texts-" . $pid . "-" . $tid;
    $query = "SELECT MAX(time) FROM `$tablename`;";
    $result = mysqli_query($link, $query);
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $last_time = $row[0];
    if($last_time == "")
      $last_time = 0;
    echo "yes|" . $last_time;
  }
?>
