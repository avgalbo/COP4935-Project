<?php

  //  Inspired by Arup Ghosh, March 2018
  //  Updated by Kenneth Lasseter, David Yun, Abhishek Matange, Allison Brignol, October 2021

  require 'vendor/autoload.php';
include(dirname(__FILE__)."/../breakfast.php");
  include 'connectDB.php';
  
  // FOR TESTING
  /*
  $id = '1';
  $pid = '1';
  $tid = '1';
   */
  
  $flagged = 0;
  $moderation_results = null;
  
  
  /////////////////////////////////////////////////////////       COMPREHEND/ REKOGNITION SETUP       ////////////////////////////////////////////////////////


  $resultAWSRekognition = new \Aws\Rekognition\RekognitionClient([ 
    
    'version' => 'latest',   
    
    'region'  => 'us-east-1',

    'verify' => 'false',      

    'credentials' => [
                    //OLD: AKIAUDVZXAUHZM2VB7FD
                    //AKIAUDVZXAUHQT2JRRYD
        'key'    => $hamburger,
                    //OLD: hh2Aa2PY43ku45TutGFARCs7DjyppSfXF2KqkXU7
                    //fq2WCtD4gbPcLTwumYXnkN4/4cIHm6eGcBPH4Pxd
        'secret' => $fries
        
    ]

  ]);

  
  $resultAWSComprehend = new \Aws\Comprehend\ComprehendClient([ 

    'version' => 'latest',

    'region'  => 'us-east-1', 

    'verify' => 'false',      

    'credentials' => [
                    //OLD: AKIAUDVZXAUHZM2VB7FD
                    //AKIAUDVZXAUHQT2JRRYD
        'key'    => $hamburger,
                    //OLD: hh2Aa2PY43ku45TutGFARCs7DjyppSfXF2KqkXU7
                    //fq2WCtD4gbPcLTwumYXnkN4/4cIHm6eGcBPH4Pxd
        'secret' => $fries
        
    ]

  ]);


  $tablename = "texts-" . $pid . "-" . $id; 

  $tablenameConversations = "conversations-" . $pid . "-" . $id; 

  $query = "SELECT number, AES_DECRYPT(text, UNHEX(SHA2('$milkshake',512))) AS text, media, mime, tag FROM `$tablename` WHERE tid = '$tid';";

  $resulttexts = mysqli_query($link, $query);

  if(mysqli_num_rows($resulttexts) == 0) {

    error_log("results empty\n\n", 3, "error_log.log");
    
    exit;

  }
    
 
  $rowtexts = $resulttexts->fetch_assoc();
  //$rowtexts['text']="Let's have sex";
  

  
  
  
  
  
  
 
  /////////////////////////////////////////////////////////       COMPREHEND /TEXT ANALYSIS       ////////////////////////////////////////////////////////
  
  
  if (!empty($rowtexts)) {
    if($rowtexts["text"] != NULL){
      
      ////////////////////////////////////////////    DECRYPT TEXT    /////////////////////////////////////////////////////////////
/*
      function stringDecrypt ($encodedText, $cryptKey) {
      
        $c = base64_decode($encodedText);
        $cipher = 'aes-128-cbc';
      
        if (in_array($cipher, openssl_get_cipher_methods()))
        {
          $ivlen = openssl_cipher_iv_length($cipher);
          $iv = substr($c, 0, $ivlen);
          $sha2len = 32;
          $hmac = substr($c, $ivlen, $sha2len);
          $ivlenSha2len = $ivlen+$sha2len;
          $ciphertext_raw = substr($c, $ivlenSha2len);
          $plainText = openssl_decrypt(
            $ciphertext_raw, $cipher, $cryptKey, $options=OPENSSL_RAW_DATA, $iv);
        }
      
        return $plainText;
      }
      
      $decryptedMessage = stringDecrypt($rowtexts["text"], $milkshake);
      
      error_log($decryptedMessage, 3, "error_log.log");
  
  */    /*
      $cipher = "aes-128-gcm";
      $key = 'KaPdSgVkYp3s6v9y';
      error_log($cipher, 3, "error_log.log");
      
      
      
      if (in_array($cipher, openssl_get_cipher_methods()))
      {
        $iv = base64_decode($rowtexts["iv"]);
        $tag = base64_decode($rowtexts["tag"]);
        $encodedText = base64_decode($rowtexts["text"]);
        $decryptedmessage = openssl_decrypt($encodedText, $cipher, $key, $options=0, $iv, $tag);
        //$decryptedmessage = base64_decode($decryptedmessage);
      }
  
      error_log($decryptedmessage, 3, "error_log.log");
      */
      
      
      
      
      
      
      $resultSentimentScore = $resultAWSComprehend->DetectSentiment([
  
      'LanguageCode' => 'en',
  
      'Text' => strval($rowtexts["text"]),
  
      ]);
    
  
      $sentiment_results= ""; 
    
      $sentiment_score_results= ""; 
    
      
      $sentiment_score_results= $sentiment_score_results . "MIXED" . "," . $resultSentimentScore['SentimentScore']['Mixed'] . "," . "NEGATIVE" . "," . $resultSentimentScore['SentimentScore']['Negative'] . "," . "NEUTRAL" . "," . $resultSentimentScore['SentimentScore']['Neutral'] . "," . "POSITIVE" . "," . $resultSentimentScore['SentimentScore']['Positive'] . ",";
    
    
      $sentiment_results= $resultSentimentScore['Sentiment'];
  
    
      $query = "UPDATE `$tablename` SET `text-sentiment-score` = '$sentiment_score_results' WHERE `tid` = '$tid';";
    
    
      $resultQuery = mysqli_query($link, $query);
    
      /*
       if($resultQuery){
    
         //echo "Sentiment score results uploaded successfully." . "<br>";
    
       }
      */
    
      $query = "UPDATE `$tablename` SET `text-sentiment` = '$sentiment_results' WHERE `tid` = '$tid';";
    
    
      $resultQuery = mysqli_query($link, $query);
    
      if($resultQuery){
    
         //echo "Sentiment results uploaded successfully." . "<br>";
    
    
        if($sentiment_results=="MIXED") {
    
          $queryConversations = "UPDATE `$tablenameConversations` SET `mixed-sentiment-score`=`mixed-sentiment-score`+1 WHERE `number` = '$number';";
    
          $resultQueryConversations = mysqli_query($link, $queryConversations);
    
        }
    
    
        if($sentiment_results=="NEGATIVE") {
    
          $queryConversations = "UPDATE `$tablenameConversations` SET `negative-sentiment-score`=`negative-sentiment-score`+1 WHERE `number` = '$number';";
    
          $resultQueryConversations = mysqli_query($link, $queryConversations);
    
        }
    
    
        if($sentiment_results=="NEUTRAL") {
    
          $queryConversations = "UPDATE `$tablenameConversations` SET `neutral-sentiment-score`=`neutral-sentiment-score`+1 WHERE `number` = '$number';";
    
          $resultQueryConversations = mysqli_query($link, $queryConversations);
    
        }
    
    
         if($sentiment_results=="POSITIVE") {
    
          $queryConversations = "UPDATE `$tablenameConversations` SET `positive-sentiment-score`=`positive-sentiment-score`+1 WHERE `number` = '$number';";
    
          $resultQueryConversations = mysqli_query($link, $queryConversations);
    
        }
    
        /*
        if($resultQueryConversations) {
    
          //echo "success" . "<br>";
    
        }
        */
      }
    
      $resultEntities = $resultAWSComprehend->detectEntities([
    
        'LanguageCode' => 'en', // REQUIRED
    
        'Text' => strval($rowtexts["text"]), // REQUIRED
    
      ]);
    
      $entities_results= "";
    
      for ($n=0;$n<sizeof($resultEntities['Entities']); $n++){
    
        $entities_results= $entities_results . $resultEntities['Entities'][$n]['Score'] . ","
    
        . $resultEntities['Entities'][$n]['Text'] . "," . $resultEntities['Entities'][$n]['Type'] . ",";
    
      }
    
    
      if(!empty($entities_results)) {
    
        $query = "UPDATE `$tablename` SET `text-entities` = '$entities_results' WHERE `tid` = '$tid';";
    
        $resultEntities = mysqli_query($link, $query);
        
        /*
        if($resultEntities){
    
          //echo "Text entities uploaded successfully." . "<br>";
    
        }
        */
    
      }
      
      $messages = array('messages' => [array (
    
        'text' => strval($rowtexts["text"]),
    
        'id' => "1"
    
      )]);
    }
    


         /////////////////////////////////////////////////////////       STIR LAB MACHINE LEARNING API       ////////////////////////////////////////////////////////


  $ch = curl_init();
  curl_setopt($ch, CURLOPT_URL, 'http://35.172.201.116/predict');
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
  curl_setopt($ch, CURLOPT_POST, 1);

  $data = [
    'messages'=>[[
      'sender_name' => 'Fake User',
      'timestamp_ms' => '1234567890',
      'content' => $rowtexts["text"]
    ]],
    'thread_path' => "mlapi doesn't even read this"
  ];

  echo ($data);

  $payload = json_encode($data);
  echo "This should give me the json payload\n";
  echo $payload . "\n";
  curl_setopt($ch, CURLOPT_POSTFIELDS, $payload);

 $headers = array('Content-Type: application/json');
  curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

  $result = curl_exec($ch);
  if (curl_errno($ch)) {
      echo 'Error:' . curl_error($ch);
  }
  echo("This should give me a result\n");
  echo ($result) . '\n';
  curl_close($ch);
  $arr = json_decode($result, true);
  echo "This should give me the result array\n";
  echo($arr);
  echo ($arr['sexual_msg'][0][0] . " " . $arr['cyber_msg'][0][0]);
  /*
  error_log($arr["Classification"]["Category1"]["Score"] , 3, "error_log.log");
  error_log("\n", 3, "error_log.log");
  error_log($arr["Classification"]["Category2"]["Score"] , 3, "error_log.log");
  error_log("\n", 3, "error_log.log");
  error_log($arr["Classification"]["Category3"]["Score"] , 3, "error_log.log");
  error_log("\n", 3, "error_log.log");
  */



  // Category 1 -- refers to potential presence of language that may be considered sexually explicit or adult in certain situations.

  // Category 2 -- refers to potential presence of language that may be considered sexually suggestive or mature in certain situations.

  // Category 3 -- refers to potential presence of language that may be considered offensive in certain situations.

  // Score -- is between 0 and 1. The higher the score, the higher the model is predicting that the category may be applicable. This feature relies on a statistical model rather
  //          than manually coded outcomes. We recommend testing with your own content to determine how each category aligns to your requirements.

  // Review Recommended -- is either true or false depending on the internal score thresholds. Customers should assess whether to use this value or
  //                       decide on custom thresholds based on their content policies.


  // This used to be in the if statement but was taken out for better control of moderation : /*$arr["Classification"]["ReviewRecommended"] == TRUE  ||*/


  if ( $arr['sexual_msg'][0][0] > 0.85
  || $arr['cyber_msg'][0][0] > 0.85 )
  {
    $flagged = 1;

    $query = "UPDATE `$tablename` SET `text-flagged`='$flagged' WHERE `tid`='$tid';";

    $resultQuery = mysqli_query($link, $query);

    if($resultQuery){

      $queryConversations = "UPDATE `$tablenameConversations` SET `text-flag`=`text-flag`+1 WHERE number = '$number';";

      $resultQueryConversations = mysqli_query($link, $queryConversations);
      /*
      if($resultQueryConversations) {
          //echo "success" . "<br>";
      }
      */
    }
  } else {

    $flagged = 0;

    $query = "UPDATE `$tablename` SET `text-flagged`='$flagged' WHERE `tid`='$tid';";

    $resultQuery = mysqli_query($link, $query);

    /*
    if($resultQuery){
        //echo "Text flag 0 uploaded successfully." . "<br>";
    }
    */
  }
  
  
  
  
  
  
  /////////////////////////////////////////////////////////       AZURE CONTENT MODERATION/ FLAGGING       ////////////////////////////////////////////////////////

/*
 $ch = curl_init();
  curl_setopt($ch, CURLOPT_URL, 'https://cot2021.cognitiveservices.azure.com/contentmoderator/moderate/v1.0/ProcessText/Screen?classify=True&listId=1608');
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
  curl_setopt($ch, CURLOPT_POST, 1);
  curl_setopt($ch, CURLOPT_POSTFIELDS, $rowtexts["text"]);

  $headers = array();
  $headers[] = 'Content-Type: text/plain';
  $headers[] = 'Ocp-Apim-Subscription-Key: 08a009bce8c04aa0a670b315abef7f00';
  curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

  $result = curl_exec($ch);
  if (curl_errno($ch)) {
      echo 'Error:' . curl_error($ch);
  }
  curl_close($ch);
  $arr = json_decode($result, true);
 */
  
  /*
  error_log($arr["Classification"]["Category1"]["Score"] , 3, "error_log.log");
  error_log("\n", 3, "error_log.log");
  error_log($arr["Classification"]["Category2"]["Score"] , 3, "error_log.log");
  error_log("\n", 3, "error_log.log");
  error_log($arr["Classification"]["Category3"]["Score"] , 3, "error_log.log");
  error_log("\n", 3, "error_log.log");
  */
  
  
  
  // Category 1 -- refers to potential presence of language that may be considered sexually explicit or adult in certain situations.
  
  // Category 2 -- refers to potential presence of language that may be considered sexually suggestive or mature in certain situations.
  
  // Category 3 -- refers to potential presence of language that may be considered offensive in certain situations.
  
  // Score -- is between 0 and 1. The higher the score, the higher the model is predicting that the category may be applicable. This feature relies on a statistical model rather 
  //          than manually coded outcomes. We recommend testing with your own content to determine how each category aligns to your requirements.
  
  // Review Recommended -- is either true or false depending on the internal score thresholds. Customers should assess whether to use this value or 
  //                       decide on custom thresholds based on their content policies.
  
  
  // This used to be in the if statement but was taken out for better control of moderation : /*$arr["Classification"]["ReviewRecommended"] == TRUE  ||*/
  
  
  //if ( $arr["Classification"]["Category1"]["Score"] > 0.9 || $arr["Classification"]["Category2"]["Score"] > 0.5 || $arr["Classification"]["Category3"]["Score"] > 0.99999 || ( $sentiment_results=="NEGATIVE" && ( $arr["Classification"]["Category1"]["Score"] > 0.9 || $arr["Classification"]["Category2"]["Score"] > 0.5 || $arr["Classification"]["Category3"]["Score"] > 0.85 ) )) {
  
    //$flagged = 1;
  
    //$query = "UPDATE `$tablename` SET `text-flagged`='$flagged' WHERE `tid`='$tid';";
  
    //$resultQuery = mysqli_query($link, $query);
  
    //if($resultQuery){
  
      //$queryConversations = "UPDATE `$tablenameConversations` SET `text-flag`=`text-flag`+1 WHERE number = '$number';";
  
      //$resultQueryConversations = mysqli_query($link, $queryConversations);
      /*
      if($resultQueryConversations) {
          //echo "success" . "<br>";
      }
      */
   // }
  //} else {
  
    //$flagged = 0;
  
    //$query = "UPDATE `$tablename` SET `text-flagged`='$flagged' WHERE `tid`='$tid';";
  
    //$resultQuery = mysqli_query($link, $query);
    
    /*
    if($resultQuery){
        //echo "Text flag 0 uploaded successfully." . "<br>";
    }
    */
  //}

   

  
  
  
  
  
  
  ///////////////////////////////////////////////       REKOGNITION/ PICTURE/MEDIA ANALYSIS      /////////////////////////////////////////////////////////////
  
  
  if ($rowtexts["media"] != NULL && ($rowtexts["mime"] == "image/jpeg" || $rowtexts["mime"] == "image/png")) {
    error_log("picture detected\n" , 3, "error_log.log");
    
    
    $resultLabels = $resultAWSRekognition->detectLabels([ //Suspect Rekognition
  
    'Image' => [ // REQUIRED
  
      'Bytes' => base64_decode($rowtexts["media"]),
  
    ],
  
    'MaxLabels' => 5,
  
    'MinConfidence' => 10,
  
    ]);
  
  
    $labels_results= "";
    $labels_results_wc= ""; //for image word cloud
  
  
  
    for ($n=0;$n<sizeof($resultLabels['Labels']); $n++){
  
      $labels_results= $labels_results . $resultLabels['Labels'][$n]['Confidence'] . "," . $resultLabels['Labels'][$n]['Name'] . ",";
      $labels_results_wc= $labels_results_wc . $resultLabels['Labels'][$n]['Name'] . " ";
  
    }

  
    $query = "UPDATE `$tablename` SET `media-Labels` = '$labels_results' WHERE `tid`='$tid';";
  
    
    $resultQuery = mysqli_query($link, $query);
  
  
    // Add labels from image to the word cloud
    /*
    if($resultQuery){
  
      $queryConversations = "UPDATE `$tablenameConversations` SET `word-cloud-text`= CONCAT(ifnull(`word-cloud-text`,''),'$labels_results_wc') WHERE number = '$number';"; //Suspect Word Cloud
  
      $resultQueryConversationsWC = mysqli_query($link, $queryConversations);
  
    }
    */
  
    $resultModeration = $resultAWSRekognition->DetectModerationLabels([ //Suspect Rekognition
  
      'Image' => [ // REQUIRED
  
      'Bytes' => base64_decode($rowtexts["media"]),
  
       ],
  
       'MinConfidence' => 10,
  
    ]);
  
  
    $moderation_results= "";
  
  
    for ($n=0;$n<sizeof($resultModeration['ModerationLabels']); $n++){
  
      $moderation_results= $moderation_results . $resultModeration['ModerationLabels'][$n]['Confidence'] . ","  . $resultModeration['ModerationLabels'][$n]['Name'] . "," . $resultModeration['ModerationLabels'][$n]['ParentName'] . ",";
  
    }
  
  
    $run = 1;
    if(!empty($moderation_results)) {
  
        $query = "UPDATE `$tablename` SET `media-moderation` = '$moderation_results' WHERE `tid`='$tid';";
  
  
  
        $resultModeration = mysqli_query($link, $query);
  
        if($resultModeration){
  
          //echo "Image moderation results updaloaded successfully." . "<br>";
          // Changed so that only flagged image labels are added to the word cloud
          $queryConversations = "UPDATE `$tablenameConversations` SET `word-cloud-text`= CONCAT(ifnull(`word-cloud-text`,''),'$labels_results_wc') WHERE number = '$number';"; //Suspect Word Cloud
  
          $resultQueryConversationsWC = mysqli_query($link, $queryConversations);
  
          $queryConversations = "UPDATE `$tablenameConversations` SET `image-flag`=`image-flag`+1 WHERE `number` = '$number';";
  
          $resultQueryConversations = mysqli_query($link, $queryConversations);
  
          if($resultQueryConversations) {
  
            //echo "success" . "<br>";
  
          }
        }
    }
    
    //$run = 0;
  } elseif ($rowtexts["media"] != NULL && ($rowtexts["mime"] == "video/mp4" || $rowtexts["mime"] == "video/quicktime")) {
  
      // video
  
      // $query = "UPDATE `$tablename` SET `media-Labels` = 'NO_PROCESSING', `media-moderation` = 'NO_PROCESSING' WHERE `tid`='$tid';";
      //
      // $resultQuery = mysqli_query($link, $query);
      //
      // if($resultQuery){
      //
      //   //echo "Video NO PROCESSING tag uploaded successfully." . "<br>";
      //
      // }
      $run = 0;
  
      //exit; //no need to update word cloud
  
  
  } elseif ($rowtexts["media"] != NULL) {
  
      //$query = "UPDATE `$tablename` SET `media-Labels` = 'FILE_TYPE_NOT_SUPPORTED', `media-moderation` = 'FILE_TYPE_NOT_SUPPORTED' WHERE `tid`='$tid';";
  
      //$resultQuery = mysqli_query($link, $query);
  
      //echo "Hello";
  
      //if($resultQuery){
  
        //echo $rowtexts["mime"] . "file type not supported." . "<br>";
  
      //}
      $run = 0;
  
      //exit; //no need to update word cloud
  
  
  } elseif($rowtexts["media"] == NULL)  {
  
      $run = 0;
      //exit; //no need to update word cloud
  
  } else {
  
  }

  }
  
  $query = "UPDATE `processing-status` SET `processed`=`processed`+1 WHERE `pid`=$pid AND `id`=$id;";
  mysqli_query($link, $query);
  //echo "success";
  //exit;

















