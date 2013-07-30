# the internal representation for images.
# Currently just the rgba channels.

require 'vips'
require 'RMagick'

include VIPS

# Some ImageMagick installs default to a quantum depth of 16.
# 16 bits per pixel.
# For simplicity/consistency, they will be converted to a standard depth of 8.
DESIRED_QUANTUM_DEPTH = 8
QUANTUM_CONVERSION_FACTOR = ((2.0 ** DESIRED_QUANTUM_DEPTH) - 1) /
                            ((2.0 ** Magick::QuantumDepth) - 1)

class InternalImage
   def self.read(filename)
      image = Magick::Image.read(filename)[0]
      pixels = image.get_pixels(0, 0, image.columns, image.rows)

      rgba = [[], [], [], []]
      pixels.each{|pixel|
         rgba[0] << (pixel.red * QUANTUM_CONVERSION_FACTOR).to_i
         rgba[1] << (pixel.green * QUANTUM_CONVERSION_FACTOR).to_i
         rgba[2] << (pixel.blue * QUANTUM_CONVERSION_FACTOR).to_i

         # A top opacity (2 ^ Magick::QuantumDepth) is the same as an alpha of 0.
         if (pixel.opacity == 0)
            rgba[3] << 1
         else
            opacityRatio = (2 ** Magick::QuantumDepth - 1.0) / pixel.opacity
            rgba[3] << 1.0 - opacityRatio
         end
      }

      return InternalImage.new(image.columns, image.rows,
                               rgba[0], rgba[1], rgba[2], rgba[3])
   end

   def initialize(width, height, r, g, b, a)
      @width = width
      @height = height

      @r = r
      @g = g
      @b = b
      @a = a
   end

   def write(basename)
      rgba = []
      for i in 0...@r.length
         rgba << (@r[i] / QUANTUM_CONVERSION_FACTOR).to_i
         rgba << (@g[i] / QUANTUM_CONVERSION_FACTOR).to_i
         rgba << (@b[i] / QUANTUM_CONVERSION_FACTOR).to_i

         opacityRatio = @a[i]
         rgba << (opacityRatio * (2 ** Magick::QuantumDepth - 1)).to_i
      end

      image = Magick::Image.constitute(@width, @height, 'RGBA', rgba)

      image.write("#{basename}.png")
   end
end

#TEST
img = InternalImage::read('testImages/testAlpha.png')
img.write('asd')
