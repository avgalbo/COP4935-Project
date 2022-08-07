<?php
  include 'connectDB.php';
  if(!isset($_POST['dev_id']))
    exit;

  $devid = $_POST['dev_id'];
  $devid = mysqli_real_escape_string($link, $devid);

  $query = "SELECT id, pid, name from teeninfo where devicehash = '$devid';";
  $result = mysqli_query($link, $query);
  if(mysqli_num_rows($result) == 0) {
    echo "do setup";
    exit;
  }

  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $id = $row[0];
  $pid = $row[1];
  $teenName = $row[2];
  $tablename = "apps-" . $pid . "-" . $id;

  $package = $_POST['package'];
  $installTime = null;
  $package = mysqli_real_escape_string($link, $package);

  if(isset($_POST['install-time']))
    $installTime = mysqli_real_escape_string($link, $_POST['install-time']);

  if(isset($_POST['uninstalled'])){
    $query = "SELECT name FROM `$tablename` WHERE package='$package';";
    $result = mysqli_query($link, $query);
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $appName = $row[0];

    $query = "UPDATE `$tablename` SET `installed`= '0', `installTime`='$installTime' WHERE `package`='$package';";
  }
  else{
    $appName = $_POST['name'];
    $appName = mysqli_real_escape_string($link, $appName);
    $icon_file = $_FILES['icon']['tmp_name'];
    $imgData= base64_encode(file_get_contents($icon_file)); 
    
    
    //$handle = fopen($icon_file, "r");
    //$icon = fread($handle, filesize($icon_file));

    // //$imageProperties = getimageSize($_FILES['icon']['tmp_name']);
    // $sql = "INSERT INTO output_images(imageType ,imageData)
    // VALUES('{$imageProperties['mime']}', '{$imgData}')";
    // $imgData =addslashes(file_get_contents($_FILES['icon']['tmp_name']));
    //$imgData = addslashes($icon);

    // $check = getimagesize($_FILES["icon"]["tmp_name"]);
    // if($check == false) {
    //     $imgData = null;
    // }

    // if(isset($_POST['uninstalled'])){
    //   $query = "UPDATE `$tablename` SET installed = '0', installTime='$installTime' WHERE package = '$package';";
    //   $result = mysqli_query($link, $query);
    // }
    // $query = "INSERT INTO `$tablename` (`name`, `package`, `installTime`, `icon`, `installed`) VALUES ('$appName', '$package', '$installTime', '{$imgData}', '0') ON DUPLICATE KEY UPDATE `installTime`='$installTime', `installed`='0';";

    if($installTime == null)
        $query = "INSERT INTO `$tablename` (`name`, `package`, `icon`) VALUES ('$appName', '$package', '$imgData');";
    else{
        $query = "INSERT INTO `$tablename` (`name`, `package`, `installTime`, `icon`, `installed`) VALUES ('$appName', '$package', '$installTime', '$imgData', '1') ON DUPLICATE KEY UPDATE `installTime`='$installTime', `installed`='1';";
    }
  }
  $result = mysqli_query($link, $query);

  if($result){
    echo "success";

    if(isset($_POST['initial-upload'])){
      exit;
    }

    $query = "SELECT fcm_token FROM userinfo WHERE id = '$pid';";
    $result = mysqli_query($link, $query);
    if(mysqli_num_rows($result) == 1) {
      $row = mysqli_fetch_array($result, MYSQLI_NUM);
      $fcm_token = $row[0];
    }
    else {
      exit;
    }

    $installoruninstall = isset($_POST['uninstalled'])?"uninstalled":"installed";
    $url = 'https://fcm.googleapis.com/fcm/send';
    $message = "App '" . $appName . "' has been " . $installoruninstall . " by ". $teenName . ".";

    $fields = array (
            'to' => $fcm_token,
            'notification' => array (
              'sound' => 'default',
              'title' => 'App ' . $installoruninstall . ' on teen Device',
              'body' => $message,
              'tag' => $appName,
              'click_action' => 'MAIN_ACTIVITY'
            ),
            'data' => array (
              'action' => 'apps'
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
  }

?>
