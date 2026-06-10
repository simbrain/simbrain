function p = setTask(p,cond)

w = p.AVWeights;

% set attention weights for T1 and T2
switch cond
    case 'cueT1'
        attWeights = [w(1) w(2)]; % 1 = high, 2 = low
    case 'cueT2'
        attWeights = [w(2) w(1)];
    case 'cueN'
        if p.distributeVoluntary
            attWeights = w;
        else
            attWeights = [max(w) max(w)];
        end
    otherwise
        error('attention cond not recognized');
end

% start and end times of attention to T1
attStart = p.stimOnset + p.AVOnset;
attEnd = p.stimOnset + p.AVOnset + p.AVDur;

% make voluntary attention input (same across features)
timeSeries = zeros([p.ntheta p.nt 2]);
timeSeries(:,unique(round((attStart:p.dt:attEnd)/p.dt)), 1) = attWeights(1); % T1
timeSeries(:,unique(round(((attStart:p.dt:attEnd) + p.soa)/p.dt)), 2) = attWeights(2); % T2
timeSeries = max(timeSeries,[],3);

p.task = timeSeries;
