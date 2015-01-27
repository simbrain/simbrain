import numpy
numVals = int(raw_input("Number of dimensions: "))
ones = numpy.diag(numpy.ones(numVals))
numpy.savetxt("orthogonal_" + str(numVals) + ".csv", ones ,fmt='%i', delimiter=",")
