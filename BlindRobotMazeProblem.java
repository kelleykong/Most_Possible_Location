

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class BlindRobotMazeProblem  {

	private static int actions[][] = {Maze.NORTH, Maze.EAST, Maze.SOUTH, Maze.WEST}; 

	private Maze maze;
	
	// transition model P(Lt|Lt-1) = probability
	// key: ArrayList<Integer>{Lt-1.x, Lt-1.y, Lt.x, Lt.y} value: probability
	private HashMap<ArrayList<Integer>, Double> transModel;
	
	
	
	public BlindRobotMazeProblem(Maze m) {
//		System.out.println("Blind Robot begin!");
		maze = m;		
				
		// build transition model
		buildTransitionModel();
		
	}
	
	private void buildTransitionModel() {
		transModel = new HashMap<ArrayList<Integer>, Double>();
		
		for (int i = 0; i < maze.height; i++)
			for (int j = 0; j < maze.width; j++) {
				//Location (j,i)
				for (int[] action: actions) {
					int curXNew = j + action[0];
					int curYNew = i + action[1];
					
					// if the action can be done, P(L(curXnew, curYNew)|L(j,i)) = 0.25
					// 		else P(L(j,i)|L(j,i)) += 0.25
					if (maze.isLegal(curXNew, curYNew)) {
						ArrayList<Integer> trans = new ArrayList<Integer>();
						trans.add(j);
						trans.add(i);
						trans.add(curXNew);
						trans.add(curYNew);
						transModel.put(trans, 0.25);
					}
					else {
						ArrayList<Integer> trans = new ArrayList<Integer>();
						trans.add(j);
						trans.add(i);
						trans.add(j);
						trans.add(i);
						if (transModel.containsKey(trans)) 
							transModel.put(trans, transModel.remove(trans) + 0.25);
						else
							transModel.put(trans, 0.25);
					}
				}
			}
	}
	
	// get P(C==s|L(x,y)) from sensor model
	// sensor model: if c(x,y) == s, p = 0.88; else p = 0.04
	private double sensorModel(int x, int y, int s) {
		int color = maze.getColor(x, y);
		
		if (color == s)
			return 0.88;
		else
			return 0.04;
	}
	
	// get probability distributions describing the possible locations 
	//		of the robot at each time step.	
	public ArrayList<double[][]> getProbDistr(int[] colors) {
//		return Filtering(colors);
		return ForwardBackward(colors);
	}
	
	// Filtering: Forward P(Xt|e1:t)
	private ArrayList<double[][]> Filtering(int[] colors) {
		// probability distributions sequence of each steps.
		ArrayList<double[][]> probDistrSeq = new ArrayList<double[][]>();
		
		// before first step, initial probability distribution: P(L0)
		double[][] prob_L0 = new double[maze.height][maze.width];
		
		// get the num of valid locations except walls
		int num = 0;
		for (int i = 0; i < maze.height; i++)
			for (int j = 0; j < maze.width; j++)
				if (maze.isLegal(j, i))
					num++;
		
		for (int i = 0; i < maze.height; i++)
			for (int j = 0; j < maze.width; j++)
				if (maze.isLegal(j, i))
					prob_L0[i][j] = 1.0 / num;
		
		probDistrSeq.add(prob_L0);
		
		// one step, get a color as evidence variable Ct
		// Then do filtering to get P(Xt|e1:t) = P(Lt|c1:t)
		for (int color: colors) {
			// step1: P(Xt+1|e1:t) = sum_xt(P(Xt+1|Xt)*P(Xt|e1:t))
			// 		P(Xt+1|Xt) got from transition model
			// 		P(Xt|e1:t) is last step probability distribution
			
			// P(Xt+1|e1:t) = P(Lt+1|c1:t)
			double[][] prob_t1_t = new double[maze.height][maze.width];
			
			// P(Xt|e1:t) = P(Lt|c1:t)
			double[][] prob_t_t = probDistrSeq.get(probDistrSeq.size()-1);
			
			for (int i = 0; i < maze.height; i++)
				for (int j = 0; j < maze.width; j++) {
					// Location (j,i)
					prob_t1_t[i][j] = 0;
					for (int m = 0; m < maze.height; m++)
						for (int n = 0; n < maze.width; n++) {
							// transition (n,m)->(j,i)
							ArrayList<Integer> trans = new ArrayList<Integer>();
							trans.add(n);
							trans.add(m);
							trans.add(j);
							trans.add(i);
							// in transModel, only store nonzero transition
							if (transModel.containsKey(trans))
								prob_t1_t[i][j] += transModel.get(trans) * prob_t_t[m][n];
						}
				}
				
			// step2: P(Xt+1|e1:t+1) = alpha * P(et+1|Xt+1)*P(Xt+1|e1:t)
			//		P(et+1|Xt+1) got from sensor model
			//		P(Xt+1|e1:t) got from step1
			
			// P(Xt+1|e1:t+1) = P(Lt+1:c1:t+1)
			double[][] prob_t1_t1 = new double[maze.height][maze.width];
			double sum = 0;
			
			// P(et+1|Xt+1)*P(Xt+1|e1:t)
			for (int i = 0; i < maze.height; i++)
				for (int j = 0; j < maze.width; j++) {
					// getP(et+1|Xt+1) from sensor model: Location (j,i)-> Ct+1
					prob_t1_t1[i][j] = sensorModel(j,i, color) * prob_t1_t[i][j];
					sum += prob_t1_t1[i][j];
				}
			// alpha
			for (int i = 0; i < maze.height; i++)
				for (int j = 0; j < maze.width; j++) {
					prob_t1_t1[i][j] = prob_t1_t1[i][j] / sum;
				}
					
			probDistrSeq.add(prob_t1_t1);
		}
		
		return probDistrSeq;
	}
	
	// forward-backward algorithm for smoothing P(Xk|e1:t) 0 < k < t
	private ArrayList<double[][]> ForwardBackward(int[] colors) {
		// fv[t] = P(Xt|e1:t)
		ArrayList<double[][]> fv = Filtering(colors);
		// sv[t] = P（Xk|e1:t)
		ArrayList<double[][]> sv = new ArrayList<double[][]>();
		// b = P(ek+1:t|Xk)
		double[][] b = new double[maze.height][maze.width];
		
		// the first b = P(ek+1:t|Xk) = 1 
		for (int m = 0; m < maze.height; m++)
			for (int n = 0; n < maze.width; n++) {
				b[m][n] = 1;
			}
		
		for (int i = fv.size()-1; i > 0; i--) {
			int color = colors[i-1];
			//sv[i] = Normalize(fv[i] x b)
			//sv[i] = alpha P(Xk|e1:k) * P(ek+1:k|Xk)
			double[][] svi = new double[maze.height][maze.width];
			double[][] fvi = fv.get(i);

			// P(Xk|e1:k) * P(ek+1:t|Xk)
			double sum = 0;
			for (int m = 0; m < maze.height; m++)
				for (int n = 0; n < maze.width; n++) {
					svi[m][n] = fvi[m][n] * b[m][n];
					sum += svi[m][n];
				}
			
			// alpha 
			for (int m = 0; m < maze.height; m++)
				for (int n = 0; n < maze.width; n++) {
					svi[m][n] = svi[m][n] / sum;
				}
			
			sv.add(0, svi);
			
			// b = Backward(b,ev[i])
			// b = P(ek:t|Xk-1) = sum_xk(P(ek|xk) * P(ek+1:t|xk) * P(xk|xk-1))
			// Xk-1 = (n,m)
			double[][] tmp = new double[maze.height][maze.width];
			
			for (int m = 0; m < maze.height; m++)
				for (int n = 0; n < maze.width; n++) {
					// xk = (l,k)
					for (int k = 0; k < maze.height; k++)
						for (int l = 0; l < maze.width; l++) {
							// P(xk|xk-1)
							ArrayList<Integer> trans = new ArrayList<Integer>();
							trans.add(m);
							trans.add(n);
							trans.add(k);
							trans.add(l);
							if (transModel.containsKey(trans))
								tmp[m][n] += sensorModel(l,k, color) * b[k][l] * transModel.get(trans);
						}
				}
			b = tmp;
		}
		
		// P(X0|e1:t) = alpha P(X0) * P(e1:t|X0) = alpha * P(X0) * b
		double[][] px0 = new double[maze.height][maze.width];
		double sum = 0;
		for (int m = 0; m < maze.height; m++)
			for (int n = 0; n < maze.width; n++) {
				px0[m][n] = fv.get(0)[m][n] * b[m][n];
				sum += px0[m][n];
			}
		
		for (int m = 0; m < maze.height; m++)
			for (int n = 0; n < maze.width; n++) {	
				px0[m][n] /= sum;
			}
		sv.add(0, px0);
		
		return sv;
		
	}
	
	// using Viterbi to get the most likely sequence, maybe more than one
	public ArrayList<ArrayList<int[]>> getPath_Viterbi(int[] colors, ArrayList<double[][]> probDistr) {
	
		// P(x1,...,xt|e1:t)
		// Initial probability distribution: P(L0)
		double[][] prob_xt = new double[maze.height][maze.width];
		
		// get the num of valid locations except walls
		int num = 0;
		for (int i = 0; i < maze.height; i++)
			for (int j = 0; j < maze.width; j++)
				if (maze.isLegal(j, i))
					num++;
		
		for (int i = 0; i < maze.height; i++)
			for (int j = 0; j < maze.width; j++)
				if (maze.isLegal(j, i))
					prob_xt[i][j] = 1.0 / num;
		
		probDistr.add(prob_xt);
		
		// keep maxarg xt
		// ArrayList[0] = null, because no location before L0
		// key: L[x,y] 	value: locations(x2,y2) lead to this (x,y)
		ArrayList<HashMap<ArrayList<Integer>, ArrayList<int[]>>> maxarg_xt_list = new ArrayList<HashMap<ArrayList<Integer>, ArrayList<int[]>>>();
		maxarg_xt_list.add(null);
		
		// max P(x1,...xt, Xt+1| e1:t+1) = alpha P(et+1|Xt+1) max( P(Xt+1|xt) max(x1,...,xt|e1:t) )
		for (int color: colors) {
			// max P(x1,...xt, Xt+1| e1:t+1)
			double[][] prob = new double[maze.height][maze.width];		
			
			// keep maxarg xt in this step
			HashMap<ArrayList<Integer>, ArrayList<int[]>> maxarg_xt = new HashMap<ArrayList<Integer>, ArrayList<int[]>>();
			
			// for each Xt+1, prob[i][j] = max( P(Xt+1|xt) max(x1,...,xt|e1:t) )
			for (int i = 0; i < maze.height; i++)
				for (int j = 0; j < maze.width; j++) {
					
					// keep max 
					double max = 0;
					ArrayList<int[]> maxLoc = new ArrayList<int[]>();
					
					// for each xt, max ( P(Xt+1|xt) max(x1,...,xt|e1:t) )
					for (int m = 0; m < maze.height; m++)
						for (int n = 0; n < maze.width; n++) {
							ArrayList<Integer> trans = new ArrayList<Integer>();
							trans.add(m);
							trans.add(n);
							trans.add(i);
							trans.add(j);
							if (transModel.containsKey(trans)) {
								double tmp = transModel.get(trans) * prob_xt[m][n];
								if (tmp > max) {
									max = tmp;
									if (!maxLoc.isEmpty())
										maxLoc.clear();
									maxLoc.add(new int[]{n,m});
								}
								else if (tmp == max) {
									maxLoc.add(new int[]{n,m});
								}
							}
						}
					prob[i][j] = max;
					ArrayList<Integer> loc = new ArrayList<Integer>();
					loc.add(j);
					loc.add(i);
					maxarg_xt.put(loc, maxLoc);
				}
			
			// P(et+1|Xt+1) max( P(Xt+1|xt) max(x1,...,xt|e1:t) )
			double sum = 0;
			for (int i = 0; i < maze.height; i++)
				for (int j = 0; j < maze.width; j++) {
					prob[i][j] = sensorModel(j,i, color) * prob[i][j];
					sum += prob[i][j];
				}
			
			// alpha
			for (int i = 0; i < maze.height; i++)
				for (int j = 0; j < maze.width; j++) {
					prob[i][j] = prob[i][j]/sum;
				}
			
			// prepare for next loop
			prob_xt = prob;
			probDistr.add(prob_xt);
			maxarg_xt_list.add(maxarg_xt);
			
		}
		
		// maxarg P(x1,...xt, Xt+1| e1:t+1) find the path
		// find maxarg P(x1,...xt, Xt+1| e1:t+1)
		double max = 0;
		int[] maxLoc = new int[2];
		for (int i = 0; i < maze.height; i++)
			for (int j = 0; j < maze.width; j++) {
				if (prob_xt[i][j] > max) {
					max = prob_xt[i][j];
					maxLoc[0] = j;
					maxLoc[1] = i;
				}
			}
		
		// find path
		ArrayList<ArrayList<int[]>> paths = new ArrayList<ArrayList<int[]>>();
		ArrayList<int[]> p = new ArrayList<int[]>();
		p.add(0, maxLoc);
		
		findPath(maxarg_xt_list, p, paths);
		
		return paths;
	}
	
	private void findPath(ArrayList<HashMap<ArrayList<Integer>, ArrayList<int[]>>> maxarg_xt_list, 
			ArrayList<int[]> p, ArrayList<ArrayList<int[]>> paths) {
		// if path is complete
		if (maxarg_xt_list.size() == p.size()) {
			paths.add(p);
			return;
		}
		
		// find 1 step previous locations
		HashMap<ArrayList<Integer>, ArrayList<int[]>> maxarg_xt = maxarg_xt_list.get(maxarg_xt_list.size()-p.size());
		ArrayList<Integer> loc = new ArrayList<Integer>();
		loc.add(p.get(0)[0]);
		loc.add(p.get(0)[1]);
		ArrayList<int[]> locs = maxarg_xt.get(loc);

		for (int j = 1; j < locs.size(); j++) {
			ArrayList<int[]> np = new ArrayList<int[]>(p);
			np.add(0, locs.get(j));
			findPath(maxarg_xt_list, np, paths);
		}
		p.add(0, locs.get(0));
		findPath(maxarg_xt_list, p, paths);
	}
	
}