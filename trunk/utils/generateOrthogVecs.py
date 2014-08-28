import numpy
numVals = 26;
ones = numpy.diag(numpy.ones(26))
numpy.savetxt("orthongoal_" + str(numVals) + ".csv", ones ,fmt='%i', delimiter=",")
