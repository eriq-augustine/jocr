package com.eriqaugustine.ocr.image;

import java.awt.Dimension;

import magick.MagickImage;

public class Filters {
   public static final int DEFAULT_BW_THRESHOLD = 150;

   public static byte[] bwPixels(MagickImage image, int threshold) throws Exception {
      Dimension dimensions = image.getDimension();
      byte[] pixels = new byte[dimensions.width * dimensions.height * 3];

      image.dispatchImage(0, 0,
                          dimensions.width, dimensions.height,
                          "RGB",
                          pixels);

      for (int row = 0; row < dimensions.height; row++) {
         for (int col = 0; col < dimensions.width; col++) {
            int index = (row * dimensions.width + col) * 3;

            // The unsigned bytes need to be converted to ints.
            int value = ((0xFF & pixels[index + 0]) +
                         (0xFF & pixels[index + 1]) +
                         (0xFF & pixels[index + 2])) / 3;

            byte fill = 0;
            if (value > threshold) {
               fill = (byte)0xFF;
            }

            pixels[index + 0] = fill;
            pixels[index + 1] = fill;
            pixels[index + 2] = fill;
         }
      }

      return pixels;
   }

   public static byte[] bwPixels(MagickImage image) throws Exception {
      return bwPixels(image, DEFAULT_BW_THRESHOLD);
   }

   /**
    * Take an image and make it only black and white.
    * This filter doesn't mess around, no greys.
    * Only true black and true white.
    */
   public static MagickImage bw(MagickImage image, int threshold) throws Exception {
      Dimension dimensions = image.getDimension();
      byte[] pixels = bwPixels(image, threshold);

      MagickImage newImage = new MagickImage();
      newImage.constituteImage(dimensions.width, dimensions.height,
                               "RGB",
                               pixels);

      return newImage;
   }

   public static MagickImage bw(MagickImage image) throws Exception {
      return bw(image, DEFAULT_BW_THRESHOLD);
   }

   public static byte[] averageChannels(byte[] multiChannels, int numChannels) {
      assert(multiChannels.length % numChannels == 0);

      byte[] singleChannel = new byte[multiChannels.length / numChannels];

      for (int i = 0; i < singleChannel.length; i++) {
         int baseIndex = i * numChannels;
         singleChannel[i] = (byte)(((0xFF & multiChannels[baseIndex + 0]) +
                                    (0xFF & multiChannels[baseIndex + 1]) +
                                    (0xFF & multiChannels[baseIndex + 2])) / 3);
      }

      return singleChannel;
   }
}
