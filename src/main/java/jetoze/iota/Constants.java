package jetoze.iota;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class Constants {
	
	public static final int MAX_LINE_LENGTH = 4;
	
	public static final int MIN_FACE_VALUE = 1;
	
	public static final int MAX_FACE_VALUE = MAX_LINE_LENGTH;
	
	public static final int NUMBER_OF_WILDCARDS = 2;
	
	public static final int NUMBER_OF_CARDS_PER_PLAYER = MAX_LINE_LENGTH;
	
	public static final int MAX_NUMBER_OF_PLAYERS = 4;
	
	public static enum Color {
		
		RED, GREEN, BLUE, YELLOW
		
	}


	public static enum Shape {
		
		CIRCLE, SQUARE, TRIANGLE, CROSS
	}

	
	public static Set<Object> collectAllCardProperties() {
		Set<Object> props = new HashSet<>();
		props.addAll(EnumSet.allOf(Color.class));
		props.addAll(EnumSet.allOf(Shape.class));
		for (int i = MIN_FACE_VALUE; i <= MAX_FACE_VALUE; ++i) {
			props.add(i);
		}
		return props;
	}

	private Constants() {/**/}

}
