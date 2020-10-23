package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static uk.ac.bris.cs.scotlandyard.model.Piece.MrX.MRX;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		Model newModel = new Model() {
			Set<Observer> observers = new HashSet<>();
			MyGameStateFactory game = new MyGameStateFactory();
			Board.GameState modelState = game.build(setup, mrX, detectives);
			@Nonnull
			@Override
			public Board getCurrentBoard() {
				return modelState;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				if(observer == null)
					throw new NullPointerException();
				if(observers.contains(observer))
					throw new IllegalArgumentException();
				observers.add(observer);
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				if(observer == null)
					throw new NullPointerException();
				if(observers.contains(observer))
					observers.remove(observer);
				else
					throw new IllegalArgumentException();
			}

			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				return ImmutableSet.copyOf(observers);
			}

			@Override
			public void chooseMove(@Nonnull Move move) {
				modelState = modelState.advance(move);
				var event = modelState.getWinner().isEmpty() ? Observer.Event.MOVE_MADE : Observer.Event.GAME_OVER;
				for (Observer o : observers) o.onModelChanged(modelState, event);
			}
		};
		return newModel;
	}
}

