<?php

  //Arup Ghosh, March 2018
  //Updated on April 13, 2018


  require 'aws/aws-autoloader.php';

  include 'connectDB.php';





  $resultAWSRekognition = new \Aws\Rekognition\RekognitionClient([

    'version' => 'latest',

    'region'  => 'us-east-1',

    'verify' => 'false',

    'credentials' => [

    'key'    => 'AKIAJTQMDVHEFOX7JIQQ',

    'secret' => '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'

    ]

  ]);



  $resultAWSComprehend = new \Aws\Comprehend\ComprehendClient([

    'version' => 'latest',

    'region'  => 'us-east-1',

    'verify' => 'false',

    'credentials' => [

    'key'    => 'AKIAJTQMDVHEFOX7JIQQ',

    'secret' => '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'

    ]

  ]);

  //for testing 
  //$_POST['id'] = '10';
  //$_POST['pid'] = '10';
  //$id = $_POST['id'];
  //$pid = $_POST['pid'];


  $tablename = "texts-" . $pid . "-" . $id;

  //conversations table

  $tablenameConversations = "conversations-" . $pid . "-" . $id;

  //echo $tablename . "<br>";



  $query = "SELECT tid, number, text, media, mime FROM `$tablename`;";



  $resulttexts = mysqli_query($link, $query);

  //$rowtexts = mysqli_fetch_array($resulttexts, MYSQLI_NUM);


  // In the beginning, each and every aggregate score = 0 
  $queryConversations = "UPDATE `$tablenameConversations` SET `mixed-sentiment-score`=0;";

  $resultQueryConversations = mysqli_query($link, $queryConversations);

  $queryConversations = "UPDATE `$tablenameConversations` SET `negative-sentiment-score`=0;";

  $resultQueryConversations = mysqli_query($link, $queryConversations);

  $queryConversations = "UPDATE `$tablenameConversations` SET `neutral-sentiment-score`=0;";

  $resultQueryConversations = mysqli_query($link, $queryConversations);

  $queryConversations = "UPDATE `$tablenameConversations` SET `positive-sentiment-score`=0;";

  $resultQueryConversations = mysqli_query($link, $queryConversations);

  $queryConversations = "UPDATE `$tablenameConversations` SET `text-flag`=0;";

  $resultQueryConversations = mysqli_query($link, $queryConversations);

  $queryConversations = "UPDATE `$tablenameConversations` SET `image-flag`=0;";

  $resultQueryConversations = mysqli_query($link, $queryConversations);

  $queryConversations = "UPDATE `$tablenameConversations` SET `word-cloud`= NULL;";

  $resultQueryConversations = mysqli_query($link, $queryConversations);

  $queryConversations = "UPDATE `$tablenameConversations` SET `word-cloud-text`= NULL;";

  $resultQueryConversations = mysqli_query($link, $queryConversations);


  $total_rows = $resulttexts->num_rows;
  $current_row = 0;
  while($rowtexts = $resulttexts->fetch_assoc()) {

    $current_row = $current_row + 1; 

    //$myqueryresult = "text-" . $rowtexts["text"] . "mime-" . $rowtexts["mime"];

    //echo $myqueryresult . "<br>";



    $tid = $rowtexts["tid"];

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

      $query = "UPDATE `$tablename` SET `text-sentiment-score` = '$sentiment_score_results' WHERE `tid`='$tid';";



      $resultQuery = mysqli_query($link, $query);

       if($resultQuery){

         //echo "Sentiment score results uploaded successfully." . "<br>";

       }



      $query = "UPDATE `$tablename` SET `text-sentiment` = '$sentiment_results' WHERE `tid`='$tid';";



      $resultQuery = mysqli_query($link, $query);

       if($resultQuery){

         //echo "Sentiment results uploaded successfully." . "<br>";

          //update conversations table

          //print_r($tablenameConversations . "<br>");

          if($sentiment_results=="MIXED") {

            $queryConversations = "UPDATE `$tablenameConversations` SET `mixed-sentiment-score`=`mixed-sentiment-score`+1 WHERE `number` = '$number';";

            $resultQueryConversations = mysqli_query($link, $queryConversations);

          }



          else if($sentiment_results=="NEGATIVE") {

            $queryConversations = "UPDATE `$tablenameConversations` SET `negative-sentiment-score`=`negative-sentiment-score`+1 WHERE `number` = '$number';";

            $resultQueryConversations = mysqli_query($link, $queryConversations);

          }



          else if($sentiment_results=="NEUTRAL") {

            $queryConversations = "UPDATE `$tablenameConversations` SET `neutral-sentiment-score`=`neutral-sentiment-score`+1 WHERE `number` = '$number';";

            $resultQueryConversations = mysqli_query($link, $queryConversations);

          }



          else if($sentiment_results=="POSITIVE") {

            $queryConversations = "UPDATE `$tablenameConversations` SET `positive-sentiment-score`=`positive-sentiment-score`+1 WHERE `number` = '$number';";

            $resultQueryConversations = mysqli_query($link, $queryConversations);

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

         $query = "UPDATE `$tablename` SET  `text-entities` = '$entities_results' WHERE `tid`='$tid';";



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

        //update conversations table

        //print_r($tablenameConversations . "<br>");

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



        $labels_results= $labels_results . $resultLabels['Labels'][$n]['Confidence'] . ","

        . $resultLabels['Labels'][$n]['Name'] . ","

        ;

        $labels_results_wc= $labels_results_wc . $resultLabels['Labels'][$n]['Name'] . " "

        ;

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

      if(!empty($moderation_results)) {

          $query = "UPDATE `$tablename` SET `media-moderation` = '$moderation_results' WHERE `tid` = '$tid';";





          $resultModeration = mysqli_query($link, $query);

          if($resultModeration){

            //echo "Image moderation results updaloaded successfully." . "<br>";

            //update conversations table

            //print_r($tablenameConversations . "<br>");

            $queryConversations = "UPDATE `$tablenameConversations` SET `image-flag`=`image-flag`+1 WHERE number = '$number';";

            $resultQueryConversations = mysqli_query($link, $queryConversations);



            if($resultQueryConversations) {

              //echo "success" . "<br>";

            }

          }



      }





      } elseif ($rowtexts["media"] != NULL && ($rowtexts["mime"] == "video/mp4" || $rowtexts["mime"] == "video/quicktime")) {

        // video

        $query = "UPDATE `$tablename` SET `media-Labels` = 'NO_PROCESSING', `media-moderation` = 'NO_PROCESSING' WHERE `tid` = '$tid';";

        $resultQuery = mysqli_query($link, $query);

        if($resultQuery){

          //echo "Video NO PROCESSING tag uploaded successfully." . "<br>";

        }



      } elseif ($rowtexts["media"] != NULL) {

        //$query = "UPDATE `$tablename` SET `media-Labels` = 'FILE_TYPE_NOT_SUPPORTED', `media-moderation` = 'FILE_TYPE_NOT_SUPPORTED' WHERE `tid` = '$tid';";

        //$resultQuery = mysqli_query($link, $query);

        //echo "Hello";

      //  if($resultQuery){

          //echo $rowtexts["mime"] . "file type not supported." . "<br>";

        //}



      } else {



      }

      //echo "==============================================================" . "<br>";

      if($current_row==$total_rows) { 

         $query = "SELECT number, `word-cloud-text` FROM `$tablenameConversations`;";

         $result = mysqli_query($link, $query);
         while($row = $result->fetch_assoc()) {

            //$myqueryresult = "text-" . $rowtexts["text"] . "mime-" . $rowtexts["mime"];

            //echo $myqueryresult . "<br>";

            $number = $row["number"];

            if($row["word-cloud-text"] != NULL) {
              $word_cloud_text = $row["word-cloud-text"];
              //echo $number . "<br>";
              //echo $word_cloud_text . "<br>";

              $inputWC = array (
                'text' => $word_cloud_text,
                'scale' => 1,
                'width' => 400,
                'height' => 300,
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
                //echo 'Error:' . curl_error($ch);
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

      }



    }



  mysqli_close($link);



?>

