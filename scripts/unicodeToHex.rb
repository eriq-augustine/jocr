# Convert a unicode string (the first arg) to a hex string.

input = ARGV.shift

input.each_char{|char|
   print "\\u#{char.unpack('U*')[0]}"
}

puts ''
