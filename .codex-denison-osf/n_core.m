function [r, f, s] = n_core(d, sigma, p, r_prev, tau, dt)

% function [r, f, s] = n_core(d, sigma, p, r_prev, tau, dt)
%
% r_prev = p.r(:,idx-1)
% d = d(:,idx)
% s = p.s(:,idx)
% f = p.f(:,idx)
% r = p.r(:,idx)

% We let the suppressive drive be broad in feature space by letting all
% features contribute equally to the normalization pool

% Normalization pool
pool = abs(d); % abs in case drive is negative

% Suppressive Drive
s = sum(pool(:)); 

% Normalization
f = d ./ (s + halfExp(sigma, p));

% Update firing rates
r = r_prev + (dt/tau)*(-r_prev + f);
