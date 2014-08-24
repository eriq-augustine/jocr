package com.eriqaugustine.ocr.classifier.reduce;

/**
 * Do no reduction!.
 */
public class NoReducer extends FeatureVectorReducer {
   public NoReducer(int inputSize) {
      super(inputSize, inputSize);
   }

   public double[] reduceSample(double[] data) {
      assert(data.length == super.inputSize);

      return data;
   }

   public double[][] reduceTraining(double[][] data, String[] classLabels) {
      assert(data.length > 0);
      assert(data[0].length == super.inputSize);

      return data;
   }
}
