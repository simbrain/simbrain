function plotTimeSeries(p, condname)


%% Inputs
if strfind(p.modelClass,'LC')
    plotS3 = 1;
    nr = 7;
else
    plotS3 = 0;
    nr = 6;
end

%% Plot
figTitle = sprintf('%s, soa: %d, seq: %d %d', condname, p.soa, p.stimseq);

cpsFigure(.5,1.2);
set(gcf,'Name',figTitle);
% nr = 7;
nc = 1;
xlims = [0 2];

subplot(nr,nc,1)
hold on
plot(p.tlist/1000,p.stim','color',[0 0 0]);
xlim(xlims)
set(gca,'XTickLabel',[])
ylabel('Stimulus')

subplot(nr,nc,2)
hold on
plot(p.tlist/1000,p.rav','color',[153 124 107]/255);
xlim(xlims)
ylim([-.1 .1])
set(gca,'XTickLabel',[])
ylabel('AV')

subplot(nr,nc,3)
hold on
plot(p.tlist/1000,p.rai','color',[185 178 170]/255);
xlim(xlims)
ylim([-.2 .2])
set(gca,'XTickLabel',[])
ylabel('AI')

subplot(nr,nc,4)
hold on
plot(p.tlist/1000,p.r1','color',[112 191 65]/255);
xlim(xlims)
ylim([0 max(p.r1(:))*1.1])
set(gca,'XTickLabel',[])
ylabel('S1')

subplot(nr,nc,5)
hold on
plot(p.tlist/1000,p.r2','color',[58 154 217]/255);
xlim(xlims)
ylim([0 max(p.r2(:))*1.1])
set(gca,'XTickLabel',[])
ylabel('S2')

if plotS3
    subplot(nr,nc,6)
    hold on
    plot(p.tlist/1000,p.r3','color',[110 119 143]/255);
    xlim(xlims)
    ylim([0 max(p.r3(:))*1.1])
    set(gca,'XTickLabel',[])
    ylabel('S3')
end

subplot(nr,nc,nr)
hold on
plot(p.tlist/1000, zeros(size(p.tlist)), 'k')
plot(p.tlist/1000,p.rd','color',[53 68 88]/255);
plot(p.tlist/1000,p.decisionWindows(:,1:length(p.tlist))*max(abs(p.rd(:)))*1.1,'color',[127 127 127]/255);
xlim(xlims)
set(gca,'XTick',[-0.5 0 0.5 1 1.5 2])
ylabel('Decision')
xlabel('Time (s)')

supertitle(figTitle);
