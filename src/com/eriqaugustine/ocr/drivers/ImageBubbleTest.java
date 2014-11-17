package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.BubbleDetection;
import com.eriqaugustine.ocr.image.BubbleDetector;
import com.eriqaugustine.ocr.image.BubbleText;
import com.eriqaugustine.ocr.image.ImageTranslator;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.FileUtils;
import com.eriqaugustine.ocr.utils.SystemUtils;

import com.eriqaugustine.ocr.classifier.OCRClassifier;
import com.eriqaugustine.ocr.classifier.PLOVEClassifier;
import com.eriqaugustine.ocr.classifier.reduce.FeatureVectorReducer;
import com.eriqaugustine.ocr.classifier.reduce.KLTReducer;

import com.eriqaugustine.ocr.plove.PLOVE;

import com.eriqaugustine.ocr.utils.Props;

/**
 * Translate a single image (page).
 */
public class ImageBubbleTest {
   public static void main(String[] args) throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "imageBubble");

      String image = "testImages/testSets/youbatoVol1_kana/Yotsubato_v01_022.jpg";

      imageBubbleTest(image, outDirectory);
   }

   public static void imageBubbleTest(String imagePath, String outDirectory) throws Exception {
      WrapImage baseImage = WrapImage.getImageFromFile(imagePath);
      baseImage.write(outDirectory + "/0-base.png");

      BubbleDetector bubbleDetector = new BubbleDetection();
      WrapImage fillBubbles = bubbleDetector.colorBubbles(baseImage);
      fillBubbles.write(outDirectory + "/1-base.png");

      WrapImage[] bubbles = bubbleDetector.extractBubbles(baseImage);
      for (int i = 0; i < bubbles.length; i++) {
         bubbles[i].write(outDirectory + "/2-" + i + "-base.png");

         BubbleText text = BubbleText.constructBubbleText(bubbles[i]);
         if (text == null) {
            System.err.println("Could not construct BubbleText.");
            return;
         }

         for (int j = 0; j < text.getTextSets().size(); j++) {
            BubbleText.TextSet textSet = text.getTextSets().get(j);

            WrapImage baseSet = textSet.baseImage;
            baseSet.write(outDirectory + "/3-" + i + "-" + j + "-base.png");

            for (int k = 0; k < textSet.noFuriganaText.size(); k++) {
               WrapImage characterImage = textSet.noFuriganaText.get(k);
               characterImage.write(outDirectory + "/4-" + i + "-" + j + "-" + k + "-no-furi.png");
            }

            for (int k = 0; k < textSet.furiganaReplacementText.size(); k++) {
               WrapImage characterImage = textSet.furiganaReplacementText.get(k);
               characterImage.write(outDirectory + "/4-" + i + "-" + j + "-" + k + "-replacement.png");
            }
         }
      }
   }
}
