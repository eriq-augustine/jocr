package com.eriqaugustine.ocr.classifier;

import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.classifier.reduce.KLTReducer;
import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.plove.PLOVE;

import com.eriqaugustine.ocr.utils.MathUtils;
import com.eriqaugustine.ocr.utils.Props;
import com.eriqaugustine.ocr.utils.StringUtils;
import com.eriqaugustine.ocr.utils.SystemUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This "classifier" is actually a collection of many different classifiers.
 * Each classifier will be trained on a subset of the data bassed off of the
 * density of each training character.
 * This way, the training time and memory size of each classifier can be more manageable.
 *
 * For example:
 * Let's divide the training set into 10 approximatley evenly sized groups.
 * Now, lets take adjacent groups of three and train a classifier on it.
 * We will have eight classifiers:
 * (0, 1, 2), (1, 2, 3), (2, 3, 4), ect...
 * We want to overlap because we can easily see a font that has different densities.
 *
 * Note that we don't have exact sizings on the groups because there will probably be duplicate characters
 * in each group that will get de-duplicated.
 *
 * TODO(eriq): Include bold fonts.
 */
public class OverlappingRegionClassifier implements OCRClassifier {
   private static Logger logger = LogManager.getLogger(OverlappingRegionClassifier.class.getName());

   private static final int DEFAULT_NUM_GROUPS = 10;
   private static final int DEFAULT_GROUPS_PER_CLASSIFIER = 3;

   /**
     * The classifiers.
     */
   private OCRClassifier[] classifiers;

   /**
    * The breakpoints for each classifier.
    * The first one will be the point between the first and second classifier and so on.
    * Note that we don't need to keep track of any minimum or maximum densities,
    * those just go in the first and last groups respectivley.
    */
   private double[] densityBreaks;

   /**
    * The characters that will appear in each group.
    * Note that these sets are not disjoint.
    */
   private String[] trainingGroups;

   // TEST
   public static void main(String[] args) {
      String trainingCharacters = Props.getString("KYOIKU_FULL") +
                                  Props.getString("KANA_FULL") +
                                  Props.getString("PUNCTUATION");
      String[] fonts = Props.getList("CLASSIFIER_TRAINING_FONTS").toArray(new String[0]);
      // String[] fonts = new String[]{Props.getString("DEFAULT_FONT_FAMILY")};

      OverlappingRegionClassifier classy = new OverlappingRegionClassifier(trainingCharacters, fonts);
   }

   public OverlappingRegionClassifier(String trainingCharacters, String[] fonts) {
      this(DEFAULT_NUM_GROUPS, DEFAULT_GROUPS_PER_CLASSIFIER, trainingCharacters, fonts);
   }

   public OverlappingRegionClassifier(int numGroups, int groupsPerClassifier,
                                      String trainingCharacters, String[] fonts) {
      initDensities(numGroups, groupsPerClassifier, trainingCharacters, fonts);
      initClassifiers(groupsPerClassifier, fonts);
   }

   private void initDensities(int numGroups, int groupsPerClassifier,
                              String trainingCharacters, String[] fonts) {
      classifiers = new OCRClassifier[numGroups + 1 - groupsPerClassifier];
      densityBreaks = new double[classifiers.length - 1];

      // We can't make arrays of generics.
      List<Set<Character>> trainingGroupSets = new ArrayList<Set<Character>>();
      trainingGroups = new String[numGroups];

      for (int i = 0; i < numGroups; i++) {
         trainingGroups[i] = "";
         trainingGroupSets.add(new HashSet<Character>());
      }

      // Note that we only want 1x1 density maps.
      double[][][] densityMaps = CharacterImage.getFontDensityMaps(trainingCharacters, 1, 1, fonts);

      double[] densities = new double[densityMaps.length];
      for (int i = 0; i < densityMaps.length; i++) {
         densities[i] = densityMaps[i][0][0];
      }

      // Get the orders for each density.
      // Note that the largest value will get a 0.
      int[] ranks = MathUtils.rank(densities);
      int groupSize = (int)(Math.ceil(ranks.length / (double)numGroups));

      for (int i = 0; i < ranks.length; i++) {
         // Invert the rank to get ascending ranks.
         int groupIndex = (ranks.length - 1 - ranks[i]) / groupSize;
         trainingGroupSets.get(groupIndex).add(new Character(trainingCharacters.charAt(i % trainingCharacters.length())));
      }

      // Get the density breaks.
      Arrays.sort(densities);

      // Note that sense we overlap groups, we do not need to be 100% precise.
      // Remember, there are one less breaks than group sizes.
      for (int i = 0; i < densityBreaks.length; i++) {
         densityBreaks[i] = densities[groupSize * (i + 1)];
      }

      // Collapse the training sets down into strings.
      for (int i = 0; i < numGroups; i++) {
         trainingGroups[i] = StringUtils.join(trainingGroupSets.get(i), "");
      }
   }

   private void initClassifiers(int groupsPerClassifier, String[] fonts) {
      for (int i = 0; i < classifiers.length; i++) {
         String trainingCharacters = "";

         for (int groupIndex = 0; groupIndex < groupsPerClassifier; groupIndex++) {
            trainingCharacters += trainingGroups[i + groupIndex];
         }

         FeatureVectorReducer reduce = new KLTReducer(PLOVE.getNumberOfFeatures(), 650);

         try {
            logger.debug(SystemUtils.memoryMarkString("Training (" + i + ") BEGIN"));

            classifiers[i] = new PLOVEClassifier(trainingCharacters, fonts, reduce);

            logger.debug(SystemUtils.memoryMarkString("Training (" + i + ") END"));
         } catch (Exception ex) {
            logger.fatal("Could not create classifier.", ex);
            System.exit(1);
         }
      }
   }

   public String classify(WrapImage image) {
      // Get the density of the image to decide which classifier to use.
      double density = CharacterImage.getDensityMap(image, 1, 1)[0][0];
      OCRClassifier classifierToUse = null;

      for (int i = 0; i < densityBreaks.length; i++) {
         if (density < densityBreaks[i]) {
            classifierToUse = classifiers[i];
            break;
         }
      }

      if (classifierToUse == null) {
         classifierToUse = classifiers[classifiers.length - 1];
      }

      return classifierToUse.classify(image);
   }
}
