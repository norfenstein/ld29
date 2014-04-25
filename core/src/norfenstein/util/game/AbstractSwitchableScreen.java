package norfenstein.util.game;

import com.badlogic.gdx.InputProcessor;

public abstract class AbstractSwitchableScreen implements SwitchableScreen, InputProcessor {

	// SwitchableScreen //////////////////////////////////////////////////

	@Override public void show() { }

	@Override public void hide() { }

	@Override public InputProcessor getInputProcessor() {
		return this;
	}

	// Screen //////////////////////////////////////////////////
	
	@Override public void resize(int width, int height) { }

	@Override public void pause() { }

	@Override public void resume() { }

	@Override public void step(float delta) { }

	@Override public void processInput(float delta) { }

	@Override public abstract void render(float delta);

	// InputProcessor //////////////////////////////////////////////////

	@Override public boolean keyDown(int keycode) {
		return false;
	}

	@Override public boolean keyUp(int keycode) {
		return false;
	}

	@Override public boolean keyTyped(char character) {
		return false;
	}

	@Override public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override public boolean scrolled(int amount) {
		return false;
	}
}

