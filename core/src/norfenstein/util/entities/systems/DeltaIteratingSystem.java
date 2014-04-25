package norfenstein.util.entities;

import norfenstein.util.entities.EntityStore.Entity;

public abstract class DeltaIteratingSystem extends IteratingSystem {
	public DeltaIteratingSystem(EntityManager entityManager, long componentBitMask) {
		super(entityManager, componentBitMask);
	}

	public final void process(float delta) {
		super.updateEntities();

		begin(delta);
		for (Entity entity : super.entities) {
			processEntity(entity, delta);
		}
		end(delta);
	}

	protected void begin(float delta) { }

	protected abstract void processEntity(Entity entity, float delta);

	protected void end(float delta) { }
}
