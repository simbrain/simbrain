function p = setParameters(opt, modelClass)

% function p = setParameters(opt, modelClass)
%
% opt is an options structure containing values of p fields. will overwrite existing values. 

%% Deal with input
if ~exist('opt','var')
    opt = [];
end
if ~exist('modelClass','var')
    modelClass = '';
end

%% Process modelClass
modelParts = strsplit(modelClass,'_');
modelClass = modelParts{1};
if numel(modelParts)>1
    if strcmp(modelParts{2},'nolimit')
        noLimit = 1;
    end
end

%% Model
if ~isempty(modelClass)
    switch modelClass
        case {'Main','No-IA','EG','LC'}
            % ok
        otherwise
            error('modelClass not recognized. Check options in setParameters.m')
    end
    p.modelClass = modelClass;
else
    % 'Main', 'No-IA', 'EG', 'LC'
    p.modelClass  = 'Main';
end

%% Time
p.dt              = 2;              % time-step (ms)
p.T               = 2.1*1000;       % duration (ms)
p.nt              = p.T/p.dt+1;
p.tlist           = 0:p.dt:p.T;

%% Space and feature space
p.x               = 0;              % sampling of space
p.nx              = numel(p.x);
p.ntheta          = 12;             % should match RF

%% Exponent
p.p               = 1.5;

%% Sensory layer 1
p.tau1            = 52;             % time constant (ms)
p.sigma1          = 1.4;            % semisaturation constant

%% Sensory layer 2
p.tau2            = 100;
p.sigma2          = .1;

%% Sensory layer 3
p.tau3            = 2;
p.sigma3          = .3; 

%% Attention
p.tauAI           = 2;              % time constant involuntary attention (ms)
p.tauAV           = 50;             % time constant voluntary attention (ms)

p.aAV             = 40;
p.gamPSh          = 2.2;
p.gamPSc          = 0.023;

p.aAI             = 8.5;
p.aANeg           = 0;
p.gamNSh          = NaN;
p.gamNSc          = NaN;
p.sigmaA          = 20;

h0                = makePrefilter(0:p.dt/1000:0.8, p.gamPSh, p.gamPSc, p.gamNSh, p.gamNSc, p.aANeg);
p.h               = repmat(h0, p.ntheta, 1);

%% Stimulus
p.stimOnset       = 500;            % relative to start of trial (ms)
p.stimDur         = 30;   

%% Task (control input)
p.AVOnset         = -34; 
p.AVDur           = 124; 
p.tR              = 918;            % recovery time

if exist('noLimit','var') && noLimit==1
    p.distributeVoluntary = 0;
else
    p.distributeVoluntary = 1;
end

p.AVWeights         = [1 0];        % [high low]
p.AVNeutralT1Weight = 0.5;          % bias to treat neutral like attend to T1. 0.5 is no bias (with distributeVoluntary only)
p.AVProp            = 1;            % proportion of attention allocated to cued target (with distributeVoluntary only)

%% Decision
p.sigmaD          = .7; 
p.tauD            = 100000;

%% Scaling (for fitting only)
p.scaling1        = 1e5;
p.scaling2        = 1e5;

%% Set params from opt
if ~isempty(opt)
    fieldNames = fields(opt);
    for iF = 1:numel(fieldNames)
        f = fieldNames{iF};
        p.(f) = opt.(f);
    end
    
    h0            = makePrefilter(0:p.dt/1000:0.8, p.gamPSh, p.gamPSc, p.gamNSh, p.gamNSc, p.aANeg);
    p.h           = repmat(h0, p.ntheta, 1);
end

