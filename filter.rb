require './image'

MAX_THREADS = 14

# Filters can be chained together.
module Filter
   # Note, the output image will be smaller than the input image because
   #  only pixels that can full fix the kernel will be evaluated.
   #  The pixels will be dropped from the bottom and right sides of the image.
   # |kernel| is assumed to be a rectangular kernel.
   # Alpha levels will be maintained.
   def Filter.convolution(image, kernel)
      assert(kernel.length > 0 && kernel[0].length > 0)

      kHeight = kernel.length
      kWidth = kernel[0].length

      rgba = [[], [], [], []]
      for row in 0...(image.height - kHeight + 1)
         for col in 0...(image.width - kWidth + 1)
            r = 0.0
            g = 0.0
            b = 0.0

            for kRow in 0...kHeight
               for kCol in 0...kWidth
                  pixel = image.pixel(col + kCol, row + kRow)

                  r += pixel[R] * kernel[kRow][kCol]
                  g += pixel[G] * kernel[kRow][kCol]
                  b += pixel[B] * kernel[kRow][kCol]
               end
            end

            rgba[R] << r.to_i
            rgba[G] << g.to_i
            rgba[B] << b.to_i
            rgba[A] << image.pixel(col, row)[A]
         end
      end

      return InternalImage.new(image.width - kWidth + 1, image.height - kHeight + 1,
                               rgba[R], rgba[G], rgba[B], rgba[A])
   end

   def Filter.threadConvolution(image, kernel)
      assert(kernel.length > 0 && kernel[0].length > 0)

      kHeight = kernel.length
      kWidth = kernel[0].length

      newHeight = image.height - (kHeight - 1)
      newWidth = image.width - (kWidth - 1)

      rgba = [Array.new(newHeight * newWidth){|index| 0},
              Array.new(newHeight * newWidth){|index| 0},
              Array.new(newHeight * newWidth){|index| 0},
              Array.new(newHeight * newWidth){|index| 0}]

      threadLambda = lambda{|rowStart, numRows, colStart, numCols|
         #TEST
         puts "In lambda: #{rowStart}"

         for row in rowStart...(rowStart + numRows)
            for col in colStart...(colStart + numCols)
               r = 0.0
               g = 0.0
               b = 0.0

               for kRow in 0...kHeight
                  for kCol in 0...kWidth
                     pixel = image.pixel(col + kCol, row + kRow)

                     r += pixel[R] * kernel[kRow][kCol]
                     g += pixel[G] * kernel[kRow][kCol]
                     b += pixel[B] * kernel[kRow][kCol]
                  end
               end

               index = row * newWidth + col

               rgba[R][index] << r.to_i
               rgba[G][index] << g.to_i
               rgba[B][index] << b.to_i
               rgba[A][index] << image.pixel(col, row)[A]
            end
         end
      }
      Filter::threadFilter(0, image.height - kHeight + 1,
                           0, image.width - kWidth + 1,
                           threadLambda)

      return InternalImage.new(newWidth, newHeight,
                               rgba[R], rgba[G], rgba[B], rgba[A])
   end

   # Anything averaging under |breakpoint| will be black.
   # This filter doesn't play games.
   # This is meant for text-processing, not general image work.
   # So the resulting image will only contain two colors, black and white.
   # All alphas wii be set to 1.
   def Filter.bw(image, breakpoint = 150)
      rgba = [[], [], [], []]

      for row in 0...image.height
         for col in 0...image.width
            val = image.avg(col, row)

            if (val < breakpoint)
               fill = 0
            else
               fill = 255
            end

            rgba[R] << fill
            rgba[G] << fill
            rgba[B] << fill
            rgba[A] << 1.0
         end
      end

      return InternalImage.new(image.width, image.height,
                               rgba[R], rgba[G], rgba[B], rgba[A])
   end

   # |workLambda| should take four parameters:
   #  rowStart, numRows, colStart, numCols
   def Filter.threadFilter(rowStart, rowEnd, colStart, colEnd,
                           workLambda, numThreads = MAX_THREADS)
      assert(workLambda.arity == 4)

      pool = []
      rowDelta = (rowEnd - rowStart) / numThreads
      currentRow = rowStart

      for i in 0...numThreads
         #TEST
         puts "Init: #{currentRow}"

         pool << Thread.new(currentRow){|rowStart|
            #TEST
            puts "Thread start: #{currentRow}"

            workLambda.call(rowStart, rowDelta, colStart, colEnd)
            #TEST
            puts "Thread done: #{currentRow}"

         }

         #TEST
         puts "Back to main: #{currentRow}"

         currentRow += rowDelta
      end

      pool.each{|thread|
         thread.join()
      }
   end
end
