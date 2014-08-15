package com.eriqaugustine.ocr.classifier.reducer;

import com.eriqaugustine.ocr.utils.MathUtils;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Use entropy to chose the best N features.
 * Before calculating entropy, a ChangingValueReduction will be used.
 *
 * The data will get bucketed into |numBuckets| equally sized intervals.
 * The range of the discretized data is [0, |numBuckets|).
 */
public class EntropyReducer extends FeatureVectorReducer {
   private static Logger logger = LogManager.getLogger(EntropyReducer.class.getName());

   // TEST
   // private static final int DEFAULT_FEATURE_SET_SIZE = 128;
   private static final int DEFAULT_FEATURE_SET_SIZE = 2660;
   private static final int DEFAULT_NUM_BUCKETS = 10;

   /**
    * The attributes that will be kept.
    * NOTE: This array is in parallel with the REDUCED ATTRRIBUTES returned from |changingValueReducer|.
    * NOT the original data.
    */
   private boolean[] activeFeatures;

   private double[] trainingRange;

   private int numBuckets;

   private FeatureVectorReducer changingValueReducer;

   public EntropyReducer(int inputSize) {
      this(inputSize, DEFAULT_FEATURE_SET_SIZE, DEFAULT_NUM_BUCKETS);
   }

   // TODO(eriq): Deal with the situtation where the CVR says that there are less values than |outputSize|.
   public EntropyReducer(int inputSize, int outputSize, int numBuckets) {
      super(inputSize, outputSize);

      this.numBuckets = numBuckets;
      changingValueReducer = new ChangingValueReducer(inputSize);
      trainingRange = null;
   }

   public double[] reduceSample(double[] data) {
      assert(data.length == super.inputSize);

      double[] reducedData = changingValueReducer.reduceSample(data);
      double[] rtn = new double[super.outputSize];

      int outputIndex = 0;
      for (int i = 0; i < activeFeatures.length; i++) {
         if (activeFeatures[i]) {
            rtn[outputIndex++] = reducedData[i];
         }
      }

      return rtn;
   }

   // In our discretized data, we will continually use -1 to represent an absent value.
   // The valid range of the discretized data is [0, |numBuckets|).
   // For the class label arrays, we will use null.
   public double[][] reduceTraining(double[][] data, String[] trainingClasses) {
      assert(data.length > 0);
      assert(data[0].length == super.inputSize);
      assert(data.length == trainingClasses.length);

      int[][] discretizedData = changingValueReducer.reduceTraining(discretizeTrainingData(data), trainingClasses);

      // Now that we know the size of the reduced features, initialize them.
      activeFeatures = new boolean[discretizedData.length];
      for (int i = 0; i < discretizedData.length; i++) {
         activeFeatures[i] = false;
      }

      // TODO(eriq): Properly deal with this.
      assert(discretizedData[0].length >= super.outputSize);

      for (int attributeCount = 0; attributeCount < super.outputSize; attributeCount++) {
         // Check all the still active attributes and find the one with the highest information gain ratio.
         double maxGainRatio = Double.NaN;
         int maxIndex = -1;

         double gainRatio;
         double currentDatasetEntropy = datasetEntropy(trainingClasses);

         for (int i = 0; i < discretizedData[0].length; i++) {
            gainRatio = informationGainRatio(discretizedData, trainingClasses, i, currentDatasetEntropy);

            if (maxIndex == -1 || gainRatio > maxGainRatio) {
               maxIndex = i;
               maxGainRatio = gainRatio;
            }
         }

         // Active the chosen feature.
         activeFeatures[maxIndex] = true;

         // Remove the chosen attribute from the dataset.
         for (int i = 0; i < discretizedData.length; i++) {
            discretizedData[i][maxIndex] = -1;
         }
      }

      // Make sure to use the original data, and not the discretized data.
      double[][] rtn = new double[data.length][];
      for (int i = 0; i < rtn.length; i++) {
         rtn[i] = reduceSample(data[i]);
      }

      return rtn;
   }

   /**
    * In addition to returning discretized data, this will populate |trainingRange|.
    */
   private int[][] discretizeTrainingData(double[][] data) {
      trainingRange = new double[2];

      double[] rowRange;
      for (int i = 0; i < data.length; i++) {
         rowRange = MathUtils.range(data[i]);

         if (rowRange[0] <= trainingRange[0]) {
            trainingRange[0] = rowRange[0];
         }

         if (rowRange[1] >= trainingRange[1]) {
            trainingRange[1] = rowRange[1];
         }
      }

      int[][] rtn = new int[data.length][];
      for (int i = 0; i < data.length; i++) {
         rtn[i] = discretizeData(data[i]);
      }

      return rtn;
   }

