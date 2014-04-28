package norfenstein.ld29;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

public class SoundManager {
	private Array<Sound> gulpSounds;
	private Array<Sound> splashSounds;

	public SoundManager() {
		gulpSounds = new Array<Sound>();
		splashSounds = new Array<Sound>();
	}

	public Sound getGulp() {
		return gulpSounds.get(MathUtils.random(0, gulpSounds.size - 1));
	}

	public Sound getSplash() {
		return splashSounds.get(MathUtils.random(0, splashSounds.size - 1));
	}

	public void create() {
		gulpSounds = new Array<Sound>();
		gulpSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/gulp1.wav")));
		gulpSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/gulp2.wav")));
		gulpSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/gulp3.wav")));
		gulpSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/gulp4.wav")));
		gulpSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/gulp5.wav")));
		gulpSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/gulp6.wav")));
		gulpSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/gulp7.wav")));
		gulpSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/gulp8.wav")));
		gulpSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/gulp9.wav")));

		splashSounds = new Array<Sound>();
		splashSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/splash1.wav")));
		splashSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/splash2.wav")));
		splashSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/splash3.wav")));
		splashSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/splash4.wav")));
		splashSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/splash5.wav")));
		splashSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/splash6.wav")));
		splashSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/splash7.wav")));
		splashSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/splash8.wav")));
		splashSounds.add(Gdx.audio.newSound(Gdx.files.internal("sounds/splash9.wav")));
	}

	public void dispose() {
		for (Sound sound : gulpSounds) {
			sound.dispose();
		}

		for (Sound sound : splashSounds) {
			sound.dispose();
		}
	}
}
