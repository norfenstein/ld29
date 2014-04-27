package norfenstein.ld29;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import norfenstein.ld29.RenderableBody.FillType;
import norfenstein.util.entities.EntityStore.Entity;
import norfenstein.util.entities.EntityStore;
import norfenstein.util.game.ViewportScreen;

public class GameScreen extends ViewportScreen {
	private World world;
	private EntityStore entityStore;
	private ShapeRenderSystem shapeRenderSystem;

	private final float UNITS_PER_SCREEN = 30f;
	private final float WATER_DEPTH = 8f;
	private final float MAX_FLAP_STRENGTH = 40f;
	private final float FLAP_REGEN_TIME = 0.7f;
	private final float GRAVITY = 15f;

	private final short COLLISION_NONE  = 0;
	private final short COLLISION_WALL  = 1 << 0;
	private final short COLLISION_BIRD  = 1 << 1;
	private final short COLLISION_FISH  = 1 << 2;
	private final short COLLISION_WATER = 1 << 3;

	private float flapStrength;
	private Body birdBody;
	private Body waterBody;

	public void create() {
		initializeViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), FixedAxis.HORIZONTAL, UNITS_PER_SCREEN);

		world = new World(new Vector2(0, -GRAVITY), true);
		world.setContactListener(new CollisionHandler());

		entityStore = new EntityStore();

		shapeRenderSystem = new ShapeRenderSystem(entityStore, getCamera());

		entityStore.addListener(shapeRenderSystem);

		flapStrength = MAX_FLAP_STRENGTH;
		addBird();
		addWalls();
		addWater();
	}

	public void dispose() {
		world.dispose();
		shapeRenderSystem.dispose();
	}

	@Override public void step(float delta) {
		world.step(delta, 6, 2);

		if (flapStrength < MAX_FLAP_STRENGTH) {
			flapStrength += MAX_FLAP_STRENGTH * delta / FLAP_REGEN_TIME;
			if (flapStrength > MAX_FLAP_STRENGTH) flapStrength = MAX_FLAP_STRENGTH;
		}
	}

	@Override public void render(float delta) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		shapeRenderSystem.process();
	}

	@Override public void resize(int width, int height) {
		super.resize(width, height);
	}

	@Override public boolean keyDown(int keycode) {
		switch (keycode) {
			case Input.Keys.ESCAPE:
			case Input.Keys.BACK:
			case Input.Keys.Q:
				Gdx.app.exit();
				return true;

			case Input.Keys.UP:
			case Input.Keys.SPACE:
				System.out.println("flapStrength: " + flapStrength);
				birdBody.applyLinearImpulse(new Vector2(0, flapStrength), birdBody.getWorldCenter(), true);
				flapStrength = 0f;
				return true;
		}

		return false;
	}

	private void addBird() {
		CircleShape shape = new CircleShape();
		shape.setRadius(1);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.isSensor = false;
		fixtureDef.filter.categoryBits = COLLISION_BIRD;
		fixtureDef.filter.maskBits = COLLISION_WALL | COLLISION_WATER | COLLISION_FISH;
		fixtureDef.density = 1.0f;
		fixtureDef.restitution = 1.0f;
		fixtureDef.friction = 0;

		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.fixedRotation = true;
		bodyDef.position.x = 0;
		bodyDef.position.y = 0;

		Body body = world.createBody(bodyDef);
		body.createFixture(fixtureDef);


		PhysicsBody physicsBody = new PhysicsBody();
		physicsBody.body = body;

		RenderableBody renderableBody = new RenderableBody();
		renderableBody.fill = FillType.DIRECTED;
		renderableBody.color = Color.WHITE;

		Entity entity = entityStore.new Entity(physicsBody, renderableBody);
		body.setUserData(entity);
		body.applyLinearImpulse(new Vector2(0, flapStrength).rotate(-30f), body.getWorldCenter(), true);

		birdBody = body;
	}

	private void addWalls() {
		addWall(
			pixelsToUnits(-getViewport().getViewportWidth() / 2f),
			pixelsToUnits(-getViewport().getViewportHeight() / 2f + 1),
			pixelsToUnits(getViewport().getViewportWidth() / 2f),
			pixelsToUnits(-getViewport().getViewportHeight() / 2f + 1)
		);
		addWall(
			pixelsToUnits(-getViewport().getViewportWidth() / 2f),
			pixelsToUnits(getViewport().getViewportHeight() / 2f - 1),
			pixelsToUnits(getViewport().getViewportWidth() / 2f),
			pixelsToUnits(getViewport().getViewportHeight() / 2f - 1)
		);
		addWall(
			pixelsToUnits(-getViewport().getViewportWidth() / 2f + 1),
			pixelsToUnits(-getViewport().getViewportHeight() / 2f),
			pixelsToUnits(-getViewport().getViewportWidth() / 2f + 1),
			pixelsToUnits(getViewport().getViewportHeight() / 2f)
		);
		addWall(
			pixelsToUnits(getViewport().getViewportWidth() / 2f - 1),
			pixelsToUnits(-getViewport().getViewportHeight() / 2f),
			pixelsToUnits(getViewport().getViewportWidth() / 2f - 1),
			pixelsToUnits(getViewport().getViewportHeight() / 2f)
		);
	}
	private void addWall(float x1, float y1, float x2, float y2) {
		EdgeShape shape = new EdgeShape();
		System.out.println("width: " + getViewport().getViewportWidth() + ", height: " + getViewport().getViewportHeight());
		shape.set(x1, y1, x2, y2);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.isSensor = false;
		fixtureDef.filter.categoryBits = COLLISION_WALL;
		fixtureDef.filter.maskBits = COLLISION_BIRD;
		fixtureDef.density = 1.0f;
		fixtureDef.restitution = 1.0f;
		fixtureDef.friction = 0f;

		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.x = 0;
		bodyDef.position.y = 0;

		Body body = world.createBody(bodyDef);
		body.createFixture(fixtureDef);


		PhysicsBody physicsBody = new PhysicsBody();
		physicsBody.body = body;

		RenderableBody renderableBody = new RenderableBody();
		renderableBody.fill = FillType.LINE;
		renderableBody.color = Color.WHITE;

		Entity entity = entityStore.new Entity(physicsBody, renderableBody);
		body.setUserData(entity);
	}

	private void addWater() {
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(
			pixelsToUnits(getViewport().getViewportWidth() / 2f),
			WATER_DEPTH);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.isSensor = true;
		fixtureDef.filter.categoryBits = COLLISION_WATER;
		fixtureDef.filter.maskBits = COLLISION_BIRD;

		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.x = 0;
		bodyDef.position.y = -pixelsToUnits(getViewport().getViewportHeight() / 2f) + WATER_DEPTH;

		Body body = world.createBody(bodyDef);
		body.createFixture(fixtureDef);


		PhysicsBody physicsBody = new PhysicsBody();
		physicsBody.body = body;

		RenderableBody renderableBody = new RenderableBody();
		renderableBody.fill = FillType.LINE;
		renderableBody.color = Color.CYAN;

		Entity entity = entityStore.new Entity(physicsBody, renderableBody);
		body.setUserData(entity);

		waterBody = body;
	}

	private class CollisionHandler implements ContactListener {
		@Override public void preSolve(Contact contact, Manifold oldManifold) { }

		@Override public void postSolve(Contact contact, ContactImpulse impulse) { }

		@Override public void beginContact(Contact contact) {
			System.out.println("beginContact");
			Body bodyA = contact.getFixtureA().getBody();
			Body bodyB = contact.getFixtureB().getBody();

			if ((birdBody == bodyA && waterBody == bodyB) || (birdBody == bodyB && waterBody == bodyA)) {
			}
		}

		@Override public void endContact(Contact contact) {
			System.out.println("endContact");
			Body bodyA = contact.getFixtureA().getBody();
			Body bodyB = contact.getFixtureB().getBody();

			if ((birdBody == bodyA && waterBody == bodyB) || (birdBody == bodyB && waterBody == bodyA)) {
			}
		}
	}
}
