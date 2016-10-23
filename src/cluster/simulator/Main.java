package cluster.simulator;

import java.util.Queue;
import java.util.logging.Logger;

import cluster.datastructures.BaseDag;
import cluster.simulator.Main.Globals.Method;
import cluster.simulator.Main.Globals.Runmode;
import cluster.simulator.Main.Globals.SchedulingPolicy;
import cluster.utils.GenInput;

public class Main {
	
	

	private static Logger LOG = Logger.getLogger(Main.class.getName());

	public static class Globals {

		public final static String WORK_LOAD = "workload/queries_bb_FB_distr.txt"; // BigBench
		// public static String WORK_LOAD =
		// "workload/queries_tpcds_FB_distr_new.txt"; // TPC-DS
		// public final static String WORK_LOAD =
		// "workload/queries_tpch_FB_distr.txt"; // TPC-H --> 250

		public static int SMALL_JOB_THRESHOLD = 250; // for 80 for 2 BB TPCDS,
														// => TPC-H --> 250

		public enum WorkLoadType {
			BB, TPC_DS, TPC_H, SIMPLE
		};

		public enum SetupMode {
			VeryShortInteractive, ShortInteractive, LongInteractive, VeryLongInteractive
		};

		public enum Runmode {
			SingleRun, MultipleBatchQueueRun, MultipleInteractiveQueueRun
		}

		public static boolean USE_TRACE = true;

		public static final int TASK_ARRIVAL_RANGE = 50;

		public static double DEBUG_START = 0;
		public static double DEBUG_END = -1;

		public static int SCALE_UP_INTERACTIV_JOB = 50;
		public static int SCALE_UP_BATCH_JOB = 1;

		public static double SMALL_JOB_DUR_THRESHOLD = 40.0;
		public static double LARGE_JOB_MAX_DURATION = 0.0;

		public static String DIST_FILE = "dist_gen/poissrnd.csv";

		public static Runmode runmode = Runmode.MultipleBatchQueueRun;

		public static boolean DEBUG_ALL = false;
		public static boolean DEBUG_LOCAL = true;

		public static enum Method {
			DRF, DRFW, SpeedFair, Strict
		}

		public static enum QueueSchedulerPolicy {
			Fair, DRF, SpeedFair
		};

		public static QueueSchedulerPolicy QUEUE_SCHEDULER = QueueSchedulerPolicy.DRF;

		public static enum SchedulingPolicy {
			Random, BFS, CP, Tetris, Carbyne, SpeedFair, Yarn
		};

		public static SchedulingPolicy INTRA_JOB_POLICY = SchedulingPolicy.CP;

		public enum SharingPolicy {
			Fair, DRF, SJF, TETRIS_UNIVERSAL, SpeedFair
		};

		public static SharingPolicy INTER_JOB_POLICY = SharingPolicy.Fair;

		public enum JobsArrivalPolicy {
			All, One, Distribution, Trace;
		}

		public static JobsArrivalPolicy JOBS_ARRIVAL_POLICY = JobsArrivalPolicy.Trace;

		public static boolean GEN_JOB_ARRIVAL = true;

		public static int NUM_MACHINES = 1; // TODO: NUM_MACHINES > 1 may
											// results in
											// low utilization this simulation.
		public static int NUM_DIMENSIONS = 6; // TODO: change to 6
		public static double MACHINE_MAX_RESOURCE;
		public static int DagIdStart, DagIdEnd;

		public static Method METHOD = Method.SpeedFair;
		public static double DRFW_weight = 4.0;
		public static double STRICT_WEIGHT = (Double.MAX_VALUE / 100.0);
		// public static double STRICT_WEIGHT = 100000.00;
		public static int PERIODIC_INTERVAL = 100;

		public static int TOLERANT_ERROR = 1; // 10^(-TOLERANT_ERROR)

		public static boolean ADJUST_FUNGIBLE = false;
		public static double ZERO = 0.001;

		public static double SIM_END_TIME = 50000;

		public static int NUM_OPT = 0, NUM_PES = 0;

		public static int MAX_NUM_TASKS_DAG = 10000;

		public static boolean TETRIS_UNIVERSAL = false;
		/**
		 * these variables control the sensitivity of the simulator to various
		 * factors
		 */
		// between 0.0 and 1.0; 0.0 it means jobs are not pessimistic at all
		public static double LEVEL_OF_OPTIMISM = 0.0;

		public static boolean COMPUTE_STATISTICS = false;
		public static double ERROR = 0.0;
		public static boolean IS_GEN = true;

