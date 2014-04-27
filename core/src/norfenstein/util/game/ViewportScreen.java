package norfenstein.util.game;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public abstract class ViewportScreen extends AbstractSwitchableScreen {
	public enum FixedAxis {
		NONE,
		HORIZONTAL,
		VERTICAL
	}

	private Viewport viewport;
	private FixedAxis fixedAxis;
	private float unitsPerScreen;
	private float pixelsPerUnit;
	private float width;
	private float height;

	protected final void initializeViewport(int width, int height) {
		initializeViewport(width, height, FixedAxis.NONE, 0);
	}
	protected final void initializeViewport(int width, int height, FixedAxis fixedAxis) {
		initializeViewport(width, height, fixedAxis, 0);
	}
	protected final void initializeViewport(int width, int height, FixedAxis fixedAxis, float unitsPerScreen) {
		this.fixedAxis = fixedAxis;
		switch (this.fixedAxis) {
			case NONE:
				viewport = new FillViewport(unitsPerScreen, unitsPerScreen);
				break;
			case HORIZONTAL:
				viewport = new ExtendViewport(unitsPerScreen, (height / width * unitsPerScreen), unitsPerScreen, 0);
				break;
			case VERTICAL:
				viewport = new ExtendViewport((width / height * unitsPerScreen), unitsPerScreen, 0, unitsPerScreen);
				break;
		}

		this.unitsPerScreen = unitsPerScreen; //set manually instead of with setPixelsPerUnit() since resize() will call it anyway
		resize(width, height); //viewport.viewportWidth/Height (needed by setPixelsPerUnit) not set until update() is first called
	}

	@Override public void resize(int width, int height) {
		this.width = width;
		this.height = height;

		viewport.update(width, height);
		setPixelsPerUnit();
	}

	protected final OrthographicCamera getCamera() {
		return (OrthographicCamera)viewport.getCamera();
	}

	protected final Viewport getViewport() {
		return viewport;
	}

	protected final float getUnitsPerScreen() {
		return unitsPerScreen;
	}
	protected final void setUnitsPerScreen(float unitsPerScreen) {
		this.unitsPerScreen = unitsPerScreen;
		setPixelsPerUnit();
	}

	//at 100% zoom
	protected final float getPixelsPerUnit() {
		return pixelsPerUnit;
	}
	private void setPixelsPerUnit() {
		if (unitsPerScreen > 0) {
			switch (fixedAxis) {
				case NONE:
					pixelsPerUnit = (width >= height ? width : height) / unitsPerScreen;
					break;
				case HORIZONTAL:
					pixelsPerUnit = viewport.getViewportWidth() / unitsPerScreen;
					break;
				case VERTICAL:
					pixelsPerUnit = viewport.getViewportHeight() / unitsPerScreen;
					break;
			}
		} else {
			pixelsPerUnit = 1;
		}
	}

	protected final float pixelsToUnits(float pixels) {
		return pixels / pixelsPerUnit;
	}

	protected final float unitsToPixels(float units) {
		return units * pixelsPerUnit;
	}
}

