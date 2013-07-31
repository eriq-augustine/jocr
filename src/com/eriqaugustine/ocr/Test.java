package com.eriqaugustine.ocr;

import magick.ImageInfo;
import magick.MagickImage;

public class Test {
   public static void main(String[] args) throws Exception {
      ImageInfo info = new ImageInfo("testImages/test.png");

      MagickImage image = new MagickImage(info);
      image.setFileName("out/test01-base.png");
      image.writeImage(info);

      MagickImage blurImage = image.blurImage(3, 1);
      blurImage.setFileName("out/test02-blur.png");
      blurImage.writeImage(info);

      MagickImage edgeImage = image.edgeImage(0);
      edgeImage.setFileName("out/test03-edge.png");
      edgeImage.writeImage(info);

      MagickImage blurEdgeImage = image.blurImage(3, 1).edgeImage(0);
      blurEdgeImage.setFileName("out/test04-blur-edge.png");
      blurEdgeImage.writeImage(info);

      MagickImage edgeBlurImage = image.edgeImage(0).blurImage(3, 1);
      edgeBlurImage.setFileName("out/test05-edge-blur.png");
      edgeBlurImage.writeImage(info);

      MagickImage bw = new MagickImage(info);
      bw.thresholdImage(40000);
      bw.setFileName("out/test06-thresh.png");
      bw.writeImage(info);

      blurImage.thresholdImage(40000);
      blurImage.setFileName("out/test07-blur-thresh.png");
      blurImage.writeImage(info);

      MagickImage blurThreshEdgeImage = blurImage.edgeImage(0);
      blurThreshEdgeImage.setFileName("out/test08-blur-thresh-edge.png");
      blurThreshEdgeImage.writeImage(info);

      MagickImage threshBlur = bw.blurImage(3, 1);
      threshBlur.setFileName("out/test09-thresh-blur.png");
      threshBlur.writeImage(info);

      MagickImage threshBlurEdge = threshBlur.edgeImage(0);
      threshBlurEdge.setFileName("out/test10-thresh-blur-edge.png");
      threshBlurEdge.writeImage(info);

      MagickImage threshEdge = bw.edgeImage(0);
      threshEdge.setFileName("out/test11-thresh-edge.png");
      threshEdge.writeImage(info);
   }
}