		/**
		 * these variables will be set by the static constructor based on
		 * runmode
		 */
		public static String DataFolder;
		public static String outputFolder = "output";
		public static String FileInput = "dags-input.txt";
		public static String QueueInput = "queue_input.txt";
		public static String FileOutput = "dags-output.txt";
		public static String PathToInputFile = DataFolder + "/" + FileInput;
		public static String PathToQueueInputFile;
		public static String PathToOutputFile = "";
		public static String PathToResourceLog = "";

		public static double[] RATES = null;
		public static double[] RATE_DURATIONS = null;
		public static double SpeedFair_WEIGHT = 1.0; // not use anymore

		public static int numInteractiveQueues = 1, numInteractiveJobPerQueue = 0, numInteractiveTask = 0;
		public static int numBatchJobs = 160;
		public static int numBatchQueues = 1;
		public static int numbatchTask = 10000;

		public static double STEP_TIME = 1.0;

		public static boolean ENABLE_PREEMPTION = false;

		public static int LARGE_JOB_THRESHOLD = 100;

		public static double SCALE_INTERACTIVE_DURATION = 1.0;
		public static double SCALE_BATCH_DURATION = 1.0;

		public static void setupParameters(SetupMode setup, WorkLoadType workload) {
			DagIdStart = 0;
			DagIdEnd = 400;
			NUM_DIMENSIONS = 2;
			MACHINE_MAX_RESOURCE = 100;
			DRFW_weight = 4.0;
			SpeedFair_WEIGHT = 0.5;

			DataFolder = "input";
			FileInput = "dags-input-simple.txt";
			QueueInput = "queue_input.txt";

			COMPUTE_STATISTICS = false;

			Globals.PERIODIC_INTERVAL = 100;

			switch (workload) {
			case BB:
				switch (setup) {
				case VeryShortInteractive:
					Globals.numInteractiveJobPerQueue = 20;
					Globals.SCALE_UP_INTERACTIV_JOB = 50;
					Globals.SCALE_INTERACTIVE_DURATION = 1 / 30.0;
					Globals.SCALE_BATCH_DURATION = 1 / 5.0;
					Globals.STEP_TIME = 0.1;
					break;
				case ShortInteractive:
					Globals.numInteractiveJobPerQueue = 25;
					Globals.SMALL_JOB_DUR_THRESHOLD = 30.0;
					Globals.SMALL_JOB_THRESHOLD = 80;
					Globals.SCALE_UP_INTERACTIV_JOB = 50; 
					Globals.SCALE_INTERACTIVE_DURATION = 1/2.0;
					// Globals.SCALE_BATCH_DURATION = 1/3.0;
					// for generated workload only
					Globals.numInteractiveTask = 2000;
					Globals.SCALE_UP_BATCH_JOB = 1; 
					// we can improve performance by reduce batch duration
					break;
				case LongInteractive:
					Globals.numInteractiveJobPerQueue = 20; // the larger
					Globals.SCALE_UP_INTERACTIV_JOB = 250;
					Globals.SCALE_INTERACTIVE_DURATION = 1 / 5.0;
					// Globals.SCALE_BATCH_DURATION = 1/5.0;
					// for generated workload only
					numBatchJobs = 120;
					Globals.numInteractiveTask = 8000;
					Globals.numbatchTask = 10000;
					break;
				case VeryLongInteractive:
					Globals.numInteractiveJobPerQueue = 5;
					Globals.SCALE_UP_INTERACTIV_JOB = 400;
					// for generated workload only
					Globals.numInteractiveTask = 80000;
					Globals.numbatchTask = 10000;
					break;
				default:
					System.err.println("Unknown runmode");
				}
				break;
			case TPC_DS:
				switch (setup) {
				case ShortInteractive:
					Globals.numInteractiveJobPerQueue = 25;
					Globals.SCALE_UP_INTERACTIV_JOB = 2; // 50 for BB, 1 for
															// TPC-H
					Globals.SCALE_INTERACTIVE_DURATION = 10 / Globals.SMALL_JOB_DUR_THRESHOLD;
					// Globals.SCALE_BATCH_DURATION = 1/3.0;
					// for generated workload only
					Globals.numInteractiveTask = 2000;
					Globals.SCALE_UP_BATCH_JOB = 3; // 2 for BB, 2 for TPC-H, 3
													// TPC-DS
					Globals.PERIODIC_INTERVAL = 100;
					break;
				default:
					System.err.println("Unknown runmode");
				}
				break;
			case TPC_H:
				switch (setup) {
				case ShortInteractive:
					Globals.numInteractiveJobPerQueue = 25;
					Globals.SCALE_UP_INTERACTIV_JOB = 2; // 50 for BB, 1 for
					Globals.SCALE_INTERACTIVE_DURATION = 10 / Globals.SMALL_JOB_DUR_THRESHOLD;
					// Globals.SCALE_BATCH_DURATION = 1/3.0;
					// for generated workload only
					Globals.numInteractiveTask = 2000;
					Globals.SCALE_UP_BATCH_JOB = 3; // 2 for BB, 2 for TPC-H, 3
													// TPC-DS
					Globals.PERIODIC_INTERVAL = 100;
					break;
				default:
					System.err.println("Unknown runmode");
				}
			case SIMPLE:
				Globals.STEP_TIME = 1.0;
				SIM_END_TIME = 500;
				Globals.USE_TRACE = false;
				Globals.numInteractiveJobPerQueue = 10;
				Globals.numBatchJobs=16;
				Globals.numInteractiveTask = 2000;
				Globals.numbatchTask = 5000;
				Globals.PERIODIC_INTERVAL = 100;
				break;
			default:
				System.err.println("Unknown workload");
			}
		}
	}

