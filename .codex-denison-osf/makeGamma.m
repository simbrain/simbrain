function g = makeGamma(space,center,shape,scale,height)
%
% g = makeGamma(space,center,shape,scale,[height])
%
% This is a function creates a gamma peaking at "center", 
% over the values defined by vector "space".  
%
% height, if specified is the height of the peak of the gamma.
% Otherwise, it is scaled to unit volume

if isempty(shape)
    shape = 12;
end
if isempty(scale)
    scale = 12;
end

plot_figure = 0; % if 1, then plot the two kernels.

g0 = gampdf(space,shape,scale); 

if ~isempty(center)
    peak = find(g0==max(g0));
    shift = center - peak;
    if shift > 0
        g = [zeros(1,shift) g0(1:end-shift)];
    elseif shift < 0
        g = [g0(1+shift:end) zeros(1,shift)];
    end
else
    g = g0;
end

if exist('height','var')
  if height==0
      g = zeros(size(space));
  else
      g = g./max(g) * height;
  end
end

if plot_figure == 1
    figure; plot (space,g)
end
