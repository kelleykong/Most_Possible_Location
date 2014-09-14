

import java.util.ArrayList;
import java.util.List;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;


public class BlindRobotMazeDriver extends Application {

	Maze maze;
	
	// instance variables used for graphical display
	private static final int PIXELS_PER_SQUARE = 64;
	MazeView mazeView;
	List<AnimationPath> animationPathList;
	
	// some basic initialization of the graphics; needs to be done before 
	//  runSearches, so that the mazeView is available
	private void initMazeView() {
		maze = Maze.readFromFile("simple.maz");
		
		animationPathList = new ArrayList<AnimationPath>();
		// build the board
		mazeView = new MazeView(maze, PIXELS_PER_SQUARE);
		
	}
	
	// assumes maze and mazeView instance variables are already available
	private void runSearches() {
		
		//motions: e w w e
		ArrayList<int[]> path = new ArrayList<int[]>();
		path.add(new int[]{0,0});
		path.add(new int[]{1,0});
		path.add(new int[]{0,0});
		path.add(new int[]{0,0});
		path.add(new int[]{1,0});
//		path.add(new int[]{1,1});
//		path.add(new int[]{0,1});
/*		path.add(new int[]{2,0});
		path.add(new int[]{3,0});
		path.add(new int[]{3,1});
		path.add(new int[]{4,1});
		path.add(new int[]{4,2});
		path.add(new int[]{5,2});
		path.add(new int[]{5,3});
		path.add(new int[]{5,4});
*/
		
		//colors: b r r b //y r y b g y y r
		int[] colors = new int[]{3,1,1,3/*, 4, 1, 4, 3, 2, 4, 4, 1*/};
		
		BlindRobotMazeProblem mazeProblem = new BlindRobotMazeProblem(maze);

		ArrayList<double[][]> probdistr = mazeProblem.getProbDistr(colors);
	
		animationPathList.add(new AnimationPath(mazeView, path, probdistr));

/*		ArrayList<double[][]> probdistr = new ArrayList<double[][]>();
		ArrayList<ArrayList<int[]>> paths = mazeProblem.getPath_Viterbi(colors, probdistr);
		for (ArrayList<int[]> p: paths) {
			animationPathList.add(new AnimationPath(mazeView, p, probdistr));
			printPath(p);
		}
*/		
	}
	
	private void printPath(ArrayList<int[]> path) {
		for (int[] loc: path) {
			System.out.print("(" + loc[0] + "," + loc[1] + ") ");
		}
		System.out.println();
	}


	public static void main(String[] args) {
		launch(args);
	}

	// javafx setup of main view window for mazeworld
	@Override
	public void start(Stage primaryStage) {
		
		initMazeView();
	
		primaryStage.setTitle("CS 76 Probabilistic Reasing over time");

		// add everything to a root stackpane, and then to the main window
		StackPane root = new StackPane();
		root.getChildren().add(mazeView);
		primaryStage.setScene(new Scene(root));

		primaryStage.show();

		// do the real work of the driver; run search tests
		runSearches();

		// sets mazeworld's game loop (a javafx Timeline)
		Timeline timeline = new Timeline(1.0);
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.getKeyFrames().add(
				new KeyFrame(Duration.seconds(.05), new GameHandler()));
		timeline.playFromStart();

	}

	// every frame, this method gets called and tries to do the next move
	//  for each animationPath.
	private class GameHandler implements EventHandler<ActionEvent> {

		@Override
		public void handle(ActionEvent e) {
			// System.out.println("timer fired");
			for (AnimationPath animationPath : animationPathList) {
				// note:  animationPath.doNextMove() does nothing if the
				//  previous animation is not complete.  If previous is complete,
				//  then a new animation of a piece is started.
				animationPath.doNextMove();
			}
		}
	}

	// each animation path needs to keep track of some information:
	// the underlying search path, the "piece" object used for animation,
	// etc.
	private class AnimationPath {
		private Node piece;
		private List<int[]> searchPath;
		private int currentMove = 0;

		private int lastX;
		private int lastY;

		boolean animationDone = true;
		
		private ArrayList<double[][]> probs;

		public AnimationPath(MazeView mazeView, List<int[]> path, ArrayList<double[][]> probdistr) {
			searchPath = path;
			int[] firstNode = searchPath.get(0);
			piece = mazeView.addPiece(firstNode[0], firstNode[1]);
			lastX = firstNode[0];
			lastY = firstNode[1];
			
			probs = probdistr;
			mazeView.setTexts(probs.get(0));
		}

		// try to do the next step of the animation. Do nothing if
		// the mazeView is not ready for another step.
		public void doNextMove() {

			// animationDone is an instance variable that is updated
			//  using a callback triggered when the current animation
			//  is complete
			if (currentMove < searchPath.size() && animationDone) {
				int[] mazeNode = searchPath
						.get(currentMove);
				int dx = mazeNode[0] - lastX;
				int dy = mazeNode[1] - lastY;
				// System.out.println("animating " + dx + " " + dy);
				animateMove(piece, dx, dy);
				lastX = mazeNode[0];
				lastY = mazeNode[1];

				currentMove++;
				
//				mazeView.setTexts(probs.remove(0));
			}

		}

		// move the piece n by dx, dy cells
		public void animateMove(Node n, int dx, int dy) {
			animationDone = false;
			TranslateTransition tt = new TranslateTransition(
					Duration.millis(300), n);
			tt.setByX(PIXELS_PER_SQUARE * dx);
			tt.setByY(-PIXELS_PER_SQUARE * dy);
			// set a callback to trigger when animation is finished
			tt.setOnFinished(new AnimationFinished());

			tt.play();
			
			mazeView.setTexts(probs.get(currentMove));

		}

		// when the animation is finished, set an instance variable flag
		//  that is used to see if the path is ready for the next step in the
		//  animation
		private class AnimationFinished implements EventHandler<ActionEvent> {
			@Override
			public void handle(ActionEvent event) {
				animationDone = true;
//				mazeView.setTexts(probs.remove(0));

			}
		}
	}
}