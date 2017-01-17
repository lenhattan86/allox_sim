package queue.schedulers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cluster.datastructures.BaseDag;
import cluster.datastructures.JobQueue;
import cluster.datastructures.Resource;
import cluster.datastructures.Resources;
import cluster.datastructures.Session;
import cluster.datastructures.StageDag;
import cluster.simulator.Simulator;
import cluster.simulator.Main.Globals;
import cluster.utils.JobArrivalComparator;
import cluster.utils.Output;

public class SpeedFairScheduler implements Scheduler {
  private boolean DEBUG = false;
  private boolean SCHEDULING_OVERHEADS = false;

  private Queue<JobQueue> admittedBurstyQueues = null;
//  private Queue<JobQueue> admittedBatchQueues = null;
  private Queue<JobQueue> bestEffortQueues = null;
  private Queue<JobQueue> elasticQueues = null;

  private String schedulePolicy;

  Resource clusterTotCapacity = null;

  public SpeedFairScheduler() {
    clusterTotCapacity = Simulator.cluster.getClusterMaxResAlloc();
    schedulePolicy = "SpeedFair";
    admittedBurstyQueues = new LinkedList<JobQueue>();
//    admittedBatchQueues = new LinkedList<JobQueue>();
    bestEffortQueues = new LinkedList<JobQueue>();
    elasticQueues = new LinkedList<JobQueue>();
  }

  @Override
  public void computeResShare() {
    periodicSchedule();
  }

  private void periodicSchedule() {
    if (Simulator.CURRENT_TIME >= Globals.DEBUG_START
        && Simulator.CURRENT_TIME <= Globals.DEBUG_END) {
      DEBUG = false;
    } else
      DEBUG = false;
    // update queue status
    Output.debugln(DEBUG,
        "\n==== STEP_TIME:" + Simulator.CURRENT_TIME + " ====");

    updateQueueStatus();

    admit();
    
    allocate();

    // update resources
    for (JobQueue q : Simulator.QUEUE_LIST.getJobQueues()) {
      q.addResourcesList(q.getResourceUsage());
    }
  }

  private void allocateSpareResources() {
    // allocate the spare resource
    Resource remainingResources = Resources
        .clone(Simulator.cluster.getClusterResAvail());
    DRFScheduler.onlineDRFShare(remainingResources, (List) bestEffortQueues);
  }

  private void allocate() {
//    if (Simulator.CURRENT_TIME == 964.0)
//      DEBUG = true;
    
    Resource avaiRes = Simulator.cluster.getClusterResAvail();
    
    Resource maxResource = Simulator.cluster.getClusterMaxResAlloc();
    boolean enableCompensation = true;
    // hard guarantee
    for (JobQueue q : admittedBurstyQueues) {
      Resource gRes = getBurstyGuarantee(q, enableCompensation);
      
      Resource moreRes = Resources.subtractPositivie(gRes, q.getResourceUsage());
      moreRes = Resources.piecewiseMin(moreRes, avaiRes);
      Resource remain = q.assign(moreRes);
      // assign the task
      Resource rsrcQuota = null;
      rsrcQuota = Resources.subtract(gRes, remain);
      moreRes = Resources.subtract(moreRes, remain);
      q.setRsrcQuota(rsrcQuota);
      avaiRes = Resources.subtractPositivie(avaiRes, moreRes);
      Output.debugln(DEBUG, "[SpeedFairScheduler] [allocate] "
          + q.getQueueName() + ": " + rsrcQuota);
      
//      gRes = Resources.piecewiseMin(gRes, moreRes);
      maxResource = Resources.subtractPositivie(maxResource, q.getInStage1Alpha(Simulator.CURRENT_TIME));
    }
    // soft guarantee
    
    for (JobQueue q : bestEffortQueues) { // q just be TQ
      if (!q.isLQ){
        System.err.println("queue "+q.getQueueName() + " is not TQ.");
      }
      
      Resource gRes = getBurstyGuarantee(q, true);
      
      if(gRes.fitsIn(maxResource)){
        Resource moreRes = Resources.subtractPositivie(gRes, q.getResourceUsage());
        moreRes = Resources.piecewiseMin(moreRes, avaiRes);
        Resource remain = q.assign(moreRes);
        // assign the task
        Resource rsrcQuota = null;
        rsrcQuota = Resources.subtract(gRes, remain);
        moreRes = Resources.subtract(moreRes, remain);
        q.setRsrcQuota(rsrcQuota);
        avaiRes = Resources.subtractPositivie(avaiRes, moreRes);
        Output.debugln(DEBUG, "[SpeedFairScheduler] [allocate] "
            + q.getQueueName() + ": " + rsrcQuota);
        
//        gRes = Resources.piecewiseMin(gRes, moreRes);
        
        maxResource = Resources.subtractPositivie(maxResource, q.getInStage1Alpha(Simulator.CURRENT_TIME));
      } else {
        elasticQueues.add(q);
      }
    }
    
    // spare resouce allocation
//    elasticQueues.addAll(admittedBatchQueues);
    Resource remainingResources = Resources.clone(avaiRes);
    if (remainingResources.distinct(Resources.ZEROS))
      DRFScheduler.onlineDRFShare(remainingResources,
          (List) elasticQueues);
  }

