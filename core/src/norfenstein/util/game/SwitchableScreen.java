package norfenstein.util.game;

import com.badlogic.gdx.InputProcessor;

public interface SwitchableScreen extends Screen {
	void show();
	void hide();
	InputProcessor getInputProcessor();
}