	public static void runSimulationScenario() {

		 double[] rates = {
		 Globals.MACHINE_MAX_RESOURCE*Globals.NUM_MACHINES/Globals.numInteractiveQueues};
//		double[] rates = { 500 };
		double[] durations = { 30 };
		Globals.RATES = rates;
		Globals.RATE_DURATIONS = durations;

		long tStart = System.currentTimeMillis();
		if (Globals.IS_GEN) {
			if (Globals.USE_TRACE) {
				Queue<BaseDag> tracedJobs = GenInput.readWorkloadTrace(Globals.WORK_LOAD);
				GenInput.genInputFromWorkload(Globals.numInteractiveQueues, Globals.numInteractiveJobPerQueue,
						Globals.numInteractiveTask, Globals.numBatchQueues,
						Globals.numBatchJobs / Globals.numBatchQueues, tracedJobs);
			} else
				GenInput.genInput(Globals.numInteractiveQueues, Globals.numInteractiveJobPerQueue,
						Globals.numInteractiveTask, Globals.numBatchQueues,
						Globals.numBatchJobs / Globals.numBatchQueues);
		}

		if (Globals.METHOD.equals(Method.DRF)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
			Globals.INTRA_JOB_POLICY = SchedulingPolicy.Yarn;
			Globals.FileOutput = "DRF-output" + "_" + Globals.numInteractiveQueues + "_" + Globals.numBatchQueues
					+ ".csv";
		} else if (Globals.METHOD.equals(Method.SpeedFair)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.SpeedFair;
			Globals.INTRA_JOB_POLICY = SchedulingPolicy.Yarn;
			Globals.FileOutput = "SpeedFair-output" + "_" + Globals.numInteractiveQueues + "_" + Globals.numBatchQueues
					+ ".csv";
		} else if (Globals.METHOD.equals(Method.DRFW)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
			Globals.INTRA_JOB_POLICY = SchedulingPolicy.Yarn;
			Globals.FileOutput = "DRF-W-output" + "_" + Globals.numInteractiveQueues + "_" + Globals.numBatchQueues
					+ ".csv";
		} else if (Globals.METHOD.equals(Method.Strict)) {
			Globals.QUEUE_SCHEDULER = Globals.QueueSchedulerPolicy.DRF;
			Globals.INTRA_JOB_POLICY = SchedulingPolicy.Yarn;
			Globals.FileOutput = "Strict-output" + "_" + Globals.numInteractiveQueues + "_" + Globals.numBatchQueues
					+ ".csv";
		} else {
			System.err.println("Error! test case");
			return;
		}

		if (Globals.IS_GEN) {
			Globals.DataFolder = "input_gen";
			Globals.FileInput = "jobs_input_" + Globals.numInteractiveQueues + '_' + Globals.numBatchQueues + ".txt";
			Globals.QueueInput = "queue_input_" + Globals.numInteractiveQueues + '_' + Globals.numBatchQueues + ".txt";
		}

		Globals.PathToInputFile = Globals.DataFolder + "/" + Globals.FileInput;
		Globals.PathToQueueInputFile = Globals.DataFolder + "/" + Globals.QueueInput;
		Globals.PathToOutputFile = Globals.outputFolder + "/" + Globals.FileOutput;
		Globals.PathToResourceLog = "log" + "/" + Globals.FileOutput;

		// print ALL parameters for the record
		System.out.println("=====================");
		System.out.println("Simulation Parameters");
		System.out.println("=====================");
		System.out.println("Workload            = " + Globals.WORK_LOAD);
		System.out.println("PathToInputFile     = " + Globals.PathToInputFile);
		System.out.println("PathToInputFile     = " + Globals.PathToOutputFile);
		System.out.println("SIMULATION_END_TIME = " + Globals.SIM_END_TIME);
		System.out.println("STEP_TIME           = " + Globals.STEP_TIME);
		System.out.println("METHOD              = " + Globals.METHOD);
		System.out.println("QUEUE_SCHEDULER     = " + Globals.QUEUE_SCHEDULER);
		System.out.println("=====================\n");

		System.out.println("Start simulation ...");
		System.out.println("Please wait ...");
		Simulator simulator = new Simulator();
		simulator.simulateMultiQueues();
		System.out.println("\nEnd simulation ...");
		long duration = System.currentTimeMillis() - tStart;
		System.out.print("========== " + (duration / (1000)) + " seconds ==========\n");
	}
	

