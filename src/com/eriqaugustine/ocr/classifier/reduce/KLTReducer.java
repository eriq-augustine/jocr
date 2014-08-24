package com.eriqaugustine.ocr.classifier.reduce;

import com.eriqaugustine.ocr.utils.MathUtils;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Do a KLT (PCA (Principle Components Analysis)) reduction.
 * Before doing a KLT reduction, a ChangingValueReduction will be used.
 * Finding eigen vectors is not always possible,
 * especially if there are many more feature than observations (as with our ocr situation).
 * So, doing a ChangingValueReduction will help to reduction the input features into the eigen decomposition.
 */
public class KLTReducer extends FeatureVectorReducer {
   private static Logger logger = LogManager.getLogger(KLTReducer.class.getName());

   // TEST
   // private static final int DEFAULT_FEATURE_SET_SIZE = 128;
   // private static final int DEFAULT_FEATURE_SET_SIZE = 1024;
   private static final int DEFAULT_FEATURE_SET_SIZE = 1000;

   private FeatureVectorReducer changingValueReducer;

   /**
    * A matrix that will transform a feature vector (each row is a document, and each column is a feature)
    * into the reduced feature set.
    * Mutiply this by the transpose of the data minus the mean to get the transpose of the final data.
    */
   private RealMatrix transformationMatrix;

   public KLTReducer(int inputSize) {
      this(inputSize, DEFAULT_FEATURE_SET_SIZE);
   }

   // TODO(eriq): Deal with the situtation where the CVR says that there are less values than |outputSize|.
   public KLTReducer(int inputSize, int outputSize) {
      super(inputSize, outputSize);

      changingValueReducer = new ChangingValueReducer(inputSize);
   }

   public double[] reduceSample(double[] data) {
      assert(data.length == super.inputSize);

      double[] reducedData = changingValueReducer.reduceSample(data);

      RealMatrix dataMatrix = new Array2DRowRealMatrix(1, reducedData.length);
      dataMatrix.setRow(0, reducedData);

      RealMatrix finalData = transformationMatrix.multiply(dataMatrix.transpose()).transpose();

      return finalData.getRow(0);
   }

   public double[][] reduceTraining(double[][] data, String[] classLabels) {
      assert(data.length > 0);
      assert(data[0].length == super.inputSize);

      double[][] reducedData = changingValueReducer.reduceTraining(data, classLabels);

      RealMatrix dataMatrix = new Array2DRowRealMatrix(reducedData);

      // NOTE(eriq): The math says that we should first center our data.
      //  But, centering the data (subtracting the mean) gives up much worse results...
      // RealMatrix centeredData = dataMatrix.subtract(getMeanMatrix(dataMatrix));
      RealMatrix centeredData = dataMatrix;

      Covariance covariance = new Covariance(reducedData);
      EigenDecomposition eigenDecomp = new EigenDecomposition(covariance.getCovarianceMatrix());

      // Get the eigen vectors.
      // Transpose them (each eigen vector is now in a row).
      // Take the top |reducedFeatureVectorLength| vectors.
      // TODO(eriq): Verify |reducedFeatureVectorLength| > |num reduced features|.
      // NOTE(eriq): Are the eigen vectors along the vertical or horizontal.
      // transformationMatrix = eigenDecomp.getV().transpose();
      transformationMatrix = eigenDecomp.getV();

      // Get only the top |super.outputSize| eigen vectors.
      transformationMatrix = transformationMatrix.getSubMatrix(0, super.outputSize - 1, 0, reducedData[0].length - 1);

      RealMatrix finalData = transformationMatrix.multiply(centeredData.transpose()).transpose();

      return finalData.getData();
   }

   private RealMatrix getMeanMatrix(RealMatrix matrix) {
      RealMatrix rtn = new Array2DRowRealMatrix(matrix.getRowDimension(), matrix.getColumnDimension());

      for (int col = 0; col < matrix.getColumnDimension(); col++) {
         double mean = MathUtils.mean(matrix.getColumn(col));

         for (int row = 0; row < matrix.getRowDimension(); row++) {
            rtn.setEntry(row, col, mean);
         }
      }

      return rtn;
   }
}