  private Resource getBurstyGuarantee(JobQueue q) {
    return getBurstyGuarantee(q, true);
  }
  
  private Resource getBurstyGuarantee(JobQueue q, boolean enableCompensation) {
    int numQueues = elasticQueues.size()
        + admittedBurstyQueues.size() + bestEffortQueues.size();
    Resource res = new Resource();
    Session s = q.getCurrSession(Simulator.CURRENT_TIME);
    if (s == null)
      System.err.println(q.getQueueName());
    
    Resource alpha = s.getAlpha();
    Resource guaranteedRes = Resources.multiply(alpha, s.getAlphaDuration());
    double lasting = (Simulator.CURRENT_TIME - q.getCurrSessionStartTime())
        % s.getPeriod();
    boolean inStage1 = lasting <= s.getAlphaDuration();
    Resource receivedRes = q.getReceivedRes(lasting);
    boolean isGuaranteed = receivedRes.greaterOrEqual(guaranteedRes);
    if (inStage1 || (!isGuaranteed && enableCompensation))
//    if (inStage1)
      res = alpha;
    else {
      Resource nom = Resources.multiply(clusterTotCapacity,
          s.getPeriod()
              / (numQueues));
      nom = Resources.subtractPositivie(nom, receivedRes);
      Resource beta = Resources.divide(nom,
          (s.getPeriod() - s.getAlphaDuration()));
      res = beta;
    }
    return res;
  }

  /*
   * private Resource getBurstyGuaranteePreemption(JobQueue q){ Resource res =
   * new Resource(); Resource alpha = q.getAlpha(Simulator.CURRENT_TIME);
   * Resource nom = Resources.multiply(clusterTotCapacity,
   * q.getPeriod()/(admittedBatchQueues.size()+admittedBurstyQueues.size()));
   * nom = Resources.subtract(nom, Resources.multiply(alpha,
   * q.getStage1Duration(Simulator.CURRENT_TIME))); Resource beta =
   * Resources.divide(nom,
   * (q.getPeriod()-q.getStage1Duration(Simulator.CURRENT_TIME))); if
   * (Simulator.CURRENT_TIME % Globals.PERIODIC_INTERVAL <=
   * q.getStage1Duration(Simulator.CURRENT_TIME)) res = alpha; else res = beta;
   * return res; }
   */