///////////////////////////////////////////////////////////////////////////   WORD CLOUD    ///////////////////////////////////////////////////////////////////////////////////////////////////////

if($run==1 /*&& $initial!=1*/){
    //update word cloud
    
    $query = "SELECT `word-cloud-text` FROM `$tablenameConversations` WHERE number='$number';";
    $result = mysqli_query($link, $query);
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    
    error_log("attempting to update word cloud\n", 3, "error_log.log");

    if($row[0] != NULL) { //check if word-cloud-text not null
      $word_cloud_text = $row[0];
      //echo $number . "<br>";
      //echo $word_cloud_text . "<br>";
      
      $inputWC = array (
        'text' => $word_cloud_text,
        'scale' => 1,
        'width' => 400,
        'height' => 300,
        'colors' => array(
          '#EB575C',
          '#F3CC7B',
          '#565655',
          '#57CC82'
        ),
        'font'=> 'Roboto',
        'use_stopwords' => true,
        'language' => 'en'


      );
      $inputWC_json = json_encode ( $inputWC );


      $ch = curl_init();
      $headers = array (
        'x-rapidapi-host:' . "textvis-word-cloud-v1.p.rapidapi.com",
        'x-rapidapi-key:' . "865c618e38mshb5a7a5cc6b2632ep1dfe0bjsnf0c66e52196b",
        'Content-Type: application/json'
      );
      curl_setopt($ch, CURLOPT_URL, "https://textvis-word-cloud-v1.p.rapidapi.com/v1/textToCloud");
      curl_setopt ( $ch, CURLOPT_CUSTOMREQUEST,"POST");
      curl_setopt ( $ch, CURLOPT_HTTPHEADER, $headers );
      curl_setopt ( $ch, CURLOPT_POSTFIELDS, $inputWC_json );
      curl_setopt ( $ch, CURLOPT_RETURNTRANSFER, true);
      

      $resultWC = curl_exec($ch);
      //print_r($resultWC);
      list($mimetypeWC, $mediaWC) = explode(",", $resultWC);
      //echo $mediaWC;
      //echo '<img src="data:image/png;base64,'.$mediaWC.'"/>' . "<br>";

      if (curl_errno($ch)) {
        error_log("Error retrieving wordcloud\n", 3, "error_log.log");
        echo 'Error:' . curl_error($ch);
      }
      curl_close ($ch);

      $queryWCImage = "UPDATE `$tablenameConversations` SET `word-cloud`='$mediaWC' WHERE number = '$number';";

      $resultQueryConversations = mysqli_query($link, $queryWCImage);


      if($resultQueryConversations) {

        //echo "word cloud uploaded successfully" . "<br>";

      }

      $queryWCImage = "SELECT `word-cloud` FROM `$tablenameConversations` WHERE number = '$number';";

      $resultQueryConversations = mysqli_query($link, $queryWCImage);



      if($resultQueryConversations) {

        //echo "word cloud downloaded successfully" . "<br>";

      }
      $rowWC = mysqli_fetch_array($resultQueryConversations, MYSQLI_NUM);

      //echo '<img src="data:image/png;base64,'. $rowWC[0].'"/>' . "<br>";



      if($resultQueryConversations) {

        //echo "success" . "<br>";

      }

    } else {

      //echo $number . "<br>";
      //echo "empty" . "<br>";


    }

  }







































