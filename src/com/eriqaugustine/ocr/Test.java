package com.eriqaugustine.ocr;

import com.eriqaugustine.ocr.image.BubbleDetection;
import com.eriqaugustine.ocr.image.Filters;
import com.eriqaugustine.ocr.image.TextImage;
import com.eriqaugustine.ocr.utils.FileUtils;
import com.eriqaugustine.ocr.utils.ImageUtils;

import magick.ImageInfo;
import magick.MagickImage;

public class Test {
   public static void main(String[] args) throws Exception {
      String outDirectory = FileUtils.itterationDir("out", "blob");
      ImageInfo info = new ImageInfo("testImages/test.png");
      // ImageInfo info = new ImageInfo("testImages/test2Text.png");
      // ImageInfo info = new ImageInfo("testImages/testSmall.png");

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
            }
         }

         count++;
      }

      /*
      MagickImage bw40 = Filters.bw(image, 40);
      bw40.setFileName(outDirectory + "/test01-bw40.png");
      bw40.writeImage(info);

      MagickImage bwEdge = bw40.edgeImage(3);
      bwEdge.setFileName(outDirectory + "/test02-bw40-edge.png");
      bwEdge.writeImage(info);

      MagickImage bwEdgeBubbles = BubbleDetection.findBubbles(bwEdge);
      bwEdgeBubbles.setFileName(outDirectory + "/test03-bw40-edge-bubbles.png");
      bwEdgeBubbles.writeImage(info);
      */

      /*
      MagickImage image = new MagickImage(info);
      image.setFileName(outDirectory + "/test01-base.png");
      image.writeImage(info);

      MagickImage blurImage = image.blurImage(3, 1);
      blurImage.setFileName(outDirectory + "/test02-blur.png");
      blurImage.writeImage(info);

      MagickImage edgeImage = image.edgeImage(0);
      edgeImage.setFileName(outDirectory + "/test03-edge.png");
      edgeImage.writeImage(info);

      MagickImage blurEdgeImage = image.blurImage(3, 1).edgeImage(0);
      blurEdgeImage.setFileName(outDirectory + "/test04-blur-edge.png");
      blurEdgeImage.writeImage(info);

      MagickImage edgeBlurImage = image.edgeImage(0).blurImage(3, 1);
      edgeBlurImage.setFileName(outDirectory + "/test05-edge-blur.png");
      edgeBlurImage.writeImage(info);

      MagickImage bw = new MagickImage(info);
      bw.thresholdImage(40000);
      bw.setFileName(outDirectory + "/test06-thresh.png");
      bw.writeImage(info);

      blurImage.thresholdImage(40000);
      blurImage.setFileName(outDirectory + "/test07-blur-thresh.png");
      blurImage.writeImage(info);

      MagickImage blurThreshEdgeImage = blurImage.edgeImage(0);
      blurThreshEdgeImage.setFileName(outDirectory + "/test08-blur-thresh-edge.png");
      blurThreshEdgeImage.writeImage(info);

      MagickImage threshBlur = bw.blurImage(3, 1);
      threshBlur.setFileName(outDirectory + "/test09-thresh-blur.png");
      threshBlur.writeImage(info);

      MagickImage threshBlurEdge = threshBlur.edgeImage(0);
      threshBlurEdge.setFileName(outDirectory + "/test10-thresh-blur-edge.png");
      threshBlurEdge.writeImage(info);

      MagickImage threshEdge = bw.edgeImage(0);
      threshEdge.setFileName(outDirectory + "/test11-thresh-edge.png");
      threshEdge.writeImage(info);

      threshBlurEdge.thresholdImage(65500);
      threshBlurEdge.setFileName(outDirectory + "/test12-thresh-blur-edge-hiThresh.png");
      threshBlurEdge.writeImage(info);
      */

      /*
      int count = 0;
      for (int threshold = 100; threshold >= 0; threshold -= 10) {
         MagickImage bw = Filters.bw(image, threshold);
         bw.setFileName(String.format("%s/test01-%02d-bw-%d.png",
                                      outDirectory,
                                      count,
                                      threshold));
         bw.writeImage(info);
         count++;
      }

      image.thresholdImage(40000);
      MagickImage blur = image.blurImage(3, 1).edgeImage(3);
      blur.setFileName(outDirectory + "/test02-thresh-blur-edge.png");
      blur.writeImage(info);

      MagickImage bw1 = Filters.bw(blur);
      bw1.setFileName(outDirectory + "/test03-thresh-blur-edge-bw.png");
      bw1.writeImage(info);

      MagickImage bubbles = BubbleDetection.findBubbles(blur);
      bubbles.setFileName(outDirectory + "/test04-thresh-blur-edge-bubbles.png");
      bubbles.writeImage(info);
      */

      /*
      MagickImage bwBlur = bw40.blurImage(3, 1);
      bwBlur.setFileName(outDirectory + "/test08-bw40-blur.png");
      bwBlur.writeImage(info);

      MagickImage bwBlurEdge = bwBlur.edgeImage(3);
      bwBlurEdge.setFileName(outDirectory + "/test09-bw40-blur-edge.png");
      bwBlurEdge.writeImage(info);

      MagickImage bwBlurEdgeBubbles = BubbleDetection.findBubbles(bwBlurEdge);
      bwBlurEdgeBubbles.setFileName(outDirectory + "/test10-bw40-blur-edge-bubbles.png");
      bwBlurEdgeBubbles.writeImage(info);
      */

      /*
      MagickImage blur2 = blur.blurImage(2, 1);
      blur2.setFileName(outDirectory + "/test02-thresh-blur-edge-blur.png");
      blur2.writeImage(info);

      blur.negateImage(100);
      blur.setFileName(outDirectory + "/test03-thresh-blur-edge-negate.png");
      blur.writeImage(info);

      blur.thresholdImage(50);
      blur.setFileName(outDirectory + "/test04-thresh-blur-edge-negate-hiThresh.png");
      blur.writeImage(info);
      */
   }
}
