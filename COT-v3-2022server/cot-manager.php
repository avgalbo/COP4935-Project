<?php
include 'connectDB.php';

if(!isset($_POST['from']) || !isset($_POST['cot'])
  || !isset($_POST['number']) || !isset($_POST['name']))
  exit;

$cot = mysqli_real_escape_string($link, $_POST['cot']);
$number = mysqli_real_escape_string($link, $_POST['number']);
$name = mysqli_real_escape_string($link, $_POST['name']);
$fcm_token;

if($_POST['from']=="teen"){
  if(!isset($_POST['dev_id']))
    exit;

  $devid = $_POST['dev_id'];
  $devid = mysqli_real_escape_string($link, $devid);
  $contact_pic_data = NULL;
  if(isset($_FILES['contact_pic'])){
    $contact_pic_file = $_FILES['contact_pic']['tmp_name'];
    $handle = fopen($contact_pic_file, "r");
    $pic_data = fread($handle, filesize($contact_pic_file));
    $contact_pic_data = addslashes($pic_data);
  }

  $query = "SELECT id, pid, name from teeninfo where devicehash = '$devid';";
  $result = mysqli_query($link, $query);
  if(mysqli_num_rows($result) == 1){
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $tid = $row[0];
    $pid = $row[1];
    $teenName = $row[2];
    $tablename = "conversations-" . $pid . "-" . $tid;
    if(!isset($_POST['response'])){
      if($cot>1) $cot=1;
    }

    $query = "UPDATE `$tablename` SET `cot`='$cot', `name`='$name' WHERE `number`='$number';";
    $result = mysqli_query($link, $query);
    echo $cot;
    if($contact_pic_data!=NULL){
      $query = "UPDATE `$tablename` SET `pic`='{$contact_pic_data}' WHERE `number`='$number';";
      $result = mysqli_query($link, $query);
    }

    //send notification to parent asking for cot approval
    $query = "SELECT fcm_token FROM userinfo WHERE id = '$pid';";
    $result = mysqli_query($link, $query);
    if(mysqli_num_rows($result) == 1) {
      $row = mysqli_fetch_array($result, MYSQLI_NUM);
      $fcm_token = $row[0];
    }
    else {
      exit;
    }

    $url = 'https://fcm.googleapis.com/fcm/send';
    $message = "";
    $title = "";
    if($cot=="0"){
      $title = "Person removed from Circle of Trust";
      if(isset($_POST['response'])){
        $message = $teenName . " accepted your request to remove ". $name . " from Circle of Trust";
      }else{
        $message = $teenName . " removed ". $name . " from Circle of Trust";
      }
    }
    else if($cot=="1"){
      $title = "New Circle of Trust request";
      $message = $teenName ." requested to add ". $name . " to Circle of Trust. Tap to review.";
    }
    else if($cot=="2"){
      $title = "Circle of Trust removal denied";
      $message = $teenName ." denied to remove ". $name . " from Circle of Trust.";
    }

    $fields = array (
            'to' => $fcm_token,
            'notification' => array (
              'sound' => 'default',
              'title' => $title,
              'body' => $message,
              'tag' => $number,
              'click_action' => 'MAIN_ACTIVITY'
            ),
            'data' => array (
              'action' => 'cot',
              'name' => $name,
              'number' => $number
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
}
else if($_POST['from']=="parent"){
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

  $number = $_POST['number'];
  $number = mysqli_real_escape_string($link, $number);
  $tablename = "conversations-" . $pid . "-" . $teenid;
  if(!isset($_POST['response'])){
    if($cot<1) $cot=3;
  }

  $query = "UPDATE `$tablename` SET `cot`='$cot', `name`='$name' WHERE `number`='$number';";
  $result = mysqli_query($link, $query);
  echo $cot;

  //send notification to parent asking for cot approval
  $query = "SELECT fcm_token FROM teeninfo WHERE pid = '$pid' AND id = '$teenid';";
  $result = mysqli_query($link, $query);
  if(mysqli_num_rows($result) == 1) {
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $fcm_token = $row[0];
  }
  else {
    exit;
  }

  $url = 'https://fcm.googleapis.com/fcm/send';
  $message = "";
  if($cot=="2"){
    $title = "Person added to Circle of Trust";
    if(isset($_POST['response'])){
      $message = "Your parent approved your request to add " . $name ." to Circle of Trust";
    }else{
      $message = "Your parent added ". $name . " to Circle of Trust";
    }
  }
  else if($cot=="3"){
    $title = "New Circle of Trust removal request";
    $message = "Your parent requested to remove ". $name . " from Circle of Trust. Tap to review.";
  }
  else if($cot=="0"){
    $title = "New Circle of Trust addition denied";
    $message = "Your parent denied your request to add ". $name . " to Circle of Trust.";
  }

  $fields = array (
          'to' => $fcm_token,
          'notification' => array (
            'sound' => 'default',
            'title' => $title,
            'body' => $message,
            'tag' => $number,
            'click_action' => 'MAIN_ACTIVITY'
          ),
          'data' => array (
            'action' => 'cot',
            'name' => $name,
            'number' => $number
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
