package norfenstein.util.game;

public interface Screen {
	void resize(int width, int height);
	void pause();
	void resume();

	void step(float delta);
	void processInput(float delta);
	void render(float delta);
}

