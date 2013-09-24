package com.eriqaugustine.ocr.math;

import java.util.Arrays;

/**
 * A general confusion matrix that should work for binary and multiclass condusion matricies.
 * Actual classes go down the vertical, predicted go across the horizontal.
 */
public class GeneralConfusionMatrix {
   private String[] labels;
   private int[][] matrix;
   private int count = 0;

   /**
    * A binary classification confuciton martix should be constructed with ["T", "F"].
    * This will yeild:
    * A  Classifier
    * c     T   F
    * t T  TP | FN
    * u    ---+---
    * a F  FP | TN
    * l
    */
   public GeneralConfusionMatrix(String[] labels) {
      this.labels = labels;

      matrix = new int[labels.length][labels.length];
      count = 0;
   }

   public void add(String predictedLabel, String actualLabel) {
      add(Arrays.binarySearch(labels, predictedLabel), Arrays.binarySearch(labels, actualLabel));
   }

   public void add(int predictedIndex, int actualIndex) {
      matrix[actualIndex][predictedIndex]++;
      count++;
   }

   /**
    * Always safe to use.
    */
   public double accuracy() {
      if (count == 0) {
         return 0;
      }

      int hits = 0;

      for (int i = 0; i < labels.length; i++) {
         hits += matrix[i][i];
      }

      return hits / (double)count;
   }

   /**
    * Currently only makes sense in the context of a 2x2 matrix.
    * However, it can be extended to get the accuracy of a specific label.
    */
   public double precision() {
      if (labels.length != 2) {
         return -1;
      }

      if (count == 0) {
         return 0;
      }

      return matrix[0][0] / (double)(matrix[0][0] + matrix[1][0]);
   }

   /**
    * Currently only makes sense in the context of a 2x2 matrix.
    * However, it can be extended to get the accuracy of a specific label.
    */
   public double recall() {
      if (labels.length != 2) {
         return -1;
      }

      if (count == 0) {
         return 0;
      }

      return matrix[0][0] / (double)(matrix[0][0] + matrix[0][1]);
   }

   public double fscore() {
      return fscore(1);
   }

   /**
    * Currently only makes sense in the context of a 2x2 matrix.
    * However, it can be extended to get the accuracy of a specific label.
    */
   public double fscore(double beta) {
      if (labels.length != 2) {
         return -1;
      }

      if (count == 0) {
         return 0;
      }

      double precision = precision();
      double recall = recall();

      return (1.0 + Math.pow(beta, 2)) *
             ((precision * recall) / ((Math.pow(beta, 2) * precision) + recall));
   }

   public String toString() {
      String rtn = "";

      for (int col = 0; col < labels.length; col++) {
         rtn += String.format(" %3s |", labels[col]);
      }
      rtn = rtn.replaceFirst("\\s*\\|\\s*$", "\n");
      rtn += ">----------<\n";

      for (int row = 0; row < labels.length; row++) {
         for (int col = 0; col < labels.length; col++) {
            rtn += String.format(" %3d |", matrix[row][col]);
         }
         rtn = rtn.replaceFirst("\\|$", "\n");

         if (row != (labels.length - 1)) {
            for (int col = 0; col < labels.length; col++) {
               rtn += "-----+";
            }
            rtn = rtn.replaceFirst("\\+$", "\n");
         }
      }

      return rtn;
   }

   public String fullToString() {
      return fullToString(1);
   }

   public String fullToString(double beta) {
      return String.format("%s" +
                           "Accuracy: %f\n" +
                           "Recall: %f\n" +
                           "Precision: %f\n" +
                           "Fscore: %f",
                           toString(),
                           accuracy(),
                           recall(),
                           precision(),
                           fscore(beta));
   }
}
