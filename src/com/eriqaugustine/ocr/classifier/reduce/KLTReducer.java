package com.eriqaugustine.ocr.classifier.reducer;

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

   private static final int DEFAULT_FEATURE_SET_SIZE = 128;

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
      RealMatrix centeredData = dataMatrix.subtract(getMeanMatrix(dataMatrix));

      Covariance covariance = new Covariance(reducedData);
      EigenDecomposition eigenDecomp = new EigenDecomposition(covariance.getCovarianceMatrix());

      // TEST
      System.err.println("TEST00");

      /*
      double[] eigenValues = eigenDecomp.getRealEigenvalues();
      for (int i = 0; i < eigenValues.length; i++) {
         System.err.print(eigenValues[i] + ", ");
      }
      System.err.println();
      */

      System.err.println("TEST01");

      /*
      //TEST
      Jama.EigenvalueDecomposition jmaDecomp = new Jama.EigenvalueDecomposition(new Jama.Matrix(covariance.getCovarianceMatrix().getData()));
      double[] jmaEigen = jmaDecomp.getRealEigenvalues();
      for(int i = 0; i < jmaEigen.length; i++){
         System.err.print(jmaEigen[i] + ", ");
      }
      System.err.println();
      */

      //TEST
      /*
      RealMatrix covMatrix = covariance.getCovarianceMatrix();
      for (int i = 0; i < covMatrix.getColumnDimension(); i++) {
         EigenDecomposition eigenDecomp2 = new EigenDecomposition(covMatrix.getColumnMatrix(i));
         double[] eigen = eigenDecomp2.getRealEigenvalues();
         System.err.println(eigen.length + ": " + eigen[0]);
      }
      */

      System.err.println("TEST02");

      // Get the eigen vectors.
      // Transpose them (each eigen vector is now in a row).
      // Take the top |reducedFeatureVectorLength| vectors.
      // TODO(eriq): Verify |reducedFeatureVectorLength| > |num reduced features|.
      transformationMatrix = eigenDecomp.getV().transpose();

      // Get only the top |super.outputSize| eigen vectors.
      transformationMatrix = transformationMatrix.getSubMatrix(0, super.outputSize - 1, 0, reducedData[0].length - 1);

      //TEST
      System.err.println("TEST03");
      System.err.println("Row: " + transformationMatrix.getRowDimension() + ", Col: " + transformationMatrix.getColumnDimension());
      System.err.println("Row: " + centeredData.getRowDimension() + ", Col: " + centeredData.getColumnDimension());

      RealMatrix finalData = transformationMatrix.multiply(centeredData.transpose()).transpose();

      // TEST
      System.err.println("TEST09");

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
