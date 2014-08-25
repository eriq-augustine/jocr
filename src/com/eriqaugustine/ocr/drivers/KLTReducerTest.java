package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.OCRClassifier;
import com.eriqaugustine.ocr.classifier.PLOVEClassifier;
import com.eriqaugustine.ocr.classifier.reduce.KLTReducer;
import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.plove.PLOVE;

import com.eriqaugustine.ocr.utils.Props;

/**
 * Quck test for PLOVEClassifier.
 */
public class KLTReducerTest extends ClassifierTest {
   public static void main(String[] args) throws Exception {
      KLTReducerTest test = new KLTReducerTest();
      test.run();
   }

   private void run() throws Exception {
      int numFeaturesStart = 300;
      int numFeaturesEnd = 2400;
      int numFeaturesDelta = 50;

      String[] fonts = Props.getList("CLASSIFIER_TRAINING_FONTS").toArray(new String[0]);

      System.err.print("Fonts: ");
      for (int i = 0; i < fonts.length; i++) {
         System.err.print(fonts[i] + ", ");
      }
      System.err.println();

      for (int numFeatures = numFeaturesStart; numFeatures <= numFeaturesEnd; numFeatures += numFeaturesDelta) {
         FeatureVectorReducer reduce = new KLTReducer(PLOVE.getNumberOfFeatures(), numFeatures);

         OCRClassifier classy =
            new PLOVEClassifier(trainingCharacters, fonts, reduce);

         long startTime = System.currentTimeMillis();
         double res = classifierTest(classy, false);
         long totalTime = System.currentTimeMillis() - startTime;

         System.out.println(String.format("%6.3f, PLOVE, KNN, %d, %d", res, numFeatures, totalTime));
         System.gc();
      }
   }
}
