function h = cpsFigure(width,height,num,name)

if nargin<3
    h = figure;
else
    h = figure(num);
end
if nargin==4
    set(gcf,'Name',name);
end

Position    = get(h,'Position');
Position(3) = width*Position(3);
Position(4) = height*Position(4);
set(h,'Position', Position,'color','w',...
    'PaperUnits','inches','PaperPosition',[0 0 width height]*4);
