package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import javafx.util.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.primitives.Ints.max;
import static java.lang.Integer.min;


public class MyAi implements Ai {

	 //write the value for the minimum int (MIN)
	private final int MIN = -2147483648; //-infinity
	//write the value for the maximum int (MAX)
	private final int MAX = 2147483647; //+infinity

	@Nonnull @Override public String name() { return "Minimax MrX / Dijsktra Detectives"; }

	//The score is going to be the distance between MrX and the closest detective [...]
	// [...] plus the average between all the detectives and MrX
	int getScore(Board board, int mrXPosition, ImmutableList<Player> detectives){
		if(!board.getWinner().isEmpty()){
			if(board.getWinner().contains(Piece.MrX.MRX))
				return MAX-1;
			else
				return MIN+1;
		}

		int noOfNodes = board.getSetup().graph.nodes().size(); //there are 199 nodes [1..199]
		int[] dflsm = new int[noOfNodes+1]; //distance From Mrx
		int mrXPos = mrXPosition;
		dijsktra(dflsm, mrXPos, board.getSetup().graph, noOfNodes);
		int min = MAX;
		int nrOfDetectives = detectives.size();
		int avg = 0;
		for(final var d : detectives){
			avg = avg + dflsm[d.location()];
			if(dflsm[d.location()] < min)
				min = dflsm[d.location()];
		}
		avg = avg / nrOfDetectives;

		return min*4+avg;
	}

	int ticketPrice(ScotlandYard.Ticket t){
		if(t == ScotlandYard.Ticket.TAXI)
			return 1; // 1
		else if(t == ScotlandYard.Ticket.BUS)
			return 2; // 2
		else if(t == ScotlandYard.Ticket.UNDERGROUND)
			return 5; // 5
		else if(t == ScotlandYard.Ticket.SECRET)
			return 6; // 6
		else if(t == ScotlandYard.Ticket.DOUBLE)
			return 3; // 2
		return  0;
	}

	int getValue(ImmutableSet<ScotlandYard.Transport> transport){
		int sum = 0;
		for (final var t : transport)
			sum = sum + ticketPrice(t.requiredTicket());
		return sum;
	}

	//The array "distance" will have the distances between the start node and all the other nodes
	void dijsktra (int[] distance, int startPos, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph, int noOfNodes){
		boolean[] visited = new boolean[noOfNodes+1];
		for (int i=1; i<=noOfNodes; i++) {
			distance[i] = MAX;
			visited[i] = false;
		}
		distance[startPos] = 0;

		PriorityQueue<Integer> priorityQueue = new PriorityQueue<>(new Comparator<Integer>() {
			@Override
			public int compare(Integer integer, Integer t1) {
				return Integer.compare(distance[integer], distance[t1]);
			}
		});

		priorityQueue.add(startPos);
		while(! priorityQueue.isEmpty()){
			int currentNode = priorityQueue.poll();
			visited[currentNode] = true;
			for(int node : graph.adjacentNodes(currentNode)){
				int edgeValue = getValue(graph.edgeValue(currentNode, node).get());
				if(! visited[node] && distance[node] > distance [currentNode] + edgeValue){
					distance[node] = distance[currentNode] + edgeValue;
					priorityQueue.add(node);
				}
			}
		}
	}

	int getDetectivesBestMove(Board board){
		int noOfNodes = board.getSetup().graph.nodes().size();
		//there are 199 nodes [1..199]
		int[] dflsm = new int[noOfNodes+1]; //distance From Last Seen Mrx
		int lastSeenMrX = 82; //The position where MrX was seen the last time (initialised with the middle of the map)

		for (final var l : board.getMrXTravelLog()) { // Get the location where MrX was seen the last time
			if (l.location().isPresent())
				lastSeenMrX = l.location().get();
		}

		dijsktra(dflsm, lastSeenMrX, board.getSetup().graph, noOfNodes);

		int min = MAX; // The minimum distance between lastSeenMrX and the outcome of a move (location)
		int resultPos = 0; // The index of the chosen move
		int acctualPos = -1; // The index of every move
		var moves = board.getAvailableMoves().asList(); // The moves

		for(final var m : moves){ // Finds the move that is going to bring the detective as close to lastSeenMrX as possible
			acctualPos++;
			MakeMove myVisit = new MakeMove();
			m.visit(myVisit);                   // Finds the destination
			if ( dflsm[myVisit.destination2] <= min ){
				min = dflsm[myVisit.destination2];
				resultPos = acctualPos;
			}
		}

		return resultPos; // Returns the index of the best move
	}



