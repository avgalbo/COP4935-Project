<?php
  include 'connectDB.php';
  if(!isset($_POST['dev_id']))
    exit;

  $devid = $_POST['dev_id'];
  $devid = mysqli_real_escape_string($link, $devid);

  $query = "SELECT id, pid, name from teeninfo where devicehash = '$devid';";
  $result = mysqli_query($link, $query);
  if(mysqli_num_rows($result) == 0) {
    exit;
  }
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $id = $row[0];
  $pid = $row[1];
  $teenName = $row[2];

  if(isset($_POST['initial-upload'])){
    $json = file_get_contents('php://input');
    $textsArray = json_decode($json);

    foreach($textsArray as $item) {
      $number = $item['number']; //etc
      $time = $item['time'];
      $message = $item['b64msg'];
      $name = $item['name'];
      $sent = $item['sent'];
      $mimeType = NULL;
      $media_data = NULL;
      $contact_pic_data = NULL;

      if(issset($item['pic'])){
        $contact_pic_data = addslashes(base64_decode($item['pic']));
      }

      if(issset($item['mime'])){
        $mimeType = $item['mime'];
      }

      if(issset($item['media'])){
        $media_data = addslashes(base64_decode($item['media']));
      }

      storeText($number, $time, $message, $name, $sent, $contact_pic_data, $mimeType, $media_data);
    }

    echo "success";
    exit;
  }

  function storeText($number, $time, $message, $name, $sent, $contact_pic_data, $mimeType, $media_data){
    $number = mysqli_real_escape_string($link, $number);
    $time = mysqli_real_escape_string($link, $time);
    $message = addslashes(base64_decode($message));
    $name = mysqli_real_escape_string($link, $name);
    $sent = mysqli_real_escape_string($link, $sent);
    $mimeType = mysqli_real_escape_string($link, $mimeType);

    $tablename = "texts-" . $pid . "-" . $id;

    if($mimeType==NULL || $media_data==NULL)
      $query = "INSERT INTO `$tablename` (`number`, `time`, `text`, `name`, `sent`) VALUES ('$number', '$time', '$message', '$name', '$sent') ON DUPLICATE KEY UPDATE `name`='$name';";
    else {
      $query = "INSERT INTO `$tablename` (`number`, `time`, `text`, `name`, `sent`, `media`, `mime`) VALUES ('$number', '$time', '$message', '$name', '$sent', '{$media_data}', '$mimeType') ON DUPLICATE KEY UPDATE `name`='$name';";
    }
    $result = mysqli_query($link, $query);

    if($result){
      //update conversations table
      $tablename = "conversations-" . $pid . "-" . $id;
      $received = abs($sent-1);
      if($contact_pic_data==NULL)
        $query = "INSERT INTO `$tablename` (`name`, `number`, `time`, `sent`, `received`) VALUES ('$name', '$number', '$time', '$sent', '$received') ON DUPLICATE KEY UPDATE `name`='$name', `time`='$time', `sent`=sent+$sent, `received`=received+$received;";
      else
        $query = "INSERT INTO `$tablename` (`name`, `number`, `time`, `sent`, `received`, `pic`) VALUES ('$name', '$number', '$time', '$sent', '$received', '{$contact_pic_data}') ON DUPLICATE KEY UPDATE `name`='$name', `time`='$time', `sent`=sent+$sent, `received`=received+$received, `pic`='{$contact_pic_data}';";

      mysqli_query($link, $query);
    }

  }

  $number = $_POST['number'];
  $time = $_POST['time'];
  $message = $_POST['text'];
  $name = $_POST['name'];
  $sent = $_POST['sent'];
  $mimeType = NULL;
  $media_data = NULL;
  $contact_pic_data = NULL;

  if(isset($_POST['mime']) && isset($_FILES['media'])){
    $media_file = $_FILES['media']['tmp_name'];
    $mimeType = mysqli_real_escape_string($link, $_POST['mime']);
    $handle = fopen($media_file, "r");
    $file_data = fread($handle, filesize($media_file));
    $media_data = addslashes($file_data);
  }

  if(isset($_FILES['contact_pic'])){
    $contact_pic_file = $_FILES['contact_pic']['tmp_name'];
    $handle = fopen($contact_pic_file, "r");
    $pic_data = fread($handle, filesize($contact_pic_file));
    $contact_pic_data = addslashes($pic_data);
  }

  $number = mysqli_real_escape_string($link, $number);
  $time = mysqli_real_escape_string($link, $time);
//  $message = mysqli_real_escape_string($link, base64_decode($message));
  $message = addslashes(base64_decode($message));
  $name = mysqli_real_escape_string($link, $name);
  $sent = mysqli_real_escape_string($link, $sent);

  // if($sent == "true")
  //   $sent = 1;
  // else
  //   $sent = 0;

  $tablename = "texts-" . $pid . "-" . $id;

  if($mimeType==NULL || $media_data==NULL)
    $query = "INSERT INTO `$tablename` (`number`, `time`, `text`, `name`, `sent`) VALUES ('$number', '$time', '$message', '$name', '$sent') ON DUPLICATE KEY UPDATE `name`='$name';";
  else {
    $query = "INSERT INTO `$tablename` (`number`, `time`, `text`, `name`, `sent`, `media`, `mime`) VALUES ('$number', '$time', '$message', '$name', '$sent', '{$media_data}', '$mimeType') ON DUPLICATE KEY UPDATE `name`='$name';";
  }
  $result = mysqli_query($link, $query);

  if($result){
    echo "success";

    //update conversations table
    $tablename = "conversations-" . $pid . "-" . $id;
    $received = abs($sent-1);
    if($contact_pic_data==NULL)
      $query = "INSERT INTO `$tablename` (`name`, `number`, `time`, `sent`, `received`) VALUES ('$name', '$number', '$time', '$sent', '$received') ON DUPLICATE KEY UPDATE `name`='$name', `time`='$time', `sent`=sent+$sent, `received`=received+$received;";
    else
      $query = "INSERT INTO `$tablename` (`name`, `number`, `time`, `sent`, `received`, `pic`) VALUES ('$name', '$number', '$time', '$sent', '$received', '{$contact_pic_data}') ON DUPLICATE KEY UPDATE `name`='$name', `time`='$time', `sent`=sent+$sent, `received`=received+$received, `pic`='{$contact_pic_data}';";

    mysqli_query($link, $query);

    if(isset($_POST['initial-upload'])){
      exit;
    }

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

    $url = 'https://fcm.googleapis.com/fcm/send'; //Suspect
    $message = "";
    if($name == "")
      $name = $number;

    if($sent == 1)
      $message = $teenName . " sent a text to " . $name . ".";
    else
      $message = $teenName . " received a text from " . $name . ".";

    $fields = array (
            'to' => $fcm_token,
            'notification' => array (
              'sound' => 'default',
              'title' => 'Texting Activity on '. $teenName .'\'s Device',
              'body' => $message,
              'tag' => $name,
              'click_action' => 'MAIN_ACTIVITY'
            ),
            'data' => array (
              'action' => 'text',
              'name' => $name,
              'number' => $number
            )
    );
    $fields = json_encode ( $fields );
    $headers = array (
            'Authorization: key=' . "AIzaSyBEqD3oAxLsKPY482POGCBWzB4uFgRFwLo",
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
