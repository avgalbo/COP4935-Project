<?php
  $pid = "";
  $otpname = true;
  include 'gtoken-signin.php';
  if($pid == ""){
    echo "auth error";
    exit;
  }

  if(!isset($_POST['name']))
    exit;

  $name = mysqli_real_escape_string($link, $_POST['name']);

  $query = "UPDATE otp_map SET teen_name = '$name' WHERE pid = '$pid';";
  $result = mysqli_query($link, $query);
  echo "success";
?>
