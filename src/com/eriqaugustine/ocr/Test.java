package com.eriqaugustine.ocr;

import com.eriqaugustine.ocr.image.BubbleDetection;
import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.image.Filters;
import com.eriqaugustine.ocr.image.TextImage;
import com.eriqaugustine.ocr.utils.CharacterUtils;
import com.eriqaugustine.ocr.utils.FileUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;

import magick.DrawInfo;
import magick.ImageInfo;
import magick.MagickImage;
import magick.PixelPacket;

import java.awt.GraphicsEnvironment;

public class Test {
   public static void main(String[] args) throws Exception {
      //fontGenTest();
      //imageBreakdown();
      densityComparisonTest();
   }

   public static void densityComparisonTest() throws Exception {
      ImageInfo info = new ImageInfo("testImages/2Text.png");
      MagickImage baseImage = new MagickImage(info);

      double[][][] fontDensityMaps = CharacterImage.getTestingFontDensityMaps(3, 3);

      MagickImage[][] gridTextImages = TextImage.gridBreakup(baseImage);
      for (int row = 0; row < gridTextImages.length; row++) {
         for (int col = 0; col < gridTextImages[row].length; col++) {
            MagickImage gridTextImage = ImageUtils.shrinkImage(gridTextImages[row][col]);

            double[][] densityMap = CharacterImage.getDensityMap(gridTextImage, 3, 3);

            if (densityMap == null) {
               System.out.println(String.format("(%d, %d) -> <space>", row, col));
            } else {
               int guess = bestMatch(fontDensityMaps, densityMap);
               System.out.println(String.format("(%d, %d) -> %c",
                                                row, col, (char)(guess + (int)'あ')));
            }
         }
      }
   }

   private static int bestMatch(double[][][] haystack, double[][] needle) {
      int bestGuess = 0;
      double bestDistance = Double.MAX_VALUE;

      for (int i = 0; i < haystack.length; i++) {
         double dist = CharacterImage.densityMapDistance(haystack[i], needle);

         if (dist < bestDistance) {
            bestGuess = i;
            bestDistance = dist;
         }
      }

      return bestGuess;
   }

   public static void fontGenTest() throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "fontGen");

      String hiragana = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん";

      /*
      for (int i = 0; i < hiragana.length(); i++) {
         System.out.println(i + " -- " + hiragana.charAt(i) + " (" + (int)hiragana.charAt(i) + ")");
      }
      */

      MagickImage image = null;

      int current = (int)'あ';
      int count = 0;
      do {
         // System.err.println(count + " -- " + (char)current + " (" + current + ")");

         image = CharacterUtils.generateCharacter((char)current, false);
         image.setFileName(String.format("%s/char-%03d-%c.png",
                                         outDirectory,
                                         count,
                                         (char)current));
         image.writeImage(new ImageInfo());

         count++;
         current++;
      } while ((char)(current -1) != 'ん');
   }

   public static void imageBreakdown() throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "blob");
      ImageInfo info = new ImageInfo("testImages/page.png");
      // ImageInfo info = new ImageInfo("testImages/2Text.png");
      // ImageInfo info = new ImageInfo("testImages/small.png");

      MagickImage baseImage = new MagickImage(info);
      baseImage.setFileName(outDirectory + "/test00-base.png");
      baseImage.writeImage(info);

      MagickImage bubbles = BubbleDetection.fillBubbles(baseImage);
      bubbles.setFileName(outDirectory + "/test01-bubbles.png");
      bubbles.writeImage(info);

      int count = 0;
      MagickImage[] bubbleImages = BubbleDetection.extractBubbles(baseImage);
      for (MagickImage bubbleImage : bubbleImages) {
         bubbleImage.setFileName(
            String.format("%s/test02-bubbles-%02d-0.png", outDirectory, count));
         bubbleImage.writeImage(info);

         MagickImage[][] gridTextImages = TextImage.gridBreakup(bubbleImage);
         for (int row = 0; row < gridTextImages.length; row++) {
            for (int col = 0; col < gridTextImages[row].length; col++) {
               MagickImage gridTextImage = ImageUtils.shrinkImage(gridTextImages[row][col]);
               gridTextImage.setFileName(
                  String.format("%s/test02-bubbles-%02d-gridTexts-%02d-%02d.png",
                                outDirectory, count, row, col));
               gridTextImage.writeImage(info);

               double[][] densityMap =
                  CharacterImage.getDensityMap(gridTextImage, 3, 3);

               if (densityMap == null) {
                  continue;
               }

               System.out.println(row + ", " + col);
               for (int i = 0; i < densityMap.length; i++) {
                  System.out.print(" ");
                  for (int j = 0; j < densityMap[i].length; j++) {
                     System.out.print(String.format("  %6.4f", densityMap[i][j]));
                  }
                  System.out.println();
               }
            }
         }

         count++;
      }
   }
}