  private boolean resGuarateeCond(JobQueue newQueue) {
    Session currSession = newQueue.getCurrSession(Simulator.CURRENT_TIME);
    if (currSession == null)
      System.err.println(newQueue.getQueueName());
    int numQueues = elasticQueues.size()
        + admittedBurstyQueues.size()+bestEffortQueues.size();
    for (int j = 0; j < currSession.getNumOfJobs(); j++) {
      double currTime = currSession.getStartTime()
          + j * currSession.getPeriod();
      Resource alpha = currSession.getAlpha();
      for (double t = currTime; t < currTime
          + currSession.getAlphaDuration(); t += Globals.STEP_TIME) {
        Resource burstyRes = new Resource(Resources.ZEROS);
        for (JobQueue q : admittedBurstyQueues) {
          burstyRes.addWith(q.getGuaranteeRate(t));
        }
        boolean result = alpha.smallerOrEqual(
            Resources.subtractPositivie(clusterTotCapacity, burstyRes));

        if (!result)
          return result;
      }
    }
    return true;
  }
  
  private boolean resShortGuaranteeCond(JobQueue newQueue) {
    Session currSession = newQueue.getCurrSession(Simulator.CURRENT_TIME);
    if (currSession == null)
      System.err.println(newQueue.getQueueName());
//    double currTime = currSession.getStartTime();
    double startPeriodTime = currSession.getStartPeriodTime(Simulator.CURRENT_TIME);
    Resource alpha = currSession.getAlpha();
    Queue<JobQueue> admittedQueues = new LinkedList<JobQueue>();
    admittedQueues.addAll(admittedBurstyQueues);
    admittedQueues.addAll(bestEffortQueues);
    for (double t = startPeriodTime; t < startPeriodTime
        + currSession.getAlphaDuration(); t += Globals.STEP_TIME) {
      Resource burstyRes = new Resource(Resources.ZEROS);
      for (JobQueue q : admittedQueues) {
        burstyRes.addWith(q.getGuaranteeRate(t));
      }
      boolean result = alpha.smallerOrEqual(
          Resources.subtractPositivie(clusterTotCapacity, burstyRes));

      if (!result)
        return result;
    }
    return true;
  }
  
  private boolean resShortTermFairness(JobQueue newQueue) {
    
    Session currSession = newQueue.getCurrSession(Simulator.CURRENT_TIME);
    double period = newQueue.getCurrSession().getPeriod();
    double t=0.0;
    for (t=0.0; t<period; t++){
      int numQueues = 0;
      double currTime = Simulator.CURRENT_TIME+t;
      for (JobQueue q:elasticQueues){
        if (q.isActive(currTime) && !q.equals(newQueue))
          numQueues++;
      }
      
      for (JobQueue q:admittedBurstyQueues){
        if (q.isActive(currTime))
          numQueues++;
      }
      
      for (JobQueue q:bestEffortQueues){
        if (q.isActive(currTime))
          numQueues++;
      }
      
      Resource alpha = currSession.getAlpha();
      Resource lhs = Resources.multiply(alpha, currSession.getAlphaDuration());
      Resource rhs = Resources.multiply(clusterTotCapacity,
          currSession.getPeriod());
      double denom = numQueues + 1;
      rhs.divide(denom);
  
      if (!lhs.smallerOrEqual(rhs))
        return false;
    }
    
    return true;
  }

  private boolean resLongTermFairnessCond(JobQueue newQueue) {
    Session currSession = newQueue.getCurrSession(Simulator.CURRENT_TIME);
    int numQueues = 0;
    
    for (JobQueue q:elasticQueues){
      if (!q.equals(newQueue))
        numQueues++;
    }
    
    numQueues += admittedBurstyQueues.size();
    numQueues += bestEffortQueues.size();
    
    // TODO: do not need the loop.
    for (int j = 0; j < currSession.getNumOfJobs(); j++) {
      Resource alpha = currSession.getAlpha();
      Resource lhs = Resources.multiply(alpha, currSession.getAlphaDuration());
      Resource rhs = Resources.multiply(clusterTotCapacity,
          currSession.getPeriod());
      double denom = numQueues + 1;
      rhs.divide(denom);

      if (!lhs.smallerOrEqual(rhs))
        return false;
    }
    return true;
  }

