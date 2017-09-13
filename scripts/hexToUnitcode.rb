# text = '\uff5b\uff5d\uff08\uff09\uff3b\uff3d\u3010\u3011\u3001\uff0c\u2026\u2025\u002e\u3002\u002d\u300c\u300d\u300e\u300f\u301c\u30fb\uff1a\uff01\uff1f\u007c'

text = ARGV.join('').gsub(/\s+/, '')
puts text

text.scan(/\\u(\w{4})/){|hex|
   print [hex[0].hex].pack("U")
}

puts ''
