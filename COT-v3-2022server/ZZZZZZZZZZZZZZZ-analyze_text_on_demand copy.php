<?php



  //Arup Ghosh, March 2018

  //Updated on April 13, 2018

  //Updated on May 1, 2018



  require 'aws/aws-autoloader.php';



  include 'connectDB.php';







  //for testing

  //$_POST['id'] = '10';

  //$_POST['pid'] = '10';

  //$_POST['tid'] = '3';

  //$id = $_POST['id'];

  //$pid = $_POST['pid'];

  //$tid = $_POST['tid'];


  //$initial = 0;


  $flagged = 0;

  $moderation_results = null;



  $resultAWSRekognition = new \Aws\Rekognition\RekognitionClient([ //Suspect



    'version' => 'latest',



    'region'  => 'us-east-1',



    'verify' => 'false',



    'credentials' => [



    'key'    => 'AKIAJTQMDVHEFOX7JIQQ',



    'secret' => 'I6yFFS5mi/3bumbHAxxpUK7eNW34iVIyI2u369bw'



    ]



  ]);







  $resultAWSComprehend = new \Aws\Comprehend\ComprehendClient([ //Suspect



    'version' => 'latest',



    'region'  => 'us-east-1',



    'verify' => 'false',



    'credentials' => [



    'key'    => 'AKIAJTQMDVHEFOX7JIQQ',



    'secret' => 'I6yFFS5mi/3bumbHAxxpUK7eNW34iVIyI2u369bw'



    ]



  ]);







  $tablename = "texts-" . $pid . "-" . $id;



  //echo $tablename . "<br>";





  //conversations table



  $tablenameConversations = "conversations-" . $pid . "-" . $id;



  $query = "SELECT number, text, media, mime FROM `$tablename` WHERE tid='$tid';";



  $resulttexts = mysqli_query($link, $query);



  if(mysqli_num_rows($resulttexts) == 0) {



    //echo "No data found in " . $tablename . "  based on inputs." . "<br>";



    exit;



  }







  //$row = mysqli_fetch_array($resulttexts, MYSQLI_NUM);



  //$text = $row[0];



  //$media = $row[1];



  //$mime = $row[2];



  $rowtexts = $resulttexts->fetch_assoc();



  $number = $rowtexts["number"];







  if ($rowtexts["text"] != NULL) {



    $resultSentimentScore = $resultAWSComprehend->DetectSentiment([



    'LanguageCode' => 'en', // REQUIRED



    'Text' => strval($rowtexts["text"]), // REQUIRED



    ]);



  //print_r($resultSentimentScore. "<br>");







  $sentiment_results= "";



  $sentiment_score_results= "";











  $sentiment_score_results= $sentiment_score_results . "MIXED" . "," . $resultSentimentScore['SentimentScore']['Mixed'] . ","



    . "NEGATIVE" . "," . $resultSentimentScore['SentimentScore']['Negative'] . "," . "NEUTRAL" . "," . $resultSentimentScore['SentimentScore']['Neutral'] . "," . "POSITIVE" . "," . $resultSentimentScore['SentimentScore']['Positive'] . ","



  ;











  $sentiment_results= $resultSentimentScore['Sentiment'];



  //print_r($sentiment_score_results. "<br>");



  //print_r($sentiment_results. "<br>");







  //$query = "UPDATE `$tablename` SET `text-sentiment`='$sentiment_results' WHERE time= $time";



  $query = "UPDATE `$tablename` SET `text-sentiment-score` = '$sentiment_score_results' WHERE `tid` = '$tid';";







  $resultQuery = mysqli_query($link, $query);



   if($resultQuery){



     //echo "Sentiment score results uploaded successfully." . "<br>";



   }







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







    if($resultQueryConversations) {



      //echo "success" . "<br>";



    }







   }











  $resultEntities = $resultAWSComprehend->detectEntities([



    'LanguageCode' => 'en', // REQUIRED



    'Text' => strval($rowtexts["text"]), // REQUIRED



  ]);







  //print_r($resultEntities. "<br>");



  $entities_results= "";











  for ($n=0;$n<sizeof($resultEntities['Entities']); $n++){



    $entities_results= $entities_results . $resultEntities['Entities'][$n]['Score'] . ","



    . $resultEntities['Entities'][$n]['Text'] . "," . $resultEntities['Entities'][$n]['Type'] . ","



  ;



  }







  //print_r($entities_results. "<br>");



  //$query = "UPDATE `$tablename` SET `text-entities`='$entities_results' WHERE time= $time";







  if(!empty($entities_results)) {



    $query = "UPDATE `$tablename` SET `text-entities` = '$entities_results' WHERE `tid` = '$tid';";







    $resultEntities = mysqli_query($link, $query);



    if($resultEntities){



      //echo "Text entities uploaded successfully." . "<br>";



    }







  }











  /////url -X POST https://flagger.rakkoon.com/tc -d '{"messages": [{"text": "some text here", "id":"1"}, {"text": "other text here", "id": "2"}]}'











  //$text = array (



  //  'text' => strval($rowtexts["text"]),



  //  'id' => "1"



  //);



  $messages = array('messages' => [array (



    'text' => strval($rowtexts["text"]),



    'id' => "1"



  )]);











  //print_r($messages);



  $messages2 = json_encode ( $messages);



  //print_r($messages2. "<br>");



  $ch = curl_init();







  curl_setopt($ch, CURLOPT_URL, "https://flagger.rakkoon.com/tc");



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







}



