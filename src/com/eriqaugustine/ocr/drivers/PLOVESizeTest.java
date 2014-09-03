package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.classifier.OCRClassifier;
import com.eriqaugustine.ocr.classifier.PLOVEClassifier;
import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.classifier.reduce.NoReducer;
import com.eriqaugustine.ocr.classifier.reduce.KLTReducer;

import com.eriqaugustine.ocr.plove.PLOVE;

import com.eriqaugustine.ocr.utils.Props;

/**
 * Quck test for PLOVEClassifier.
 */
public class PLOVESizeTest extends KanjiClassifierTest {
   public static void main(String[] args) throws Exception {
      PLOVESizeTest test = new PLOVESizeTest();

      System.out.println("ScaleSize,ReduceSize,TotalTimeMS,Hits");

      for (int size = 16; size <= 64; size += 4) {
         try {
            PLOVE.SCALE_SIZE = size;
            System.err.println("--- " + size + " ---");
            test.run();
         } catch (Exception ex) {
            System.err.println(ex);
         }
         System.err.println("---------");
      }
   }

   private void run() throws Exception {
      OCRClassifier classy = null;
      int res;

      for (int reduceSize = 300; reduceSize <= 1400; reduceSize += 100) {
         for (int scaleSize = 16; scaleSize <= 64; scaleSize += 4) {
            PLOVE.SCALE_SIZE = scaleSize;

            long startTime = System.currentTimeMillis();

            System.err.println("--- (" + scaleSize + ", " + reduceSize +") ---");

            try {
               FeatureVectorReducer reduce = new KLTReducer(PLOVE.getNumberOfFeatures(), reduceSize);
               classy =
                  new PLOVEClassifier(trainingCharacters,
                                      Props.getList("CLASSIFIER_TRAINING_FONTS").toArray(new String[0]),
                                      reduce);

               res = classifierTest(classy, false);
            } catch (Exception ex) {
               System.err.println("(" + scaleSize + ", " + reduceSize + ") - " + ex);
               res = -1;
            }

            System.out.println(String.format(
               "%d,%d,%d,%d",
               PLOVE.SCALE_SIZE,
               reduceSize,
               System.currentTimeMillis() - startTime,
               res));

            System.err.println("---------------");
            System.gc();
         }
      }
   }
}
