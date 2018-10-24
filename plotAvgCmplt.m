addpath('matlab_func');
common_settings;
% is_printed = 1;
% fig_path = ['figs/'];
version = '_n1000';
%%
simulation = true;
if simulation 
    queue_num = 10; cluster_size= 20;
%     queue_num = 20; cluster_size= 100;
    plots  = [true, false, false, false, false, true, false];
else 
    fprintf('Verify the experiment results');
    queue_num = 4;
    cluster_size= 4;
    % TODO: double check results of DRFS
    % ClusterAvg =  [5.3396    5.0837    4.8939    4.8312    4.4267]*1000; % eurosys 4.2
    SimAvg = [3.810    3.6888    3.3361     3.3257    2.6007] * 1000; % from simulation    
    SimWaitingAvg = [3078    2970 1835 1861 1553]; % from simulation
    SimRunningAvg = [731.5   718 1501 1628 1048]; % from simulation
    ClusterAvg = [3.8899    3.752    3.3247    3.4347    2.5079] * 1000;% eurosys 4.3
    ClusterWaitingAvg = [3.1584    3.000    1.6876+0.12    1.6690+0.12    1.3565+0.12] * 1000;
    ClusterRunningAvg = [0.7315    0.751    1.5171    1.6457    1.0314] * 1000;
    plots  = [false, false, false, false, false, false, false];
end
 sample = 20;
figureSize = figSizeThreeFourth;

% methods = {strDRF,  strES,  'DRFE', strAlloX, 'SJF', 'FS','SRPT'};
% files = {'DRF', 'ES', 'DRFExt', 'AlloX','SJF', 'FS','SRPT'};
% DRFId = 1; ESId = 2; DRFExtId = 3; AlloXId = 4; SRPTId = 5; AlloXId = 6;

methods = {strDRFFIFO, strDRFSJF, strES, strDRFExt, strAlloX, strSRPT};
files = {'DRFFIFO', 'DRF', 'ES', 'DRFExt', 'AlloX', 'SRPT'};
% methods = {'DRFF', 'DRFS', strES, 'DRFE', 'AlloX'};
% files = {'DRFFIFO', 'DRF', 'ES', 'DRFExt', 'FS'};
DRFFIFOId = 1; DRFId = 2; ESId = 3; DRFExtId = 4;  AlloXId = 5; SRPTId = 6;
plotOrders = [DRFFIFOId DRFId ESId DRFExtId SRPTId AlloXId];
% plotOrders = [DRFFIFOId DRFId DRFExtId SRPTId AlloXId];
% plotOrders = [SRPTId AlloXId];
% plotOrders = [ESId SRPTId AlloXId];
% plotOrders = [DRFId ESId];

alphas = [0.1 0.3];

methodColors = {colorES; colorDRF; colorProposed};

extraStr = ['_' int2str(queue_num) '_' int2str(cluster_size) version];
EXTRA='';
% EXTRA='_w_prof';
JobIds={};
startTimes={};
endTimes = {};
durations = {};
queueNames = {};
startRunningTimes = {};
runningTimes = {};
scaleTime = 1; % minutes

logfolder = ['log/'];
%% load data
% if (simulation)
    for i=1:length(methods)
        outputFile = [ 'output/' files{i} '-output' extraStr  '.csv'];
        [JobIds{i}, startTimes{i}, endTimes{i}, durations{i}, queueNames{i}, startRunningTimes{i}, runningTimes{i}] ...
            = import_compl_time(outputFile);   

    %     waitingTimes{i} = startRunningTimes{i} - startTimes{i};
        waitingTimes{i} = durations{i} - runningTimes{i};
    %     runningTimes{i} = durations{i} - waitingTimes{i};

        fullJobsIndices{i} = find(JobIds{i}>=0);

        durations{i} = durations{i}(fullJobsIndices{i}); 
        waitingTimes{i} = waitingTimes{i}(fullJobsIndices{i});
        runningTimes{i} = runningTimes{i}(fullJobsIndices{i});
        JobIds{i} = JobIds{i}(fullJobsIndices{i});
        startTimes{i} = startTimes{i}(fullJobsIndices{i});
        endTimes{i} = endTimes{i}(fullJobsIndices{i});
        queueNames{i} = queueNames{i}(fullJobsIndices{i});
        startRunningTimes{i} = startRunningTimes{i}(fullJobsIndices{i});
    end
