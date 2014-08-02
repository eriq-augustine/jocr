package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.CharacterClassifier;
import com.eriqaugustine.ocr.classifier.PDCClassifier;

import com.eriqaugustine.ocr.utils.Props;

/**
 * Quck test for PDCClassifier.
 */
public class PDCTest extends ClassifierTest {
   public static void main(String[] args) throws Exception {
      PDCTest test = new PDCTest();
      test.run();
   }

   private void run() throws Exception {
      CharacterClassifier classy =
         new PDCClassifier(trainingContents,
                           trainingClasses,
                           true,
                           1,
                           new String[]{Props.getString("DEFAULT_FONT_FAMILY")});

      classifierTest(classy, false);
   }
}
