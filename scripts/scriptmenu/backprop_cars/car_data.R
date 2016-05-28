# The calls to mtcars and write.table are the main thing needed for the demo.  
# The rest is just me (JKY) messing around.

# Get cars dataset
mtcars
help("mtcars")

# Show pairwise correlations
library(corrplot) 
corrplot(cor(mtcars)) 
corrplot(cor(mtcars[,c("mpg","cyl","disp","hp","qsec")])) 

# Export data
write.table(mtcars, "cars.csv", sep=",", col.names=FALSE, row.names=FALSE)
getwd()

# A bunch of pairwise regression plots
plot(mtcars$mpg, mtcars$hp,  xlab = "mpg", ylab = "hp")
abline(lm(hp~mpg, mtcars))
abline(h = mean(mtcars$hp)) # If there were no relation
plot(mtcars$cyl, mtcars$hp,  xlab = "cyl", ylab = "hp")
abline(lm(hp~cyl, mtcars))
plot(mtcars$disp, mtcars$hp,  xlab = "disp", ylab = "hp")
abline(lm(hp~disp, mtcars))
plot(mtcars$mpg, mtcars$qsec,  xlab = "mpg", ylab = "qm")
abline(lm(qsec~mpg, mtcars))
plot(mtcars$cyl, mtcars$qsec,  xlab = "cyl", ylab = "qm")
abline(lm(qsec~cyl, mtcars))
plot(mtcars$disp, mtcars$qsec,  xlab = "disp", ylab = "qm")
abline(lm(qsec~disp, mtcars))

# Multivariate multiple regression that parallels the neural network
carmodel <- lm(cbind(hp, qsec) ~ mpg + cyl + disp, data = mtcars)
summary(carmodel)
mean(carmodel$residuals^2) # mse for regression model

# Try again with rescaled data
rescale <- function(x) (x/(max(x)))
mtcars_s <- rescale(mtcars);
carmodel_s <- lm(cbind(hp, qsec) ~ mpg + cyl + disp, data = mtcars_s)
summary(carmodel_s)
mean(carmodel_s$residuals^2) # mse for regression model

# Find max value of every column
apply(mtcars, 2, max)

# Print columns of interest to console
mtcars[,c("hp","qsec")]

# Some linear models
lm1 = lm(hp ~ cyl + disp + mpg, mtcars) 
summary(lm1)
lm2 = lm(hp ~ cyl, mtcars)
summary(lm2)
lm3 = lm(hp ~ disp, mtcars)
summary(lm3)
lm4 = lm(hp ~ mpg, mtcars)
summary(lm4)

# Check collinearity using VIF
library(car)
vif(carmodel)
carmodel_all <- lm(wt ~ ., data = mtcars)
vif(carmodel_all)
carmodel <- lm(wt ~ mpg + am + vs, data = mtcars)
vif(carmodel)
carmodel <- lm(qsec ~ mpg + am + vs, data = mtcars)
vif(carmodel)
carmodel <- lm(cbind(wt,qsec) ~ mpg + am + vs, data = mtcars)
summary(carmodel)
