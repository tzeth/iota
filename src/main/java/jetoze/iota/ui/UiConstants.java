package jetoze.iota.ui;

import java.awt.Graphics2D;
import java.util.EnumMap;

import jetoze.iota.Constants.Color;
import jetoze.iota.Constants.Shape;

public final class UiConstants {

	public static final int CARD_SIZE = 80;
	
	public static final int FACE_VALUE_MARKER_GAP = 14;
	
	// The number 50 is based on a back-of-an-envelope calculation, using the
	// standard setup of 64 + 2 (wild) cards.
	public static final int NUMBER_OF_CELLS_PER_SIDE_IN_GRID = 50;
	
	public static final int GRID_CELL_MARGIN = 2;
	
	public static final int CROSS_PROTRUSION = 16;
	
	private static final EnumMap<Color, java.awt.Color> CARD_COLORS = new EnumMap<>(Color.class);
	static {
		CARD_COLORS.put(Color.RED, java.awt.Color.RED);
		CARD_COLORS.put(Color.GREEN, java.awt.Color.GREEN.darker());
		CARD_COLORS.put(Color.BLUE, new java.awt.Color(30, 144, 255));
		CARD_COLORS.put(Color.YELLOW, new java.awt.Color(255, 165, 0));
	}
	
	public static void applyCardColor(Graphics2D g, Color cardColor) {
		java.awt.Color awt = CARD_COLORS.get(cardColor);
		g.setColor(awt);
	}

	public static int getFaceValueMarkerSize(Shape cardShape) {
		return (cardShape == Shape.CROSS)
				? 10
				: 8;
	}
	
	private UiConstants() {/**/}
	
}
