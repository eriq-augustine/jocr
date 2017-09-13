package com.eriqaugustine.ocr.image;

import com.eriqaugustine.ocr.utils.MathUtils;
import com.eriqaugustine.ocr.utils.Props;

import magick.DrawInfo;
import magick.ImageInfo;
import magick.MagickException;
import magick.MagickImage;
import magick.PixelPacket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Dimension;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Queue;

/**
 * A wrapper for whatever image library/representation that we are using.
 * Currently ImageMagick.
 * This class strives to throw as few errors as possible during normal operations.
 *
 * This class will continually cache the image data in different forms.
 * You can clean it up by calling clearCache().
 *
 * WrapImages are mutable. In fact, the mutable behavior is strongly encouraged.
 * Most transformation methods will just return the status of the operation.
 * Any transformation that retuns a WrapImage instead of a status is an immutable method.
 * However, the pixels can only be directly modified through the transformation methods.
 * If you want pixel control, you will have to call one of the getPixels() methods and then
 * resonctruct via the getImageFromPixels() method.
 *
 * Note that the mutable transformation will clear the cached data before the transform starts.
 * Even if it fails, the cache will get cleared.
 *
 * When working with discrete pixels, white is nothing (false).
 */
public class WrapImage {
   private static Logger logger = LogManager.getLogger(WrapImage.class.getName());

   private MagickImage internalImage;

   private int imageWidth;
   private int imageHeight;

   private Pixel[] cachePixels;
   // {<threshold>: discretePixels}
   private Map<Integer, boolean[]> cacheDiscretePixels;

   /**
    * Call the static constructors for access.
    */
   private WrapImage() {
      internalImage = null;

      imageWidth = 0;
      imageHeight = 0;

      cachePixels = null;
      cacheDiscretePixels = null;
   }

   /**
    * The WrapImage now owns the MagickImage.
    */
   private WrapImage(MagickImage image) throws MagickException {
      internalImage = image;

      Dimension dimensions = image.getDimension();
      imageWidth = dimensions.width;
      imageHeight = dimensions.height;

      cachePixels = null;
      cacheDiscretePixels = new HashMap<Integer, boolean[]>();
   }

   // BEGIN Static contructors.

   /**
    * Get an image without any content.
    * Any operations on an empty image will return empty variants (empty images, empty arrays, etc).
    */
   public static WrapImage getEmptyImage() {
      return new WrapImage();
   }

   /**
    * Create an image from a file on the filesystem.
    * The image type is infered from the extension.
    */
   public static WrapImage getImageFromFile(String filename) {
      try {
         ImageInfo info = new ImageInfo(filename);
         MagickImage image = new MagickImage(info);
         return new WrapImage(image);
      } catch (MagickException ex) {
         logger.error("Could not load image from file.", ex);
         return null;
      }
   }

   /**
    * NOTE(eriq): You can wrap images from non RGB channels, but they
    * will be converted to RGB and stay like that.
    */
   public static WrapImage getImageFromPixels(byte[] channelPixels,
                                              int width, int height,
                                              String channelMap) {
      assert(channelPixels.length == (width * height * channelMap.length()));
      assert(channelMap.length() > 0);

      if (channelPixels.length == 0) {
         return getEmptyImage();
      }

      try {
         MagickImage newImage = new MagickImage();
         newImage.constituteImage(width, height, channelMap, channelPixels);
         return new WrapImage(newImage);
      } catch (MagickException ex) {
         logger.error("Could not load image from pixels.", ex);
         return null;
      }
   }

   /**
    * getImageFromPixels() overload for single channel images.
    */
   public static WrapImage getImageFromPixels(byte[] pixels, int width, int height) {
      return getImageFromPixels(pixels, width, height, "I");
   }

   public static WrapImage getImageFromPixels(Pixel[] pixels, int width, int height) {
      return getImageFromPixels(pixelsToRGBChannel(pixels), width, height, "RGB");
   }

