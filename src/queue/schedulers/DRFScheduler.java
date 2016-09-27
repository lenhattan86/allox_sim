package queue.schedulers;

import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.utils.Output;

public class DRFScheduler implements Scheduler {
	private static final boolean DEBUG = false;

	private String schedulePolicy;

	Resources clusterTotCapacity = null;

	public DRFScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "DRF";
	}

	// FairShare = 1 / N across all dimensions
	// N - total number of running jobs
	@Override
	public void computeResShare() {
		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}
		
		double factor = 0.0;
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			factor += q.getWeight();
		}

		Resources rsrcQuota = Resources.divideNoRound(clusterTotCapacity, factor);
		// update the resourceShareAllocated for every running job
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			Resources allocRes = Resources.multiply(rsrcQuota, q.getWeight());
			q.setRsrcQuota(allocRes);
			 Output.debugln(DEBUG,"Allocated to queue:" + q.getQueueName() + " share:" + q.getRsrcQuota());
			// TODO: share the resources among the jobs in the same queue. (using Fair)
			
			Resources rsShare = Resources.divide(allocRes, q.getRunningJobs().size());
			Queue<BaseDag> runningJobs = q.getRunningJobs();
			for (BaseDag job : runningJobs) {
					job.rsrcQuota = rsShare;
					 Output.debugln(DEBUG,"Allocated to job:" + job.dagId + " share:" + job.rsrcQuota);
			}
		}
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
