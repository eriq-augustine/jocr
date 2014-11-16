package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.PLOVEClassifier;
import com.eriqaugustine.ocr.classifier.OCRClassifier;
import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.classifier.reduce.KLTReducer;
import com.eriqaugustine.ocr.classifier.reduce.NoReducer;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.plove.PLOVE;
import com.eriqaugustine.ocr.utils.NetUtils;
import com.eriqaugustine.ocr.utils.Props;
import com.eriqaugustine.ocr.utils.SystemUtils;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * A simple server to handle tranlations.
 * This will keep a translator in memory so it doesn't have to be retrained.
 */
public class RemoteClassifierServer {
   public static void main(String[] args) throws Exception {
      String[] fonts = null;
      if (args.length == 0) {
         fonts = Props.getList("CLASSIFIER_TRAINING_FONTS").toArray(new String[0]);
      } else {
         fonts = args;
      }

      OCRClassifier classy = getClassifier(fonts);
      ServerSocket socket = new ServerSocket(Props.getInt("DEFAULT_TRANSLATION_SERVER_PORT"));

      while (true) {
         Socket clientSocket = socket.accept();
         WrapImage image = NetUtils.getImage(clientSocket);

         String text = classy.classify(image);
         NetUtils.sendString(clientSocket, text);

         clientSocket.close();
      }
   }

   public static OCRClassifier getClassifier(String[] fonts) throws Exception {
      String[] images = new String[]{"testImages/testSets/youbatoVol1_kana/Yotsubato_v01_022.jpg"};

      SystemUtils.memoryMark("Training BEGIN", System.err);

      /*
      String trainingCharacters = Props.getString("HIRAGANA");
      FeatureVectorReducer reduce = new NoReducer(PLOVE.getNumberOfFeatures());
      */

      String trainingCharacters = Props.getString("KYOIKU_FULL") + Props.getString("KANA_FULL") + Props.getString("PUNCTUATION");
      FeatureVectorReducer reduce = new KLTReducer(PLOVE.getNumberOfFeatures(), 400);

      OCRClassifier classy = new PLOVEClassifier(trainingCharacters, fonts, reduce);

      SystemUtils.memoryMark("Training END", System.err);

      return classy;
   }
}
