function [JobId, startTime, endTime, duration, queueName, startRunningTimes, runningTimes] = import_compl_time(filename, startRow, endRow)
%IMPORTFILE Import numeric data from a text file as column vectors.
%   [JOBID,STARTTIME,ENDTIME,DURATION,QUEUENAME] = IMPORTFILE(FILENAME)
%   Reads data from text file FILENAME for the default selection.
%
%   [JOBID,STARTTIME,ENDTIME,DURATION,QUEUENAME] = IMPORTFILE(FILENAME,
%   STARTROW, ENDROW) Reads data from rows STARTROW through ENDROW of text
%   file FILENAME.
%
% Example:
%   [JobId,startTime,endTime,duration,queueName] = importfile('DRF-output19_1_20.csv',2, 21);
%
%    See also TEXTSCAN.

% Auto-generated by MATLAB on 2016/09/27 10:52:16

%% Initialize variables.
delimiter = ',';
if nargin<=2
   startRow = 2;
   endRow = inf;
end

%% Format string for each line of text:
%   column1: double (%f)
%	column2: double (%f)
%   column3: double (%f)
%	column4: double (%f)
%   column5: text (%s)
%   column6: text (%f)
% For more information, see the TEXTSCAN documentation.
formatSpec = '%f%f%f%f%s%f%f%[^\n\r]';

if ~exist(filename, 'file')
    
    JobId = nan;
    startTime = nan;
    endTime = nan;
    duration = nan;
    queueName = nan;
    startRunningTimes=nan;
    runningTimes = nan;
    return;
end
%% Open the text file.
fileID = fopen(filename,'r');



%% Read columns of data according to format string.
% This call is based on the structure of the file used to generate this
% code. If an error occurs for a different file, try regenerating the code
% from the Import Tool.
dataArray = textscan(fileID, formatSpec, endRow(1)-startRow(1)+1, 'Delimiter', delimiter, 'HeaderLines', startRow(1)-1, 'ReturnOnError', false);
for block=2:length(startRow)
   frewind(fileID);
   dataArrayBlock = textscan(fileID, formatSpec, endRow(block)-startRow(block)+1, 'Delimiter', delimiter, 'HeaderLines', startRow(block)-1, 'ReturnOnError', false);
   for col=1:length(dataArray)
      dataArray{col} = [dataArray{col};dataArrayBlock{col}];
   end
end

%% Close the text file.
fclose(fileID);

%% Post processing for unimportable data.
% No unimportable data rules were applied during the import, so no post
% processing code is included. To generate code which works for
% unimportable data, select unimportable cells in a file and regenerate the
% script.

%% Allocate imported array to column variable names
JobId = dataArray{:, 1};
startTime = dataArray{:, 2};
endTime = dataArray{:, 3};
duration = dataArray{:, 4};
queueName = dataArray{:, 5};
startRunningTimes = dataArray{:, 6};
runningTimes = dataArray{:, 6};
