function p = setDecisionWindows(p)

%% Setup
decisionWindowDur = p.soa;
decisionOnsets = [p.stimOnset p.stimOnset+p.soa];

%% Define decision windows
p.decisionWindows = zeros(p.nstim,p.nt);
for iStim = 1:p.nstim
    % integrate until next stimulus or end of trial
    if iStim==p.nstim
        idx = round((decisionOnsets(iStim)/p.dt):size(p.stim,2)); % last stim - integrate to the end
    else
        idx = round((decisionOnsets(iStim)/p.dt):(decisionOnsets(iStim)/p.dt)+decisionWindowDur/p.dt);
    end
    p.decisionWindows(iStim,idx) = 1;
end

