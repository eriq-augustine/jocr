package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.MapUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A classifier that uses the regional densities of a character.
 */
public class RegionClassifier extends CharacterClassifier {
   private static Logger logger = LogManager.getLogger(RegionClassifier.class.getName());

   private static final int DEFAULT_REGIONS_PER_SIDE = 5;

   private final int regionsPerSide;

   public RegionClassifier(WrapImage[] characterImages,
                           String characters,
                           String[] fonts) throws Exception {
      this(Arrays.asList(characterImages), characters,
           DEFAULT_REGIONS_PER_SIDE, fonts);
   }

   public RegionClassifier(WrapImage[] characterImages,
                           String characters,
                           int regionsPerSide,
                           String[] fonts) throws Exception {
      this(Arrays.asList(characterImages), characters,
           regionsPerSide, fonts);
   }

   /**
    * |combineDirections| will merge DCs that are on the same line.
    *  This will half the features.
    * |groupSize| is the size of groups of DCs in the same scanning set.
    * This is meant to reduce the number of features and mitigate noise.
    * A |groupSize| of 1 means no groping will occur.
    * |fonts| are used as attributes to the classifier.
    */
   public RegionClassifier(List<WrapImage> trainingImages,
                           String trainingCharacters,
                           int regionsPerSide,
                           String[] fonts) throws Exception {
      super((int)(Math.pow(regionsPerSide, 2)));

      assert(regionsPerSide > 0);

      this.regionsPerSide = regionsPerSide;

      boolean res = train(
            trainingImages,
            trainingCharacters,
            fonts,
            MapUtils.inlinePut(
               new HashMap<String, String>(), "regions_per_side", Integer.toString(regionsPerSide)));

      if (!res) {
         logger.fatal("Failed to train a classifier.");
         System.exit(1);
      }
   }

   /**
    * @inheritDoc
    */
   protected double[] getFeatureValues(WrapImage image) {
      // Add the character densities.
      double[] characterDensities = ImageUtils.regionDensities(image,
                                                               128,
                                                               regionsPerSide);

      return characterDensities;
   }
}
