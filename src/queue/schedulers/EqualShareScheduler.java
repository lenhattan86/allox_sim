package queue.schedulers;

import java.util.Collections;
import java.util.List;

import cluster.datastructures.BaseJob;
import cluster.datastructures.InterchangableResourceDemand;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;
import cluster.utils.Utils;

public class EqualShareScheduler implements Scheduler {
	private static boolean DEBUG = false;

	private String schedulePolicy;
	// Map<String, Resources> resDemandsQueues = null;

	static Resource clusterTotCapacity = null;

	// implementation idea:
	// 1. for every queue, compute it's total resource demand vector

	public EqualShareScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "ES";
	}

	@Override
	public void computeResShare() {

		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}

		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			Collections.sort((List<BaseJob>) q.getRunningJobs(), new JobArrivalComparator());
		}
		
		// equal share all resources
		equallyAllocate(clusterTotCapacity, Simulator.QUEUE_LIST.getRunningQueues());
	}
	
	public static void equallyAllocate(Resource resCapacity, List<JobQueue> runningQueues) {
	  
	  if(runningQueues.isEmpty()) return;
	  
	  int numOfQueues = runningQueues.size();
    // retrieved current usage
    Resource[] userShareArr = new Resource[runningQueues.size()];
    int i = 0;
    for (JobQueue queue : runningQueues) {
      Resource normalizedShare = Resources.divideVector(queue.getResourceUsage(),
          Simulator.cluster.getClusterMaxResAlloc());
      userShareArr[i] = Resources.divide(normalizedShare, queue.getWeight());
      i++;
    }
    
    // step 1: compute equal share
    Resource equalShares = Resources.divide(resCapacity, numOfQueues);
    
    // step 2: allocate the share
    for (JobQueue q:runningQueues){
      Resource allocRes = q.getResourceUsage();
      boolean jobAvail = true;
      while(jobAvail) {
        BaseJob unallocJob = q.getUnallocRunningJob();
        if(unallocJob==null) {
          jobAvail = false;
          break;
        }

        boolean isResAvail = true;
        while(isResAvail){
          // allocate resource to each task.
          int taskId = unallocJob.getCommingTaskId();
          if(taskId<0)
            break;
          Resource remainingRes = Resources.subtract(equalShares, allocRes);
          
          InterchangableResourceDemand demand = unallocJob.rsrcDemands(taskId);
          Resource gDemand = demand.convertToGPUDemand();
          Resource cDemand = demand.convertToCPUDemand();
          Resource taskDemand = null;
          boolean isCPU = false;
          if(!gDemand.fitsIn(remainingRes)) {
            if(!cDemand.fitsIn(remainingRes)){
              isResAvail = false;
              jobAvail = false;
              break;
            }else{
              taskDemand = cDemand;
              isCPU=true;
            }
          } else{
            taskDemand = gDemand;
          }
          
          boolean assigned = Simulator.cluster.assignTask(unallocJob.dagId, taskId,
              unallocJob.duration(taskId), taskDemand);
          
          if(assigned){
            allocRes = Resources.sum(allocRes, taskDemand);
            if(isCPU){
              unallocJob.isCPUUsages.put(taskId, true);
//              Output.debugln(true, "job: " + unallocJob.dagId + " task: "+taskId);
            }
              
            if (unallocJob.jobStartRunningTime<0){
              unallocJob.jobStartRunningTime = Simulator.CURRENT_TIME;
            }
          } else {
//            Output.debugln(true, "Failed to assign the resource to job " + unallocJob.dagId);
            isResAvail=false;
            jobAvail=false;
            break;
          }
        }
      }
    }   
  }

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