	/////////                                                 CONTINUE FROM HERE                                     /////////////////////
	List<Player> getNewDetectives(Move move, List<Player> newDetectives, MakeMove myVisit, List<Player> detectives){
		Map<ScotlandYard.Ticket, Integer> newTickets = new HashMap<>();

		Player toRemove = null;
		for(var d : detectives) { //Finds the detective that made the move
			if (d.piece() == move.commencedBy()){
				newTickets = d.use(myVisit.tickets).tickets(); // Save it's remaining tickets
				toRemove = d; //Save that detective
			}
		}
		newDetectives.remove(toRemove); // Deletes it

		//Create the said detective again with it's new position and new tickets
		Player newDetective = new Player(move.commencedBy(), ImmutableMap.copyOf(newTickets), myVisit.destination2);
		newDetectives.add(newDetective); //adds it to the list

		return newDetectives; // Returns the new detective
	}



	boolean terminalNodes(int depth, Board.GameState simulationState){
		if(depth == 0 || (!simulationState.getWinner().isEmpty()) || simulationState.getAvailableMoves().isEmpty())
			return true;
		return false;
	}

	//Finds out if a move contains a ticket of type tType
	boolean containsTicket(MakeMove visit, ScotlandYard.Ticket tType){
		for( final var t : visit.tickets ) {
			if (t.equals(tType))
				return true;
		}
		return false;
	}

	Pair<Player, Board.GameState> makeDetectivesTurn(Board.GameState newBoard, List<Player> newDetectives, Player newMrX){
		//If it is still the detectives turn make a move so at the end all the detectives made a move.
		while(newBoard.getAvailableMoves().asList().size() != 0 && newBoard.getAvailableMoves().asList().get(0).commencedBy().isDetective()) {
			Move selectedMoveD = newBoard.getAvailableMoves().asList().get(getDetectivesBestMove(newBoard));

			MakeMove myVisitD = new MakeMove();
			selectedMoveD.visit(myVisitD);
			newMrX = newMrX.give(myVisitD.tickets); //Gives the ticket to mrX

			newDetectives = getNewDetectives(selectedMoveD, newDetectives, myVisitD, newDetectives);
			newBoard = newBoard.advance(selectedMoveD);
		}

		return new Pair<>(newMrX, newBoard);
	}

