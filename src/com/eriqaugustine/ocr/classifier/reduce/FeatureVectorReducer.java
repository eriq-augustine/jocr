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

   public abstract double[] reduceSample(double[] data);
   public abstract double[][] reduceTraining(double[][] data);
}
