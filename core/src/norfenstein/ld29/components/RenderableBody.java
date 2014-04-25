package norfenstein.ld29;

import norfenstein.util.entities.Component;
import com.badlogic.gdx.graphics.Color;

public class RenderableBody extends Component {
	public enum FillType {
		FILLED,
		LINE,
		DIRECTED
	}

	public FillType fill;
	public Color color;
}

