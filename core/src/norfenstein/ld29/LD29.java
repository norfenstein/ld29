package norfenstein.ld29;

import norfenstein.util.game.AbstractSwitchableGame;

public final class LD29 extends AbstractSwitchableGame {
	private final GameScreen gameScreen;

	public LD29() {
		super(1f / 60);
		gameScreen = new GameScreen();
	}

	@Override public void create() {
		gameScreen.create();

		switchScreen(gameScreen);
	}

	@Override public void dispose() {
		gameScreen.dispose();
	}
}

