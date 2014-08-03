package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.CharacterClassifier;
import com.eriqaugustine.ocr.classifier.PLOVEClassifier;

import com.eriqaugustine.ocr.utils.Props;

/**
 * Quck test for PLOVEClassifier.
 */
public class PLOVEClassifierTest extends ClassifierTest {
   public static void main(String[] args) throws Exception {
      PLOVEClassifierTest test = new PLOVEClassifierTest();
      test.run();
   }

   private void run() throws Exception {
      CharacterClassifier classy =
         new PLOVEClassifier(trainingContents,
                             trainingClasses,
                             new String[]{Props.getString("DEFAULT_FONT_FAMILY")});

      classifierTest(classy, true);
   }
}
