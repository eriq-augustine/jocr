package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.CharacterClassifier;
import com.eriqaugustine.ocr.classifier.PLOVEClassifier;
import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.classifier.reduce.NoReducer;
import com.eriqaugustine.ocr.classifier.reduce.KLTReducer;

import com.eriqaugustine.ocr.plove.PLOVE;

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
      // FeatureVectorReducer reduce = new NoReducer(PLOVE.getNumberOfFeatures());
      // FeatureVectorReducer reduce = new KLTReducer(PLOVE.getNumberOfFeatures(), 650);
      FeatureVectorReducer reduce = new KLTReducer(PLOVE.getNumberOfFeatures(), 400);

      CharacterClassifier classy =
         new PLOVEClassifier(trainingCharacters,
                             // new String[]{Props.getString("DEFAULT_FONT_FAMILY")});
                             // new String[]{"IPAGothic", "HGMinchoB", "RyuminStd-Regular-KS", "FutoMinA101Pro-Bold"});
                             Props.getList("CLASSIFIER_TRAINING_FONTS").toArray(new String[0]),
                             reduce);

      classifierTest(classy, true);
   }
}
