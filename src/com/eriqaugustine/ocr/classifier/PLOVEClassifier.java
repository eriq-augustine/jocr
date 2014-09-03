package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.plove.PLOVE;
import com.eriqaugustine.ocr.utils.SystemUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A classifier specialized for P-LOVE features.
 * A quick test has shown that a PLOVE scale size of 28 and a KLT reduction size of 400 yeilds good results.
 */
public class PLOVEClassifier extends CharacterClassifier {
   private static Logger logger = LogManager.getLogger(PLOVEClassifier.class.getName());

   public PLOVEClassifier(String trainingCharacters, String[] fonts, FeatureVectorReducer reduce) throws Exception {
      super(PLOVE.getNumberOfFeatures(), reduce);
      train(trainingCharacters, fonts);
   }

   public PLOVEClassifier(String trainingCharacters, String[] fonts) throws Exception {
      super(PLOVE.getNumberOfFeatures());
      train(trainingCharacters, fonts);
   }

   private void train(String trainingCharacters, String[] fonts) throws Exception {
      SystemUtils.memoryMark("Training BEGIN", System.err);

      boolean res = train(
            trainingCharacters,
            fonts,
            new HashMap<String, String>()
      );

      SystemUtils.memoryMark("Training END", System.err);

      if (!res) {
         logger.fatal("Failed to train a classifier.");
         System.exit(1);
      }
   }

   /**
    * @inheritDoc
    */
   protected double[] getFeatureValues(WrapImage image) {
      return PLOVE.plove(image);
   }
}
