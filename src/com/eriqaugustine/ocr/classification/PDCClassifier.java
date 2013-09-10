package com.eriqaugustine.ocr.classification;

import com.eriqaugustine.ocr.image.PDC;
import com.eriqaugustine.ocr.utils.StringUtils;

import magick.MagickImage;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;
import weka.core.Instance;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A classifier specialized for PDC features.
 */
public class PDCClassifier {
   private Classifier classifier;
   // WEKA wants FastVector over List, but it will be contained to this class.
   private FastVector possibleCharacters;

   private final int numFeatures;

   private FastVector featureAttributes;

   public PDCClassifier(MagickImage[] characterImages,
                        String characters) throws Exception {
      this(PDC.pdc(characterImages), StringUtils.charSplit(characters));
   }

   public PDCClassifier(List<PDCFeature[]> trainingDocuments,
                        List<String> trainingCharacters) throws Exception {
      assert(trainingDocuments.size() > 0);

      numFeatures = trainingDocuments.get(0).length;

      featureAttributes = getFeatureAttributes();

      Set<String> seenCharacters = new HashSet<String>();
      for (String seenCharacter : trainingCharacters) {
         seenCharacters.add(seenCharacter);
      }

      possibleCharacters = new FastVector();
      for (String seenCharacter : seenCharacters) {
         possibleCharacters.addElement(seenCharacter);
      }

      Instances trainingSet = prepTraining(trainingDocuments, trainingCharacters);
      classifier = new SMO();
      // TODO(eriq): This can throw.
      classifier.buildClassifier(trainingSet);
   }

   public String classify(PDCFeature[] features) {
      try {
         Instance instance = prepUnclassed(features);
         int prediction = (int)classifier.classifyInstance(instance);
         return instance.classAttribute().value(prediction);
      } catch (Exception ex) {
         return null;
      }
   }

   private Instance prepUnclassed(PDCFeature[] features) {
      assert(features.length == numFeatures);

      // Note that the first spot is reserved for the class value;
      Instance instance = new Instance(featureAttributes.size());
      for (int featureIndex = 0; featureIndex < features.length; featureIndex++) {
         PDCFeature feature = features[featureIndex];

         for (int i = 0; i < feature.length(); i++) {
            instance.setValue(1 + (featureIndex * feature.length() + i), feature.getValue(i));
         }
      }

      return instance;
   }

   private Instances prepTraining(List<PDCFeature[]> trainingDocuments,
                                  List<String> trainingCharacters) {
      Instances trainingSet = new Instances("PDCInstances",
                                            featureAttributes,
                                            trainingCharacters.size());
      trainingSet.setClassIndex(0);

      for (int i = 0; i < trainingDocuments.size(); i++) {
         Instance instance = prepUnclassed(trainingDocuments.get(i));
         // Set the class value.
         //TEST
         // instance.setValue(0, trainingCharacters.get(i));
         instance.setValue((Attribute)featureAttributes.elementAt(0), trainingCharacters.get(i));

         trainingSet.add(instance);
      }

      return trainingSet;
   }

   private FastVector getFeatureAttributes() {
      FastVector features = new FastVector(1 + numFeatures);

      features.addElement(new Attribute("document_class", possibleCharacters));
      for (int i = 0; i < numFeatures; i++) {
         for (int j = 0; j < PDC.PDC_DIRECTION_DELTAS.length; j++) {
            features.addElement(new Attribute("POINT_" + i + "_DC_" + j));
         }
      }

      return features;
   }
}
