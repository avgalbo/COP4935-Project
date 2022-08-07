<?php
  $teenid = "";
  $pid = "";
  $link = NULL;

  if(isset($_POST['auth_token'])){
    //parent incoming
    include 'gtoken-signin.php';
    if($pid == "")
      exit;

    $query = "SELECT id FROM teeninfo WHERE pid = '$pid';";
    $result = mysqli_query($link, $query);
    if(mysqli_num_rows($result) == 0) {
      echo "No teen device paired!";
      exit;
    }
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $teenid = $row[0];

  } else if(isset($_POST['dev_id'])){
    //child incoming
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
    $teenid = $row[1];
  }

  $number = $_POST['number'];
  $number = mysqli_real_escape_string($link, $number);

  $tablename = "apps-" . $pid . "-" . $teenid;

  //mysqli_query($link, 'SET CHARACTER SET utf8');
  $query = "SELECT name, package, icon, installTime, installed FROM `$tablename` ORDER BY name";
  $appJSON;
  $out = array();
  if ($result = mysqli_query($link, $query))
  {
    while ($row = mysqli_fetch_row($result))
    {
      $out[] = array('name' => $row[0], 'package' => $row[1],
        'icon' => base64_encode($row[2]), 'status' => $row[4], 'time' => $row[3]);
    }
  }
  $appJSON = json_encode($out);

  if($appJSON==null)
    echo "null";
  else
    echo $appJSON;
?>