% end


%%
if plots(1)    
   figIdx=figIdx +1;  
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');   
%    hBar = bar(avgCmplt', barWidth);
%    set(hBar,{'FaceColor'}, colorUsers);   

    stackData = zeros(length(methods), 3 , 2);
    
    for i = 1:length(methods)
%         avgCmplt(i) = mean(durations{i}) * scaleTime;
%         avgCmplt(i) = mean(runningTimes{i}) * scaleTime;
%         h=bar(i, avgCmplt(i), barWidth);
        avgCmplt(i,:) = mean([waitingTimes{i} runningTimes{i}]) * scaleTime;        
        
        [temp ids] = sort(durations{i}, 'descend');
        temp1 = waitingTimes{i}(ids);
        temp2 = runningTimes{i}(ids);
        
%         topTemp = temp(1:ceil(end*0.05));
        topTemp = [temp1(1:ceil(end*0.05)) temp2(1:ceil(end*0.05))]* scaleTime;
        avg95(i,:) = mean(topTemp);         
        
        topTemp = [temp1(1:ceil(end*0.01)) temp2(1:ceil(end*0.01))]* scaleTime;
        avg99(i,:) = mean(topTemp);         

%             avgCmplt(i,:) = mean([waitingTimes{i}]) * scaleTime;        
%         set(h,'FaceColor', methodColors{i});
    end
%     stackData(:,1,:) = sum(avgCmplt,2);
    stackData(:,1,:) = avgCmplt;
    stackData(:,2,:) = avg95;    
    stackData(:,3,:) = avg99; 

    plotBarStackGroups(stackData, methods, false);
      
    temp = sum(avgCmplt'); 
    improvement = (temp-temp(AlloXId))./temp * 100
    maxImprovement = (temp-temp(SRPTId))./temp * 100
    xLabel=strMethods;
    yLabel=strAvgCmplt;
    legendStr={'wait time','runtime','wait-top 5%','run-top 5%','wait-top 1%','run-top 1%',};
    legend(legendStr, 'Location','northeastoutside','FontSize', fontLegend);
    xlim([0.6 length(methods)+0.4]);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
%     h=bar(1:length(methods), avgCmplt(:,:), barWidth , 'stacked'); 
    
    axes('position',[.35 .25 0.32 0.7])
    box on % put box around new pair of axes
    plotBarStackGroups(stackData(3:end,:,:), methods(3:end), false);
    axis tight
%     xlim([2.6 length(methods)+0.4]);


%     set(gca, 'YScale', 'log'); 
%     grid on;
%     ylim([0 ]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize.*[1 1 1.4 1], 'PaperUnits', 'inches', 'PaperPosition', figureSize.*[1 1 1.4 1]);

    fileNames{figIdx} = 'avgCmplt';
end

    
if (~simulation)
    if plots(1)    
       figIdx=figIdx +1;         
       figures{figIdx} = figure;
       scrsz = get(groot, 'ScreenSize');   
    %    hBar = bar(avgCmplt', barWidth);
    %    set(hBar,{'FaceColor'}, colorUsers);   

    %     bar3DVals = zeros(2, );
        hold on
        for i = 1:length(methods)
    %         avgCmplt(i) = mean(durations{i}) * scaleTime;
    %         avgCmplt(i) = mean(runningTimes{i}) * scaleTime;
    %         h=bar(i, avgCmplt(i), barWidth);
            avgCmplt(i,:) = mean([waitingTimes{i} runningTimes{i}]) * scaleTime;   

            temp = sort(durations{i}, 'descend');
            topTemp = temp(1:ceil(end*0.05));
            avg95(i) = mean(topTemp);         
            topTemp = temp(floor(end*0.95):end);
            avg5(i) = mean(topTemp); 
    %             avgCmplt(i,:) = mean([waitingTimes{i}]) * scaleTime;        
    %         set(h,'FaceColor', methodColors{i});
        end
        h=bar(1:length(methods), avgCmplt(:,:), barWidth , 'stacked');    

        hold off
        temp = sum(avgCmplt'); 
        improvement = (temp-temp(AlloXId))./temp * 100
        maxImprovement = (temp-temp(SRPTId))./temp * 100
        xLabel=strMethods;
        yLabel=strAvgCmplt;
        legendStr={'wait time','runtime'};
        legend(legendStr,'Location','best','FontSize', fontLegend);
        xlim([0.6 length(methods)+0.4]);

    %     set(gca, 'YScale', 'log'); 
        grid on;
    %     ylim([0 ]);
        xLabels={strUser1, strUser2, strUser3}; 
        set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
        xlabel(xLabel,'FontSize',fontAxis);
        ylabel(yLabel,'FontSize',fontAxis);
        set(gca,'XTickLabel', methods,'FontSize',fontAxis);
        fileNames{figIdx} = 'avgCmplt';
    end
end 

if ~simulation & false
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');   
%     bar([ClusterAvg' SimAvgCmplt(1:5)'], 'group');
    bar([ClusterAvg' SimAvg'], 'group');
    xLabel=strMethods;
    yLabel='seconds';
    legendStr=methods;
    xlim([0.6 length(ClusterAvg)+0.4]);
%     ylim([0 6000]);
    legend('cluster', 'simulator');
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
    fileNames{figIdx} = 'avg_comparison';
end

if ~simulation    
    figIdx=figIdx +1;         
    scrsz = get(groot, 'ScreenSize');  
    
%     stackData = [SimWaitingAvg; ClusterWaitingAvg; SimRunningAvg; ClusterRunningAvg];
    stackData = ones(5, 2, 2);
    stackData(:,1,:) = [ClusterWaitingAvg; ClusterRunningAvg ]';
    stackData(:,2,:) = [SimWaitingAvg; SimRunningAvg ]';
    figures{figIdx}  = plotBarStackGroups(stackData, strMethods);
    
    xLabel=strMethods;
    yLabel='seconds';
    legendStr=methods;
    xlim([0.6 length(ClusterAvg)+0.4]);
%     ylim([0 6000]);
    legend({'[cluster] wait','[cluster] run', '[simulator] wait', '[simulator] run'},'Location', 'northeastoutside','Orientation','vertical');
    set (gcf, 'Units', 'Inches', 'Position', figureSize .* [1 1 1.3 1], 'PaperUnits', 'inches', 'PaperPosition', figureSize.* [1 1 1.3 1]);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
    fileNames{figIdx} = 'avg_comparison_group';
end

%%
if plots(2)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');  
   
   maxEndTime = max(endTimes{SRPTId}); % AlloX
    hold on
    normMakespan = -1;
    for i = 1:length(methods)
        numJobs = sum(endTimes{i} <= maxEndTime);    
        h=bar(i, numJobs , barWidth);
%         set(h,'FaceColor', methodColors{i});
    end
    hold off

    xLabel=strMethods;
    yLabel= ['job completed'];
    legendStr=methods;
    xlim([0.6 length(methods)+0.4]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
%     xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods ,'FontSize',fontAxis);
    fileNames{figIdx} = 'job_completed';
end

%%
if plots(3) 
   STOP_TIME = 300;
   figIdx=figIdx +1;         [~, DRFSortIds] = sort(JobIds{DRFId});
    [~, ESSortIds] = sort(JobIds{ESId});
    [~, AlloXSortIds] = sort(JobIds{AlloXId});
    
    DRFQueues = queueNames{DRFId}(DRFSortIds);
    ESQueues = queueNames{ESId}(ESSortIds);
    AlloXQueues = queueNames{AlloXId}(AlloXSortIds);
    
    ESTotalCompltTime = [];
    DRFTotalCompltTime = [];
    AlloXTotalCompltTime = [];
    
    ESDurations = durations{ESId}(ESSortIds);
    DRFDurations = durations{DRFId}(DRFSortIds);
    AlloXDurations = durations{AlloXId}(AlloXSortIds);   
    
    queueSet = {};
    for i=1:length(ESQueues)
        queueName = ESQueues{i};
        idx = 0;        
        if ~any(strcmp(queueSet,queueName))
            queueSet{length(queueSet)+1} = queueName;
            idx = length(queueSet);
            ESTotalCompltTime = [ESTotalCompltTime  ESDurations(i)];
            DRFTotalCompltTime = [DRFTotalCompltTime  DRFDurations(i)];
            AlloXTotalCompltTime = [AlloXTotalCompltTime  AlloXDurations(i)];
        else            
            idx = strcmp(queueSet,queueName);
            ESTotalCompltTime(idx) = ESTotalCompltTime(idx) + ESDurations(i);
            DRFTotalCompltTime(idx) = DRFTotalCompltTime(idx) + DRFDurations(i);
            AlloXTotalCompltTime(idx) = AlloXTotalCompltTime(idx) + AlloXDurations(i);
        end        
    end
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');  
   
    hold on
    for i = 1:length(methods)
        jobCompleted =  sum(endTimes{i}(:)<STOP_TIME);
        h=bar(i, jobCompleted, barWidth);
        set(h,'FaceColor', methodColors{i});
    end
    hold off

    xLabel=strMethods;
    yLabel='job completed';
    legendStr=methods;
    xlim([0.6 length(methods)+0.4]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods ,'FontSize',fontAxis);
    fileNames{figIdx} = 'job_completed';
end

%%
if plots(4)    
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');     
    hold on

    [~, DRFSortIds] = sort(JobIds{DRFId});
    [~, ESSortIds] = sort(JobIds{ESId});
    [~, AlloXSortIds] = sort(JobIds{AlloXId});
    [~, DRFExtSortIds] = sort(JobIds{DRFExtId});
    
    slowDownVals = (durations{AlloXId}(AlloXSortIds) - durations{ESId}(ESSortIds))./durations{ESId}(ESSortIds)*100;
%     slowDownVals = max (slowDownVals, 0);
    [f,x]=ecdf(slowDownVals);
    plot(x,f, 'LineWidth',LineWidth);
    hold on;
    slowDownVals = (durations{AlloXId}(AlloXSortIds) - durations{DRFId}(DRFSortIds))./durations{DRFId}(DRFSortIds)*100;
%     slowDownVals = max (slowDownVals, 0);
    [f,x]=ecdf(slowDownVals);
    plot(x,f, 'LineWidth',LineWidth);
    hold on;
    slowDownVals = (durations{AlloXId}(AlloXSortIds) - durations{DRFExtId}(DRFExtSortIds))./durations{DRFExtId}(DRFExtSortIds)*100;
%     slowDownVals = max (slowDownVals, 0);
    [f,x]=ecdf(slowDownVals);
    plot(x,f, 'LineWidth',LineWidth);

%     xLabel='slowdown (s)';
    xLabel='slowdown (%)';
    yLabel=strCdf;    
    xlim([-100 200]);
    
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    legendStr = {'vs. ES+RP', 'vs. DRF', 'vs. DRFExt'};
    legend(legendStr,'Location','best','FontSize',fontLegend,'Orientation','vertical');
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
    fileNames{figIdx} = 'slowdown';
end
%%
if plots(5)
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');     
    hold on;

    [~, DRFSortIds] = sort(JobIds{DRFId});
    [~, ESSortIds] = sort(JobIds{ESId});
    [~, AlloXSortIds] = sort(JobIds{AlloXId});
    
    DRFQueues = queueNames{DRFId}(DRFSortIds);
    ESQueues = queueNames{ESId}(ESSortIds);
    AlloXQueues = queueNames{AlloXId}(AlloXSortIds);
    
    ESTotalCompltTime = [];
    DRFTotalCompltTime = [];
    AlloXTotalCompltTime = [];
    
    ESDurations = durations{ESId}(ESSortIds);
    DRFDurations = durations{DRFId}(DRFSortIds);
    AlloXDurations = durations{AlloXId}(AlloXSortIds);
    
    
    queueSet = {};
    for i=1:length(ESQueues)
        queueName = ESQueues{i};
        idx = 0;        
        if ~any(strcmp(queueSet,queueName))
            queueSet{length(queueSet)+1} = queueName;
            idx = length(queueSet);
            ESTotalCompltTime = [ESTotalCompltTime  ESDurations(i)];
            DRFTotalCompltTime = [DRFTotalCompltTime  DRFDurations(i)];
            AlloXTotalCompltTime = [AlloXTotalCompltTime  AlloXDurations(i)];
        else            
            idx = strcmp(queueSet,queueName);
            ESTotalCompltTime(idx) = ESTotalCompltTime(idx) + ESDurations(i);
            DRFTotalCompltTime(idx) = DRFTotalCompltTime(idx) + DRFDurations(i);
            AlloXTotalCompltTime(idx) = AlloXTotalCompltTime(idx) + AlloXDurations(i);
        end        
    end
    
    slowdown = (AlloXTotalCompltTime - ESTotalCompltTime)./ESTotalCompltTime * 100;    
    slowdown2 = (AlloXTotalCompltTime - DRFTotalCompltTime)./DRFTotalCompltTime * 100;    
    slowDownVals = (durations{AlloXId}(AlloXSortIds) - durations{ESId}(ESSortIds))./durations{ESId}(ESSortIds)*100;
    
    [f,x]=ecdf(slowdown);
    plot(x,f, 'LineWidth',LineWidth);
    hold on;    
    [f,x]=ecdf(slowdown2);
    plot(x,f, 'LineWidth',LineWidth);

    xLabel='degradation (%)';
    yLabel=strCdf;    
    xlim([-100 100]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    legendStr = {'vs. ES+RP', 'vs. DRF'};
    legend(legendStr,'Location','best','FontSize',fontLegend,'Orientation','vertical');
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
    fileNames{figIdx} = 'degradation';
end

%%
if plots(5)
    figIdx=figIdx +1;         
    figures{figIdx} = figure;
    scrsz = get(groot, 'ScreenSize');     
    hold on;

    [~, DRFSortIds] = sort(JobIds{DRFId});
    [~, ESSortIds] = sort(JobIds{ESId});
    [~, AlloXSortIds] = sort(JobIds{AlloXId});
    
    DRFQueues = queueNames{DRFId}(DRFSortIds);
    ESQueues = queueNames{ESId}(ESSortIds);
    AlloXQueues = queueNames{AlloXId}(AlloXSortIds);
    
    ESTotalCompltTime = [];
    DRFTotalCompltTime = [];
    AlloXTotalCompltTime = [];
    
    ESDurations = durations{ESId}(ESSortIds);
    DRFDurations = durations{DRFId}(DRFSortIds);
    AlloXDurations = durations{AlloXId}(AlloXSortIds);    
    
    queueSet = {};
    for i=1:length(ESQueues)
        queueName = ESQueues{i};
        idx = 0;        
        if ~any(strcmp(queueSet,queueName))
            queueSet{length(queueSet)+1} = queueName;
            idx = length(queueSet);
            ESTotalCompltTime = [ESTotalCompltTime  ESDurations(i)];
            DRFTotalCompltTime = [DRFTotalCompltTime  DRFDurations(i)];
            AlloXTotalCompltTime = [AlloXTotalCompltTime  AlloXDurations(i)];
        else            
            idx = strcmp(queueSet,queueName);
            ESTotalCompltTime(idx) = ESTotalCompltTime(idx) + ESDurations(i);
            DRFTotalCompltTime(idx) = DRFTotalCompltTime(idx) + DRFDurations(i);
            AlloXTotalCompltTime(idx) = AlloXTotalCompltTime(idx) + AlloXDurations(i);
        end        
    end
    
    slowdown = (AlloXTotalCompltTime - ESTotalCompltTime)./ESTotalCompltTime * 100;    
    slowdown2 = (AlloXTotalCompltTime - DRFTotalCompltTime)./DRFTotalCompltTime * 100;    
    slowDownVals = (durations{AlloXId}(AlloXSortIds) - durations{ESId}(ESSortIds))./durations{ESId}(ESSortIds)*100;
    
    [f,x]=ecdf(slowdown);
    plot(x,f, 'LineWidth',LineWidth);
    hold on;    
    [f,x]=ecdf(slowdown2);
    plot(x,f, 'LineWidth',LineWidth);

    xLabel='degradation (%)';
    yLabel=strCdf;    
    xlim([-100 100]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    legendStr = {'vs. ES+RP', 'vs. DRF'};
    legend(legendStr,'Location','best','FontSize',fontLegend,'Orientation','vertical');
    xlabel(xLabel,'FontSize',fontAxis);
    ylabel(yLabel,'FontSize',fontAxis);    
    fileNames{figIdx} = 'degradation';
end

%%
USER_ID = 1;
fairVals = zeros(length(methods),3);
if plots(6)
    figIdx = figIdx + 1;
    figures{figIdx} = figure;
    % compute data
   
    maxTime = 4000;
   
    arrivals = startTimes{1};    
    [arrivals, ids] = sort(arrivals);    
    xVals = 0:sample:max(arrivals);
    jobArrivals = [];
    avgComplts = [];
    for i = 1:length(xVals)-1        
        ids  = find(arrivals >= xVals(i) & arrivals < xVals(i+1));
        jobArrivals(i) = length(ids);                
    end
    jobArrivals(i+1) = 0;
    
    
    for j=1:length(methods)
        arrivals = startTimes{j};    
        [arrivals, ids] = sort(arrivals);    
        for i = 1:length(xVals)-1        
            ids  = find(arrivals >= xVals(i) & arrivals < xVals(i+1));
            durs = durations{j}(ids);
            if (length(durs) == 0)
                avgComplts(i,j)  = NaN;
            else
%                 avgComplts(i,j)  = max(durs);
                avgComplts(i,j)  = mean(durs);
            end     
            
        end
        avgComplts(i+1,j) = 0;    
    end    
    
    %% plot demand
    
    subplot(4,1,1);   
    plot(xVals, jobArrivals,'LineWidth', lineWidth*0.75);
    ylabel('number of jobs','FontSize', fontAxis);
    xlabel(strSimTime,'FontSize', fontAxis);
    xlim([0 maxTime]);
    ylim([0 max(jobArrivals)*1.3]);
    legend({'job arrivals'},'FontSize', fontLegend,'Location','best','Orientation','horizontal');
    
    % plot performance
    subplot(4,1,2);  
    hold on;
    yMax = 0;
    for j=1:length(plotOrders)
        id = plotOrders(j);        
        if id~= AlloXId
            lineW = lineWidth*0.5;
            plot(xVals, avgComplts(:,id),'-.','LineWidth', lineW);
        else
            lineW = lineWidth;
            plot(xVals, avgComplts(:,id),'b-','LineWidth', lineW);
        end
        
        yMax = max(yMax, max(avgComplts(:,id)));
    end    
    hold off;
    xlabel(strSimTime,'FontSize', fontAxis);
    ylabel(strMaxCmplt, 'FontSize', fontAxis);
    xlim([0 maxTime]);
    ylim([0 yMax*1.3]);
%     ylim([0 2000]);
%     set(gca, 'YScale', 'log');
%     legend(methods(plotOrders),'FontSize', fontLegend, 'Location', 'north', 'Orientation', 'horizontal');
    
    % plot fair score
    num_time_steps = maxTime-0;
    subplot(4,1,3); 
    hold on;
    for i=1:length(plotOrders)  
     iFile = plotOrders(i);
     logFile = [ logfolder files{iFile} '-output' extraStr  '.csv'];
     [queueNames, res1, res2, res3, fairScores, nJobs, nQueuedJobs, flag] = importResUsageLog(logFile);
     if (flag)             
        resAll = zeros(1,queue_num*num_time_steps);
        resAll2 = zeros(1,queue_num*num_time_steps);
        
        temp = fairScores(1:length(fairScores));
        temp2 = nJobs(1:length(nJobs));
%         temp2 = nQueuedJobs(1:length(nQueuedJobs));
        if(length(resAll)>length(temp))
           resAll(1:length(temp)) = temp;
           resAll2(1:length(temp2)) = temp2;
        else
           resAll = temp(1:queue_num*num_time_steps);
           resAll2 = temp2(1:queue_num*num_time_steps);
        end
        fairScoreUsers = reshape(resAll, queue_num, num_time_steps);
        nJobsUsers = reshape(resAll2, queue_num, num_time_steps);
        
%         fairScores = fairScoreUsers(USER_ID, :)';
        yVals = zeros(1,num_time_steps);
        for iTime = 1:num_time_steps
            temp = nJobsUsers(:,iTime);
            if (sum(temp>0) > 0)
%                 yVals(iTime) = std(fairScoreUsers(temp>0,iTime))/mean(fairScoreUsers(temp>0,iTime));
                yVals(iTime) = std(fairScoreUsers(temp>0,iTime));
%                 yVals(iTime) = var(fairScoreUsers(temp>0,iTime));
%                 yVals(iTime) = std(fairScoreUsers(:,iTime));                   
            end
            
        end
%         yVals = prctile(yVals,95)*ones(1,num_time_steps);
        fairVals(iFile, 1) = mean(yVals);
        fairVals(iFile, 2) = prctile(yVals,95);
        fairVals(iFile, 3) = prctile(yVals,99);
        if iFile~= AlloXId
            lineW = lineWidth*0.5;
            plot(1:num_time_steps, yVals ,'-.','LineWidth', lineW);
        else
            
            lineW = lineWidth;
            plot(1:num_time_steps, yVals,'b-','LineWidth', lineW);
        end
             
     end
    end    
    hold off;
    ylabel('fair. std', 'FontSize', fontAxis);
    xlim([0 maxTime]);
    %
    subplot(4,1,4); 
    hold on;
    for i=1:length(plotOrders)  
        iFile = plotOrders(i);	
        if iFile~= AlloXId
            lineW = lineWidth*0.5;
            plot(0, 0 ,'-.','LineWidth', lineW);
        else
            lineW = lineWidth;
            plot(0, 0,'b-','LineWidth', lineW);
        end
    end    
    hold off;    
    axis off;
    legend(methods(plotOrders),'FontSize', fontLegend, 'Location', 'northoutside', 'Orientation', 'horizontal');
    
    %
    set (gcf, 'Units', 'Inches', 'Position', figureSize.*[1 1 1.8 2.5], 'PaperUnits', 'inches', 'PaperPosition', figureSize.*[1 1 1.8 2.5]);
    fileNames{figIdx} = 'perf_overtime';    
end

if plots(6)   
    figIdx=figIdx +1;  
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');   
   
    bar(fairVals(:,1:2), 'group');
    
    xLabel=strMethods;
    yLabel='std. of fairness score';
    legendStr={'mean','95-percentile'};
    legend(legendStr, 'Location','best','FontSize', fontLegend);
    xlim([0.6 length(methods)+0.4]);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
    
%     axes('position',[.35 .35 0.32 0.4])
%     box on % put box around new pair of axes
%     plotBarStackGroups(stackData(3:end,:,:), methods(3:end), false);
%     axis tight

    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);

    fileNames{figIdx} = 'fairness';
end


%%
if plots(7)    
   figIdx=figIdx +1;         
   figures{figIdx} = figure;
   scrsz = get(groot, 'ScreenSize');   
   
    hold on
    for i = 1:length(methods)
        temp = sort(durations{i}, 'descend');
        top5Percent = temp(1:ceil(end*0.05));
        tailPerf(i) = mean(top5Percent);    
    end
    h=bar(1:length(methods), tailPerf, barWidth , 'stacked');
    hold off
    improvement = (tailPerf-tailPerf(AlloXId))./tailPerf * 100
    xLabel=strMethods;
    yLabel=strAvgCmplt;
    xlim([0.6 length(methods)+0.4]);
    set (gcf, 'Units', 'Inches', 'Position', figureSize, 'PaperUnits', 'inches', 'PaperPosition', figureSize);
    ylabel(yLabel,'FontSize',fontAxis);
    set(gca,'XTickLabel', methods,'FontSize',fontAxis);
    fileNames{figIdx} = 'tail_performance';
end
version
%%
if~is_printed
    return;
else
   pause(1);
end
%%
for i=1:length(fileNames)
    fileName = fileNames{i};
    epsFile = [ LOCAL_FIG fileName '.eps'];
    print (figures{i}, '-depsc', epsFile);    
    pdfFile = [ fig_path fileName EXTRA '.pdf']
    cmd = sprintf(PS_CMD_FORMAT, epsFile, pdfFile);
    status = system(cmd);
end