package jetoze.iota;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

public final class Grid {

	private final Table<Integer, Integer, Card> grid = HashBasedTable.create();
	
	public void start(Card card) {
		checkState(grid.isEmpty());
		checkNotNull(card);
		grid.put(0, 0, card);
	}
	
	public boolean isCardAllowed(Card card, int row, int col) {
		checkNotNull(card);
		NewCardEffect e = new NewCardEffect(card, new Position(row, col));
		return e.isValid();
	}
	
	/**
	 * Adds a new card to the grid. Returns a set containing all cards in the lines
	 * that were appended to as a result.
	 */
	public ImmutableList<Card> addCard(Card card, int row, int col) {
		return addCard(card, new Position(row, col));
	}

	public ImmutableList<Card> addCard(Card card, Position position) {
		NewCardEffect e = new NewCardEffect(card, position);
		checkArgument(e.isValid());
		e.apply();
		return e.getListOfPointCards();
	}
	
	public int addLine(LineItem... cards) {
		checkArgument(cards.length > 0);
		List<Card> pointCards = new ArrayList<>();
		for (LineItem card : cards) {
			pointCards.addAll(addCard(card.getCard(), card.getPosition()));
		}
		return pointCards.stream()
				.mapToInt(Card::getFaceValue)
				.sum();
	}
	
	@Nullable
	private Line createHorizontalLine(Card newCard, Position p) {
		Position start = findStartOfRow(p);
		Position end = findEndOfRow(p);
		return createLine(newCard, p, start, end, Position::rightOf);
	}
	
	@Nullable
	private Line createVerticalLine(Card newCard, Position p) {
		Position start = findStartOfColumn(p);
		Position end = findEndOfColumn(p);
		return createLine(newCard, p, start, end, Position::below);
	}
	
	@Nullable
	private Line createLine(Card newCard,
							Position newCardPosition,
						 	Position start, 
						 	Position end, 
						 	Function<Position, Position> nextGenerator) {
		if (start.equals(end)) {
			return Line.singleCard(newCard, newCardPosition);
		}
		List<LineItem> items = new ArrayList<>();
		for (Position p = start; ; p = nextGenerator.apply(p)) {
			Card card = grid.get(p.row, p.col);
			if (card == null) {
				card = newCard;
			}
			items.add(new LineItem(card, p));
			if (p.equals(end)) {
				break;
			}
		}
		MatchType matchType = deduceMatchType(items.stream()
				.map(LineItem::getCard)
				.collect(Collectors.toList()));
		return (matchType != null)
				? new Line(items, matchType)
				: null;
	}
	
	@Nullable
	private static MatchType deduceMatchType(List<Card> line) {
		// Three different match types:
		// SAME == All cards must share the same property. Requires at least 
		//   two non-wildcards in the line.
		// DIFFERENT == No two cards can share a property. Requires at least 
		//   two non-wildcards in the line.
		// EITHER == We don't know yet. This will be the case if the line contains
		//   at most one concrete card.
		if (line.size() > Constants.MAX_LINE_LENGTH) {
			// The line is too long.
			return null;
		}
		long numberOfConcreteCards = line.stream()
				.filter(c -> !c.isWildcard())
				.count();
		if (numberOfConcreteCards <= 1) {
			return MatchType.EITHER;
		}
		Set<Object> matches = null;
		Set<Object> all = null;
		boolean allUnique = true;
		for (Card card : line) {
			if (matches == null) {
				// The first card in the line. If the first card is a WC
				// we move on to the next one, since a WC does not have any
				// inherent properties itself.
				if (!card.isWildcard()) {
					matches = card.getMatchProperties();
					all = new HashSet<>(matches);
				}
			} else {
				matches = card.match(matches);
				if (allUnique) {
					Set<Object> cardProperties = card.getMatchProperties();
					int expectedSizeOfAllProperties = all.size() + cardProperties.size();
					all.addAll(cardProperties);
					if (all.size() < expectedSizeOfAllProperties) {
						allUnique = false;
					}
				}
				all.addAll(card.getMatchProperties());
			}
		}
		if (matches == null) {
			// This is the case of a line consisting of wildcards only.
			return MatchType.EITHER;
		} else if (!matches.isEmpty()) {
			// All the cards share a common property
			return MatchType.SAME;
		} else if (allUnique) {
			// No matching property
			return MatchType.DIFFERENT;
		} else {
			// No match
			return null;
		}
	}
	
	private boolean contains(Position p) {
		return grid.contains(p.row, p.col);
	}
	
	private Position findStartOfRow(Position p) {
		return findEndpoint(p, Position::leftOf);
	}
	
	private Position findEndOfRow(Position p) {
		return findEndpoint(p, Position::rightOf);
	}
	
	private Position findStartOfColumn(Position p) {
		return findEndpoint(p, Position::above);
	}
	
	private Position findEndOfColumn(Position p) {
		return findEndpoint(p, Position::below);
	}