  private void admit() {
//    if(Simulator.CURRENT_TIME==250.0)
//      DEBUG = true;
    
    long tStart = System.currentTimeMillis();
    Queue<JobQueue> newAdmittedQueues = new LinkedList<JobQueue>();
    for (JobQueue q : elasticQueues) {
      if (q.isLQ && q.hasRunningJobs()) {
        boolean condition1 = resGuarateeCond(q);
        boolean condition2 = resLongTermFairnessCond(q);

        int sId = q.getCurrSessionIdx(Simulator.CURRENT_TIME);
        if (condition1 && condition2) {
          admittedBurstyQueues.add(q);
          newAdmittedQueues.add(q);
          Output.debugln(DEBUG, "[SpeedFairScheduler] admitted session " + sId
              + " of " + q.getQueueName() + " to hardGuaranteeQueues at " + Simulator.CURRENT_TIME);
        } else {
          boolean condition3 = resShortGuaranteeCond(q);
          boolean condition4 = resShortTermFairness(q);
//          if(condition2){
          if(condition3 & condition4){
            bestEffortQueues.add(q);
            Output.debugln(DEBUG, "[SpeedFairScheduler] admit session "
                + sId + " of " + q.getQueueName() + "  to softGuaranteeQueues at " + Simulator.CURRENT_TIME);
          } else {
            Output.debugln(DEBUG, "[SpeedFairScheduler] admit session "
                + sId + " of " + q.getQueueName() + "  to elasticQueues at " + Simulator.CURRENT_TIME);
          }
        }
      } 
    }
    elasticQueues.removeAll(newAdmittedQueues);
    DEBUG = false;
    long overheads = System.currentTimeMillis() - tStart;
    
    if (SCHEDULING_OVERHEADS)
      System.out.println(
          "Admit takes: " + overheads + " ms at " + Simulator.CURRENT_TIME);
  }

  private void updateQueueStatus() {
    elasticQueues.clear();
    
    Queue<JobQueue> temp = new LinkedList<JobQueue>();
    for (JobQueue q : admittedBurstyQueues) {
      if (!q.isActive())
        temp.add(q);
    }
    admittedBurstyQueues.removeAll(temp);
    
/*    temp.clear();
    for (JobQueue q : bestEffortQueues) {
      if (!q.isActive())
        temp.add(q);
    }
    bestEffortQueues.removeAll(temp);*/
    bestEffortQueues.clear();
    
    for (JobQueue q : Simulator.QUEUE_LIST.getRunningQueues()) {
      if (admittedBurstyQueues.contains(q)
          || bestEffortQueues.contains(q) || !q.isActive()) {
        // do nothing
      } else {
        elasticQueues.add(q);
      }
    }
  }

  public void fifoShareForJobs(JobQueue q, Resource availRes) {
    boolean fit = availRes.greaterOrEqual(q.getRsrcQuota());
    if (!fit) {
      Resource newQuota = Resources.piecewiseMin(availRes, q.getRsrcQuota());
      q.setRsrcQuota(newQuota);
    }
    q.receivedResourcesList.add(q.getRsrcQuota());

    Resource remain = q.getRsrcQuota();
    List<BaseDag> runningJobs = new LinkedList<BaseDag>(q.getRunningJobs());
    Collections.sort(runningJobs, new JobArrivalComparator());
    for (BaseDag job : runningJobs) {
      Resource rsShare = Resources.piecewiseMin(remain, job.getMaxDemand());
      job.rsrcQuota = rsShare;
      remain.subtract(rsShare);
      Output.debugln(DEBUG, "[SpeedFairScheduler] Allocated to job:" + job.dagId
          + " @ " + job.getQueueName() + " " + job.rsrcQuota);
    }
    availRes = Resources.subtract(availRes, q.getRsrcQuota());
  }

  @Override
  public String getSchedulePolicy() {
    return schedulePolicy;
  }
}
