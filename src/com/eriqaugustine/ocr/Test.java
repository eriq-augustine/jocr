package com.eriqaugustine.ocr;

import com.eriqaugustine.ocr.image.BubbleDetection;
import com.eriqaugustine.ocr.image.CharacterImage;
import com.eriqaugustine.ocr.image.Filters;
import com.eriqaugustine.ocr.image.TextImage;
import com.eriqaugustine.ocr.utils.FileUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;

import magick.DrawInfo;
import magick.ImageInfo;
import magick.MagickImage;
import magick.PixelPacket;

import java.awt.GraphicsEnvironment;

public class Test {
   public static void main(String[] args) throws Exception {
      fontGenTest();
      //imageBreakdown();
   }

   public static void fontGenTest() throws Exception {
      /*
      GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
      String[] fontFamilies = env.getAvailableFontFamilyNames();
      for (String fontFamily : fontFamilies) {
         System.out.println(fontFamily);
      }
      */
      //TEST IPAGothic

      String hiragana = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん";

/*
      for (int i = 0; i < hiragana.length(); i++) {
         System.out.println(i + " -- " + hiragana.charAt(i) + " (" + (int)hiragana.charAt(i) + ")");
      }

      int current = (int)'あ';
      int count = 0;
      do {
         System.err.println(count + " -- " + (char)current + " (" + current + ")");
         count++;
         current++;
      } while ((char)(current -1) != 'ん');
      */

      MagickImage image = new MagickImage();
      byte[] pixels = new byte[50 * 50 * 3];
      for (int i = 0; i < pixels.length; i++) {
         pixels[i] = (byte)0xFF;
      }
      image.constituteImage(50, 50, "RGB", pixels);

      ImageInfo drawInfo = new ImageInfo();
      DrawInfo draw = new DrawInfo(drawInfo);

      draw.setOpacity(0);
      draw.setGeometry("+0+0");
      draw.setGravity(magick.GravityType.CenterGravity);

      draw.setFill(new PixelPacket(0, 0, 0, 0));
      draw.setPointsize(24);
      draw.setFont("IPAGothic");
      draw.setText("あ");

      image.annotateImage(draw);
      image.setFileName("あ.png");
      image.writeImage(new ImageInfo());
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
