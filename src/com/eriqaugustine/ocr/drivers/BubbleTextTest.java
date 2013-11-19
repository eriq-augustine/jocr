package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.image.BubbleText;

import magick.ImageInfo;
import magick.MagickImage;

/**
 * Test breaking apart a bubble into text.
 */
public class BubbleTextTest {
   public static void main(String[] args) throws Exception {
      // String outDirectory = FileUtils.itterationDir("out", "bubbleText");

      String file = "testImages/testSets/gridBreakup/PUZZLE+_021-01.png";

      ImageInfo info = new ImageInfo(file);
      MagickImage image = new MagickImage(info);

      BubbleText text = new BubbleText(image);
   }
}