	public static void main(String[] args) {
		Globals.SIM_END_TIME = 500;
//		Globals.DEBUG_LOCAL = true;
//		Globals.DEBUG_START = 217;
//		Globals.DEBUG_END = 300;
		Globals.SetupMode mode = Globals.SetupMode.ShortInteractive;
		Globals.WorkLoadType workload = Globals.WorkLoadType.BB;
		Globals.setupParameters(mode, workload);
		// Globals.setupParameters(Globals.SetupMode.LongInteractive);
		// Globals.runmode = Runmode.MultipleInteractiveQueueRun;
		Globals.runmode = Runmode.MultipleBatchQueueRun;
		// Globals.numInteractiveQueues = 2;

		if (Globals.runmode.equals(Runmode.MultipleBatchQueueRun)) {
//			 Method[] methods = { Method.DRF, Method.DRFW, Method.Strict, Method.SpeedFair };
//			 int[] batchQueueNums = { 1, 2, 4, 8, 16, 32};
			Method[] methods = {Method.DRF, Method.DRFW, Method.Strict, Method.SpeedFair};
			int[] batchQueueNums = {1, 4};

			for (int j = 0; j < batchQueueNums.length; j++) {
				for (int i = 0; i < methods.length; i++) {
					if (i == 0)
						Globals.IS_GEN = true;
					else
						Globals.IS_GEN = false;

					Globals.METHOD = methods[i];
					Globals.numBatchQueues = batchQueueNums[j];
					System.out.println("=================================================================");
					System.out.println(
							"Run METHOD: " + Globals.METHOD + " with " + Globals.numBatchQueues + " batch queues.");
					runSimulationScenario();
					System.out.println("==================================================================");
				}
			}
		} else if (Globals.runmode.equals(Runmode.MultipleInteractiveQueueRun)) {
			Method[] methods = { Method.DRF, Method.DRFW, Method.Strict, Method.SpeedFair };
			int[] interactiveQueues = { 1, 2, 3, 4 };
			// Method[] methods = { Method.DRF, Method.SpeedFair };
			// int[] interactiveQueues = {1,4};

			for (int j = 0; j < interactiveQueues.length; j++) {
				for (int i = 0; i < methods.length; i++) {
					if (i == 0)
						Globals.IS_GEN = true;
					else
						Globals.IS_GEN = false;

					Globals.METHOD = methods[i];
					Globals.numInteractiveQueues = interactiveQueues[j];
					System.out.println("=================================================================");
					System.out.println("Run METHOD: " + Globals.METHOD + " with " + Globals.numInteractiveQueues
							+ " interactive queues.");
					runSimulationScenario();
					System.out.println("==================================================================");
				}
			}
		} else if (Globals.runmode.equals(Runmode.SingleRun)) {
			// Globals.METHOD = Method.DRFW;
			// Globals.METHOD = Method.Strict;
			// Globals.METHOD = Method.DRF;
			Globals.METHOD = Method.SpeedFair;
			Globals.SIM_END_TIME = 50000;
			// Globals.MACHINE_MAX_RESOURCE = 100;
			Globals.NUM_MACHINES = 1;
			Globals.numBatchQueues = 4;
			Globals.numInteractiveJobPerQueue = 5;
			Globals.DEBUG_LOCAL = false;
			// Globals.IS_GEN = true;
			// double[] rates = {
			// Globals.MACHINE_MAX_RESOURCE*Globals.NUM_MACHINES };
			// double[] durations = { Globals.SMALL_JOB_MAX_DURATION };
			// Globals.RATES = rates;
			// Globals.RATE_DURATIONS = durations;
			System.out.println("=================================================================");
			System.out.println("Run METHOD: " + Globals.METHOD + " with " + Globals.numBatchQueues + " batch queues.");
			runSimulationScenario();
			System.out.println();
		}

		System.out.println("\n");
		System.out.println("........FINISHED ./.");
	}

}
