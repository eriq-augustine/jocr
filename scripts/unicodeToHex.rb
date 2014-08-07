# Convert a unicode string (the first arg) to a hex string.

ARGV.each{|line|
   line.strip().each_char{|char|
      print "\\u#{char.unpack('U*')[0].to_s(16)}"
   }
   puts ''
}
