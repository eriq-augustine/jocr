package com.eriqaugustine.ocr.classifier.reducer;

import com.eriqaugustine.ocr.utils.MathUtils;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Use entropy to chose the best N features.
 * Before calculating entropy, a ChangingValueReduction will be used.
 *
 * The data will get bucketed into |numBuckets| equally sized intervals.
 * The range of the discretized data is [0, |numBuckets|).
 */
public class EntropyReducer extends FeatureVectorReducer {
   private static Logger logger = LogManager.getLogger(EntropyReducer.class.getName());

   private static final int DEFAULT_FEATURE_SET_SIZE = 128;
   private static final int DEFAULT_NUM_BUCKETS = 10;

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


      // TODO
      return reducedData;
   }

   public double[][] reduceTraining(double[][] data) {
      assert(data.length > 0);
      assert(data[0].length == super.inputSize);

      int[][] reducedData = changingValueReducer.reduceTraining(discretizeTrainingData(data));

      // TEST
      System.err.println("TEST0 -- min: " + trainingRange[0] + ", max: " + trainingRange[1]);

      // TEST
      System.err.println("TEST1 -- row: " + reducedData.length + ", col: " + reducedData[0].length);

      for (int i = 0; i < reducedData.length; i++) {
         for (int j = 0; j < reducedData[i].length; j++) {
            System.err.print(reducedData[i][j] + ", ");
         }
         System.err.println();
      }

      // TEST
      System.err.println("TEST9");

      // TEST
      System.exit(0);

      // TODO
      return null;
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
}
