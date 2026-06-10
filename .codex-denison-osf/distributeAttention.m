function attn = distributeAttention(soa, tR, weight)
%
% function attn = distributeAttention(soa, tR, weight)
%
% INPUTS:
% soa - interval between the two time points
% tR - recovery time
% weight - proportion allocated to item1

% OUTPUTS:
% attn - 1x2 vector of attentional allocation to [item1 item2]

%% calculate total attention to be allocated across the two time points
% ranges from 1 to 2
totalAttn = 1 + soa/tR;
if totalAttn>2
    totalAttn = 2;
end

%% allocate attention
attn(1) = totalAttn*weight; 
attn(2) = totalAttn*(1-weight);

%% reallocate extra attention
% check if attn exceeds 100% at either time point
if attn(1) > 1
    extra = attn(1)-1;
    attn(1) = 1;
    attn(2) = attn(2)+extra;
elseif attn(2) > 1
    extra = attn(2)-1;
    attn(2) = 1;
    attn(1) = attn(1)+extra;
end

