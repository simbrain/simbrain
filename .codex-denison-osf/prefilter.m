function z = prefilter(x, w, n, dt, idx)

% function z = prefilter(x, w, n, dt, idx)

phw = [1; -1]; % phase weights

% prepare time series
if idx > length(w)
    y = x(:,idx-length(w):idx-1);
else
    y = nan(size(w));
    y(:,end-idx+2:end) = x(:,1:idx-1);
end

% filter
inp = [];
for iPh = 1:2
    inp0 = y.*fliplr(w.*phw(iPh)); % convolve step 1 (multiply)
    inp1 = sum(inp0(:,max(end-idx+2,1):end),2)*dt; % convolve step 2 (integrate across time)
    inp(:,iPh) = halfExp(inp1,n); % rectify and raise to power
end

% on channel - off channel
z = inp*phw; 