package norfenstein.ld29;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
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
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import norfenstein.ld29.RenderableBody.FillType;
import norfenstein.util.entities.EntityStore.Entity;
import norfenstein.util.entities.EntityStore;
import norfenstein.util.game.ViewportScreen;

public class GameScreen extends ViewportScreen {
	private enum FishState {
		GONE,
		ALIVE,
		SPAWNABLE
	}

	private World world;
	private EntityStore entityStore;
	private ShapeRenderSystem shapeRenderSystem;

	private final float UNITS_PER_SCREEN = 40f;
	private final float WATER_DEPTH = 8f;
	private final float MAX_FLAP_IMPULSE = 40f;
	private final float FLAP_REGEN_TIME = 0.7f;
	private final float DIVE_FORCE = 50f;
	private final float GRAVITY = 15f;
	private final float WATER_DENSITY = 2f;
	private final float WATER_DRAG = 1.5f;

	private final short COLLISION_NONE  = 0;
	private final short COLLISION_WALL  = 1 << 0;
	private final short COLLISION_BIRD  = 1 << 1;
	private final short COLLISION_FISH  = 1 << 2;
	private final short COLLISION_WATER = 1 << 3;

	private Body anchorBody; //static body without fixtures for connecting joints to the world
	private Body birdBody;
	private Body waterBody;
	private Body fishBody;
	private Entity fishEntity;
	private float flapImpulse;
	private Joint divingJoint;
	private BuoyancyController buoyancyController;
	private FishState fishState;

