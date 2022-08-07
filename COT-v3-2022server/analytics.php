<?php
  //if(isset($_POST['auth_token'])){
  //  $pid = "";
  //  include 'gtoken-signin.php';
  //  if($pid == "")
  //    exit;
  include 'connectDB.php';
  $_POST = json_decode(file_get_contents('php://input'), true);
  $pid = $_POST['pid'];
  
  
    if(isset($_POST['launch'])){
      $query = "UPDATE userinfo SET launch_count = launch_count+1 WHERE id = $pid;";
      $result = mysqli_query($link, $query);
    }

    if(isset($_POST['add_time'])){
      $add_time = mysqli_real_escape_string($link, $_POST['add_time']);
      $query = "UPDATE userinfo SET screen_time = screen_time+$add_time WHERE id = $pid;";
      $result = mysqli_query($link, $query);
    }

    echo "OK";

 /* } else if(isset($_POST['dev_id'])){
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

    if(isset($_POST['launch'])){
      $query = "UPDATE teeninfo SET launch_count = launch_count+1 WHERE id = $pid AND id = $id;";
      $result = mysqli_query($link, $query);
    }

    if(isset($_POST['add_time'])){
      $add_time = mysqli_real_escape_string($link, $_POST['add_time']);
      $query = "UPDATE teeninfo SET screen_time = screen_time+$add_time WHERE id = $pid AND id = $id;";
      $result = mysqli_query($link, $query);
    }

    echo "OK";

  }*/
?>
