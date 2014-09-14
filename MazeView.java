
import java.util.ArrayList;

import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class MazeView extends Group {

	private int pixelsPerSquare;
	private Maze maze;
	private ArrayList<Node> pieces;
	
	private int numCurrentAnimations;
	
	private static Color[] colors = {Color.WHITE, Color.BLACK, Color.GREY, Color.VIOLET, Color.LIGHTBLUE, Color.ORANGE, Color.BROWN,
		Color.DARKGOLDENROD, Color.CRIMSON};

	int currentColor;
	
	private Text[][] labels;
	
	public MazeView(Maze m, int pixelsPerSquare) {
		currentColor = 0;
		
		pieces = new ArrayList<Node>();
		
		maze = m;
		this.pixelsPerSquare = pixelsPerSquare;

//		Color colors[] = { Color.LIGHTGRAY, Color.WHITE };
	//	int color_index = 1; // alternating index to select tile color
		
		labels = new Text[maze.height][maze.width];

		for (int c = 0; c < maze.width; c++) {
			for (int r = 0; r < maze.height; r++) {

				int x = c * pixelsPerSquare;
				int y = (maze.height - r - 1) * pixelsPerSquare;

				Rectangle square = new Rectangle(x, y, pixelsPerSquare,
						pixelsPerSquare);

				square.setStroke(Color.GRAY);
				switch (maze.getChar(c, r)) {
				case 'r': square.setFill(Color.RED); break;
				case 'g': square.setFill(Color.GREEN); break;
				case 'b': square.setFill(Color.BLUE); break;
				case 'y': square.setFill(Color.YELLOW); break;
				default: square.setFill(Color.LIGHTGRAY); break;
				}

				labels[r][c] = new Text(x, y + 12, "" + c +","+ r);

				this.getChildren().add(square);
				this.getChildren().add(labels[r][c]);

		
			}
		
		}

	}

	public void setTexts(double[][] probs) {
		for (int i = 0; i < maze.height; i++)
			for (int j = 0; j < maze.width; j++) {
				String str = String.format("%.5f", probs[i][j]);
				labels[i][j].setText(str);
			}
	}
	
	private int squareCenterX(int c) {
		return c * pixelsPerSquare + pixelsPerSquare / 2;
		
	}
	private int squareCenterY(int r) {
		return (maze.height - r) * pixelsPerSquare - pixelsPerSquare / 2;
	}
	
	// create a new piece on the board.
	//  return the piece as a Node for use in animations
	public Node addPiece(int c, int r) {
		
		int radius = (int)(pixelsPerSquare * .3);

		Circle piece = new Circle(squareCenterX(c), squareCenterY(r), radius);
		piece.setFill(colors[currentColor]);
		currentColor++;
		
		this.getChildren().add(piece);
		return piece;
		
	}
	
	
	/*
	public boolean doMove(short move) {
	
		
		Timeline timeline = new Timeline();

		if (timeline != null) {
			timeline.stop();
		}

		animateMove(l, c2 - c1, r2 - r1);

		this.game.doMove(move);

		return true;



	}
	
	*/




}
