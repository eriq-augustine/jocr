package com.eriqaugustine.ocr.pdc;

import com.eriqaugustine.ocr.utils.StringUtils;

import magick.MagickImage;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A classifier specialized for PDC features.
 */
public class PDCClassifier {
   private Classifier classifier;
   // WEKA wants FastVector over List, but it will be contained to this class only.
   private FastVector possibleCharacters;

   private final int numDCs;
   private final boolean combineDirections;

   private FastVector featureAttributes;

   /**
    * |combineDirections| will merge DCs that are on the same line.
    * This will half the features.
    */
   public PDCClassifier(MagickImage[] characterImages,
                        String characters,
                        boolean combineDirections) throws Exception {
      this(PDC.pdc(characterImages), StringUtils.charSplitArray(characters), combineDirections);
   }

   public PDCClassifier(PDCInfo[] trainingDocuments,
                        String[] trainingCharacters,
                        boolean combineDirections) throws Exception {
      assert(trainingDocuments.length > 0);

      numDCs = trainingDocuments[0].numPoints();
      this.combineDirections = combineDirections;

      Set<String> seenCharacters = new HashSet<String>();
      for (String seenCharacter : trainingCharacters) {
         seenCharacters.add(seenCharacter);
      }

      possibleCharacters = new FastVector();
      for (String seenCharacter : seenCharacters) {
         possibleCharacters.addElement(seenCharacter);
      }

      featureAttributes = getFeatureAttributes(possibleCharacters);

      Instances trainingSet = prepTraining(trainingDocuments, trainingCharacters);
      classifier = new SMO();
      // TODO(eriq): This can throw.
      classifier.buildClassifier(trainingSet);
   }

   public String classify(MagickImage image) throws Exception {
      return this.classify(PDC.pdc(image));
   }

   public String classify(PDCInfo info) {
      assert(numDCs == info.numPoints());

      try {
         Instance instance = prepUnclassed(info);
         int prediction = (int)classifier.classifyInstance(instance);
         return instance.classAttribute().value(prediction);
      } catch (Exception ex) {
         System.err.println("ERROR: " + ex);
         ex.printStackTrace(System.err);
         return null;
      }
   }

   // TODO(eriq): Probably better to classify multiple documents at once.
   private Instance prepUnclassed(PDCInfo info) {
      assert(info.numPoints() == numDCs);

      Instances instances = new Instances("Unclassified", featureAttributes, 1);
      instances.setClassIndex(0);

      double[] features = null;
      if (combineDirections) {
         features = info.halfPDCDimensions();
      } else {
         features = info.fullPDCDimensions();
      }

      // Note that the first spot is reserved for the class value;
      Instance instance = new Instance(featureAttributes.size());
      for (int i = 0; i < features.length; i++) {
         instance.setValue(1 + i, features[i]);
      }

      instances.add(instance);

      return instances.instance(0);
   }

   private Instances prepTraining(PDCInfo[] trainingDocuments,
                                  String[] trainingCharacters) {
      Instances trainingSet = new Instances("PDCInstances",
                                            featureAttributes,
                                            trainingCharacters.length);
      trainingSet.setClassIndex(0);

      for (int i = 0; i < trainingDocuments.length; i++) {
         Instance instance = prepUnclassed(trainingDocuments[i]);
         // Set the class value.
         instance.setValue((Attribute)featureAttributes.elementAt(0), trainingCharacters[i]);

         trainingSet.add(instance);
      }

      return trainingSet;
   }

   private FastVector getFeatureAttributes(FastVector possibleClasses) {
      FastVector features = new FastVector(1 + numDCs * PDC.PDC_DIRECTION_DELTAS.length);

      int numDimensions = combineDirections ? PDC.PDC_DIRECTION_DELTAS.length / 2 :
                                              PDC.PDC_DIRECTION_DELTAS.length;

      features.addElement(new Attribute("document_class", possibleClasses));
      for (int i = 0; i < numDCs; i++) {
         for (int j = 0; j < numDimensions; j++) {
            features.addElement(new Attribute("POINT_" + i + "_DC_" + j));
         }
      }

      return features;
   }
}
