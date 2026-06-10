function response = rfResponse(theta, nRF)

if nargin < 2
    nRF = 12;
end

m = 2*nRF-1;

for iRF = 1:nRF
    response(iRF) = abs(cos(theta + iRF*pi/nRF).^m);
end


%% show tuning curves
% theta = 0:.01:pi;
% 
% for iRF = 1:nRF
%     response(:,iRF) = abs(cos(theta + iRF*pi/nRF).^m);
% end
% 
% figure
% plot(response)