   /**
    * Get an image from just an array of bytes.
    * The format of the data must agree with getBytes().
    * Data Layout:
    * - width (int)
    * - height (int)
    * - number of pixels (int)
    * - pixels (bytes)
    */
   public static WrapImage getImageFromBytes(byte[] bytes) {
      assert(bytes.length > 0);

      ByteBuffer buffer = ByteBuffer.wrap(bytes);

      int width = buffer.getInt();
      int height = buffer.getInt();
      int numPixels = buffer.getInt();
      byte[] pixels = new byte[numPixels];
      buffer.get(pixels, 0, numPixels);

      return getImageFromPixels(pixels, width, height, "RGB");
   }

   /**
    * Get an image that is only white.
    * This is NOT an empty image (unless |width| or |height| are 0).
    */
   public static WrapImage getBlankImage(int width, int height) {
      assert(width >= 0 && height >= 0);

      if (width == 0 || height == 0) {
         return getEmptyImage();
      }

      byte[] pixels = new byte[width * height];

      for (int i = 0; i < pixels.length; i++) {
         pixels[i] = (byte)0xFF;
      }

      return getImageFromPixels(pixels, width, height);
   }

   /**
    * Get an image that has the specified areas blacked out.
    */
   public static WrapImage getImageWithBlackouts(int width, int height, List<Rectangle> blackouts) {
      assert(width >= 0 && height >= 0);

      if (width == 0 || height == 0) {
         return getEmptyImage();
      }

      byte[] pixels = new byte[width * height];

      for (int i = 0; i < pixels.length; i++) {
         pixels[i] = (byte)0xFF;
      }

      // Backout the rectangles.
      for (Rectangle blackout : blackouts) {
         for (int dx = 0; dx < blackout.width; dx++) {
            for (int dy = 0; dy < blackout.height; dy++) {
               pixels[MathUtils.rowColToIndex(dy + blackout.y, dx + blackout.x, width)] = 0;
            }
         }
      }

      return getImageFromPixels(pixels, width, height);
   }

