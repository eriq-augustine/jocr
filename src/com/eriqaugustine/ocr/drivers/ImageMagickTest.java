package com.eriqaugustine.ocr.drivers;

import magick.ImageInfo;
import magick.MagickImage;

/**
 * Just do the most base image magick functionality.
 * Read in and then write an image.
 * Later, more image magick functional tests can be added.
 */
public class ImageMagickTest {
   public static void main(String[] args) throws Exception {
      imageMagickBaseTest();
   }

   /**
    * Read a file in the testdir and write it out to the current dir.
    */
   public static void imageMagickBaseTest() throws Exception {
      ImageInfo info = new ImageInfo("testImages/katakana.png");
      MagickImage baseImage = new MagickImage(info);
      baseImage.setFileName("testImage.png");
      baseImage.writeImage(info);
   }
}
