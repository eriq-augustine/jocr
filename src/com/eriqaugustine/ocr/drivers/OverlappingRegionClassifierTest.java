package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.OCRClassifier;
import com.eriqaugustine.ocr.classifier.OverlappingRegionClassifier;

import com.eriqaugustine.ocr.utils.Props;
import com.eriqaugustine.ocr.utils.SystemUtils;

/**
 * Quck test for OverlappingRegionClassifier.
 */
public class OverlappingRegionClassifierTest extends ClassifierTest {
   public static void main(String[] args) throws Exception {
      OverlappingRegionClassifierTest test = new OverlappingRegionClassifierTest();
      test.run();
   }

   private void run() throws Exception {
      // String trainingCharacters = Props.getString("KYOIKU_FULL") + Props.getString("KANA_FULL") + Props.getString("PUNCTUATION");
      String trainingCharacters = Props.getString("KANA_FULL") + Props.getString("PUNCTUATION");

      SystemUtils.memoryMark("Training BEGIN", System.err);

      String[] fonts = Props.getList("CLASSIFIER_TRAINING_FONTS").toArray(new String[0]);
      // String[] fonts = new String[]{Props.getString("DEFAULT_FONT_FAMILY")};

      OCRClassifier classy = new OverlappingRegionClassifier(trainingCharacters, fonts);

      SystemUtils.memoryMark("Training END", System.err);

      SystemUtils.memoryMark("Test BEGIN", System.err);

      classifierTest(classy, true);

      SystemUtils.memoryMark("Test END", System.err);
   }
}