   /**
    * Generate an image for |character|.
    * The image will have a white backgrond with |character| in black.
    */
   public static WrapImage getCharacterImage(char character,
                                             boolean shrink,
                                             int fontSize,
                                             String fontFamily,
                                             int threshold) {
      if (character == ' ') {
         return getBlankImage(fontSize, fontSize);
      }

      try {
         int sideLength = fontSize;

         MagickImage image = new MagickImage();
         byte[] pixels = new byte[sideLength * sideLength * 3];
         for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (byte)0xFF;
         }
         image.constituteImage(sideLength, sideLength, "RGB", pixels);

         ImageInfo drawInfo = new ImageInfo();
         DrawInfo draw = new DrawInfo(drawInfo);

         draw.setOpacity(0);
         draw.setGeometry("+0+0");
         draw.setGravity(magick.GravityType.CenterGravity);

         draw.setFill(new PixelPacket(0, 0, 0, 0));
         draw.setPointsize(fontSize);
         draw.setFont(fontFamily);
         draw.setText("" + character);

         image.annotateImage(draw);

         WrapImage rtn = new WrapImage(image);

         if (shrink) {
            rtn = rtn.shrink(threshold);
         }

         return rtn;
      } catch (MagickException ex) {
         logger.error("Unable to create a character image.", ex);
         return null;
      }
   }

   public static WrapImage getCharacterImage(char character,
                                             boolean shrink,
                                             int fontSize,
                                             String fontFamily) {
      return getCharacterImage(character, shrink,
                               fontSize,
                               fontFamily,
                               Props.getInt("DEFAULT_WHITE_THRESHOLD", 150));
   }

   public static WrapImage getCharacterImage(char character,
                                             boolean shrink,
                                             String fontFamily) {
      return getCharacterImage(character, shrink,
                               Props.getInt("DEFAULT_FONT_SIZE", 128),
                               fontFamily,
                               Props.getInt("DEFAULT_WHITE_THRESHOLD", 150));
   }

   public static WrapImage getCharacterImage(char character,
                                             boolean shrink) {
      return getCharacterImage(character, shrink,
                               Props.getInt("DEFAULT_FONT_SIZE", 128),
                               Props.getString("DEFAULT_FONT_FAMILY", "IPAGothic"),
                               Props.getInt("DEFAULT_WHITE_THRESHOLD", 150));
   }

   /**
    * Get an image that containts |content|.
    * ImageMagick is strange and easiest way to get word wrap is to use the "caption" feature.
    * The same tactic as getCharacterImage().
    */
   public static WrapImage getStringImage(String content, boolean shrink,
                                          int maxWidth, int maxHeight,
                                          String font) {
      try {
         ImageInfo info = new ImageInfo("caption: " + content);
         info.setSize(String.format("%dx%d", maxWidth, maxHeight));
         info.setFont(font);
         MagickImage image = new MagickImage(info);

         WrapImage rtn = new WrapImage(image);

         if (shrink) {
            rtn = rtn.shrink();
         }

         return rtn;
      } catch (MagickException ex) {
         logger.error("Could not create an image from a string.", ex);
         return null;
      }
   }

   public static WrapImage getStringImage(String content, boolean shrink,
                                          int maxWidth, int maxHeight) {
      return getStringImage(content, shrink, maxWidth, maxHeight, "Arial");
   }

   // END Static Constructors.

   public int width() {
      return imageWidth;
   }

   public int height() {
      return imageHeight;
   }

   // The number of pixels in this image.
   public int length() {
      return imageWidth * imageHeight;
   }

   /**
    * Clear out the image and make it empty.
    */
   public void clear() {
      makeEmpty();
   }

   /**
    * Get the image as an array of bytes.
    * Great for seriaalization.
    * The format of the data must agree with getImageFromBytes().
    * Data Layout:
    * - width (int)
    * - height (int)
    * - number of pixels (int)
    * - pixels (bytes)
    */
   public byte[] getBytes() {
      if (isEmpty()) {
         return new byte[0];
      }

      byte[] pixels = pixelsToRGBChannel(getPixels(true));

      ByteBuffer buffer = ByteBuffer.allocate(12 + pixels.length);
      buffer.putInt(imageWidth);
      buffer.putInt(imageHeight);
      buffer.putInt(pixels.length);
      buffer.put(pixels, 0, pixels.length);

      return buffer.array();
   }

   /**
    * Get a single pixel from the image.
    * Typically, one would want much more than just a single pixel.
    * So, this method will cache the entire image's pixels unless you
    * explicitly prevent it with |noCache|.
    * Note that |noCache| is only a request and can be ignored.
    */
   public Pixel getPixel(int row, int col, boolean noCache) {
      if (col < 0 || row < 0 ||
          col >= imageWidth || row >= imageHeight) {
         throw new IndexOutOfBoundsException();
      }

      if (noCache) {
         try {
            return new Pixel(internalImage.getOnePixel(col, row));
         } catch (MagickException ex) {
            logger.warn("Could not get a single pixel.", ex);
            // Let it fall through to the cache version.
         }
      }

      loadPixelCache();

      return cachePixels[MathUtils.rowColToIndex(row, col, imageWidth)];
   }

   public boolean getDiscretePixel(int row, int col, int threshold, boolean noCache) {
      if (col < 0 || row < 0 ||
          col >= imageWidth || row >= imageHeight) {
         throw new IndexOutOfBoundsException();
      }

      if (noCache) {
         try {
            PixelPacket pixel = internalImage.getOnePixel(col, row);
            return (new Pixel(internalImage.getOnePixel(col, row))).average() <= threshold;
         } catch (MagickException ex) {
            logger.warn("Could not get a single discrete pixel.", ex);
            // Let it fall through to the cache version.
         }
      }

      loadDiscretePixelCache(threshold);

      int index = MathUtils.rowColToIndex(row, col, imageWidth);
      return cacheDiscretePixels.get(new Integer(threshold))[index];
   }

   /**
    * Get a new version of this image.
    * The new image will not have any cached data.
    */
   public WrapImage copy() {
      if (isEmpty()) {
         return getEmptyImage();
      }

      loadPixelCache();
      return getImageFromPixels(cachePixels, imageWidth, imageHeight);
   }

   /**
    * Returns the pixels for this image.
    * If |fromCache| is true, then this WrapImage will sacrafice it's pixel cache (if it exists)
    * and return it in this method. This means that the pixel cache will be emptied, but no
    * copies will be made.
    * The cache is guarenteed to be correct, it is purely a speed issue.
    */
   public Pixel[] getPixels(boolean fromCache) {
      if (isEmpty()) {
         return new Pixel[0];
      }

      if (fromCache && cachePixels != null) {
         Pixel[] rtn = cachePixels;
         cachePixels = null;
         return rtn;
      }

      return extractPixels();
   }

   /**
    * Simple version of getPixels() that does not take from the cache.
    */
   public Pixel[] getPixels() {
      return getPixels(false);
   }

   public byte[] getAveragePixels() {
      if (isEmpty()) {
         return new byte[0];
      }

      loadPixelCache();

      byte[] rtn = new byte[cachePixels.length];
      for (int i = 0; i < rtn.length; i++) {
         rtn[i] = cachePixels[i].average();
      }

      return rtn;
   }

   /**
    * Get the discrete pixels for this image.
    * See getPixels() for all the notes on this.
    */
   public boolean[] getDiscretePixels(int threshold, boolean fromCache) {
      if (isEmpty()) {
         return new boolean[0];
      }

      if (fromCache && cacheDiscretePixels.containsKey(new Integer(threshold))) {
         return cacheDiscretePixels.remove(new Integer(threshold));
      }

      return extractDiscretePixels(extractPixels(), threshold);
   }

   /**
    * Simple version of getDiscretePixels() that does not take from the cache.
    */
   public boolean[] getDiscretePixels(int threshold) {
      return getDiscretePixels(threshold, false);
   }

   public boolean[] getDiscretePixels() {
      return getDiscretePixels(Props.getInt("DEFAULT_WHITE_THRESHOLD", 150), false);
   }

   /**
    * Write out the image to a file.
    * The image type is infered from the extension.
    */
   public boolean write(String filename) {
      if (isEmpty()) {
         try {
            ImageInfo info = new ImageInfo(filename);
            MagickImage image = new MagickImage(info);

            image.constituteImage(1, 1, "RGB",
                                  new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF});
            image.setFileName(filename);
            image.writeImage(info);

            return true;
         } catch (MagickException ex) {
            logger.error("Unable to write empty image.", ex);
            return false;
         }
      }

      try {
         ImageInfo info = new ImageInfo(filename);
         internalImage.setFileName(filename);
         internalImage.writeImage(info);
      } catch (MagickException ex) {
         logger.error("Unable to write image.", ex);
         return false;
      }

      return true;
   }

   /**
    * Does this image has any real content?
    * The only way to get an empty image is to call WrapImage.getEmptyImage().
    */
   public boolean isEmpty() {
      return internalImage == null;
   }

   // Transformations
   // Writer of transformation functions sould take great care to handle the caches
   // properly. Most mutable transformation will invalidate the cache, so they will need
   // to be cleared AT THE BEGINNING of the method.
   // Mutable methods should return a status (preferably a boolean);
   // immutable methods should return the new WrapImage.
   // Mutable operations on an empty image will return false;
   // imutable opertions on an empty image will return another empty image.

   // Mutable Transformations

   public boolean blur(double radius, double sigma) {
      if (isEmpty()) {
         return false;
      }

      clearCache();

      try {
         internalImage = internalImage.blurImage(radius, sigma);
      } catch (MagickException ex) {
         logger.error("Unable to blur image.", ex);
         return false;
      }

      return true;
   }

   /**
    * Find the edges in an image.
    * This will convert the image into bw where black pixels are edges and white is the rest.
    */
   public boolean edge(double radius) {
      if (isEmpty()) {
         return false;
      }

      clearCache();

      try {
         internalImage = internalImage.edgeImage(radius);
      } catch (MagickException ex) {
         logger.error("Unable to edge image.", ex);
         return false;
      }

      return true;
   }

   public boolean scale(int newWidth, int newHeight) {
      if (isEmpty()) {
         return false;
      }

      // Don't bother scaling if the image is already the correct size.
      if (imageWidth == newWidth && imageHeight == newHeight) {
         return true;
      }

      clearCache();

      try {
         internalImage = internalImage.scaleImage(newWidth, newHeight);

         Dimension dimensions = internalImage.getDimension();
         imageWidth = dimensions.width;
         imageHeight = dimensions.height;
      } catch (MagickException ex) {
         logger.error("Unable to scale image.", ex);
         return false;
      }

      return true;
   }

   /**
    * Clean up the image some.
    * Do the following operations:
    *  - Remove white-out any pixels with an intensity greater than |threhsold|.
    *  - Remove and blobs smaller than |minBlobSize|.
    */
   public boolean scrub(int threshold, int minBlobSize) {
      if (isEmpty()) {
         return false;
      }

      clearCache();

      boolean[] pixels = extractDiscretePixels(extractPixels(), threshold);
      List<Integer> blob;

      boolean[] visited = new boolean[pixels.length];
      for (int i = 0; i < pixels.length; i++) {
         visited[i] = false;
      }

      for (int i = 0; i < pixels.length; i++) {
         if (visited[i]) {
            continue;
         }

         visited[i] = true;

         if (pixels[i]) {
            blob = getBlob(pixels, imageWidth, i);

            // If the blob is too small, clear out all its pixels.
            if (blob.size() < minBlobSize) {
               for (Integer blobPixel : blob) {
                  pixels[blobPixel.intValue()] = false;
               }
            }

            // Mark all the blob pixels as visited.
            // Could use an else when checking the blob size, but this is
            // more readable.
            for (Integer blobPixel : blob) {
               visited[blobPixel.intValue()] = true;
            }
         }
      }

      byte[] bytePixels = new byte[pixels.length];
      for (int i = 0; i < pixels.length; i++) {
         bytePixels[i] = pixels[i] ? 0 : (byte)0xFF;
      }

      try {
         internalImage.constituteImage(imageWidth, imageHeight, "I", bytePixels);
      } catch (MagickException ex) {
         logger.error("Could not consitite scrubbed image from bytes.", ex);
         return false;
      }

      return true;
   }

   /**
    * Find the region of connecting pixels that contains |startIndex|.
    */
   private static List<Integer> getBlob(boolean[] pixels, int width, int startIndex) {
      List<Integer> rtn = new ArrayList<Integer>();

      if (!pixels[startIndex]) {
         return rtn;
      }

      Set<Integer> explored = new HashSet<Integer>();
      Queue<Integer> toExplore = new LinkedList<Integer>();
      List<Integer> neighbors;

      toExplore.add(new Integer(startIndex));
      while (!toExplore.isEmpty()) {
         int index = toExplore.remove().intValue();

         if (explored.contains(new Integer(index))) {
            continue;
         }

         explored.add(new Integer(index));
         rtn.add(new Integer(index));

         // Check all the cardinal directions.
         neighbors = MathUtils.neighbors(index, width, pixels.length);
         for (Integer neighborIndex : neighbors) {
            if (pixels[neighborIndex.intValue()] && !explored.contains(neighborIndex.intValue())) {
               toExplore.add(neighborIndex);
            }
         }
      }

      return rtn;
   }

   // Immutable Transformations

   /**
    * This is NOT a scale.
    * This method removes any border (white-only area) that the image has.
    */
   public WrapImage shrink(int threshold) {
      if (isEmpty()) {
         return getEmptyImage();
      }

      loadPixelCache();

      int minRow = forwardScanRows(threshold);
      int maxRow = backScanRows(threshold);
      int minCol = forwardScanCols(threshold);
      int maxCol = backScanCols(threshold);

      if (minRow == -1 || maxRow == -1 || minCol == -1 || maxCol == -1) {
         return getEmptyImage();
      }

      return crop(new Rectangle(minCol, minRow, maxCol - minCol + 1, maxRow - minRow + 1));
   }

   public WrapImage shrink() {
      return shrink(Props.getInt("DEFAULT_WHITE_THRESHOLD", 150));
   }

   public WrapImage crop(Rectangle bounds) {
      if (isEmpty()) {
         return getEmptyImage();
      }

      assert(bounds.x >= 0 && bounds.y >= 0);

      try {
         // ImageMagick is pretty flaky about crops of crops, so just make a full copy.
         MagickImage image = copy().internalImage.cropImage(bounds);
         return new WrapImage(image);
      } catch (MagickException ex) {
         logger.error("Could not crop image.", ex);
         return null;
      }
   }

   /**
    * We need a special crop method for many crops at once because ImageMagick cannot crop a crop.
    * So, if the base image is large, we would have to make many copies of it.
    * Instead, we will make one copy and then crop that many times.
    */
   public List<WrapImage> crop(List<Rectangle> bounds) {
      List<WrapImage> rtn = new ArrayList<WrapImage>();

      if (isEmpty()) {
         for (int i = 0; i < bounds.size(); i++) {
            rtn.add(getEmptyImage());
         }
         return rtn;
      }

      MagickImage copyImage = copy().internalImage;

      for (Rectangle bound : bounds) {
         assert(bound.x >= 0 && bound.y >= 0);

         try {
            // ImageMagick is pretty flaky about crops of crops, so just make a full copy.
            MagickImage image = copyImage.cropImage(bound);
            rtn.add(new WrapImage(image));
         } catch (MagickException ex) {
            logger.error("Could not crop image.", ex);
            rtn.add(getEmptyImage());
         }
      }

      return rtn;
   }

   // Cache operations

   /**
    * Dump all of the internal cache that this image holds.
    */
   public void clearCache() {
      cachePixels = null;
      cacheDiscretePixels = new HashMap<Integer, boolean[]>();
   }

   /**
    * Load up the cache from the internal image.
    * Will not load if there is already something in the cache.
    * It is best to call this in every method that will use the cache.
    */
   private void loadPixelCache() {
      if (isEmpty() || cachePixels != null) {
         return;
      }

      cachePixels = extractPixels();
   }

   /**
    * Load up the cache from the internal image.
    * Will not load if there is already something in the cache.
    * It is best to call this in every method that will use the cache.
    */
   private void loadDiscretePixelCache(int threshold) {
      if (isEmpty() || cacheDiscretePixels.containsKey(new Integer(threshold))) {
         return;
      }

      loadPixelCache();

      cacheDiscretePixels.put(new Integer(threshold),
                              extractDiscretePixels(cachePixels, threshold));
   }

   // Non-Static Utilities

   /**
    * A simple forward scan of this image.
    */
   public int forwardScanRows(int threshold) {
      if (isEmpty()) {
         return -1;
      }

      loadPixelCache();
      return scanRows(cachePixels, imageWidth, threshold, 0, imageHeight - 1, 1);
   }

   /**
    * A simple backwards scan of this image.
    */
   public int backScanRows(int threshold) {
      if (isEmpty()) {
         return -1;
      }

      loadPixelCache();
      return scanRows(cachePixels, imageWidth, threshold, imageHeight - 1, 0, -1);
   }

   public int forwardScanCols(int threshold) {
      if (isEmpty()) {
         return -1;
      }

      loadPixelCache();
      return scanCols(cachePixels, imageWidth, threshold, 0, imageWidth - 1, 1);
   }

   public int backScanCols(int threshold) {
      if (isEmpty()) {
         return -1;
      }

      loadPixelCache();
      return scanCols(cachePixels, imageWidth, threshold, imageWidth - 1, 0, -1);
   }

   // Static Utilities

   /**
    * Returns an "RGB" array of pixels.
    */
   public static byte[] pixelsToRGBChannel(Pixel[] pixels) {
      byte[] channelPixels = new byte[pixels.length * 3];

      for (int i = 0; i < channelPixels.length; i += 3) {
         channelPixels[i + 0] = pixels[i / 3].red;
         channelPixels[i + 1] = pixels[i / 3].green;
         channelPixels[i + 2] = pixels[i / 3].blue;
      }

      return channelPixels;
   }

   /**
    * Convert an "RGB" pixel array to a Pixel array.
    */
   public static Pixel[] rgbChannelToPixels(byte[] channelPixels) {
      assert(channelPixels.length % 3 == 0);

      Pixel[] pixels = new Pixel[channelPixels.length / 3];

      for (int i = 0; i < channelPixels.length; i += 3) {
         pixels[i / 3] = new Pixel(channelPixels[i + 0],
                                   channelPixels[i + 1],
                                   channelPixels[i + 2]);
      }

      return pixels;
   }

   /**
    * Find the first occurance of a non-white pixel (this is inclusive).
    * Will return a number between rowStart and rowEnd (inclusivley) or -1.
    * If there is no occurance of a non-white pixel, then -1 is returned.
    * Note that the bounds are handled differently because this can go backwards.
    */
   public static int scanRows(Pixel[] pixels, int width, int threshold,
                              int rowStart, int rowEnd, int rowStep) {
      assert((Math.abs(rowEnd - rowStart) + 1) % Math.abs(rowStep) == 0);

      for (int row = rowStart; row != rowEnd + rowStep; row += rowStep) {
         for (int col = 0; col < width; col++) {
            int index = MathUtils.rowColToIndex(row, col, width);
            if ((0xFF & pixels[index].average()) <= threshold) {
               return row;
            }
         }
      }

      return -1;
   }

   public static int scanCols(Pixel[] pixels, int width, int threshold,
                              int colStart, int colEnd, int colStep) {
      assert((Math.abs(colEnd - colStart) + 1) % Math.abs(colStep) == 0);

      for (int col = colStart; col != colEnd + colStep; col += colStep) {
         for (int row = 0; row < pixels.length / width; row++) {
            int index = MathUtils.rowColToIndex(row, col, width);
            if ((0xFF & pixels[index].average()) <= threshold) {
               return col;
            }
         }
      }

      return -1;
   }

   // Support Classes

   /**
    * These will get thrown in place of MagicExceptions in the case of
    * a non-recoverable, unexpected exception during image operations when
    * a return status is not appropriate.
    * These should be very rare.
    */
   @SuppressWarnings("serial")
   public class RuntimeImageException extends RuntimeException {
      public MagickException magickException;

      public RuntimeImageException(String message, MagickException magickException) {
         super(message);
         this.magickException = magickException;
      }

      public RuntimeImageException(String message) {
         super(message);
      }
   }

   public static class Pixel {
      public byte red;
      public byte green;
      public byte blue;

      public Pixel(byte red, byte green, byte blue) {
         this.red = red;
         this.green = green;
         this.blue = blue;
      }

      /**
       * Just a white pixel.
       */
      public Pixel() {
         this(0xFF, 0xFF, 0xFF);
      }

      public Pixel(int red, int green, int blue) {
         this((byte)red, (byte)green, (byte)blue);
      }

      public Pixel(Pixel pixel) {
         this(pixel.red, pixel.green, pixel.blue);
      }

      public Pixel(Color color) {
         this(color.getRed(), color.getGreen(), color.getBlue());
      }

      public Pixel(PixelPacket pixelPacket) {
         this(pixelPacket.getRed(), pixelPacket.getGreen(), pixelPacket.getBlue());
      }

      /**
       * Get the average intensity value for this pixel.
       * Return is byte, and not float on purpose.
       */
      public byte average() {
         return (byte)((red + green + blue) / 3);
      }
   }

   // Deep Internals

   private Pixel[] extractPixels() {
      byte[] channelPixels = new byte[length() * 3];

      try {
         internalImage.dispatchImage(0, 0, imageWidth, imageHeight, "RGB", channelPixels);
      } catch (MagickException ex) {
         logger.error("Could not dispatch image for cache.", ex);
         throw new RuntimeImageException("Could not dispatch image for cache.", ex);
      }

      return rgbChannelToPixels(channelPixels);
   }

   private boolean[] extractDiscretePixels(Pixel[] pixels, int threshold) {
      boolean[] discretePixels = new boolean[pixels.length];

      for (int i = 0; i < discretePixels.length; i++) {
         discretePixels[i] = (0xFF & pixels[i].average()) <= threshold;
      }

      return discretePixels;
   }

   /**
    * Make this image an empty image.
    */
   private void makeEmpty() {
      clearCache();

      imageWidth = 0;
      imageHeight = 0;

      internalImage = null;
   }
}
