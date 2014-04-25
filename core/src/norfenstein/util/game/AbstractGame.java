package norfenstein.util.game;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;

public abstract class AbstractGame implements ApplicationListener {
	protected final float timeStep;
	private float remainingTime;

	protected abstract Screen getCurrentScreen();

	protected AbstractGame(float timeStep) {
		this.timeStep = timeStep;
		remainingTime = 0f;
	}

	@Override public abstract void create();

	@Override public void dispose() { }

	@Override public final void pause() {
		getCurrentScreen().pause();
	}

	@Override public final void resume() {
		getCurrentScreen().resume();
	}

	@Override public void render() {
		for (remainingTime += Gdx.graphics.getRawDeltaTime(); remainingTime >= timeStep; remainingTime -= timeStep) {
			getCurrentScreen().step(timeStep);
		}
		getCurrentScreen().processInput(Gdx.graphics.getRawDeltaTime());
		getCurrentScreen().render(Gdx.graphics.getRawDeltaTime());
	}

	@Override public final void resize(int width, int height) {
		getCurrentScreen().resize(width, height);
	} 
}

