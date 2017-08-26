package cluster.datastructures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cluster.simulator.Main.Globals;
import cluster.simulator.Main.Globals.Method;
import cluster.utils.JobArrivalComparator;
import cluster.utils.QueueArrivalComparator;
import cluster.simulator.Simulator;

public class JobQueueList {
	private List<JobQueue> jobQueues = null;
	private List<JobQueue> runningQueues = null; 
	
	public  List<JobQueue>  getJobQueues(){
		return jobQueues;
	}
	
	public JobQueueList(){
		jobQueues = new LinkedList<JobQueue>();
		runningQueues = new LinkedList<JobQueue>();
	}
	
	public JobQueue getJobQueue(String queueName){
		JobQueue queue = null;
		for (JobQueue q: this.jobQueues){
			if (q.getQueueName().equals(queueName)){
				queue = q;
				break;
			}
		}
		return queue;
	}
	
	public JobQueue addJobQueue(String queueName){
		JobQueue queue = getJobQueue(queueName);
		if (queue==null){
			queue = new JobQueue(queueName);
			jobQueues.add(queue);
		}
		return queue;
	}
	
	public JobQueue addJobQueue(JobQueue queue){
		JobQueue q = getJobQueue(queue.queueName);
		if (q==null){
			jobQueues.add(queue);
		}
		return queue;
	}
	
	@SuppressWarnings("unchecked")
  public void sortJobQueues(){
	  Collections.sort((List<JobQueue>) this.jobQueues, new QueueArrivalComparator());
	}
	
	public void sortRunningQueues(){
    Collections.sort((List<JobQueue>) this.runningQueues, new QueueArrivalComparator());
  }

/*	public void addRunnableJob2Queue(BaseDag newJob, String queueName) {
		JobQueue queue = getJobQueue(queueName);
		queue.addRunnableJob(newJob);
	}*/
	
	public void addRunningJob2Queue(BaseJob newJob, String queueName) {
		JobQueue queue = getJobQueue(queueName);
		queue.addRunningJob(newJob);
//		queue.removeRunnableJob(newJob);
	}
	
	public void addCompletionJob2Queue(BaseJob newJob, String queueName) {
		JobQueue queue = getJobQueue(queueName);
		queue.removeRunningJob(newJob);
		queue.addCompletedJob(newJob);
	}
	
	public void addRunningQueue(JobQueue runningQueue){
		if (runningQueue.isLQ)
			this.runningQueues.add(0,runningQueue);
		else
			this.runningQueues.add(runningQueue);
	}
	
	public boolean removeRunningQueue(String queueName){
		boolean res =  false;
		for (JobQueue q:runningQueues){
			if (q.queueName.equals(queueName)){
				this.runningQueues.remove(q);
				res = true;
				break;
			}
		}
		return res;
	}
	
	public boolean removeRunningQueue(JobQueue q){
		boolean res =  false;
		if (this.runningQueues.contains(q)){
			this.runningQueues.remove(q);
			res = true;
		}
		return res;
	}
	
	public List<JobQueue> updateRunningQueues(){
		this.runningQueues = new LinkedList<JobQueue>();
		for (JobQueue q:jobQueues){
			if (q.isActive()){
				this.addRunningQueue(q);
			}
		}
		sortRunningQueues();
		return this.runningQueues;
	}
	
	public List<JobQueue> getRunningQueues(){
		return runningQueues;
	}
	
	public Queue<JobQueue> getRunningInteractiveQueues(){
		Queue<JobQueue> intQueues = new LinkedList<JobQueue>();
		for (JobQueue q: this.runningQueues)
			if (q.isLQ)
				intQueues.add(q);
		return intQueues;
	}
	
	public void printQueueInfo(){
		for (JobQueue q: this.jobQueues)
			System.out.println(q.getQueueName() + " weight: " + q.getWeight() + " isLQ: " + q.isLQ + " startTime:"+q.getStartTime());
	}
	
	public void readQueue(String filePathString){
		File file = new File(filePathString);
    assert (file.exists() && !file.isDirectory());
    String[] args = null;
    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line;
      String queueName = "default";
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.startsWith("#")) {
        	continue; // if comments, pass
        }
        
        args = line.split(" ");
        if (args.length != 3) 
          System.err.println("[ERROR] queueName queueTypestart time");
        
        queueName = args[0];        
      	queueName = queueName.trim();
      	JobQueue queue = new JobQueue(queueName);
      	int queueType = Integer.parseInt(args[1]);
      	double startTime = Double.parseDouble(args[2]);
      	if (queueType==0) {
      	//TODO: hard-code the high weight for interactive queues for DRF-W
      	  double weight = Double.parseDouble(br.readLine().trim());
      	  queue.setWeight(weight);
      	  queue.isLQ = false;
      	} else if(queueType==1) {
      	  queue.isLQ = true;
      	  int numOfJobs = Integer.parseInt(br.readLine().trim());
      	  double[] alphaDurations =  new double[numOfJobs];
      	  double[] periods =  new double[numOfJobs];
      	  Resource[] alphas =  new Resource[numOfJobs];
      	  
      	  if (Globals.METHOD.equals(Method.DRFW)) {
            queue.setWeight(Globals.DRFW_weight);
          } 
      	  
      	  for(int i=0; i<numOfJobs; i++){
      	    args = br.readLine().split(" ");
      	    if(args.length!=(2+Globals.NUM_DIMENSIONS))
      	        System.err.println("alphaPeriod Period [resources...]");
      	    alphaDurations[i] = Double.parseDouble(args[0]);
      	    periods[i] = Double.parseDouble(args[1]);
      	    alphas[i] = new Resource();
      	    for(int r =0; r<Globals.NUM_DIMENSIONS; r++)
      	      alphas[i].resources[r] = Double.parseDouble(args[2+r])*Globals.CAPACITY;
      	  }
      	  queue.session = new Session(numOfJobs, alphas, alphaDurations, startTime, periods);
      	}
        Simulator.QUEUE_LIST.addJobQueue(queue);
      }
      br.close();
      sortJobQueues();
    } catch (Exception e) {
      e.printStackTrace();
    }
	}
	
}
