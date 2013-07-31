require './image'
require './filter'

IMAGE = 'testImages/test.png'
#IMAGE = 'testImages/testAlpha.png'
#IMAGE = 'testImages/testStrip.png'

time = Time.now()
baseImg = InternalImage::read(IMAGE)

newTime = Time.now()
puts "Read Time: #{newTime - time}"
time = newTime

testGaussian = [[0.0037, 0.0147, 0.0256, 0.0147, 0.0037],
                [0.0147, 0.0586, 0.0952, 0.0586, 0.0147],
                [0.0256, 0.0952, 0.1502, 0.0952, 0.0256],
                [0.0147, 0.0586, 0.0952, 0.0586, 0.0147],
                [0.0037, 0.0147, 0.0256, 0.0147, 0.0037]]

baseImg.write('test1')

conv = Filter::threadConvolution(baseImg, testGaussian)
conv.write('test2-0')

newTime = Time.now()
puts "Thread Conv Time: #{newTime - time}"
time = newTime

conv = Filter::convolution(baseImg, testGaussian)
conv.write('test2-1')

newTime = Time.now()
puts "Conv Time: #{newTime - time}"
time = newTime

exit

bw = Filter::bw(baseImg, 150)
bw.write('test3')

newTime = Time.now()
puts "BW Time: #{newTime - time}"
time = newTime

bwConv = Filter::convolution(bw, testGaussian)
bwConv.write('test4')

newTime = Time.now()
puts "Conv (bw) Time: #{newTime - time}"
time = newTime

convBw = Filter::bw(conv, 150)
convBw.write('test5')

newTime = Time.now()
puts "BW (Conv) Time: #{newTime - time}"
time = newTime
