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
		EATEN,
		ALIVE,
		SPAWNABLE
	}

	private World world;
	private EntityStore entityStore;
	private ShapeRenderSystem shapeRenderSystem;
	private FishSystem fishSystem;

	private final float UNITS_PER_SCREEN = 40f;
	private final float WATER_DEPTH = 10f;
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

	private SoundManager soundManager;

	private float inputTimer;

	public void create() {
		initializeViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), FixedAxis.HORIZONTAL, UNITS_PER_SCREEN);

		soundManager = new SoundManager();
		soundManager.create();

		world = new World(new Vector2(0, -GRAVITY), true);
		world.setContactListener(new CollisionHandler());

		entityStore = new EntityStore();

		shapeRenderSystem = new ShapeRenderSystem(entityStore, getCamera());
		fishSystem = new FishSystem(entityStore);

		entityStore.addListener(shapeRenderSystem);
		entityStore.addListener(fishSystem);

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
		soundManager.dispose();
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
				addFish();
				break;
			case EATEN:
				destroyFish();
				break;
			case SPAWNABLE:
				addFish();
				break;
		}

		fishSystem.process(delta);
	}

	@Override public void render(float delta) {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		shapeRenderSystem.process();
	}

	@Override public void resize(int width, int height) {
		super.resize(width, height);
	}

	@Override public void processInput(float delta) {
		if (Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isTouched(0)) {
			if (inputTimer <= 0) {
				birdBody.applyForce(new Vector2(0, -DIVE_FORCE), birdBody.getWorldCenter(), true);
			} else {
				inputTimer -= delta;
			}
		}
	}

	private void flap() {
		birdBody.applyLinearImpulse(new Vector2(0, flapImpulse), birdBody.getWorldCenter(), true);
		flapImpulse = 0f;
	}

	@Override public boolean keyDown(int keycode) {
		switch (keycode) {
			case Input.Keys.ESCAPE:
			case Input.Keys.BACK:
			case Input.Keys.Q:
				Gdx.app.exit();
				return true;

			case Input.Keys.SPACE:
				if (divingJoint != null) destroyDivingJoint();
				inputTimer = 0.3f;
				return true;
		}

		return false;
	}

	@Override public boolean keyUp(int keycode) {
		switch (keycode) {
			case Input.Keys.SPACE:
				if (inputTimer > 0) {
					flap();
				} else {
					addDivingJoint();
				}
				return true;
		}

		return false;
	}

	@Override public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (divingJoint != null) destroyDivingJoint();
		inputTimer = 0.3f;
		return true;
	}

	@Override public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (inputTimer > 0) {
			flap();
		} else {
			addDivingJoint();
		}
		return true;
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
		float fishSize = MathUtils.random();
		boolean goingRight = MathUtils.randomBoolean();

		CircleShape shape = new CircleShape();
		shape.setRadius(0.75f + 1.5f * fishSize);

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
		bodyDef.position.x = goingRight ?
			-pixelsToUnits(getViewport().getViewportWidth()) / 2 :
			pixelsToUnits(getViewport().getViewportWidth()) / 2;
		bodyDef.position.y = waterBody.getPosition().y + WATER_DEPTH / 2 - fishSize * WATER_DEPTH;

		Body body = world.createBody(bodyDef);
		body.createFixture(fixtureDef);

		PhysicsBody physicsBody = new PhysicsBody();
		physicsBody.body = body;

		RenderableBody renderableBody = new RenderableBody();
		renderableBody.fill = FillType.LINE;
		renderableBody.color = Color.BLUE;

		Fish fish = new Fish();
		fish.timer = 0;
		fish.direction = goingRight ? 1 : -1;
		fish.minImpulse = 30f + fishSize * 60;
		fish.maxImpulse = 60f + fishSize * 120;

		Entity entity = entityStore.new Entity(physicsBody, renderableBody, fish);
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
		float targetY = fishBody != null ? fishBody.getPosition().y : waterBody.getPosition().y;
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
				beginContactWaterAny(bodyB);
				if (bodyB == birdBody) beginContactBirdWater(birdBody, waterBody);
			} else if (bodyB == waterBody) {
				beginContactWaterAny(bodyA);
				if (bodyA == birdBody) beginContactBirdWater(birdBody, waterBody);
			} else if ((bodyA == birdBody && bodyB == fishBody) || (bodyA == fishBody && bodyB == birdBody)) {
				beginContactBirdFish(birdBody, fishBody);
			}
		}

		@Override public void endContact(Contact contact) {
			if (contact.getFixtureA() == null || contact.getFixtureB() == null) return;

			Body bodyA = contact.getFixtureA().getBody();
			Body bodyB = contact.getFixtureB().getBody();

			if (bodyA == waterBody) {
				endContactWaterAny(bodyB);
				if (bodyB == birdBody) {
					endContactBirdWater(birdBody, waterBody);
				} else if (bodyB == fishBody) {
					endContactFishWater();
				}
			} else if (bodyB == waterBody) {
				endContactWaterAny(bodyB);
				if (bodyA == birdBody) {
					endContactBirdWater(birdBody, waterBody);
				} else if (bodyA == fishBody) {
					endContactFishWater();
				}
			}
		}

		private void beginContactWaterAny(Body any) {
			buoyancyController.addBody(any);
		}
		private void beginContactBirdWater(Body bird, Body water) {
			soundManager.getSplash().play();
		}
		private void beginContactBirdFish(Body bird, Body fish) {
			fishState = FishState.EATEN;
			soundManager.getGulp().play();
		}

		private void endContactWaterAny(Body any) {
			buoyancyController.removeBody(any);
		}
		private void endContactBirdWater(Body bird, Body water) {
			if (fishState == FishState.EATEN) {
				fishState = FishState.SPAWNABLE;
			}
			soundManager.getSplash().play(0.3f);
		}
		private void endContactFishWater() {
			fishState = FishState.GONE;
		}
	}
}
