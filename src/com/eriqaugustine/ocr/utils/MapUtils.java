package com.eriqaugustine.ocr.utils;

import java.util.Map;

/**
 * Map utilities.
 */
public class MapUtils {
   /**
    * Put an object in a map and return that map.
    */
   public static <K, V> Map<K, V> inlinePut(Map<K, V> map, K key, V value) {
      map.put(key, value);
      return map;
   }
}
