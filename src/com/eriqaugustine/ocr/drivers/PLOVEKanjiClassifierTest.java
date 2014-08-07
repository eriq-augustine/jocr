package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.CharacterClassifier;
import com.eriqaugustine.ocr.classifier.PLOVEClassifier;

import com.eriqaugustine.ocr.utils.Props;

/**
 * Quck test for PLOVEClassifier.
 */
public class PLOVEKanjiClassifierTest extends KanjiClassifierTest {
   public static void main(String[] args) throws Exception {
      PLOVEKanjiClassifierTest test = new PLOVEKanjiClassifierTest();
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
