function [x] = halfExp(base,n)

if ~exist('n','var')
    n=1;
end

x = (max(0,base)).^n;