function decisionEvidence = decodeEvidence(response, templateResponses, decisionBias)

% function decisionEvidence = decodeEvidence(response, templateResponses, decisionBias)
%
% Negative evidence favors template 1, positive favors template 2.

if nargin < 3
    decisionBias = 0;
end

% decision weights = difference between templates
w = templateResponses(2,:) - templateResponses(1,:);

% project response onto decision weights
decisionEvidence = w*response' - decisionBias;


