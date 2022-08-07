<?php
  include 'connectDB.php';

  $devid = $_POST['dev_id'];
  $devid = mysqli_real_escape_string($link, $devid);

  if(isset($_POST['upload_done']) && $_POST['upload_done']==1){
      //just need to sent push to parent device

      error_log("Teen texts uploaded, processing with ML apis\n", 3, "error_log.log");

      $query = "SELECT pid, id from teeninfo where devicehash = '$devid';";
      $result = mysqli_query($link, $query);
      if(mysqli_num_rows($result) == 0) {
        error_log("setup failed\n", 3, "error_log.log");
        echo "do setup";
        exit;
      }

      $uploadCount = $_POST['upload_count'] - 1;
      if($uploadCount < 0){
        $uploadCount = 0;
      }
      $row = mysqli_fetch_array($result, MYSQLI_NUM);
      $pid = $row[0];
      $id = $row[1];



      $processedcount=0;
      if($uploadCount>$processedcount){
        do{
          sleep(1);
          $query = "SELECT `processed` FROM `processing-status` WHERE `pid`='$pid' AND `id`='$id';";
          $result = mysqli_query($link, $query);
          $row = mysqli_fetch_array($result, MYSQLI_NUM);
          $processedcount = $row[0];
          //error_log("processed: $processedcount\n upload_count: $uploadCount\n", 3, "error_log.log");
        }
        while($uploadCount>$processedcount);
      }
      error_log("All texts analyzed and processed\n", 3, "error_log.log");

















      //we need to remove word_cloud
      //include 'word_cloud_initial.php';




























      $query = "SELECT fcm_token FROM userinfo WHERE id = '$pid';";
      $result = mysqli_query($link, $query);
      if(mysqli_num_rows($result) == 1) {
        $row = mysqli_fetch_array($result, MYSQLI_NUM);
        $fcm_token_parent = $row[0];

        $query = "SELECT fcm_token FROM teeninfo WHERE pid = '$pid' AND id = '$id';";
        $result = mysqli_query($link, $query);
        $row = mysqli_fetch_array($result, MYSQLI_NUM);
        $fcm_token_child = $row[0];
        error_log("\n\nFCM NOTIFICATION REQUESTED OTP PAIR\nchild:$fcm_token_child\nparent:$fcm_token_parent\n", 3, "error_log.log");
        //fcm push to parent app
        $url = 'https://fcm.googleapis.com/fcm/send';
        $fields = array (
                'registration_ids' => [$fcm_token_parent, $fcm_token_child],
                'data' => array (
                  'pairing' => 'success'
                ),
                'priority' => 'high'
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



      /*
      //send to child
      $query = "SELECT fcm_token FROM teeninfo WHERE pid = '$pid' AND id = '$id';";
      $result = mysqli_query($link, $query);
      if(mysqli_num_rows($result) == 1) {
        $row = mysqli_fetch_array($result, MYSQLI_NUM);
        $fcm_token = $row[0];
        //fcm push to parent app
        $url = 'https://fcm.googleapis.com/fcm/send';
        $fields = array (
                'to' => $fcm_token,
                'data' => array (
                  'pairing' => 'success'
                ),
                'priority' => 'high'
        );
        $fields = json_encode ( $fields );
        $headers = array (
                'Authorization: key=' . "AAAAz1-tpsM:APA91bEMOObtajHFwl7mNDJ16omYii70Jn67SWM8DstGw_GmM2v_-mvm-PBsH1SlgFR-cJLCCM5OpDk8eNkw7KzzKXHONsgoDTv0PUNcwmeOJxEM5X2dZZCy-qNtgFpvfPTNlcv_HTOz", //Suspect Key most likely firebase
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
      */

      exit;
  }

  $otp = $_POST['otp_entered'];
  $otp = mysqli_real_escape_string($link, $otp);

  $query = "SELECT pid, teen_name FROM otp_map WHERE otp = '$otp';";
  $result = mysqli_query($link, $query);
  if(mysqli_num_rows($result) == 0) {
    echo "failed";
  }
  else{
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $pid = $row[0];
    $name = $row[1];
    $query = "INSERT INTO teeninfo (pid, devicehash, name) VALUES ('$pid', '$devid', '$name');";
    $result = mysqli_query($link, $query);

    $query = "SELECT id FROM teeninfo WHERE devicehash = '$devid';";
    $result = mysqli_query($link, $query);
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $id = $row[0];
    $tablename = "texts-" . $pid . "-" . $id;
    $query = "CREATE TABLE `$tablename`(
      `tid` int UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
      `name` varchar(50) NOT NULL,
      `number` varchar(15) NOT NULL,
      `time` varchar(25) NOT NULL,
      `text` varbinary(2000) NOT NULL,
      `sent` tinyint(1) NOT NULL,
      `media` mediumblob NULL,
      `mime` varchar(50) NULL,
      `text-flagged` tinyint(1) NULL,
      `text-sentiment-score` varchar(400) NULL,
      `text-sentiment` varchar(8) NULL,
      `text-entities` varchar(2000) NULL,
      `media-moderation` varchar(2000) NULL,
      `media-labels` varchar(2000) NULL,
      `tag` varchar(256) NULL,
      `iv` varchar(256) NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;";
    $result = mysqli_query($link, $query);

    $query = "ALTER TABLE `$tablename`
      ADD UNIQUE KEY `row_unique` (`number`,`time`,`sent`,`text`(100)) USING BTREE;";
    $result = mysqli_query($link, $query);

    $tablename = "apps-" . $pid . "-" . $id;
    $query = "CREATE TABLE `$tablename`(
      `name` varchar(100) NOT NULL,
      `package` varchar(190) NOT NULL PRIMARY KEY,
      `installTime` varchar(25) NULL,
      `icon` BLOB NOT NULL,
      `installed` BOOLEAN NOT NULL DEFAULT TRUE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;";
    $result = mysqli_query($link, $query);

    $tablename = "conversations-" . $pid . "-" . $id;
    $query = "CREATE TABLE `$tablename`(
      `name` varchar(50) NULL,
      `number` varchar(15) NOT NULL PRIMARY KEY,
      `time` varchar(25) NOT NULL,
      `sent` int UNSIGNED NOT NULL,
      `received` int UNSIGNED NOT NULL,
      `cot` tinyint(1) NOT NULL DEFAULT '0',
      `pic` blob NULL,
      `unread_count` int UNSIGNED NOT NULL DEFAULT '0',
      `positive-sentiment-score` int UNSIGNED NULL DEFAULT '0',
      `negative-sentiment-score` int UNSIGNED NULL DEFAULT '0',
      `neutral-sentiment-score` int UNSIGNED NULL DEFAULT '0',
      `mixed-sentiment-score` int UNSIGNED NULL DEFAULT '0',
      `text-flag` int UNSIGNED NULL DEFAULT '0',
      `image-flag` int UNSIGNED NULL DEFAULT '0',
      `lastMsgId` varchar(15) NULL,
      `word-cloud` mediumblob NULL,
      `word-cloud-text` text NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci;";
    $result = mysqli_query($link, $query);

    $tablename = "gc_conversation_" . $pid . "_" . $id;
    // $query = "CREATE TABLE `$tablename`(
    //   `pkGroupID`             int NOT NULL,
    //   `vcNumber`              varchar(256) NOT NULL,
    //   `vcName`                varchar(256) DEFAULT NULL,
    //   `vbMessage`             varbinary(2048) DEFAULT NULL,
    //   `mbImage`               mediumblob DEFAULT NULL,
    //   `mime`                  varchar(64) DEFAULT NULL,
    //   `bOutgoing`             boolean NOT NULL DEFAULT 0,
    //   `iMessageType`          int NOT NULL DEFAULT 1, -- 1:Text 2:Image 3:Video
    //   `bFlag`                 boolean NOT NULL DEFAULT 0,
    //   `dtTimeReceived`        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    //   `dSentimentPos`         double DEFAULT NULL,
    //   `dSentimentNeg`         double DEFAULT NULL,
    //   `dSentimentNeu`         double DEFAULT NULL,
    //   `dSentimentMix`         double DEFAULT NULL,
    //   `vcTextEntities`        varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    //   `vcMediaModeration`     varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    //   `vcMediaLables`         varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    //   `vcTag`                 varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    //   `vcIV`                  varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL
    // );";
    $query = "CREATE TABLE $tablename LIKE gc_conversation;"; // This is an easier, simpler query
    $result = mysqli_query($link, $query);

    $tablename = "gc_summary_" . $pid . "_" . $id;
    // $query = "CREATE TABLE `$tablename`(
    //   `pkGroupID`             int NOT NULL,
    //   `vcNumber`              varchar(256) NOT NULL,
    //   `vcName`                varchar(256) DEFAULT NULL,
    //   `vbMessage`             varbinary(2048) DEFAULT NULL,
    //   `mbImage`               mediumblob DEFAULT NULL,
    //   `mime`                  varchar(64) DEFAULT NULL,
    //   `bOutgoing`             boolean NOT NULL DEFAULT 0,
    //   `iMessageType`          int NOT NULL DEFAULT 1, -- 1:Text 2:Image 3:Video
    //   `bFlag`                 boolean NOT NULL DEFAULT 0,
    //   `dtTimeReceived`        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    //   `dSentimentPos`         double DEFAULT NULL,
    //   `dSentimentNeg`         double DEFAULT NULL,
    //   `dSentimentNeu`         double DEFAULT NULL,
    //   `dSentimentMix`         double DEFAULT NULL,
    //   `vcTextEntities`        varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    //   `vcMediaModeration`     varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    //   `vcMediaLables`         varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    //   `vcTag`                 varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
    //   `vcIV`                  varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL
    // );";
    $query = "CREATE TABLE $tablename LIKE gc_summary;"; // This is an easier, simpler query
    $result = mysqli_query($link, $query);

    if(isset($_POST['fcm_token'])){
      $fcm_token = mysqli_real_escape_string($link, $_POST['fcm_token']);
      $query = "UPDATE teeninfo SET fcm_token = '$fcm_token' WHERE id = '$id' AND pid = '$pid';";
      $result = mysqli_query($link, $query);
    }

    $query = "DELETE FROM otp_map WHERE otp = $otp;";
    $result = mysqli_query($link, $query);
    echo "success";
    flush();
  }
?>
