clear all; close all; clc;

numOfStages = 600;
numDimemsion = 6;

minVal = -1;
maxVal = 1;
stdev = 0.1;
meanVal = 0;

%%
y = stdev.*randn(numOfStages, numDimemsion) + meanVal;
% y = stdev.*trandn(minVal*ones(numOfStages, numDimemsion), maxVal*ones(numOfStages, numDimemsion));

y = max(y,minVal);
y = min(y,maxVal);

mean(y)
std(y)

errors = '{';
for i=1:numOfStages
  jobDemand = '{';
  for j=1:numDimemsion
    jobDemand = [jobDemand  num2str(y(i,j)) ','];    
  end
  jobDemand = [jobDemand '}'];
  errors = [errors jobDemand ',']; 
end
errors = [errors '};'];

fid=fopen('err.txt','w');

fprintf(fid, errors);

fprintf(fid, '\n');

%%

y = stdev.*randn(numOfStages, 1) + meanVal;

y = max(y,minVal);
y = min(y,maxVal);

mean(y)
std(y)

errors = '{';
for i=1:numOfStages
  errors = [errors num2str(y(i)) ',']; 
end
errors = [errors '}'];

fid=fopen('err.txt','a');
fprintf(fid, errors);
