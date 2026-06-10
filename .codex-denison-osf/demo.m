% demo.m

% This script demonstrates how to run the model. 
% Each cell specifies a set of options, runs the model with those options, 
% and generates a figure (or figures) to show the model performance.
% The cells are independent; they do not have to be run in order.

%% Run the model with default values
% runModel is the main top-level function for running the model. It outputs
% the performance of the model for all simulated experimental conditions
% (perf) and a structure of model parameters and simulation results (p).
% In this example, we run the model with all default settings.

[perf, p] = runModel;
supertitle('Default settings')

disp(p)

%% Change the parameter values using opt
% Use the opt structure to specify model parameters set by default in
% setParameters. Parameter values can also be changed in setParameters.
% In this example, we set the tR parameter (which determines the recovery
% time for voluntary attention) to 500 ms.

opt = [];
opt.tR = 500;

[~, p] = runModel(opt);
supertitle(sprintf('Parameter tR = %d ms',p.tR))

%% Change the model class
% modelClass is an optional argument to runModel. It can also be set in
% setParameters.
% In this example, we set modelClass to 'Main_nolimit'. This runs the
% 'Main' model (which is the default) but with no temporal limit on 
% voluntary attention.

opt = [];
modelClass = 'Main_nolimit'; 

runModel(opt, modelClass);
supertitle(sprintf('Model class = Main, no limit'))

%% Run at selected SOA and cueing conditions and plot the time series
% Experiment condition values (SOA, target orientation sequence, and 
% attentional cue condition) are additional optional arguments to runModel.
% They can also be set in runModel, which lists the condition options.
% In this example, we simulate two experimental conditions (cueT1 and
% cueT2 at an SOA of 300 ms) and plot the time series the model generates
% for the different model layers in each simulation.

opt = [];
modelClass = [];
rsoa = 5; % SOA = 300 ms (see runModel)
rseq = []; % default orientation sequence
rcond = 1:2; % cueT1, cueT2

opt.display.plotTS = 1; % plot the time series for each simulation

runModel(opt, modelClass, rsoa, rseq, rcond);

%% Run conditions in parallel
% runModelParallel uses parfor to run multiple experiment conditions in
% parallel. It calls runModel.
% In this example, we run the model in parallel using the default settings.

opt = [];
modelClass = [];
plotFigs = 1; 

runModelParallel(opt, modelClass, plotFigs);
supertitle('Default settings, parallel')
