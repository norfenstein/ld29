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
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import norfenstein.ld29.RenderableBody.FillType;
import norfenstein.util.entities.EntityStore.Entity;
import norfenstein.util.entities.EntityStore;
import norfenstein.util.game.ViewportScreen;

public class GameScreen extends ViewportScreen {
	private World world;
	private EntityStore entityStore;
	private ShapeRenderSystem shapeRenderSystem;

	private Vector2 tempVector = new Vector2();
	private Body birdBody;
	private static final float MAX_FLAP_STRENGTH = 40.0f;
		private static final float FLAP_REGEN_TIME = 0.7f;
	private float flapStrength;

	public void create() {
		initializeViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), FixedAxis.VERTICAL, 30);
		System.out.println("viewportWidth: " + getViewport().getViewportWidth() + ", viewportHeight: " + getViewport().getViewportHeight() + ", pixelsPerUnit: " + getPixelsPerUnit());

		world = new World(new Vector2(0, -15f), true);

		entityStore = new EntityStore();

		shapeRenderSystem = new ShapeRenderSystem(entityStore, getCamera());

		entityStore.addListener(shapeRenderSystem);

		flapStrength = MAX_FLAP_STRENGTH;
		addBird();
		addWalls();
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

	@Override public void processInput(float delta) {
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
				System.out.println("flapStrength: " + flapStrength);
				birdBody.applyLinearImpulse(new Vector2(0, flapStrength), birdBody.getWorldCenter(), true);
				flapStrength = 0f;
				return true;
			case Input.Keys.LEFT:
				System.out.println("flapStrength: " + flapStrength);
				birdBody.applyLinearImpulse(new Vector2(0, flapStrength).rotate(30f), birdBody.getWorldCenter(), true);
				flapStrength = 0f;
				return true;
			case Input.Keys.RIGHT:
				System.out.println("flapStrength: " + flapStrength);
				birdBody.applyLinearImpulse(new Vector2(0, flapStrength).rotate(-30f), birdBody.getWorldCenter(), true);
				flapStrength = 0f;
				return true;
			case Input.Keys.DOWN:
				birdBody.applyLinearImpulse(new Vector2(0, -60f), birdBody.getWorldCenter(), true);
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
		//fixtureDef.filter.categoryBits = categoryBits;
		//fixtureDef.filter.maskBits = maskBits;
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

		birdBody = body;
		birdBody.applyLinearImpulse(new Vector2(0, flapStrength).rotate(-30f), birdBody.getWorldCenter(), true);
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
		//fixtureDef.filter.categoryBits = categoryBits;
		//fixtureDef.filter.maskBits = maskBits;
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

	/*
	public static Body createBody(World world, BodyType bodyType, boolean isSensor, short categoryBits, short maskBits, float x, float y, Shape shape) {
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = bodyType;
		bodyDef.position.x = x;
		bodyDef.position.y = y;
		Body body = world.createBody(bodyDef);

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
		fixtureDef.isSensor = isSensor;
		fixtureDef.filter.categoryBits = categoryBits;
		fixtureDef.filter.maskBits = maskBits;
		fixtureDef.density = 1;
		fixtureDef.restitution = 0.5f;
		body.createFixture(fixtureDef);

		return body;
	}

	public Body createCircle(World world, BodyType bodyType, boolean isSensor, short categoryBits, short maskBits, float x, float y, float radius) {
		CircleShape shape = new CircleShape();
		shape.setRadius(radius);

		Body body = createBody(world, bodyType, isSensor, categoryBits, maskBits, x, y, shape);

		shape.dispose();

		return body;
	}

	public Entity createBodyEntity(EntityStore entityStore, Body body, FillType fill, Color color) {
		PhysicsBody physicsBody = new PhysicsBody();
		physicsBody.body = body;

		RenderableBody renderableBody = new RenderableBody();
		renderableBody.fill = fill;
		renderableBody.color = color;

		Entity entity = entityStore.new Entity(physicsBody, renderableBody);
		body.setUserData(entity);

		return entity;
	}
	*/
}
