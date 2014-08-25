package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.OCRClassifier;
import com.eriqaugustine.ocr.classifier.RegionClassifier;

import com.eriqaugustine.ocr.utils.Props;

/**
 * Quck test for  RegionClassifier.
 */
public class RegionClassifierTest extends ClassifierTest {
   public static void main(String[] args) throws Exception {
      RegionClassifierTest test = new  RegionClassifierTest();
      test.run();
   }

   private void run() throws Exception {
      OCRClassifier classy =
         new RegionClassifier(trainingCharacters,
                              8,
                              new String[]{Props.getString("DEFAULT_FONT_FAMILY")});


      classifierTest(classy, false);
   }
}