if ($rowtexts["media"] != NULL && ($rowtexts["mime"] == "image/jpeg" || $rowtexts["mime"] == "image/png")) {



  //echo "Picture: " . "<br>";



  if ($rowtexts["mime"] == "image/jpeg") {



    //echo '<img src="data:image/jpeg;base64,'.base64_encode( $rowtexts["media"] ).'"/>' . "<br>";



  }







  if ($rowtexts["mime"] == "image/png") {



    //echo '<img src="data:image/png;base64,'.base64_encode( $rowtexts["media"] ).'"/>' . "<br>";



  }











  $resultLabels = $resultAWSRekognition->detectLabels([



    'Image' => [ // REQUIRED



    'Bytes' => $rowtexts["media"],



  ],



  'MaxLabels' => 5,



  'MinConfidence' => 10,



  ]);







  $labels_results= "";

  $labels_results_wc= ""; //for image word cloud







  for ($n=0;$n<sizeof($resultLabels['Labels']); $n++){



    

    if ((strcmp($resultLabels['Labels'][$n]['Name'],"People") ==0 || strcmp($resultLabels['Labels'][$n]['Name'],"Human") ==0)) {

      //echo "Label: ". $resultLabels['Labels'][$n]['Name']."<br>";
      //echo "Don't add it to db"."<br>";

    } else {

      //echo "Label: ". $resultLabels['Labels'][$n]['Name']."<br>";

      $labels_results= $labels_results . $resultLabels['Labels'][$n]['Confidence'] . ","

 

      . $resultLabels['Labels'][$n]['Name'] . ","



      ;

      $labels_results_wc= $labels_results_wc . $resultLabels['Labels'][$n]['Name'] . " "

      ;
    }






  }



  //print_r($labels_results. "<br>");







  //$query = "UPDATE `$tablename` SET `media-labels`='$labels_results' WHERE time= $time";



  $query = "UPDATE `$tablename` SET `media-Labels` = '$labels_results' WHERE `tid`='$tid';";











  $resultQuery = mysqli_query($link, $query);



  if($resultQuery){



    //echo "Image labels updated successfully." . "<br>";

    //echo $labels_results_wc;

    //echo $number;



    $queryConversations = "UPDATE `$tablenameConversations` SET `word-cloud-text`= CONCAT(ifnull(`word-cloud-text`,''),'$labels_results_wc') WHERE number = '$number';";



    $resultQueryConversationsWC = mysqli_query($link, $queryConversations);



    if($resultQueryConversationsWC) {



      //echo "success" . "<br>";



    }



  }







  $resultModeration = $resultAWSRekognition->DetectModerationLabels([



    'Image' => [ // REQUIRED



    'Bytes' => $rowtexts["media"],



     ],



     'MinConfidence' => 10,



  ]);



  //print_r($resultModeration. "<br>");







  $moderation_results= "";







  for ($n=0;$n<sizeof($resultModeration['ModerationLabels']); $n++){



    $moderation_results= $moderation_results . $resultModeration['ModerationLabels'][$n]['Confidence'] . ","



    . $resultModeration['ModerationLabels'][$n]['Name'] . ","



    . $resultModeration['ModerationLabels'][$n]['ParentName'] . ","



    ;



  }







  //print_r($moderation_results. "<br>");



  //$query = "UPDATE `$tablename` SET `media-moderation`='$moderation_results' WHERE time= $time";





  $run = 1;

  if(!empty($moderation_results)) {



      $query = "UPDATE `$tablename` SET `media-moderation` = '$moderation_results' WHERE `tid`='$tid';";







      $resultModeration = mysqli_query($link, $query);



      if($resultModeration){



        //echo "Image moderation results updaloaded successfully." . "<br>";



        $queryConversations = "UPDATE `$tablenameConversations` SET `image-flag`=`image-flag`+1 WHERE `number` = '$number';";



        $resultQueryConversations = mysqli_query($link, $queryConversations);







        if($resultQueryConversations) {



          //echo "success" . "<br>";



        }



      }



      //create_word_cloud();



  }







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



  $query = "UPDATE `processing-status` SET `processed`=`processed`+1 WHERE `pid`=$pid AND `id`=$id;";

  mysqli_query($link, $query);



  if($run==1 && $initial!=1){

    //update word cloud

    $query = "SELECT `word-cloud-text` FROM `$tablenameConversations` WHERE number='$number';";

    $result = mysqli_query($link, $query);

    $row = mysqli_fetch_array($result, MYSQLI_NUM);



    if($row[0] != NULL) { //check if word-cloud-text not null

      $word_cloud_text = $row[0];

      //echo $number . "<br>";

      //echo $word_cloud_text . "<br>";

      $WordCloudText = $word_cloud_text; 
      //echo $WordCloudText . "<br>";
      $WordCloudText = str_replace("People", "", $WordCloudText); //not needed if we refresh the db
      $WordCloudText = str_replace("Human", "", $WordCloudText); //not needed if we refresh the db
      //echo "after deleting:" . $word_cloud_text . "<br>";
      if (strpos($WordCloudText, 'Person') !== false) {
        //echo "Person found" . "<br>";
        $WordCloudText = str_replace("Person", "", $WordCloudText); 
        $word_cloud_text = $WordCloudText." "."Person";

      }



      $inputWC = array (

        'text' => $word_cloud_text,

        'scale' => 1,

        'width' => 600,

        'height' => 500,

        'colors' => array(

          '#375E97',

          '#FB6542',

          '#FFBB00',

          '#3F681C'

        ),

        'font'=> 'Tahoma',

        'use_stopwords' => true,

        'language' => 'en'





      );

      $inputWC_json = json_encode ( $inputWC );





      $ch = curl_init();

      $headers = array (

        'X-Mashape-Key:' . "2F44W4TqE6msh83O4YdfZflG1YPqp1qpn6ijsn2KSxzwoNk2hr",

        'Content-Type: application/json'

      );

      curl_setopt($ch, CURLOPT_URL, "https://textvis-word-cloud-v1.p.mashape.com/v1/textToCloud");

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

            'title' => 'New Risk Identified On'. $teenName .'\'s Device',

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

    $message = "A text sent by you to " . $name . " was flagged.";

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

