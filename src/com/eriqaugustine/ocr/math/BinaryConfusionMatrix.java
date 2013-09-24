package com.eriqaugustine.ocr.math;

/**
 * A confusion matrix for T/F scenarios.
 * Just adds some usefull binary methods.
 */
public class BinaryConfusionMatrix extends GeneralConfusionMatrix {
   public BinaryConfusionMatrix() {
      super(new String[]{"T", "F"});
   }

   public void truePositive() {
      add(0, 0);
   }

   public void trueNegative() {
      add(1, 1);
   }

   public void falsePositive() {
      add(0, 1);
   }

   public void falseNegative() {
      add(1, 0);
   }
}
