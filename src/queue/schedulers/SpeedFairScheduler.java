package queue.schedulers;

import java.util.LinkedList;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resources;
import cluster.simulator.Simulator;
import cluster.utils.Output;

public class SpeedFairScheduler implements Scheduler {
	private static final boolean DEBUG = false;

	private String schedulePolicy;

	Resources clusterTotCapacity = null;

	public SpeedFairScheduler() {
		clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
		this.schedulePolicy = "SpeedFair";
	}

	// FairShare = 1 / N across all dimensions
	// N - total number of running jobs
	@Override
	public void computeResShare() {
//		if(Simulator.CURRENT_TIME>=90.0)
//			System.out.println("Stop to debug");
		Output.debugln(DEBUG, "STEP_TIME:" + Simulator.CURRENT_TIME);
		int numQueuesRuning = Simulator.QUEUE_LIST.getRunningQueues().size();
		if (numQueuesRuning == 0) {
			return;
		}
		Resources availRes = Simulator.cluster.getClusterMaxResAlloc();

		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			// compute the rsrcQuota based on the guarateed rate.
			Resources rsrcQuota = Resources.piecewiseMin(q.getMinService(Simulator.CURRENT_TIME), q.getMaxDemand());
			q.setRsrcQuota(rsrcQuota);
			availRes = Resources.subtractPositivie(availRes, q.getRsrcQuota());
			Output.debugln(DEBUG, "Allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
		}

		// Share the remaining resources
		if (availRes.greaterOrEqual(new Resources())) {
			double factor = 0.0;
			Queue<JobQueue> allocatedQueues = new LinkedList<JobQueue>();
			for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
				factor += q.getSpeedFairWeight();
				allocatedQueues.add(q);
			}
			Resources share = Resources.divide(availRes, factor);
//			while (allocatedQueues.size()>0){ //TODO utilize more resources.
			for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
				Resources res = q.getRsrcQuota();
				res.addWith(Resources.multiply(share, q.getSpeedFairWeight()));
				// compare the real demand and the fair share
				res = Resources.piecewiseMin(res, q.getMaxDemand());
				q.setRsrcQuota(res);
				availRes = Resources.subtractPositivie(availRes, res);
				allocatedQueues.remove(q);
				
				// recalculate the share.
//				if(allocatedQueues.size()>0){
//					factor = 0.0;
//					for (JobQueue qTemp : allocatedQueues) {
//						factor += qTemp.getSpeedFairWeight();
//					}
//					share = Resources.divide(availRes, factor);
//				}
			}
		}
		
		// TODO: deal with the max demand is less than the allocated share. 

		// TODO: sort queues for interactive jobs.

		// Resource admission control for the queues.
		availRes = Simulator.cluster.getClusterMaxResAlloc();
		for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
			boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
			if (!fit) {
				Resources newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
				q.setRsrcQuota(newQuota);
			}
			Output.debugln(DEBUG, "Allocated to queue:" + q.getQueueName() + " " + q.getRsrcQuota());
			q.receivedResourcesList.add(q.getRsrcQuota());

			// TODO: share the resources among the jobs in the same queue. (using
			// Fair)
			Resources rsShare = Resources.divide(q.getRsrcQuota(), q.runningJobsSize());
			Queue<BaseDag> runningJobs = q.getRunningJobs();
			for (BaseDag job : runningJobs) {
				job.rsrcQuota = rsShare;
//				Output.debugln(DEBUG, "Allocated to job:" + job.dagId + " " + job.rsrcQuota);
			}
			availRes = Resources.subtract(availRes, q.getRsrcQuota());
		}

	}

	public void computeResShare_prev() {
		int numJobsRunning = Simulator.runningJobs.size();
		if (numJobsRunning == 0) {
			return;
		}

		Resources clusterResQuotaAvail = Simulator.cluster.getClusterResQuotaAvail();

		// TODO: sort the runningJobs

		// update the resourceShareAllocated for every running job
		// assign the resources based on service curves.
		Queue<BaseDag> unhappyRunningJobs = new LinkedList<BaseDag>();
		for (BaseDag job : Simulator.runningJobs) {
			// TODO: change job.jobStartTime to job.jobStartRunningTime (dynamic for
			// each job).
			Resources guaranteedResource = job.serviceCurve.getMinReqService(Simulator.CURRENT_TIME - job.jobStartTime);
			Resources resToBeShared = Resources.subtractPositivie(guaranteedResource, job.receivedService);
			boolean fit = clusterResQuotaAvail.greater(resToBeShared);
			if (fit) {
				job.rsrcQuota = resToBeShared;
				clusterResQuotaAvail = Resources.subtract(clusterResQuotaAvail, resToBeShared);
			} else {
				job.rsrcQuota = new Resources(0.0); // unable to allocate the resources
				unhappyRunningJobs.add(job);
			}
		}

		// equally share the remaining resources to the unhappy jobs or all jobs.
		int numUnhappyJobs = unhappyRunningJobs.size();
		if (numUnhappyJobs > 0) {
			Output.debugln(DEBUG, "number of unhappy jobs: " + numUnhappyJobs);
			if (clusterResQuotaAvail.greater(new Resources(0))) {
				Resources quotaRsrcShare = Resources.divide(clusterResQuotaAvail, unhappyRunningJobs.size());
				for (BaseDag job : unhappyRunningJobs) {
					job.rsrcQuota.addRes(quotaRsrcShare);
				}
			}
		} else {
			if (clusterResQuotaAvail.greater(new Resources(0))) {
				Resources quotaRsrcShare = Resources.divide(clusterResQuotaAvail, numJobsRunning);
				for (BaseDag job : Simulator.runningJobs) {
					job.rsrcQuota.addRes(quotaRsrcShare);
				}
			}
		}

		// TODO: share the remaining resources on demand
	}

	@Override
	public String getSchedulePolicy() {
		return this.schedulePolicy;
	}
}
