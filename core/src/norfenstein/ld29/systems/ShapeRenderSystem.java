package norfenstein.ld29;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.utils.Array;
import norfenstein.util.entities.Component;
import norfenstein.util.entities.EntityManager;
import norfenstein.util.entities.EntityStore.Entity;
import norfenstein.util.entities.InstantIteratingSystem;

public class ShapeRenderSystem extends InstantIteratingSystem {
	private final Camera camera;
	private final ShapeRenderer shapeRenderer;

	private final Vector2 vertex; //temporary variable for extracting vertices from shapes
	private final float[] vertices; //required because ShapeRenderer doesn't have polygon(Vector2[] vertices)

	private final int physicsBodyComponentId;
	private final int renderableBodyComponentId;

	public ShapeRenderSystem(EntityManager entityManager, Camera camera) {
		super(entityManager, 1 << entityManager.getComponentId(PhysicsBody.class) | 1 << entityManager.getComponentId(RenderableBody.class));

		physicsBodyComponentId = entityManager.getComponentId(PhysicsBody.class);
		renderableBodyComponentId = entityManager.getComponentId(RenderableBody.class);

		this.camera = camera;
		shapeRenderer = new ShapeRenderer();

		vertex = new Vector2();
		vertices = new float[2000];
	}

	public void dispose() {
		shapeRenderer.dispose();
	}

	@Override protected void begin() {
		shapeRenderer.setProjectionMatrix(camera.combined);
	}

	@Override protected void processEntity(Entity entity) {
		PhysicsBody physicsBody = (PhysicsBody)entity.getComponent(physicsBodyComponentId);
		RenderableBody renderableBody = (RenderableBody)entity.getComponent(renderableBodyComponentId);

		shapeRenderer.begin(renderableBody.fill == RenderableBody.FillType.FILLED ? ShapeType.Filled : ShapeType.Line);
		shapeRenderer.setColor(renderableBody.color);
		shapeRenderer.identity();
		shapeRenderer.translate(physicsBody.body.getPosition().x, physicsBody.body.getPosition().y, 0);
		shapeRenderer.rotate(0, 0, 1, physicsBody.body.getAngle() * MathUtils.radDeg);

		for (Fixture fixture : physicsBody.body.getFixtureList()) {
			switch (fixture.getType()) {
				case Chain:
					//TODO
					//ChainShape chainShape = (ChainShape)fixture.getShape();
					break;
				case Circle:
					shapeRenderer.circle(0, 0, fixture.getShape().getRadius(), 16);
					if (renderableBody.fill == RenderableBody.FillType.DIRECTED) {
						shapeRenderer.line(0, 0, fixture.getShape().getRadius(), 0);
					}
					break;
				case Edge:
					if (renderableBody.fill == RenderableBody.FillType.LINE) {
						EdgeShape edgeShape = (EdgeShape)fixture.getShape();

						edgeShape.getVertex1(vertex);
						vertices[0] = vertex.x;
						vertices[1] = vertex.y;
						edgeShape.getVertex2(vertex);
						vertices[2] = vertex.x;
						vertices[3] = vertex.y;
						shapeRenderer.line(vertices[0], vertices[1], vertices[2], vertices[3]);
					}
					break;
				case Polygon:
					PolygonShape polygonShape = (PolygonShape)fixture.getShape();

					int vertexCount = polygonShape.getVertexCount();
					for (int i = 0; i < vertexCount; i++) {
						polygonShape.getVertex(i, vertex);
						vertices[i * 2] = vertex.x;
						vertices[i * 2 + 1] = vertex.y;
					}

					if (vertexCount == 3) {
						shapeRenderer.triangle(vertices[0], vertices[1], vertices[2], vertices[3], vertices[4], vertices[5]);
					} else {
						if (renderableBody.fill == RenderableBody.FillType.LINE) { //for some reason polygon doesn't like Fill
							shapeRenderer.polygon(vertices, 0, vertexCount * 2);
						}
					}
					break;
			}
		}

		shapeRenderer.end();
	}
}