	public void create() {
		initializeViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), FixedAxis.HORIZONTAL, UNITS_PER_SCREEN);

		world = new World(new Vector2(0, -GRAVITY), true);
		world.setContactListener(new CollisionHandler());

		entityStore = new EntityStore();

		shapeRenderSystem = new ShapeRenderSystem(entityStore, getCamera());

		entityStore.addListener(shapeRenderSystem);

		divingJoint = null;
		flapImpulse = MAX_FLAP_IMPULSE;
		fishState = FishState.SPAWNABLE;

		addAnchor();
		addWalls();
		addBird();
		addWater();

		buoyancyController = new BuoyancyController(
			new Vector2(0, 1), //surface normal
			new Vector2(0, 0), //fluid velocity
			world.getGravity(),
			0, //surface height
			WATER_DENSITY, //fluid density
			WATER_DRAG, //linear drag
			0f //angular drag
		);
	}

	public void dispose() {
		world.dispose();
		shapeRenderSystem.dispose();
	}

	@Override public void step(float delta) {
		world.step(delta, 6, 2);

		if (flapImpulse < MAX_FLAP_IMPULSE) {
			flapImpulse += MAX_FLAP_IMPULSE * delta / FLAP_REGEN_TIME;
			if (flapImpulse > MAX_FLAP_IMPULSE) flapImpulse = MAX_FLAP_IMPULSE;
		}

		if (divingJoint != null) {
			if (birdBody.getPosition().y >= anchorBody.getWorldPoint(divingJoint.getAnchorA()).y) {
				destroyDivingJoint();
			}
		}

		buoyancyController.step();

		switch (fishState) {
			case GONE:
				destroyFish();
				break;
			case SPAWNABLE:
				addFish();
				break;
		}
	}

	@Override public void render(float delta) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		shapeRenderSystem.process();
	}

	@Override public void resize(int width, int height) {
		super.resize(width, height);
	}

	@Override public void processInput(float delta) {
		if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			birdBody.applyForce(new Vector2(0, -DIVE_FORCE), birdBody.getWorldCenter(), true);
		}
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
				if (divingJoint != null) destroyDivingJoint();
				birdBody.applyLinearImpulse(new Vector2(0, flapImpulse), birdBody.getWorldCenter(), true);
				flapImpulse = 0f;
				return true;

			case Input.Keys.LEFT:
				if (divingJoint != null) destroyDivingJoint();
				birdBody.applyLinearImpulse(new Vector2(0, flapImpulse).rotate(30), birdBody.getWorldCenter(), true);
				flapImpulse = 0f;
				return true;

			case Input.Keys.RIGHT:
				if (divingJoint != null) destroyDivingJoint();
				birdBody.applyLinearImpulse(new Vector2(0, flapImpulse).rotate(-30), birdBody.getWorldCenter(), true);
				flapImpulse = 0f;
				return true;

			case Input.Keys.DOWN:
				if (divingJoint != null) destroyDivingJoint();
				flapImpulse = 0f;
				return true;
		}

		return false;
	}

	@Override public boolean keyUp(int keycode) {
		switch (keycode) {
			case Input.Keys.DOWN:
				addDivingJoint();
				return true;
		}

		return false;
	}

	private void addAnchor() {
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.x = 0;
		bodyDef.position.y = 0;

		Body body = world.createBody(bodyDef);

		anchorBody = body;
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
		renderableBody.fill = FillType.LINE;
		renderableBody.color = Color.WHITE;

		Entity entity = entityStore.new Entity(physicsBody, renderableBody);
		body.setUserData(entity);

		birdBody = body;
	}

	private void addFish() {
		CircleShape shape = new CircleShape();
		shape.setRadius(MathUtils.random(0.8f, 2f));

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.isSensor = true;
		fixtureDef.filter.categoryBits = COLLISION_FISH;
		fixtureDef.filter.maskBits = COLLISION_WALL | COLLISION_WATER | COLLISION_BIRD;
		fixtureDef.density = WATER_DENSITY; //fish should have neutral buoyancy
		fixtureDef.restitution = 0.0f;
		fixtureDef.friction = 0;

		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.DynamicBody;
		bodyDef.fixedRotation = true;
		bodyDef.position.x = 0;
		bodyDef.position.y = waterBody.getPosition().y;

		Body body = world.createBody(bodyDef);
		body.createFixture(fixtureDef);

		PhysicsBody physicsBody = new PhysicsBody();
		physicsBody.body = body;

		RenderableBody renderableBody = new RenderableBody();
		renderableBody.fill = FillType.LINE;
		renderableBody.color = Color.BLUE;

		Entity entity = entityStore.new Entity(physicsBody, renderableBody);
		body.setUserData(entity);
 
		fishBody = body;
		fishEntity = entity;

		fishState = FishState.ALIVE;
	}

	private void destroyFish() {
		if (fishEntity != null) {
			entityStore.removeEntity(fishEntity);
			fishEntity = null;
		}

		if (fishBody != null) {
			world.destroyBody(fishBody);
			fishBody = null;
		}
	}

	private void addWalls() {
		//Bottom
		addWall(0.0f,
			pixelsToUnits(-getViewport().getViewportWidth() / 2f),
			pixelsToUnits(-getViewport().getViewportHeight() / 2f + 1),
			pixelsToUnits(getViewport().getViewportWidth() / 2f),
			pixelsToUnits(-getViewport().getViewportHeight() / 2f + 1)
		);

		//Top
		addWall(0.0f,
			pixelsToUnits(-getViewport().getViewportWidth() / 2f),
			pixelsToUnits(getViewport().getViewportHeight() / 2f - 1),
			pixelsToUnits(getViewport().getViewportWidth() / 2f),
			pixelsToUnits(getViewport().getViewportHeight() / 2f - 1)
		);

		//Left
		addWall(1.0f,
			pixelsToUnits(-getViewport().getViewportWidth() / 2f + 1),
			pixelsToUnits(-getViewport().getViewportHeight() / 2f),
			pixelsToUnits(-getViewport().getViewportWidth() / 2f + 1),
			pixelsToUnits(getViewport().getViewportHeight() / 2f)
		);

		//Right
		addWall(1.0f,
			pixelsToUnits(getViewport().getViewportWidth() / 2f - 1),
			pixelsToUnits(-getViewport().getViewportHeight() / 2f),
			pixelsToUnits(getViewport().getViewportWidth() / 2f - 1),
			pixelsToUnits(getViewport().getViewportHeight() / 2f)
		);
	}
	private void addWall(float restitution, float x1, float y1, float x2, float y2) {
		EdgeShape shape = new EdgeShape();
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
		fixtureDef.filter.maskBits = COLLISION_BIRD | COLLISION_FISH;

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

	private void addDivingJoint() {
		if (birdBody.getLinearVelocity().y >= 0) return;

		float fishX = fishBody != null ? fishBody.getPosition().x : 0f;
		float fieldWidth = pixelsToUnits(getViewport().getViewportWidth());
		float fieldEdgeX = fieldWidth / 2;
		float fieldHeight = pixelsToUnits(getViewport().getViewportHeight());
		float fieldEdgeY = fieldHeight / 2;
		float bufferSize = 1f; //a little more than bird's radius, to avoid scraping the edge on the upswing
		float birdX = birdBody.getPosition().x;
		float birdY = birdBody.getPosition().y;
		float targetY = waterBody.getPosition().y;
		if (targetY > birdY) targetY = -fieldEdgeY + bufferSize;
		float idealRadius = birdY - targetY;

		float anchorX = 0f;
		if (birdX <= fishX) { //going to the right
			if (birdX + idealRadius * 2 < fieldEdgeX - bufferSize) {
				anchorX = birdX + idealRadius;
			} else {
				anchorX = birdX + (fieldEdgeX - birdX) / 2 - bufferSize;
			}
		} else if (birdX > fishX) { //going to the left
			if (birdX - idealRadius * 2 > -fieldEdgeX + bufferSize) {
				anchorX = birdX - idealRadius;
			} else {
				anchorX = birdX - (fieldEdgeX + birdX) / 2 + bufferSize;
			}
		} else {
			return;
		}
		Vector2 anchorPoint = new Vector2(anchorX, birdBody.getPosition().y);

		RopeJointDef ropeJointDef = new RopeJointDef();
		ropeJointDef.bodyA = anchorBody;
		ropeJointDef.bodyB = birdBody;
		ropeJointDef.localAnchorA.set(anchorBody.getLocalPoint(anchorPoint));
		ropeJointDef.localAnchorB.set(Vector2.Zero);
		ropeJointDef.maxLength = birdBody.getPosition().dst(anchorPoint);

		if (divingJoint != null) destroyDivingJoint();
		divingJoint = world.createJoint(ropeJointDef);
	}

	private void destroyDivingJoint() {
		world.destroyJoint(divingJoint);
		divingJoint = null;
	}

	private class CollisionHandler implements ContactListener {
		@Override public void preSolve(Contact contact, Manifold oldManifold) { }

		@Override public void postSolve(Contact contact, ContactImpulse impulse) { }

		@Override public void beginContact(Contact contact) {
			Body bodyA = contact.getFixtureA().getBody();
			Body bodyB = contact.getFixtureB().getBody();

			if (bodyA == waterBody) {
				buoyancyController.addBody(bodyB);
			} else if (bodyB == waterBody) {
				buoyancyController.addBody(bodyA);
			} else if ((bodyA == birdBody && bodyB == fishBody) || (bodyA == fishBody && bodyB == birdBody)) {
				fishState = FishState.GONE;
			}
		}

		@Override public void endContact(Contact contact) {
			if (contact.getFixtureA() == null || contact.getFixtureB() == null) return;

			Body bodyA = contact.getFixtureA().getBody();
			Body bodyB = contact.getFixtureB().getBody();

			if (bodyA == waterBody) {
				buoyancyController.removeBody(bodyB);
				if (bodyB == birdBody && fishState == FishState.GONE) {
					fishState = FishState.SPAWNABLE;
				} else if (bodyB == fishBody) {
					fishState = FishState.GONE;
				}
			} else if (bodyB == waterBody) {
				buoyancyController.removeBody(bodyA);
				if (bodyA == birdBody && fishState == FishState.GONE) {
					fishState = FishState.SPAWNABLE;
				} else if (bodyA == fishBody) {
					fishState = FishState.GONE;
				}
			}
		}
	}
}
