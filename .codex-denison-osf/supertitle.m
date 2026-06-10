function supertitle(name)
%
% supertitle(name)
%
% places supertitle as the title of a current figure. useful for subplots.

set(gcf,'NextPlot','add');
axes;
h = title(name);
set(gca,'Visible','off');
set(h,'Visible','on');

amt = 1.27;
pos = get(gca,'Position');
pos(2) = pos(2)*amt;
set(gca,'Position',pos);