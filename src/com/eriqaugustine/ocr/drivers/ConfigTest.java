package com.eriqaugustine.ocr.drivers;

import com.eriqaugustine.ocr.utils.Props;

public class ConfigTest {
   public static void main(String[] args) {
      String punct = Props.getString("PUNCTUATION");
      System.out.println("`" + punct + "`");
   }
}
