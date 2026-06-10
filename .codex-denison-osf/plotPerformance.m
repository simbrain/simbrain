function perfv = plotPerformance(condnames, soas, perf, plotFigs)

if nargin < 4
    plotFigs = 1;
end

if numel(soas)==1
%     fprintf('plotPerformance is for multiple SOAS, not plotting ...\n')
    perfv = [];
    return
end

%% re-sort cue condition data into valid, invalid, neutral
if isequal(condnames,{'cueT1','cueT2'})
    cueValidityNames = {'valid','invalid'};
    
    perfv{1}(1,:) = perf(1,:,1); % T1 valid
    perfv{1}(2,:) = perf(1,:,2); % T1 invalid
    
    perfv{2}(1,:) = perf(2,:,2); % T2 valid
    perfv{2}(2,:) = perf(2,:,1); % T2 invalid
    
elseif isequal(condnames,{'cueT1','cueT2','cueN'})
    cueValidityNames = {'valid','invalid','neutral'};
    
    perfv{1}(1,:) = perf(1,:,1); % T1 valid
    perfv{1}(2,:) = perf(1,:,2); % T1 invalid
    perfv{1}(3,:) = perf(1,:,3); % T1 neutral 
    
    perfv{2}(1,:) = perf(2,:,2); % T2 valid
    perfv{2}(2,:) = perf(2,:,1); % T2 invalid
    perfv{2}(3,:) = perf(2,:,3); % T2 neutral 

else
    fprintf('conds must be {"cueT1","cueT2"} or {"cueT1","cueT2","cueN"}\nnot plotting ...\n')
    return
end

%% plot
if plotFigs
    % sorted by validity
    targetNames = {'T1','T2'};
    ylims = [0 max([perfv{1}(:); perfv{2}(:)])*1.1];
    xlims = [soas(1)-100 soas(end)+100];
    
    % evidence
    figure
%     figure(gcf)
%     clf
    
    for iT = 1:numel(perfv)
        subplot(1,numel(perfv),iT)
        hold on
        p1 = plot(repmat(soas',1,numel(cueValidityNames)),...
            perfv{iT}', '.-', 'MarkerSize', 20);
        
        title(targetNames{iT})
        xlim(xlims)
        try
            ylim(ylims)
        end
        
        if iT==1
            xlabel('SOA (ms)')
            ylabel('Evidence (au)')
        end
        if iT==numel(perfv)
            legend(p1, cueValidityNames,'location','best')
        end
    end
end

