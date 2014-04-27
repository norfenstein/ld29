package norfenstein.ld29;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;

public final class BuoyancyController {
	private ObjectSet<Body> bodies;

	private final Vector2 surfaceNormal;
	private final Vector2 fluidVelocity;
	private final Vector2 gravity;
	private float linearDrag;
	private float angularDrag;
	private float surfaceHeight;
	private float fluidDensity;

	private final Vector2 tempVector;
	private final Vector2 sc;
	private final Vector2 areac;
	private final Vector2 massc;

	public BuoyancyController(Vector2 surfaceNormal, Vector2 fluidVelocity, Vector2 gravity, float surfaceHeight, float fluidDensity, float linearDrag, float angularDrag) {
		bodies = new ObjectSet<Body>();

		this.surfaceNormal = new Vector2(surfaceNormal);
		this.fluidVelocity = new Vector2(fluidVelocity);
		this.gravity = new Vector2(gravity);
		this.surfaceHeight = surfaceHeight;
		this.fluidDensity = fluidDensity;
		this.linearDrag = linearDrag;
		this.angularDrag = angularDrag;

		tempVector = new Vector2();
		sc = new Vector2();
		areac = new Vector2();
		massc = new Vector2();
	}

	public void addBody(Body body) {
		bodies.add(body);
	}

	public void removeBody(Body body) {
		bodies.remove(body);
	}

	public void step() {
		for (Body body : bodies) {
			Array<Fixture> fixtures = body.getFixtureList();
			for (int i = 0; i < fixtures.size; i++) {
				applyToFixture(fixtures.get(i));
			}
		}
	}

	private boolean applyToFixture(Fixture fixture) {
		if (fixture.isSensor() || fixture.getDensity() == 0) {
			return false;
		}

		Body body = fixture.getBody();

		sc.set(Vector2.Zero);
		float sarea;
		switch (fixture.getShape().getType()) {
			case Circle:
				sarea = computeSubmergedArea((CircleShape)fixture.getShape(), surfaceNormal, surfaceHeight, body.getTransform(), sc);
				break;

			default:
				return false;
		}

		float area = sarea;
		areac.set(Vector2.Zero);
		areac.x += sarea * sc.x;
		areac.y += sarea * sc.y;

		float mass = sarea * fixture.getDensity();
		massc.set(Vector2.Zero);
		massc.x += sarea * sc.x * fixture.getDensity();
		massc.y += sarea * sc.y * fixture.getDensity();

		areac.x /= area;
		areac.y /= area;
		massc.x /= mass;
		massc.y /= mass;

		if (area < Float.MIN_VALUE) {
			return false;
		}

		// buoyancy force
		tempVector.set(gravity).scl(-fluidDensity * area);
		body.applyForce(tempVector, massc, true); // multiply by -density to invert gravity

		// linear drag
		tempVector.set(body.getLinearVelocityFromWorldPoint(areac).sub(fluidVelocity).scl(-linearDrag * area));
		body.applyForce(tempVector, areac, true);

		// angular drag
		float bodyMass = body.getMass();
		if (bodyMass < 1) bodyMass = 1; // prevent a huge torque from being generated...
		body.applyTorque(-body.getInertia() / bodyMass * area * body.getAngularVelocity() * angularDrag, true);

		return true;
	}


	public static float computeSubmergedArea(CircleShape shape, Vector2 normal, float offset, Transform xf, Vector2 c) {
		Vector2 p = xf.mul(shape.getPosition());
		float l = -(normal.dot(p) - offset);
		float r = shape.getRadius();

		if (l < -r) { // completely dry
			return 0;
		}

		if (l > r) { // completely wet
			c.set(p);
			return MathUtils.PI * r * r;
		}

		float r2 = r * r;
		float l2 = l * l;
		float area = r2 * ((float)Math.asin(l / r) + MathUtils.PI / 2) + l * (float)Math.sqrt(r2 - l2);
		float com = -2.0f / 3.0f * (float)Math.pow(r2 - l2, 1.5f) / area;
		c.x = p.x + normal.x * com;
		c.y = p.y + normal.y * com;

		return area;
	}
}
