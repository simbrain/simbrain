function p = initTimeSeries(p)

% Sensory layer 1
p.d1   = zeros(p.ntheta,p.nt); % drive
p.s1   = zeros(p.ntheta,p.nt); % suppressive drive
p.f1   = zeros(p.ntheta,p.nt); % normalization result
p.r1   = zeros(p.ntheta,p.nt); % response

% Sensory layer 2
p.d2   = zeros(p.ntheta,p.nt); 
p.s2   = zeros(p.ntheta,p.nt); 
p.f2   = zeros(p.ntheta,p.nt); 
p.r2   = zeros(p.ntheta,p.nt); 

% Sensory layer 3
p.d3   = zeros(p.ntheta,p.nt); 
p.s3   = zeros(p.ntheta,p.nt); 
p.f3   = zeros(p.ntheta,p.nt); 
p.r3   = zeros(p.ntheta,p.nt);

% Decision
p.dd   = zeros(p.nstim,p.nt);
p.sd   = zeros(p.nstim,p.nt);  
p.fd   = zeros(p.nstim,p.nt); 
p.rd   = zeros(p.nstim,p.nt);
    
% Voluntary attention
p.dav  = zeros(p.ntheta,p.nt); 
p.sav  = zeros(p.ntheta,p.nt); 
p.fav  = zeros(p.ntheta,p.nt); 
p.rav  = zeros(p.ntheta,p.nt); 
p.task = zeros(p.ntheta,p.nt); % voluntary control signal

% Involuntary attention
p.dai  = zeros(p.ntheta,p.nt); 
p.sai  = zeros(p.ntheta,p.nt); 
p.fai  = zeros(p.ntheta,p.nt); 
p.rai  = zeros(p.ntheta,p.nt); 

