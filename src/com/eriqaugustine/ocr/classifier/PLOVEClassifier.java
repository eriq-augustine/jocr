package com.eriqaugustine.ocr.classifier;

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

   public PLOVEClassifier(
         WrapImage[] characterImages,
         String characters,
         String[] fonts) throws Exception {
      this(Arrays.asList(characterImages), characters, fonts);
   }

   public PLOVEClassifier(
         List<WrapImage> trainingImages,
         String trainingCharacters,
         String[] fonts) throws Exception {
      super(PLOVE.getNumberOfFeatures());

      boolean res = train(
            trainingImages,
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
