package norfenstein.util.entities;

import norfenstein.util.entities.EntityStore.Entity;

public abstract class InstantIteratingSystem extends IteratingSystem {
	public InstantIteratingSystem(EntityManager entityManager, long componentBitMask) {
		super(entityManager, componentBitMask);
	}

	public final void process() {
		super.updateEntities();

		begin();
		for (Entity entity : super.entities) {
			processEntity(entity);
		}
		end();
	}

	protected void begin() { }

	protected abstract void processEntity(Entity entity);

	protected void end() { }
}
