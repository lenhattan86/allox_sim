package cluster.utils;

import java.util.Comparator;

import cluster.datastructures.BaseDag;

public class JobArrivalComparator implements Comparator<BaseDag>{

	@Override
	public int compare(BaseDag job1, BaseDag job2) {
		int res = 0; 
		if (job1.arrivalTime > job2.arrivalTime){
			res = 1;
		}else if (job1.arrivalTime > job2.arrivalTime){
			res = -1;
		}
		return res;
	}
}
