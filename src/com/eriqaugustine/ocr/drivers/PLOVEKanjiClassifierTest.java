package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.OCRClassifier;
import com.eriqaugustine.ocr.classifier.PLOVEClassifier;
import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.classifier.reduce.NoReducer;
import com.eriqaugustine.ocr.classifier.reduce.KLTReducer;

import com.eriqaugustine.ocr.plove.PLOVE;

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
      // FeatureVectorReducer reduce = new NoReducer(PLOVE.getNumberOfFeatures());
      FeatureVectorReducer reduce = new KLTReducer(PLOVE.getNumberOfFeatures(), 650);

      OCRClassifier classy =
         new PLOVEClassifier(trainingCharacters,
                             // new String[]{Props.getString("DEFAULT_FONT_FAMILY")});
                             // new String[]{"IPAGothic", "HGMinchoB", "RyuminStd-Regular-KS", "FutoMinA101Pro-Bold"});
                             Props.getList("CLASSIFIER_TRAINING_FONTS").toArray(new String[0]),
                             reduce);

      classifierTest(classy, true);
   }
}
