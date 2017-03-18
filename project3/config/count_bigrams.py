import sys

if __name__ == '__main__':
    if len(sys.argv) <= 1:
        sys.exit("Usage: python count_bigrams.py <bigram result>")

    filename = sys.argv[1]
    with open(filename, 'r') as f:
        bigrams = [i.split() for i in f]
        bigrams = sorted([ (int(count), x, y) for (x, y, count) in bigrams ], reverse=True)
        total = sum([ count for (count, x, y) in bigrams ])

        counted, i = 0, 0
        while counted < total * 0.1:
            count, x, y = bigrams[i]
            #print "Bigram (%s,%s) has count = %d" %(x, y, count)
            i += 1
            counted += count
        print "Total number of bigrams = %d." % len(bigrams);
        print "The most common bigram is (%s,%s)." %(bigrams[0][1],bigrams[0][2])
        print "%d bigrams required to add up to 10%% of all bigrams." % i
