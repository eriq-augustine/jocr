jocr
====

Japanese OCR

===
Setup

Make sure to install ImageMagick (http://www.imagemagick.org/) and JMagick(http://www.imagemagick.org/).

Set you LD_LIBRARY_PATH to wherever JMagick was installed to (/usr/local/lib is default on linux).

==
Fonts

Until we get away from JMagick/ImageMagick, you must load your fonts manually (they cannot be taken from the JVM).

Different OS's/Distros handle fonts differently, but Arch and Ubuntu (12) seem to be happy with:
mkdif -p ~/.fonts
cp -r fonts ~/.fonts/jocrFonts
fc-cache -vf

===
Recommended Classpath

.:bin:lib:lib/*:config
