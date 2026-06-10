function h = makePrefilter(x, posShape, posScale, negShape, negScale, ampNeg)

hPos = makeGamma(x,[],posShape,posScale,1);
hNeg = makeGamma(x,[],negShape,negScale,ampNeg);
h = hPos - hNeg;