package com.eriqaugustine.ocr.classifier;

// TEST
import com.eriqaugustine.ocr.classifier.reducer.NoReducer;
import com.eriqaugustine.ocr.classifier.reducer.ChangingValueReducer;
import com.eriqaugustine.ocr.classifier.reducer.FeatureVectorReducer;
import com.eriqaugustine.ocr.classifier.reducer.KLTReducer;
import com.eriqaugustine.ocr.classifier.reducer.EntropyReducer;

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
 * A classifier for numeric vectors.
 * |ToClassify| is the object that needs to be classified.
 * Children will need to provide the features for an instance of |ToClassify|.
 */
public abstract class VectorClassifier<ToClassify> {
   private static Logger logger = LogManager.getLogger(VectorClassifier.class.getName());

   private Classifier classifier;

   private FeatureVectorReducer reducer;

   /**
    * All the possible classes except the default class.
    * WEKA wants FastVector over List, but it will be contained to this class only.
    */
   private FastVector classes;

   // The names ("Attribute") for each feature.
   // The list (FastVector) of classes is always the first element.
   private FastVector featureAttributes;

   private final String defaultClass;

   protected final int featureVectorLength;

   // TODO(eriq): We no longer need to pass in featureVectorLength since the reducer can tell us.
   protected VectorClassifier(int featureVectorLength,
                              String defaultClass) {
      // TEST
      // this(featureVectorLength, defaultClass, new NoReducer(featureVectorLength));
      this(featureVectorLength, defaultClass, new EntropyReducer(featureVectorLength));
      // this(featureVectorLength, defaultClass, new ChangingValueReducer(featureVectorLength));
      // this(featureVectorLength, defaultClass, new KLTReducer(featureVectorLength));
   }

   /**
    * |featureVectorLength| is the length of the vector that will be obtained from getFeatureValues().
    */
   protected VectorClassifier(int featureVectorLength,
                              String defaultClass,
                              FeatureVectorReducer reducer) {
      this.defaultClass = defaultClass;
      this.featureVectorLength = featureVectorLength;
      this.reducer = reducer;

      this.classifier = null;
      this.classes = null;
      this.featureAttributes = null;
   }

   protected boolean train(List<ToClassify> trainingContents,
                           List<String> trainingClasses,
                           Map<String, String> classifierAttributes) {
      return train(trainingContents,
                   trainingClasses,
                   "weka.classifiers.functions.SMO" /* classifier */,
                   "" /* notes */,
                   classifierAttributes,
                   true /* use cache */);
   }

   /**
    * Train the classifier.
    * This needs to be done before any classification attempt.
    */
   // Suppress the classifier Class cast.
   @SuppressWarnings("unchecked")
   protected boolean train(List<ToClassify> trainingContents,
                           List<String> trainingClasses,
                           String wekaClassifier,
                           String classifierNotes,
                           Map<String, String> classifierAttributes,
                           boolean useCache) {
      assert(trainingContents.size() > 0);
      assert(trainingContents.size() == trainingClasses.size());

      Set<String> seenClasses = new HashSet<String>();
      for (String seenClass : trainingClasses) {
         seenClasses.add(seenClass);
      }

      classes = new FastVector();
      for (String seenClass : seenClasses) {
         classes.addElement(seenClass);
      }

      // |featureAttributes| will be initialized AFTER training reduction.

      Instances trainingSet = prepTraining(trainingContents, trainingClasses);

      try {
         Class<? extends Classifier> classifierClass =
               (Class<? extends Classifier>)Class.forName(wekaClassifier);

         classifier = SerializedWekaClassifier.fetchClassifier(classifierClass,
                                                               trainingSet,
                                                               useCache,
                                                               classifierAttributes,
                                                               classifierNotes);
      } catch (Exception ex) {
         logger.error("Unable to make a classifier.", ex);
         return false;
      }

      if (classifier == null) {
         logger.error("Unable to make a classifier.");
         return false;
      }

      return true;
   }

   /**
    * Check to see if the object is empty.
    * If an object is empty, then a default value will be returned.
    */
   protected abstract boolean isEmpty(ToClassify objToClassify);

   /**
    * Get the actual values of the features for an object.
    */
   protected abstract double[] getFeatureValues(ToClassify objToClassify);

   public String classify(ToClassify objToClassify) {
      if (classifier == null) {
         logger.error("Attempting to use an untrained classfiier.");
         throw new RuntimeException("Attempting to use an untrained classfiier.");
      }

      // First, check for an empty object.
      if (isEmpty(objToClassify)) {
         return defaultClass;
      }

      try {
         Instance instance = prepUnclassed(objToClassify);
         int prediction = (int)classifier.classifyInstance(instance);
         return instance.classAttribute().value(prediction);
      } catch (Exception ex) {
         logger.error("Classification error.", ex);
         return null;
      }
   }

   private Instance prepUnclassed(ToClassify objToClassify) {
      return prepUnclassed(reducer.reduceSample(getFeatureValues(objToClassify)));
   }

   private Instance prepUnclassed(double[] featureValues) {
      assert(featureValues.length == reducer.getOutputSize());

      Instances instances = new Instances("Unclassified", featureAttributes, 1);
      instances.setClassIndex(0);

      // Note that the first spot is reserved for the class value.
      // Set the values for the feature instances.
      Instance instance = new Instance(featureAttributes.size());
      for (int i = 0; i < featureValues.length; i++) {
         instance.setValue(1 + i, featureValues[i]);
      }

      instances.add(instance);

      return instances.instance(0);
   }

   private Instances prepTraining(List<ToClassify> trainingContents,
                                  List<String> trainingClasses) {
      // Collect all the features in one place so we can possibly reduce them.
      // The first thing we need to do is reduce the training set so the reducer has full information.
      double[][] trainingFeatures = reducer.reduceTraining(getAllTrainingFeatures(trainingContents));

      // Get the featureAttributes AFTER reduction because we will not know how many feature we will have.
      featureAttributes = getFeatureAttributes();

      Instances trainingSet = new Instances("VectorInstances",
                                            featureAttributes,
                                            trainingFeatures.length);
      trainingSet.setClassIndex(0);

      for (int i = 0; i < trainingFeatures.length; i++) {
         Instance instance = prepUnclassed(trainingFeatures[i]);
         // Set the class value.
         instance.setValue((Attribute)featureAttributes.elementAt(0), trainingClasses.get(i));

         trainingSet.add(instance);
      }

      return trainingSet;
   }

   /**
    * Get all the features for a training set.
    * This is its own method so that subclasses have a chance to override.
    */
   protected double[][] getAllTrainingFeatures(List<ToClassify> trainingContents) {
      double[][] rtn = new double[trainingContents.size()][];

      for (int i = 0; i < trainingContents.size(); i++) {
         rtn[i] = getFeatureValues(trainingContents.get(i));
      }

      return rtn;
   }

   /**
    * Override to give custom names to your features.
    * Note that ordinal will start at 1 because the first spot is always the class.
    */
   protected String getFeatureAttributeName(int ordinal) {
      return "VECTOR_ITEM_" + ordinal;
   }

   /**
    * Get the names for all the attributes.
    * Use getFeatureAttributeName() to give custom names for attributes.
    */
   private FastVector getFeatureAttributes() {
      FastVector features = new FastVector(1 + reducer.getOutputSize());

      features.addElement(new Attribute("document_class", classes));

      for (int i = 0; i < reducer.getOutputSize(); i++) {
         features.addElement(new Attribute(getFeatureAttributeName(i)));
      }

      return features;
   }
}
