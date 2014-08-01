package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.pdc.PDC;
import com.eriqaugustine.ocr.pdc.PDCInfo;
import com.eriqaugustine.ocr.utils.MapUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.StringUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * A classifier specialized for PDC features.
 */
public class PDCClassifier extends CharacterClassifier {
   private static Logger logger = LogManager.getLogger(PDCClassifier.class.getName());

   private static final int DEFAULT_GROUP_SIZE = 1;
   private static final boolean DEFAULT_COMBINE_DIRECTIONS = false;
   private static final int DEFUALT_REGIONS_PER_SIDE = 5;

   private final int groupSize;
   private final boolean combineDirections;

   public PDCClassifier(WrapImage[] characterImages,
                        String characters,
                        String[] fonts) throws Exception {
      this(Arrays.asList(characterImages), characters,
           DEFAULT_COMBINE_DIRECTIONS, DEFAULT_GROUP_SIZE, fonts);
   }

   public PDCClassifier(WrapImage[] characterImages,
                        String characters,
                        boolean combineDirections,
                        int groupSize,
                        String[] fonts) throws Exception {
      this(Arrays.asList(characterImages), characters,
           combineDirections, groupSize, fonts);
   }

   /**
    * |combineDirections| will merge DCs that are on the same line.
    *  This will half the features.
    * |groupSize| is the size of groups of DCs in the same scanning set.
    * This is meant to reduce the number of features and mitigate noise.
    * A |groupSize| of 1 means no groping will occur.
    * |fonts| are used as attributes to the classifier.
    */
   // Suppress the classifier Class cast.
   @SuppressWarnings("unchecked")
   public PDCClassifier(List<WrapImage> trainingImages,
                        String trainingCharacters,
                        boolean combineDirections,
                        int groupSize,
                        String[] fonts) throws Exception {
      super(getNumberOfFeatues(combineDirections, groupSize));

      assert(groupSize > 0);
      assert(PDC.getNumDCs() % groupSize == 0);

      this.combineDirections = combineDirections;
      this.groupSize = groupSize;

      boolean res = train(
            trainingImages,
            trainingCharacters,
            fonts,
            MapUtils.inlinePut(MapUtils.inlinePut(
               new HashMap<String, String>(), "combine_directions", Boolean.toString(combineDirections)),
               "group_size", Integer.toString(groupSize)));

      if (!res) {
         logger.fatal("Failed to train a classifier.");
         System.exit(1);
      }
   }

   /**
    * Get the number of features.
    */
   public static int getNumberOfFeatues(boolean combineDirections, int groupSize) {
      int numDimensions = combineDirections ? PDC.PDC_DIRECTION_DELTAS.length / 2 :
                                              PDC.PDC_DIRECTION_DELTAS.length;

      int rtn = PDC.getNumDCs() / groupSize * numDimensions;

      // Add in the densities.
      rtn += Math.pow(DEFUALT_REGIONS_PER_SIDE, 2);

      return rtn;
   }

   /**
    * @inheritDoc
    */
   protected double[] getFeatureValues(WrapImage image) {
      PDCInfo info = PDC.pdc(image);

      double[] dcFeatures = null;
      if (combineDirections) {
         if (groupSize > 1) {
            dcFeatures = info.halfGroupedDimensions(groupSize);
         } else {
            dcFeatures = info.halfPDCDimensions();
         }
      } else {
         if (groupSize > 1) {
            dcFeatures = info.fullGroupedDimensions(groupSize);
         } else {
            dcFeatures = info.fullPDCDimensions();
         }
      }

      // Add the character densities.
      double[] characterDensities = ImageUtils.regionDensities(image,
                                                               128,
                                                               DEFUALT_REGIONS_PER_SIDE);

      double[] rtn = new double[dcFeatures.length + characterDensities.length];

      int i = 0;

      for (double dcFeature : dcFeatures) {
         rtn[i++] = dcFeature;
      }

      for (double characterDensity : characterDensities) {
         rtn[i++] = characterDensity;
      }

      return rtn;
   }
}
