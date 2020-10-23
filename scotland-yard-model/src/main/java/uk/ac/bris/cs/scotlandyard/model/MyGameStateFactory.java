package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.Piece.Detective;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import javax.annotation.Nonnull;
import java.util.*;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

    @Nonnull
    @Override
    public GameState build(
            GameSetup setup,
            Player mrX,
            ImmutableList<Player> detectives) {
        return new MyGameState(setup, ImmutableSet.of(MRX), ImmutableList.of(), mrX, detectives);
    }

    private final class MyGameState implements GameState {
        private GameSetup setup;
        private ImmutableSet<Piece> remaining;
        private ImmutableList<LogEntry> log;
        private Player mrX;
        private List<Player> detectives;
        private ImmutableList<Player> everyone;
        private ImmutableSet<Move> moves;
        private ImmutableSet<Piece> winner = ImmutableSet.of();

        private MyGameState(final GameSetup setup,
                            final ImmutableSet<Piece> remaining,
                            final ImmutableList<LogEntry> log,
                            final Player mrX,
                            final List<Player> detectives) {
            this.setup = Objects.requireNonNull(setup);
            this.remaining = Objects.requireNonNull(remaining);
            this.log = Objects.requireNonNull(log);
            this.mrX = Objects.requireNonNull(mrX);
            this.detectives = Objects.requireNonNull(detectives);

            if (setup.rounds.isEmpty() || setup.graph.nodes().isEmpty()) throw new IllegalArgumentException();

            Set<Integer> locations = new HashSet<Integer>();
            for (final var d : detectives) {
                if (d.has(ScotlandYard.Ticket.DOUBLE)
                        || d.has(ScotlandYard.Ticket.SECRET)
                        || locations.contains(d.location()))
                    throw new IllegalArgumentException();
                locations.add(d.location());
            }
        }

        @Override
        public GameSetup getSetup() {
            return setup;
        }

        @Override
        public ImmutableSet<Piece> getPlayers() {
            Set<Piece> players = new HashSet<>();
            players.add(mrX.piece());
            for (final var d : detectives) {
                players.add(d.piece());
            }
            return ImmutableSet.copyOf(players);
        }

        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Detective detective) {
            for (final var d : detectives) {
                if (d.piece() == detective) return Optional.of(d.location());
            }
            return Optional.empty();
        }

        boolean isAvailablePiece(Piece piece){
            for(final var d : detectives) {
                if (d.piece() == piece)
                    return true;
            }

            return piece == MRX;
        }

        @Nonnull
        @Override
        public Optional<TicketBoard> getPlayerTickets(Piece piece) {

            if(! isAvailablePiece(piece))
                return Optional.empty();

            class TB implements TicketBoard {
                @Override
                public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
                    if (piece.isMrX()) return mrX.tickets().get(ticket);
                    for (final var d : detectives) {
                        if (d.piece() == piece) return d.tickets().get(ticket);
                    }
                    return 0;
                }
            }

            TB ticketBoard = new TB();

            for (final var t : ScotlandYard.Ticket.values()) {
                if (ticketBoard.getCount(t) >= 0) return Optional.of(ticketBoard);
            }
            return Optional.empty();
        }

        @Nonnull
        @Override
        public ImmutableList<LogEntry> getMrXTravelLog() {
            return log;
        }

        boolean detectivesHaveTickets(){
            for (final var d : detectives) {
                for (final var t : d.tickets().values()) {
                    if (t != 0) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getWinner() {
            getAvailableMoves();

            if(remaining.isEmpty()){
                winner = ImmutableSet.of(MRX);
                return winner;
            }
            
            if (!detectivesHaveTickets()) {
                winner = ImmutableSet.of(mrX.piece());
                return winner;
            }

            final var detectiveSet = new HashSet<Piece>();
            for (final var d : detectives) detectiveSet.add(d.piece());

            for (final var d : detectives) {
                if (d.location() == mrX.location()) {
                    winner = ImmutableSet.copyOf(detectiveSet);
                    return winner;
                }
            }

            if (remaining.contains(mrX.piece())) {
                if (moves.isEmpty()) {
                    winner = ImmutableSet.copyOf(detectiveSet);
                    return winner;
                }
            }
            if(!remaining.contains((MRX))){
                if (moves.isEmpty()) {
                    winner = ImmutableSet.of(mrX.piece());
                    return winner;
                }
            }

            winner = ImmutableSet.of();
            return winner;
        }

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            if (! winner.isEmpty()) {
                moves = ImmutableSet.of();
                return moves;
            }

            Set<Move> availableMoves = new HashSet<>();
            for (final var d : detectives) {
                if (remaining.contains(d.piece()))
                    availableMoves.addAll(makeSingleMoves(setup, detectives, d, d.location()));
            }
            if (remaining.contains(mrX.piece())){
                availableMoves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
                availableMoves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
            }

            moves = ImmutableSet.copyOf(availableMoves);
            return moves;
        }

        void getNewDetectivesMrX(List <Player> newDetectives){
            newDetectives.addAll(detectives);
        }

        void getNewRemainingMrX(Set <Piece> newRemaining){
            for(final var d : detectives)
                newRemaining.add(d.piece());
        }

        void updateLogFirstMove(MakeMove myVisit, List <LogEntry> newLog) {
            if (setup.rounds.get(log.size()))
                newLog.add(LogEntry.reveal(myVisit.tickets.get(0), myVisit.destination1));
            else
                newLog.add(LogEntry.hidden(myVisit.tickets.get(0)));
        }

        void updateLogSecondMove(MakeMove myVisit, List <LogEntry> newLog) {
            if (setup.rounds.get(newLog.size()))
                newLog.add(LogEntry.reveal(myVisit.tickets.get(1), myVisit.destination2));
            else
                newLog.add(LogEntry.hidden(myVisit.tickets.get(1)));
        }

        Player advanceMrX(Move move, List <LogEntry> newLog, Set <Piece> newRemaining,  List <Player> newDetectives) {
            MakeMove myVisit = new MakeMove();
            move.visit(myVisit);

            getNewDetectivesMrX(newDetectives);
            getNewRemainingMrX(newRemaining);
            updateLogFirstMove(myVisit, newLog);

            if (myVisit.moveType)
                updateLogSecondMove(myVisit, newLog);

            return new Player(MRX, mrX.use(myVisit.tickets).tickets(), myVisit.destination2);
        }

        Map<ScotlandYard.Ticket, Integer> getNewDetectiveTickets(Move move, MakeMove myVisit) {
            for (final var d : detectives) {
                if (d.piece() == move.commencedBy())
                    return d.use(myVisit.tickets).tickets();
            }
            return null;
        }

        void getNewRemaining(Move move, Set <Piece> newRemaining) {
            for (final var r : remaining) {
                if (r != move.commencedBy()) {
                    for (final var d : detectives) {
                        if (d.piece() == r) {
                            boolean hasAtLeastOneTicket = false;
                            for (final var ticketValue : d.tickets().values()) {
                                if (ticketValue > 0) {
                                    hasAtLeastOneTicket = true;
                                    break;
                                }
                            }
                            if (hasAtLeastOneTicket)
                                newRemaining.add(r);
                        }
                    }
                }
            }
            if(newRemaining.isEmpty())
                newRemaining.add(mrX.piece());
        }

        void getNewDetectives(Move move, List <Player> newDetectives, Map<ScotlandYard.Ticket, Integer> newTickets, MakeMove myVisit){
            for(final var d : detectives)
                if(d.piece() != move.commencedBy())
                    newDetectives.add(d);

            Player newDetective = new Player(move.commencedBy(), ImmutableMap.copyOf(newTickets), myVisit.destination2);

            newDetectives.add(newDetective);
        }

        Player advanceDetectives(Move move, List <LogEntry> newLog, Set <Piece> newRemaining,  List <Player> newDetectives){
            MakeMove myVisit = new MakeMove();
            move.visit(myVisit);

            Map<ScotlandYard.Ticket, Integer> newTickets = new HashMap<>();

            newTickets = getNewDetectiveTickets(move, myVisit);
            getNewDetectives(move, newDetectives, newTickets, myVisit);
            getNewRemaining(move, newRemaining);

//            if (newRemaining.contains(mrX.piece()))
//                logIndex++;

            return new Player(MRX, mrX.give(myVisit.tickets).tickets(), mrX.location());
        }

        @Override
        public GameState advance(Move move) {
            final List<LogEntry> newLog = new ArrayList<>(log);
            final ImmutableSet<Piece> newRemaining;
            final List <Player> newDetectives = new ArrayList<>();
            final Player newMrX;

            getAvailableMoves();

            if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

            Set<Piece> newRemainingAdvance = new HashSet<>();

            if (move.commencedBy().isMrX())
                newMrX = advanceMrX(move, newLog, newRemainingAdvance, newDetectives);
            else
               newMrX = advanceDetectives(move, newLog, newRemainingAdvance, newDetectives);

            if(log.size() == setup.rounds.size()){
                newRemainingAdvance.clear();
                newRemaining = ImmutableSet.copyOf(newRemainingAdvance);
                GameState newGameState = new MyGameState(setup, newRemaining, ImmutableList.copyOf(newLog), newMrX, ImmutableList.copyOf(newDetectives) );
                return newGameState;
            }

            newRemaining = ImmutableSet.copyOf(newRemainingAdvance);
            return new MyGameState(setup, newRemaining, ImmutableList.copyOf(newLog), newMrX, ImmutableList.copyOf(newDetectives));
        }
    }

    private static ImmutableSet<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
        final var singleMoves = new ArrayList<Move.SingleMove>();
        for (int destination : setup.graph.adjacentNodes(source)) {
            var occupied = false;
            for (final var d : detectives) {
                if (d.location() == destination) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) continue;
            for (ScotlandYard.Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
                if (player.has(t.requiredTicket()))
                    singleMoves.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
                if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1) && t.requiredTicket()!= ScotlandYard.Ticket.SECRET)
                    singleMoves.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
            }
        }

        return ImmutableSet.copyOf(singleMoves);
    }

    private static ImmutableSet<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source) {
        final var doubleMoves = new ArrayList<Move.DoubleMove>();
        final ImmutableSet<Move.SingleMove> singleMoves = makeSingleMoves(setup, detectives, player, source);

        if (player.hasAtLeast(ScotlandYard.Ticket.DOUBLE, 1) && setup.rounds.size()>=2) {
            for (final var m : singleMoves) {
                for (int destination : setup.graph.adjacentNodes(m.destination)) {
					var occupied = false;
                    for (final var d : detectives) {
                        if (d.location() == destination) {
                            occupied = true;
                            break;
                        }
                    }
                    if (occupied) continue;
                    for (ScotlandYard.Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(m.destination, destination, ImmutableSet.of()))) {
                        if (player.has(t.requiredTicket()))
                        	if (player.has(t.requiredTicket()) && m.ticket != t.requiredTicket())
								doubleMoves.add(new Move.DoubleMove(player.piece(), m.source(), m.ticket, m.destination, t.requiredTicket(), destination));
                        	else if (m.ticket == t.requiredTicket() && player.hasAtLeast(t.requiredTicket(), 2))
                            	doubleMoves.add(new Move.DoubleMove(player.piece(), m.source(), m.ticket, m.destination, t.requiredTicket(), destination));
                        if (player.hasAtLeast(ScotlandYard.Ticket.SECRET, 1) && t.requiredTicket()!= ScotlandYard.Ticket.SECRET)
							if(m.ticket == t.requiredTicket() && player.hasAtLeast(t.requiredTicket(), 2))
                            	doubleMoves.add(new Move.DoubleMove(player.piece(), m.source(), m.ticket, m.destination, ScotlandYard.Ticket.SECRET, destination));
							else if (m.ticket != t.requiredTicket())
								doubleMoves.add(new Move.DoubleMove(player.piece(), m.source(), m.ticket, m.destination, ScotlandYard.Ticket.SECRET, destination));
                    }
                }
            }
        }
        return ImmutableSet.copyOf(doubleMoves);
    }
}