	private Position findEndpoint(Position start, Function<Position, Position> nextPositionGenerator) {
		Position p0 = start;
		Position p = nextPositionGenerator.apply(p0);
		while (contains(p)) {
			p0 = p;
			p = nextPositionGenerator.apply(p);
		}
		return p0;
	}
	
	
	private class NewCardEffect {
		
		private final Card newCard;
		
		private final Position position;
		
		@Nullable
		private final Line horizontalLine;
		
		@Nullable
		private final Line verticalLine;

		public NewCardEffect(Card newCard, Position position) {
			this.newCard = checkNotNull(newCard);
			this.position = checkNotNull(position);
			this.horizontalLine = createHorizontalLine(newCard, position);
			this.verticalLine = createVerticalLine(newCard, position);
		}

		public boolean isValid() {
			if ((this.horizontalLine == null) || (this.verticalLine == null)) {
				return false;
			}
			if (grid.isEmpty()) {
				// First card is by definition placed in origo.
				assert horizontalLine.length() == 1;
				assert verticalLine.length() == 1;
				return position.row == 0 && position.col == 0;
			} else {
				if (contains(position)) {
					return false;
				}
				if (horizontalLine.length() == 1 && verticalLine.length() == 1) {
					// At least one of the lines must contain more than one card.
					// (This ensures all cards are connected in the grid.)
					return false;
				}
				return validateWildcards();
			}
		}

		private boolean validateWildcards() {
			// Wildcard validation - ensure that a wildcard that appears in two lines
			// represent the same card in both lines. Pseudo-code:
			// for each wc in this.hLine:
			//   if wc also in a vLine (not necessarily this.vLine)
			//     collect possible card properties from this.hLine
			//     collect possible card properties from vLine
			//     look for matching set of properties
			// for each wc in this.vLine:
			//   if wc also in an hLine (not necessarily this.hLine)
			//     collect possible card properties from this.vLine
			//     collect possible card properties from hLine
			//     look for matching set of properties
			for (LineItem wcItem : this.horizontalLine.getWildcardItems()) {
				Line vLine = createVerticalLine(wcItem.getCard(), wcItem.getPosition());
				if (vLine.length() == 1) {
					continue;
				}
				Set<Card> hLineCandidates = this.horizontalLine.collectCandidatesForNextCard();
				Set<Card> vLineCandidates = vLine.collectCandidatesForNextCard();
				Set<Card> candidates = hLineCandidates;
				candidates.retainAll(vLineCandidates);
				if (candidates.isEmpty()) {
					return false;
				}
			}
			for (LineItem wcItem : this.verticalLine.getWildcardItems()) {
				Line hLine = createHorizontalLine(wcItem.getCard(), wcItem.getPosition());
				if (hLine.length() == 1) {
					continue;
				}
				Set<Card> vLineCandidates = this.verticalLine.collectCandidatesForNextCard();
				Set<Card> hLineCandidates = hLine.collectCandidatesForNextCard();
				Set<Card> candidates = vLineCandidates;
				candidates.retainAll(hLineCandidates);
				if (candidates.isEmpty()) {
					return false;
				}
			}
			// Hooray, we have a valid line!
			return true;
		}
		
		public void apply() {
			grid.put(position.row, position.col, newCard);
		}
		
		public ImmutableList<Card> getListOfPointCards() {
			checkState(this.horizontalLine != null && this.verticalLine != null);
			ImmutableList.Builder<Card> builder = ImmutableList.builder();
			// Do not include a card that belongs to a single-item line,
			// since that card will be counted in the other line. For example,
			// when adding a third card to a horizontal line, this.verticalLine
			// will be a single-card line containing the new card.
			// Only if a card appears in two multi-card lines should it be 
			// counted twice.
			if (this.horizontalLine.length() > 1) {
				builder.addAll(this.horizontalLine.getCards());
			}
			if (this.verticalLine.length() > 1) {
				builder.addAll(this.verticalLine.getCards());
			}
			return builder.build();
		}
	}
	
	
	private static class Line {
		
		private final ImmutableList<LineItem> items;
		
		private final MatchType matchType;
		
		public Line(List<LineItem> items, MatchType matchType) {
			this.items = ImmutableList.copyOf(items);
			this.matchType = matchType;
		}
		
		public static Line singleCard(Card card, Position pos) {
			return new Line(ImmutableList.of(new LineItem(card, pos)), MatchType.EITHER);
		}
		
		public int length() {
			return items.size();
		}
		
		public List<Card> getCards() {
			return items.stream()
					.map(LineItem::getCard)
					.collect(Collectors.toList());
		}
		
		public List<LineItem> getWildcardItems() {
			return items.stream()
					.filter(i -> i.getCard().isWildcard())
					.collect(Collectors.toList());
		}
		
		public Set<Card> collectCandidatesForNextCard() {
			List<Card> cards = getCards();
			return matchType.collectCandidatesForNextCard(cards);
		}
	}
	
}
