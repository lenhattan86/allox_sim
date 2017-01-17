package cluster.data;

import cluster.datastructures.Sessions;
import cluster.simulator.Main.Globals;
import cluster.simulator.Simulator;

public class SessionData {
  public Sessions[] sessionsArray = new Sessions[1000];
  public static double scaleFactor = 1.0;

  // static data
  // LQ-0
  public static double[] LQ0StartTimes = { 50.0 };
  public static double[] LQ0AlphaDurations = { 27.0 };
  public static double[] LQ0Periods = { 200.0 };
  public static int[] LQ0JobNums = { 50 };
  // public static double[] LQ0Alphas = {1.0};
  public static double[] LQ0Alphas = { 1.0 * scaleFactor };

  // LQ-1
  public static double[] LQ1StartTimes = { 100.0 };
  public static double[] LQ1AlphaDurations = { 27.0 };
  public static double[] LQ1Periods = { 150.0 };
  public static int[] LQ1JobNums = { 150 };
  public static double[] LQ1Alphas = { 1.0 * scaleFactor };

  // LQ-2
  public static double[] LQ2StartTimes = { 150.0 };
  public static double[] LQ2AlphaDurations = { 27.0 };
  public static double[] LQ2Periods = { 60.0 }; // not important
  public static int[] LQ2JobNums = { 150 }; // always one
  public static double[] LQ2Alphas = { 1.0 * scaleFactor }; // capacity = 1.0;

  // single LQ
  public static double[] LQStartTimes = { 100.0 };
  public static double[] LQAlphaDurations = { 27.0 };
  public static double[] LQPeriods = { 800.0 };
  public static int[] LQJobNums = { 150 };
  public static double[] LQAlphas = { 1.0 * scaleFactor };

  public static double[] simpleLQStartTimes = { 200.0 };
  public static double[] simpleLQAlphaDurations = { 27.0 };
  public static double[] simpleLQPeriods = { 200.0 }; // for 8 TQs
  public static int[] simpleLQJobNums = { 150 };
  public static double[] simpleLQAlphas = { 1.0 * scaleFactor };

  public static double[] errLQStartTimes = { 200.0 };
  public static double[] errLQAlphaDurations = { 27.0 };
  public static double[] errLQPeriods = { 250.0 }; // for 8 TQs
  public static int[] errLQJobNums = { 150 };
  public static double[] errLQAlphas = { 1.0 * scaleFactor };

  public SessionData() {
    if (Globals.runmode.equals(Globals.Runmode.MultipleBurstyQueues)) {
      sessionsArray[0] = new Sessions(LQ0StartTimes, LQ0AlphaDurations,
          LQ0Periods, LQ0JobNums, LQ0Alphas);
      sessionsArray[1] = new Sessions(LQ1StartTimes, LQ1AlphaDurations,
          LQ1Periods, LQ1JobNums, LQ1Alphas);
      sessionsArray[2] = new Sessions(LQ2StartTimes, LQ2AlphaDurations,
          LQ2Periods, LQ2JobNums, LQ2Alphas);
    } else if (Globals.runmode.equals(Globals.Runmode.MultipleBatchQueueRun)) {
      sessionsArray[0] = new Sessions(LQStartTimes, LQAlphaDurations, LQPeriods,
          LQJobNums, LQAlphas);
    } else if (Globals.runmode.equals(Globals.Runmode.AvgTaskDuration)) {
      sessionsArray[0] = new Sessions(simpleLQStartTimes,
          simpleLQAlphaDurations, simpleLQPeriods, simpleLQJobNums,
          simpleLQAlphas);
    } else if (Globals.runmode.equals(Globals.Runmode.EstimationErrors)) {
//      double[] newLQAlphaDurations = new double[errLQAlphaDurations.length];
      double duration = 0;
      switch (Globals.workload) {
      case BB:
        duration = 27;
        break;
      case TPC_DS:
        duration = 27;
        break;
      case TPC_H:
        duration = 27;
        break;
      default:
        duration = 25;
        break;
      }
      for (int i = 0; i<errLQAlphaDurations.length; i++)
        errLQAlphaDurations[i] = duration;
      
      sessionsArray[0] = new Sessions(errLQStartTimes, errLQAlphaDurations,
          errLQPeriods, errLQJobNums, errLQAlphas);
    } else {
      sessionsArray[0] = new Sessions(LQStartTimes, LQAlphaDurations, LQPeriods,
          LQJobNums, LQAlphas);
    }
  }
}
