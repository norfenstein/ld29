package norfenstein.util.entities;

import java.lang.IllegalArgumentException;
import norfenstein.util.entities.EntityStore.Entity;

public abstract class EntitySystem implements EntityManager.Listener {
	protected final EntityManager entityManager;
	protected long componentBitMask;

	protected EntitySystem(EntityManager entityManager, long componentBitMask) {
		this.entityManager = entityManager;
		this.componentBitMask = componentBitMask;
		System.out.println("Initialized entity system \"" + this.getClass().getName() + "\", componentBitMask: " + this.componentBitMask);
	}

	protected final boolean acceptEntity(Entity entity) {
		return (componentBitMask & entity.getComponentBitMask()) == componentBitMask;
	}
}
