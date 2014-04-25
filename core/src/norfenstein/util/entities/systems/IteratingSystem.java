package norfenstein.util.entities;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import norfenstein.util.entities.EntityStore.Entity;

public abstract class IteratingSystem extends EntitySystem {
	protected final ObjectSet<Entity> entities;
	private final Array<Entity> entitiesToAdd;
	private final Array<Entity> entitiesToRemove;

	public IteratingSystem(EntityManager entityManager, long componentBitMask) {
		super(entityManager, componentBitMask);

		entities = new ObjectSet<Entity>();
		entitiesToAdd = new Array<Entity>(false, 32);
		entitiesToRemove = new Array<Entity>(false, 8);
	}

	@Override public final void entityAdded(Entity entity) {
		if (super.acceptEntity(entity)) {
			entitiesToAdd.add(entity);
		}
	}

	@Override public final void entityRemoved(Entity entity) {
		entitiesToRemove.add(entity);
	}

	@Override public final void entityChanged(Entity entity) {
		if (super.acceptEntity(entity)) {
			entitiesToAdd.add(entity);
		} else {
			entitiesToRemove.add(entity);
		}
	}

	protected final void updateEntities() {
		if (entitiesToRemove.size > 0) {
			for (Entity entity : entitiesToRemove) {
				entities.remove(entity);
			}
			entitiesToRemove.clear();
		}

		if (entitiesToAdd.size > 0) {
			for (Entity entity : entitiesToAdd) {
				entities.add(entity);
			}
			entitiesToAdd.clear();
		}
	}
}
