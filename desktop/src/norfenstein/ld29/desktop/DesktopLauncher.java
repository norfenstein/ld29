package norfenstein.ld29.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import norfenstein.ld29.LD29;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Splashy Bird!";
		config.width = 480;
		config.height = 640;
		new LwjglApplication(new LD29(), config);
	}
}
