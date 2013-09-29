package com.eriqaugustine.ocr.pdc;

import com.eriqaugustine.ocr.utils.ImageUtils;
import com.eriqaugustine.ocr.utils.StringUtils;

import magick.MagickImage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A classifier specialized for PDC features.
 */
public class PDCClassifier {
   private static Logger logger = LogManager.getLogger(PDCClassifier.class.getName());

   private static final int DEFAULT_GROUP_SIZE = 1;
   private static final boolean DEFAULT_COMBINE_DIRECTIONS = false;
   private static final int DEFUALT_REGIONS_PER_SIDE = 5;

   private Classifier classifier;
   // WEKA wants FastVector over List, but it will be contained to this class only.
   private FastVector possibleCharacters;

   private final int numDCs;
   private final int groupSize;
   private final boolean combineDirections;

   private FastVector featureAttributes;

   public PDCClassifier(MagickImage[] characterImages,
                        String characters) throws Exception {
      this(characterImages, StringUtils.charSplitArray(characters),
           DEFAULT_COMBINE_DIRECTIONS, DEFAULT_GROUP_SIZE);
   }

   /**
    * |combineDirections| will merge DCs that are on the same line.
    *  This will half the features.
    * |groupSize| is the size of groups of DCs in the same scanning set.
    * This is meant to reduce the number of features and mitigate noise.
    * A |groupSize| of 1 means no groping will occur.
    */
   public PDCClassifier(MagickImage[] characterImages,
                        String characters,
                        boolean combineDirections,
                        int groupSize) throws Exception {
      this(characterImages, StringUtils.charSplitArray(characters),
           combineDirections, groupSize);
   }

   // Suppress the classifier Class cast.
   @SuppressWarnings("unchecked")
   public PDCClassifier(MagickImage[] trainingImages,
                        String[] trainingCharacters,
                        boolean combineDirections,
                        int groupSize) throws Exception {
      assert(trainingImages.length > 0);
      assert(groupSize > 0);
      assert(PDC.getNumDCs() % groupSize == 0);

      numDCs = PDC.getNumDCs();
      this.combineDirections = combineDirections;
      this.groupSize = groupSize;

      Set<String> seenCharacters = new HashSet<String>();
      for (String seenCharacter : trainingCharacters) {
         seenCharacters.add(seenCharacter);
      }

      possibleCharacters = new FastVector();
      for (String seenCharacter : seenCharacters) {
         possibleCharacters.addElement(seenCharacter);
      }

      featureAttributes = getFeatureAttributes(possibleCharacters);

      Instances trainingSet = prepTraining(trainingImages, trainingCharacters);

      Class<? extends Classifier> classifierClass =
            (Class<? extends Classifier>)Class.forName("weka.classifiers.functions.SMO");
      Map<String, String> attributes = new HashMap<String, String>();
      attributes.put("combine_directions", "" + this.combineDirections);
      attributes.put("group_size", "" + this.groupSize);

      classifier = SerializedWekaClassifier.fetchClassifier(classifierClass, trainingSet,
                                                            true /* cache */,
                                                            attributes, "");

      if (classifier == null) {
         logger.fatal("Unable to make a classifier.");
         System.exit(1);
      }
   }

   public String classify(MagickImage image) throws Exception {
      // First, check for an empty images (space).
      if (ImageUtils.isEmptyImage(image)) {
         return " ";
      }

      try {
         Instance instance = prepUnclassed(image);
         int prediction = (int)classifier.classifyInstance(instance);
         return instance.classAttribute().value(prediction);
      } catch (Exception ex) {
         logger.error("Classification error.", ex);
         return null;
      }
   }

   private Instance prepUnclassed(MagickImage image) throws Exception {
      PDCInfo info = PDC.pdc(image);

      assert(info.numPoints() == numDCs);

      Instances instances = new Instances("Unclassified", featureAttributes, 1);
      instances.setClassIndex(0);

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

      // Note that the first spot is reserved for the class value;
      Instance instance = new Instance(featureAttributes.size());
      for (int i = 0; i < dcFeatures.length; i++) {
         instance.setValue(1 + i, dcFeatures[i]);
      }

      // Add the character densities.
      double[] characterDensities = ImageUtils.regionDensities(image,
                                                               128,
                                                               DEFUALT_REGIONS_PER_SIDE);
      for (int i = 0; i < characterDensities.length; i++) {
         instance.setValue(1 + dcFeatures.length, characterDensities[i]);
      }

      instances.add(instance);

      return instances.instance(0);
   }

   private Instances prepTraining(MagickImage[] trainingImages,
                                  String[] trainingCharacters) throws Exception {
      Instances trainingSet = new Instances("PDCInstances",
                                            featureAttributes,
                                            trainingCharacters.length);
      trainingSet.setClassIndex(0);

      for (int i = 0; i < trainingImages.length; i++) {
         Instance instance = prepUnclassed(trainingImages[i]);
         // Set the class value.
         instance.setValue((Attribute)featureAttributes.elementAt(0), trainingCharacters[i]);

         trainingSet.add(instance);
      }

      return trainingSet;
   }

   private FastVector getFeatureAttributes(FastVector possibleClasses) {
      FastVector features =
            new FastVector(1 + (numDCs / groupSize) * PDC.PDC_DIRECTION_DELTAS.length);

      int numDimensions = combineDirections ? PDC.PDC_DIRECTION_DELTAS.length / 2 :
                                              PDC.PDC_DIRECTION_DELTAS.length;

      features.addElement(new Attribute("document_class", possibleClasses));

      // Add the DCs
      for (int i = 0; i < numDCs / groupSize; i++) {
         for (int j = 0; j < numDimensions; j++) {
            features.addElement(new Attribute("GROUP_" + i + "_DC_" + j));
         }
      }

      // Add the densities.
      for (int i = 0; i < Math.pow(DEFUALT_REGIONS_PER_SIDE, 2); i++) {
         features.addElement(new Attribute("DENSITY_" + i));
      }

      return features;
   }
}