   private int[] discretizeData(double[] data) {
      int[] rtn = new int[data.length];

      for (int i = 0; i < data.length; i++) {
         if (data[i] < trainingRange[0]) {
            data[i] = 0;
         // The subtraction is just to protect against an unlikely divide-by-zero.
         } else if (data[i] >= (trainingRange[1] - 0.000001)) {
            data[i] = numBuckets - 1;
         } else {
           rtn[i] = (int)(((data[i] - trainingRange[0]) / (trainingRange[1] - trainingRange[0])) * numBuckets);
         }
      }

      return rtn;
   }

   private double informationGain(int[][] data, String[] classLabels, int attributeIndex) {
      return informationGain(data, classLabels, attributeIndex, datasetEntropy(classLabels));
   }

   private double informationGain(int[][] data, String[] classLabels,
                                  int attributeIndex, double dataEntropy) {
      return dataEntropy - attributeEntropy(data, classLabels, attributeIndex);
   }

   private double informationGainRatio(int[][] data, String[] classLabels,
                                       int attributeIndex) {
      return informationGainRatio(data, classLabels, attributeIndex, datasetEntropy(classLabels));
   }

   private double informationGainRatio(int[][] data, String[] classLabels,
                                       int attributeIndex, double dataEntropy) {
      int datasetSize = 0;
      double splitEntropy = 0;

      // For every possible value of this attribute.
      for (int attributeValue = 0; attributeValue < numBuckets; attributeValue++) {
         datasetSize = 0;

         // Count all documents whose value for this attribute is |attributeValue|.
         for (int i = 0; i < classLabels.length; i++) {
            if (data[i][attributeIndex] != -1 && data[i][attributeIndex] == attributeValue) {
               datasetSize++;
            }
         }

         if (datasetSize > 0) {
            splitEntropy += ((datasetSize / data.length) * MathUtils.log2(datasetSize / data.length));
         }
      }

      splitEntropy *= -1;

      return informationGain(data, classLabels, attributeIndex, dataEntropy) / splitEntropy;
   }

   // Get the entropy of the dataset after splitting on |attributeIndex|.
   // Remember: Values in |data| with -1 and classLabels with null are not valid.
   //  They indicate that that attribute or document respectivley have been removed.
   private double attributeEntropy(int[][] data, String[] classLabels, int attributeIndex) {
      double entropy = 0;

      String[] labelsForAttribute = new String[classLabels.length];
      int datasetSize = 0;

      // For every possible value of this attribute.
      for (int attributeValue = 0; attributeValue < numBuckets; attributeValue++) {
         datasetSize = 0;

         // Collect all documents whose value for this attribute is |attributeValue|.
         for (int i = 0; i < classLabels.length; i++) {
            if (data[i][attributeIndex] != -1 && data[i][attributeIndex] == attributeValue) {
               labelsForAttribute[i] = classLabels[i];
               datasetSize++;
            } else {
               labelsForAttribute[i] = null;
            }
         }

         entropy += ((datasetSize / (double)classLabels.length) * datasetEntropy(labelsForAttribute));
      }

      return entropy;
   }

   private double datasetEntropy(String[] classLabels) {
      Map<String, Integer> classDistribution = getClassDistribution(classLabels);

      int datasetSize = 0;
      for (int i = 0; i < classLabels.length; i++) {
         if (classLabels[i] != null) {
            datasetSize++;
         }
      }

      return datasetEntropy(classDistribution, datasetSize);
   }

   private double datasetEntropy(Map<String, Integer> classDistribution, int datasetSize) {
      double entropy = 0;
      for (Integer classCount : classDistribution.values()) {
         double classProbability = (double)classCount.intValue() / datasetSize;
         entropy += (classProbability * Math.log(classProbability));
      }

      return entropy * -1.0;
   }

   // A null class label represents absent data.
   // This can happen when splitting up a dataset.
   private Map<String, Integer> getClassDistribution(String[] classLabels) {
      Map<String, Integer> classDistribution = new HashMap<String, Integer>();

      for (String classLabel : classLabels) {
         if (classLabel == null) {
            continue;
         }

         if (!classDistribution.containsKey(classLabel)) {
            classDistribution.put(classLabel, new Integer(1));
         } else {
            classDistribution.put(classLabel, new Integer(classDistribution.get(classLabel).intValue() + 1));
         }
      }

      return classDistribution;
   }
}
