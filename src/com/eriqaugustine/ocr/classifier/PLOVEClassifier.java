package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.plove.PLOVE;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A classifier specialized for P-LOVE features.
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
      boolean res = train(
            trainingCharacters,
            fonts,
            new HashMap<String, String>()
      );

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