/////////////////////////////////////////////////       FCM NOTIFICATION        ////////////////////////////////////////////////////////////////////////////

//Send risk notification
if($initial!=1 && ($flagged=="1" || $moderation_results != null)){
  //parent
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
  if($name == "")
    $name = $number;

  if($sent == 1)
    $message = "A text sent by " . $teenName . " to " . $name . " was flagged.";
  else
  $message = "A text received by " . $teenName . " from " . $name . " was flagged.";

  $fields = array (
          'to' => $fcm_token,
          'notification' => array (
            'sound' => 'default',
            'title' => 'New Risk Identified On '. $teenName .'\'s Device',
            'body' => $message,
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

//child
  $query = "SELECT id FROM teeninfo WHERE pid = '$pid';";
  $result = mysqli_query($link, $query);
  if(mysqli_num_rows($result) == 0) {
    //echo "No teen device paired!";
    exit;
  }
  $row = mysqli_fetch_array($result, MYSQLI_NUM);
  $teenid = $row[0];

  $query = "SELECT fcm_token FROM teeninfo WHERE pid = '$pid' AND id = '$teenid';";
  $result = mysqli_query($link, $query);
  if(mysqli_num_rows($result) == 1) {
    $row = mysqli_fetch_array($result, MYSQLI_NUM);
    $fcm_token = $row[0];
  }
  else {
    exit;
  }

  if($sent == 1)
    $message = "A text sent by you to " . $name . " was flagged."; //FireBase sending the message
  else
  $message = "A text received from " . $name . " was flagged.";


  $fields = array (
          'to' => $fcm_token,
          'notification' => array (
            'sound' => 'default',
            'title' => 'New Risk Identified',
            'body' => $message,
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

echo "success from FCM Notification";
exit;

  ///////////////////////////////////////////////////////////      RAKOON      //////////////////////////////////////////////////////////////////////////
  /*
    //print_r($messages);
  
    $messages2 = json_encode ( $messages);
  
    //print_r($messages2. "<br>");
  
    $ch = curl_init();
  
  
  
    curl_setopt($ch, CURLOPT_URL, "https://flagger.rakkoon.com/tc"); //Suspect RAKKOON
  
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
  
    curl_setopt($ch, CURLOPT_POSTFIELDS, $messages2);
  
    curl_setopt($ch, CURLOPT_POST, 1);
  
  
  
    $headers = array();
  
    $headers[] = "Content-Type: application/x-www-form-urlencoded";
  
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
  
  
  
    $resultFlagger = curl_exec($ch);
  
    if (curl_errno($ch)) {
  
        //echo 'Error:' . curl_error($ch);
  
    }
  
    curl_close ($ch);
  
  
  
    $resultString = json_decode($resultFlagger, true);
  
    //print_r($resultString['flagged'][0]. "<br>");
  
    if (isset($resultString['flagged'][0])) {
  
      $flagged = $resultString['flagged'][0];
  
      //print_r($flagged. "<br>");
  
      //$query = "UPDATE `$tablename` SET `text-flagged`='$flagged' WHERE time= $time";
  
      $query = "UPDATE `$tablename` SET `text-flagged`='$flagged' WHERE `tid`='$tid';";
  
      $resultQuery = mysqli_query($link, $query);
  
      if($resultQuery){
  
        //echo "Text flag 1 uploaded successfully." . "<br>";
  
        $queryConversations = "UPDATE `$tablenameConversations` SET `text-flag`=`text-flag`+1 WHERE number = '$number';";
  
        $resultQueryConversations = mysqli_query($link, $queryConversations);
  
        if($resultQueryConversations) {
  
          //echo "success" . "<br>";
  
        }
      }
    } else {
  
      $flagged = 0;
  
      //print_r($flagged. "<br>");
  
      //$query = "UPDATE `$tablename` SET `text-flagged`='$flagged' WHERE time= $time";
  
      $query = "UPDATE `$tablename` SET `text-flagged`='$flagged' WHERE `tid`='$tid';";
  
      $resultQuery = mysqli_query($link, $query);
  
      if($resultQuery){
        //echo "Text flag 0 uploaded successfully." . "<br>";
      }
    }
  */
?>
