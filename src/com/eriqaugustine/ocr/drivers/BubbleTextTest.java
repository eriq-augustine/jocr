package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.BubbleText;
import com.eriqaugustine.ocr.image.WrapImage;
import com.eriqaugustine.ocr.utils.FileUtils;

import java.io.File;
import java.util.Arrays;

/**
 * Test breaking apart a bubble into text.
 */
public class BubbleTextTest {
   public static void main(String[] args) throws Exception {
      String outDir = FileUtils.itterationDir("out", "bubbleText");

      File base = new File("testImages/testSets/gridBreakup");
      assert(base.exists() && base.isDirectory());

      File[] imageFiles = base.listFiles();
      Arrays.sort(imageFiles);

      for (int i = 0; i < imageFiles.length; i++) {
         testFile(outDir, imageFiles[i], i);
      }
   }

   private static void testFile(String outDir, File file, int count) throws Exception {
      String path = file.getAbsolutePath();

      WrapImage image = WrapImage.getImageFromFile(path);

      BubbleText text = BubbleText.constructBubbleText(image);
      if (text == null) {
         System.err.println("Could not construct BubbleText.");
         return;
      }

      for (int i = 0; i < text.getTextSets().size(); i++) {
         BubbleText.TextSet textSet = text.getTextSets().get(i);

         WrapImage baseSet = textSet.baseImage;
         baseSet.write(String.format("%s/%02d-%02d-00-base.png", outDir, count, i));

         for (int j = 0; j < textSet.noFuriganaText.size(); j++) {
            WrapImage characterImage = textSet.noFuriganaText.get(j);
            characterImage.write(String.format("%s/%02d-%02d-01-%02d-no-furi.png",
                                               outDir, count, i, j));
         }

         for (int j = 0; j < textSet.furiganaReplacementText.size(); j++) {
            WrapImage characterImage = textSet.furiganaReplacementText.get(j);
            characterImage.write(String.format("%s/%02d-%02d-02-%02d-replacement.png",
                                               outDir, count, i, j));
         }
      }
   }
}
