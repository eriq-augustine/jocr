package com.eriqaugustine.ocr.classifier.reducer;

import com.eriqaugustine.ocr.utils.MathUtils;

/**
 * Only keep features that show some change in the training set.
 * This will only remove features that are 100% useless, it is very conservative.
 */
public class ChangingValueReducer extends FeatureVectorReducer {
   /**
    * A boolean for every feature.
    * True if this features gets to be active.
    */
   boolean[] activeFeatures;

   public ChangingValueReducer(int inputSize) {
      super(inputSize);

      activeFeatures = new boolean[inputSize];
      for (int i = 0; i < inputSize; i++) {
         activeFeatures[i] = false;
      }
   }

   public double[] reduceSample(double[] data) {
      assert(data.length == super.inputSize);

      double[] rtn = new double[super.outputSize];

      int count = 0;
      for (int i = 0; i < data.length; i++) {
         if (activeFeatures[i]) {
            rtn[count++] = data[i];
         }
      }

      return rtn;
   }

   public double[][] reduceTraining(double[][] data) {
      assert(data.length > 0);
      assert(data[0].length == super.inputSize);

      int numKeepFeatures = 0;

      // Check each column for changing values.
      for (int col = 0; col < data[0].length; col++) {
         double initialValue = data[0][col];
         int row;

         for (row = 1; row < data.length; row++) {
            if (!MathUtils.doubleEquals(initialValue, data[row][col])) {
               numKeepFeatures++;
               activeFeatures[col] = true;
               break;
            }
         }
      }

      // Set the output size.
      super.outputSize = numKeepFeatures;

      // TEST
      System.err.println("TEST0: " + numKeepFeatures);

      // Reduce each training vector.
      double[][] rtn = new double[data.length][];
      for (int i = 0; i < data.length; i++) {
         rtn[i] = reduceSample(data[i]);
      }

      return rtn;
   }
}
