package norfenstein.util.game;

import com.badlogic.gdx.Gdx;

public abstract class AbstractSwitchableGame extends AbstractGame {
	private SwitchableScreen currentScreen;

	protected AbstractSwitchableGame(float timeStep) {
		super(timeStep);
	}

	@Override protected final Screen getCurrentScreen() {
		return currentScreen;
	}

	protected final void switchScreen(SwitchableScreen nextScreen) {
		if (currentScreen != null) {
			currentScreen.hide();
		}

		currentScreen = nextScreen;

		if (currentScreen != null) {
			Gdx.input.setInputProcessor(currentScreen.getInputProcessor());
			currentScreen.show();
			currentScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		}
	}
}

