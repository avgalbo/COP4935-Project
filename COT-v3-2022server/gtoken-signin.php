<?php
  require_once 'google-api-php-client-2.2.1/vendor/autoload.php';
  include 'connectDB.php';

  if(!isset($_POST['auth_token']))
    exit;

  $id_token = $_POST['auth_token'];

  // 1) Anthony changed this on 5/31/22 at 2:09pm	
  // Anh changed this on 5/31/22 at 9:12 pm
  $CLIENT_ID = "375757255707-hao3bt67vuk0nipjcufoj11r4o67e19t.apps.googleusercontent.com";

  $client = new Google_Client(['client_id' => $CLIENT_ID]);
  $payload = $client->verifyIdToken($id_token);
  if ($payload) {
    $userid = $payload['sub'];

    $query = "SELECT id FROM user_map WHERE gid = $userid;";
    $result = mysqli_query($link, $query);

    if(mysqli_num_rows($result) == 0) {
      // new user
        $query = "INSERT INTO user_map (gid) VALUES ($userid);";
        $result = mysqli_query($link, $query);
        
        
        $query = "SELECT id FROM user_map WHERE gid = $userid;";
        $result = mysqli_query($link, $query);
        $row = mysqli_fetch_array($result, MYSQLI_NUM);
        $id = $row[0];
        
        $name = $payload['name'];
        $email = $payload['email'];
        $query2 = "INSERT INTO `userinfo` (`id`, `name`, `email`, `fcm_token`, `launch_count`, `screen_time`) VALUES ('$id', '$name', '$email', NULL, NULL, NULL);";
        mysqli_query($link, $query2);
        
        
        $otp = 0;
        $query = "SELECT otp from otp_map WHERE pid = '$id';";
        $result = mysqli_query($link, $query);
        if(mysqli_num_rows($result) == 0) {
          do{
            $otp = rand(100000, 1000000);
            $query = "SELECT * FROM otp_map WHERE otp = $otp;";
            $result = mysqli_query($link, $query);
          } while (mysqli_num_rows($result) != 0);

          $query = "INSERT INTO otp_map (pid, otp, teen_name) VALUES ('$id', '$otp', NULL);";
          $result = mysqli_query($link, $query);
        } else{
          //unused otp already exists
          $row = mysqli_fetch_array($result, MYSQLI_NUM);
          $otp = $row[0];
        }

        echo $otp;

        if(isset($_POST['fcm_token'])){
          $fcm_token = mysqli_real_escape_string($link, $_POST['fcm_token']);
          $query = "UPDATE userinfo SET fcm_token = '$fcm_token' WHERE id = '$id';";
          $result = mysqli_query($link, $query);
        }

    } else {
      //existing user
        $row = mysqli_fetch_array($result, MYSQLI_NUM);
        $id = $row[0];
        $query = "SELECT otp from otp_map WHERE pid = '$id';";
        $result = mysqli_query($link, $query);

        if(mysqli_num_rows($result) == 0) {
          // old user
          // $query = "SELECT id FROM user_map WHERE gid = $userid;";
          // $result = mysqli_query($link, $query);
          // $row = mysqli_fetch_array($result, MYSQLI_NUM);
          // $pid = $row[0];
          if(isset($_POST['verify'])){
            //old user logging in after signout
            echo $userid;
          }
          else if(isset($_POST['setup'])){
            //old user reinstalled app

            //piggyback teen name
            $query = "SELECT name FROM teeninfo WHERE pid = '$id';";
            $result = mysqli_query($link, $query);
            $row = mysqli_fetch_array($result, MYSQLI_NUM);
            $teenName = $row[0];

            echo "already_setup|".$teenName;

            //upate fcm token in database
            if(isset($_POST['fcm_token'])){
              $fcm_token = mysqli_real_escape_string($link, $_POST['fcm_token']);
              $query = "UPDATE userinfo SET fcm_token = '$fcm_token' WHERE id = '$id';";
              $result = mysqli_query($link, $query);
            }
          }
          else {
            $pid = $id;
          }
        } else{
          if(!isset($otpname)) {
            //old user with pending pairing
            //unused otp already exists
            $row = mysqli_fetch_array($result, MYSQLI_NUM);
            $otp = $row[0];
            echo $otp;
          }
          else{
            //authenting to upload name
            $pid = $id;
          }
        }
    }

  } else {
    echo "failed";
  }
?>
