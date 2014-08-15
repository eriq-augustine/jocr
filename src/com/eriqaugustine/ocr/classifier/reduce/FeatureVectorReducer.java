package com.eriqaugustine.ocr.classifier.reducer;

/**
 * Reduce a feature set down to the important features.
 * Consider the reduceTraining() method an initializer/
 * It muct be called once before any call to reduceSample().
 *
 * getOutputSize() is not reliable until reduceTraining() is called.
 * (The reducer may not know how much is can reduce until it sees the training set).
 */
public abstract class FeatureVectorReducer {
   protected final int inputSize;
   protected int outputSize;

   public FeatureVectorReducer(int inputSize) {
      this(inputSize, -1);
   }

   public FeatureVectorReducer(int inputSize, int outputSize) {
      this.inputSize = inputSize;
      this.outputSize = outputSize;
   }

   public int getInputSize() {
      return inputSize;
   }

   /**
    * This is not reliable until after reduceTraining() is called.
    */
   public int getOutputSize() {
      // Assert that the reducer has been initialized.
      assert(outputSize != -1);

      return outputSize;
   }

   /**
    * These integer versions are not reccomended because of precision problems.
    */
   public int[] reduceSample(int[] data) {
      double[] doubleData = new double[data.length];
      for (int i = 0; i < data.length; i++) {
         doubleData[i] = (double)data[i];
      }

      double[] resultData = reduceSample(doubleData);
      int[] rtn = new int[resultData.length];

      for (int i = 0; i < resultData.length; i++) {
         rtn[i] = (int)resultData[i];
      }

      return rtn;
   }

   public int[][] reduceTraining(int[][] data, String[] classLabels) {
      double[][] doubleData = new double[data.length][];
      for (int i = 0; i < data.length; i++) {
         doubleData[i] = new double[data[i].length];
         for (int j = 0; j < data[i].length; j++) {
            doubleData[i][j] = (double)data[i][j];
         }
      }

      double[][] resultData = reduceTraining(doubleData, classLabels);
      int[][] rtn = new int[resultData.length][];

      for (int i = 0; i < resultData.length; i++) {
         rtn[i] = new int[resultData[i].length];
         for (int j = 0; j < resultData[i].length; j++) {
            rtn[i][j] = (int)resultData[i][j];
         }
      }

      return rtn;
   }

   public abstract double[] reduceSample(double[] data);
   public abstract double[][] reduceTraining(double[][] data, String[] classLabels);
}
