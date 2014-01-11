package com.eriqaugustine.ocr.image;

import magick.DrawInfo;
import magick.ImageInfo;
import magick.MagickImage;
import magick.PixelPacket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Rectangle;

/**
 * A wrapper for whatever image library/representation that we are using.
 * Currently ImageMagick.
 * This class strives to throw as few errors as possible during normal operations.
 *
 * This class will continually cache the image data in different forms.
 * You can clean it up by calling dump().
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
 */
public class WrapImage {
   private static Logger logger = LogManager.getLogger(WrapImage.class.getName());

   private MagickImage internalImage;

   // TODO(eriq): Cache
   private byte[] cahcePixels;
   private boolean[] cacheDiscretePixels;

   /**
    * Call the static constructors for access.
    */
   private WrapImage() {
      internalImage = null;
   }

   /**
    * The WrapImage now owns the MagickImage.
    */
   private WrapImage(MagickImage image) {
      internalImage = image;
   }

   // BEGIN Static contructors.

   /**
    * Get an image without any content.
    * Any operations on an empty image will return empty variants (empty images, empty arrays, etc).
    * This is the only way to get an image that will return true from isEmpty().
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
      } catch (Exception ex) {
         logger.error("Could not load image from file.", ex);
         return null;
      }
   }

   /**
    * NOTE(eriq): You can wrap images from wide (> 1) chanels, but you
    * will only be able to get pixels back in a single chanel.
    */
   public static WrapImage getImageFromPixels(byte[] pixels,
                                              int width, int height,
                                              int numChannels) {
      // TODO(eriq):
      return null;
   }

   /**
    * getImageFromPixels() overload for single channel images.
    */
   public static WrapImage getImageFromPixels(byte[] pixels,
                                              int width, int height) {
      return getImageFromPixels(pixels, width, height, 1);
   }

   /**
    * Get an image that is only white.
    * This is NOT an empty image.
    */
   public static WrapImage getBlankImage(int width, int height) {
      // TODO(eriq):
      return null;
   }

   // END Static Constructors.

   public int width() {
      // TODO(eriq)
      return -1;
   }

   public int height() {
      // TODO(eriq)
      return -1;
   }

   // The number of pixels in this image.
   public int length() {
      // TODO(eriq)
      return -1;
   }

   /**
    * Get a single pixel from the image.
    * Typically, one would want much more than just a single pixel.
    * So, this method will cache the entire image's pixels unless you
    * explicitly prevent it with |noCache|.
    */
   public byte getPixel(int row, int col, boolean noCache) {
      // TODO(eriq)
      return -1;
   }

   public boolean getDiscretePixel(int row, int col, int threshold, boolean noCache) {
      // TODO(eriq)
      return false;
   }

   /**
    * Get a new version of this image.
    * The new image will not have any cached data.
    */
   public WrapImage copy() {
      // TODO(eriq)
      return null;
   }

   /**
    * Returns the pixels for this image.
    * If |fromCache| is true, then this WrapImage will sacrafice it's pixel cache (if it exists)
    * and return it in this method. This means that the pixel cache will be emptied, but no
    * copies will be made.
    * The cache is guarenteed to be correct, it is purely a speed issue.
    * NOTE(eriq): We are not going to support multi-chanel pixel arrays at this time.
    * (eg. arrays of pixels that contains values for R, G, and B.)
    * Most of our work will be in BW. Even when we do work with color, it will be minimal.
    * One byte per pixel is more than enough for us.
    */
   public byte[] getPixels(boolean fromCache) {
      // TODO(eriq):
      return null;
   }

   /**
    * Simple version of getPixels() that does not take from the cache.
    */
   public byte[] getPixels() {
      return getPixels(false);
   }

   /**
    * Get the discrete pixels for this image.
    */
   public boolean[] getDiscretePixels(int threshold) {
      // TODO(eriq):
      return null;
   }

   /**
    * Write out the image to a file.
    * The image type is infered from the extension.
    */
   public boolean write(String filename) {
      // TODO(eriq):
      return false;
   }

   /**
    * Does this image has any real content?
    * The only way to get an empty image is to call WrapImage.getEmptyImage().
    */
   public boolean isEmpty() {
      return internalImage == null;
   }

   /**
    * Dump all of the internal cache that this image holds.
    */
   public void dump() {
      // TODO(eriq)
   }

   // Iterators
   // TODO(eriq)

   // Transformations
   // Writer of transformation functions sould take great care to handle the caches
   // properly. Most mutable transformation will invalidate the cache, so they will need
   // to be cleared AT THE BEGINNING of the method.
   // Mutable methods should return a status (preferably a boolean),
   // immutable methods should return the new WrapImage.

   // Mutable Transformations

   public boolean blur(double raduis, double sigma) {
      // TODO(eriq): shallow wrap
      return false;
   }

   /**
    * Convert this image to black and white.
    * This is not the fake black and white that uses grey.
    * This will fully discretize the image.
    */
   public boolean bw(int threshold) {
      // TODO(eriq):
      return false;
   }

   /**
    * Find the edges in an image.
    * This will convert the image into bw where black pixels are edges and white is the rest.
    */
   public boolean edge(double raduis) {
      // TODO(eriq): shallow
      return false;
   }

   /**
    * This is NOT a scale.
    * This method removes any border (white-only area) that the image has.
    */
   public boolean shrink(int threshold) {
      // TODO(eriq)
      return false;
   }

   public boolean scale(int newWidth, int newHeight) {
      // TODO(eriq): shallow
      return false;
   }

   // Immutable Transformations

   public WrapImage crap(Rectangle bounds) {
      // TODO(eriq):
      return null;
   }
}
