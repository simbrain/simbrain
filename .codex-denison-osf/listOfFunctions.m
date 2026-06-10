% listOfFunctions

%% documentation
demo                % demonstrates how to run the model with different options
listOfFunctions     % reference list of all functions

%% wrappers
runModelParallel    % runs model on different conditions with parfor, calls runModel

%% model
runModel            % main function to run the model. loops through multiple attention conds, contrasts, soas, seqs, etc
setParameters       % sets parameters, contains all default parameters
n_model             % the normalization model function, computes time series for each layer
n_core              % the core normalization and dynamic update operation
prefilter           % prefilters an input time series
initTimeSeries      % initializes all timeseries with zeros
setStim             % makes the stimulus time series
setTask             % makes the task input to voluntary attention
setDecisionWindows  % sets decision windows
rfResponse          % calculates the response of the neural population to a given orientation
distributeAttention % controls distribution of attention
decodeEvidence      % decode the CW/CCW decision

%% visualization
plotPerformance     % plots model performance, called by runModel
plotTimeSeries      % plots model timeseries, called by runModel
cpsFigure           % makes formatted figure
supertitle          % adds subplot to figure

%% helper
makePrefilter       % makes temporal prefilter, equivalent to a temporal receptive field
makeGamma           % makes gamma function
halfExp             % half-wave rectified exponentiation