	//Inspired by the minimax algorithm
	Pair<Integer, Move> likeMiniMax(AtomicBoolean terminate, int depth, int safeDistance, int safeMultiplyer,
										  Board.GameState simulationState, Player mrX, ImmutableList<Player> detectives){

		if(terminalNodes(depth, simulationState)) // If it is a terminal node we return the score
			return new Pair<>(getScore(simulationState, mrX.location(), detectives), null);

		var movesMrX = simulationState.getAvailableMoves().asList().reverse(); // All mrX's moves
		int max = MIN; // The best score
		int bestMove = -1;  // The index of the best move so far
		int moveNumber = -1; // The index of every move

		for(final var m : movesMrX){ //Goes through all the possible moves
			moveNumber++; // Increments the index

			MakeMove myVisit = new MakeMove();
			m.visit(myVisit); //saves the details of the move of MrX

			boolean secretMove = containsTicket(myVisit, ScotlandYard.Ticket.SECRET); // See if the move contains a secret ticket
			boolean doubleMove = containsTicket(myVisit, ScotlandYard.Ticket.DOUBLE); // See if the move contains a double ticket

			//A list of the detectives after they moved
			List <Player> newDetectives = new ArrayList<>(detectives); //Instantiate with the original detectives

			Player newMrX = new Player(Piece.MrX.MRX, mrX.use(myVisit.tickets).tickets(), myVisit.destination2); // makes the new mrX
			Board.GameState newBoard = simulationState.advance(m); //advances in newBoard

			Pair<Player, Board.GameState> result = makeDetectivesTurn(newBoard, newDetectives, newMrX); // moves every detective
			newMrX = result.getKey();
			newBoard = result.getValue();


			int score = likeMiniMax(terminate, depth-1, safeDistance, safeMultiplyer + 1,
													newBoard, newMrX, ImmutableList.copyOf(newDetectives)).getKey();


			if(score>=0 && max > 10) { // Modify the score if the situation is not critical
				if (secretMove) //This is used for mrX to use primarily other tickets than secret and double
					score = score - 2;
				if (doubleMove)
					score = score - 5;
			}

			if(score/5 >= (safeDistance * safeMultiplyer)) // If the move is considered safe return it
				return new Pair<>(score, m);

			if(score > max){ // selects the best move in bestMove
				max = score;
				bestMove = moveNumber;
			}

//			if(terminate.getAcquire()) // if the time is over returns the best move yet (I heard that it is bugged because of the UI)
//				return new Pair<>(max, movesMrX.get(bestMove));
		}

		return new Pair<>(max, movesMrX.get(bestMove)); // returns the move selected for mrX (and the best score)

	}

	Map<ScotlandYard.Ticket, Integer> getPlayerTickets(Board board, Piece piece){
		Map<ScotlandYard.Ticket, Integer> tickets = new HashMap<>();

		for(final var t : ScotlandYard.Ticket.values())
			if(board.getPlayerTickets(piece).isPresent())
				tickets.put(t, board.getPlayerTickets(piece).get().getCount(t));
			else
				tickets.put(t, 0);

		return tickets;
	}

	List<Player> getDetectives(Board board){
		List<Player> detectives = new ArrayList<>();
		for (final var p : board.getPlayers()){
			if(p.isDetective()){
				ImmutableMap<ScotlandYard.Ticket, Integer> ticketsD = ImmutableMap.copyOf(getPlayerTickets(board, p));
				for(final var d : Piece.Detective.values())
					if(p == d && board.getDetectiveLocation(d).isPresent()){
						detectives.add(new Player(p, ticketsD, board.getDetectiveLocation(d).get()));
					}
			}

		}
		return detectives;
	}

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			@Nonnull AtomicBoolean terminate) {

		var moves = board.getAvailableMoves().asList(); // save the available moves

		//If it's MRX's turn we will make a minimax inspired algorithm
		//MrX will choose the move that will bring him the furthest  from the nearest detective after "depth" turns
		//It can also receive a safeDistance (the distance at which he will feel safe). We considered that [...]
		//[...] if mrX is at 10 nodes away from the nearest detective he will find that enough to choose that move.
		//The safeMultiplyer  will make sure that mrX will go at a safe distance even after a some rounds (with [...]
		//[...] every round the safeDistance will increase to make sure that MrX won't get to close to the detectives [...]
		//[...] after the earlier rounds).
		if(moves.get(0).commencedBy().isMrX()){

			//Makes the Player Mrx so it can build a game state
			Player mrX = new Player(Piece.MrX.MRX, ImmutableMap.copyOf(getPlayerTickets(board, Piece.MrX.MRX)), moves.get(0).source());

			MyGameStateFactory game = new MyGameStateFactory();
			ImmutableList<Player> detectives = ImmutableList.copyOf(getDetectives(board)); // Makes all the detectives
			Board.GameState simulationState = game.build(board.getSetup(), mrX, detectives); // Creates the game state

			return likeMiniMax(terminate, 2, 10, 1, simulationState, mrX, detectives).getValue(); // Finds the best move

		}
		else{ // If it is the detectives turn it will make the detectives to make the best move so they come closer [...]
			  // [...] to the last node where mrX was seen (using dijsktra's algorithm)
			return moves.get(getDetectivesBestMove(board)); // Gets the best move
		}
	}
}
