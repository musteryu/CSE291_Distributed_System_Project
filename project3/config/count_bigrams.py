FILE_NAME = "sample_bigrams.txt"
bigrams = [i.split() for i in open(FILE_NAME).readlines()]
bigrams = sorted([[int(count),x,y] for (x,y,count) in bigrams])
bigrams.reverse()
total = sum([count for (count,x,y) in bigrams])

counted,i = 0,0
while counted < total * 0.1:
    count,x,y = bigrams[i]
    print "Bigram (%s,%s) has count = %d" %(x,y,count)
    counted += count
print "Bigrams above consists top 10% of bigrams"
