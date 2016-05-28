import numpy as np
from sklearn import linear_model, preprocessing
from sklearn.cross_validation import train_test_split
from sklearn.metrics import mean_squared_error

#
# Load the cars dataset
#
cars = np.genfromtxt ('cars.csv', delimiter=",")

# Rescale the data to [0,1]
# scaler = preprocessing.MinMaxScaler()
# cars = scaler.fit_transform(cars)
cars = cars*1./np.max(cars, axis=0) # Simple rescaling so inverse is easy

# Pull out the columns to use for input and target data  
inputs = cars[:,[0,1,2]] # mpg, cyl, disp
targets = cars[:,[3,6]]  # hp, qsec

# Divide data in to a training set and test set
[train_in, test_in, train_targ, test_targ] = \
	train_test_split(inputs,targets,test_size=0.33)

# Sci-kit linear regression for comparison
regr = linear_model.LinearRegression()
regr.fit(train_in, train_targ)
predict_training = regr.predict(train_in)
predict_testing = regr.predict(test_in)
print("MSE Training")
print(mean_squared_error(predict_training, train_targ))
# Above = np.sum(np.square(np.subtract(predict_training, train_targ)))/train_targ.size
print("MSE Test")
print(mean_squared_error(predict_testing, test_targ))

# Export to csv
np.savetxt("cars_train_in.csv", train_in, delimiter=",", fmt="%1.3f")
np.savetxt("cars_test_in.csv", test_in, delimiter=",", fmt="%1.3f")
np.savetxt("cars_train_targ.csv", train_targ, delimiter=",", fmt="%1.3f")
np.savetxt("cars_test_targ.csv", test_targ, delimiter=",", fmt="%1.3f")