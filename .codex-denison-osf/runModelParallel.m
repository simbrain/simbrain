function [perfv, p, ev] = runModelParallel(opt, modelClass, plotFigs)

% function [perfv, p, ev] = runModel(opt, modelClass, rsoa, rseq, rcond)
%
% INPUTS
% opt
%   A structure with parameter values. Any specified values will
%   overwrite the defaults in setParameters. Also use opt to pass in
%   display options for runModel.
% modelClass 
%   The model name, see setParameters
% plotFigs
%   Set to 1 to plot model performance, 0 for no plot
%
% All input arguments are optional.
%
% OUTPUTS
% perfv
%   Performance (decision evidence) for each target (in cells), cueing
%   condition, and SOA. Organized for plotting.
% p
%   Structure containing parameter values, see setParameters
% ev
%   Decision evidence for each condition combination

if nargin < 1
    opt = [];
end
if nargin < 2 || isempty(modelClass)
    modelClass = '';
end
if nargin < 3
    plotFigs = 1;
end

par = 'soa+attcond'; % 'soa+attcond','soa','seq'

% soas
soas = [100:50:500 800];
rsoa = 1:10;
nsoa = numel(rsoa);

% att conds
condnames  =  {'cueT1','cueT2','cueN'};
rcond = 1:3; 
ncond = numel(rcond);

% seqs
stimseqs  = {[2 1],[2 2],[2 3],[2 4]};
rseq = 4; 
nseq = numel(rseq);

%% run in parallel
switch par
    case 'soa+attcond'
        parconds = fullfact([nsoa, ncond]);
        nparconds = size(parconds,1);
        parfor ipc = 1:nparconds
            isoa = parconds(ipc,1);
            icond = parconds(ipc,2);
            [pperfv{ipc}, pp{ipc}, pev{ipc}] = runModel(opt, modelClass, rsoa(isoa), [], rcond(icond));
        end
    case 'soa'
        parfor isoa = 1:nsoa
            [pperfv{isoa}, pp{isoa}, pev{isoa}] = runModel(opt, modelClass, rsoa(isoa));
        end
    case 'seq'
        parfor iseq = 1:nseq
            [pperfv{iseq}, pp{iseq}, pev{iseq}] = runModel(opt, modelClass, [], rseq(iseq));
        end
    otherwise
        error('par not found')
end

%% combine ev
% ev(:,isoa,icond,icontrast,iseq)
ev = [];
switch par
    case 'soa+attcond'
        for icond = 1:ncond
            for isoa = 1:nsoa
                ipc = (icond-1)*nsoa + isoa;
                if numel(size(pev{1}))==5
                    ev(:,isoa,icond,1,:) = pev{ipc};
                else
                    ev(:,isoa,icond,:,:) = pev{ipc};
                end
            end
        end
        p = pp{end}; % specify a p
        perfv = plotPerformance(condnames(rcond), soas(rsoa), mean(ev,5), plotFigs);
    case 'soa'
        for isoa = 1:nsoa
            if numel(size(pev{1}))==5
                ev(:,isoa,:,1,:) = pev{isoa};
            else
                ev(:,isoa,:,:,:) = pev{isoa};
            end
        end
        p = pp{end};
        perfv = plotPerformance(condnames(rcond), soas(rsoa), mean(ev,5), plotFigs);
    case 'seq'
        perfv = pperfv;
        p = pp;
        ev = pev;
        for iseq = 1:nseq
            plotPerformance(condnames(rcond), soas(rsoa), ev{iseq}, plotFigs);
        end
    otherwise
        error('par not found')
end


    