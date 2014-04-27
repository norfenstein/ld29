package norfenstein.ld29;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import norfenstein.util.entities.Component;
import norfenstein.util.entities.DeltaIteratingSystem;
import norfenstein.util.entities.EntityManager;
import norfenstein.util.entities.EntityStore.Entity;

public class FishSystem extends DeltaIteratingSystem {
	private final int fishComponentId;
	private final int physicsBodyComponentId;

	public FishSystem(EntityManager entityManager) {
		super(entityManager, 1 << entityManager.getComponentId(Fish.class) | 1 << entityManager.getComponentId(PhysicsBody.class));

		physicsBodyComponentId = entityManager.getComponentId(PhysicsBody.class);
		fishComponentId = entityManager.getComponentId(Fish.class);
	}

	@Override protected void processEntity(Entity entity, float delta) {
		Fish fish = (Fish)entity.getComponent(fishComponentId);
		Body body = ((PhysicsBody)entity.getComponent(physicsBodyComponentId)).body;

		fish.timer -= delta;
		if (fish.timer <= 0) {
			body.applyLinearImpulse(new Vector2(fish.direction * MathUtils.random(fish.minImpulse, fish.maxImpulse), 0), body.getWorldCenter(), true);

			fish.timer += MathUtils.random(1.5f, 2.5f);
		}
	}
}
