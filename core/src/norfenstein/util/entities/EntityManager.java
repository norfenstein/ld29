package norfenstein.util.entities;

import norfenstein.util.entities.EntityStore.Entity;

public interface EntityManager {
	int getComponentId(Class<? extends Component> componentType);
	void addEntity(Entity entity);
	void removeEntity(Entity entity);

	interface Listener {
		void entityAdded(Entity entity);
		void entityRemoved(Entity entity);
		void entityChanged(Entity entity);
	}
}
