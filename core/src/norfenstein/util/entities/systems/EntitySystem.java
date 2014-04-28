package norfenstein.util.entities;

import java.lang.IllegalArgumentException;
import norfenstein.util.entities.EntityStore.Entity;

public abstract class EntitySystem implements EntityManager.Listener {
	protected final EntityManager entityManager;
	protected long componentBitMask;

	protected EntitySystem(EntityManager entityManager, long componentBitMask) {
		this.entityManager = entityManager;
		this.componentBitMask = componentBitMask;
	}

	protected final boolean acceptEntity(Entity entity) {
		return (componentBitMask & entity.getComponentBitMask()) == componentBitMask;
	}
}
