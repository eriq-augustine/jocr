# Generate Japanese words from the NLTK 'KNBC' corpus.

import nltk

import codecs
import locale
import sys

# Wrap sys.stdout into a StreamWriter to allow writing unicode.
sys.stdout = codecs.getwriter(locale.getpreferredencoding())(sys.stdout)

counts = {}

for corpus in nltk.corpus.knbc.fileids():
    # print corpus

    words = nltk.corpus.knbc.words(corpus)

    for word in words:
        if word in counts:
            counts[word] += 1
        else:
            counts[word] = 1

# print len(counts)

for word in counts:
    print u"{0}\t{1}".format(unicode(word), unicode(counts[word]))
