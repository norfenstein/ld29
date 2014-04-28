package norfenstein.util.entities;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectIntMap;

public final class EntityStore implements EntityManager {
	public final class Entity {
		private IntMap<Component> components;
		private long componentBitMask;

		public Entity() {
			components = new IntMap<Component>();
			componentBitMask = 0;
		}
		public Entity(Component... components) {
			this();
			addComponents(components);
		}

		public long getComponentBitMask() {
			return componentBitMask;
		}

		public void addComponents(Component... components) {
			if (components.length > 0) {
				for (Component component : components) {
					if (component != null) {
						int componentId = getComponentId(component.getClass());
						componentBitMask |= 1 << componentId;
						this.components.put(componentId, component);
					}
				}
				onEntityChanged(this);
			}
		}

		public void removeComponents(int... componentIds) {
			if (componentIds.length > 0) {
				for (int componentId : componentIds) {
					componentBitMask &= ~(1 << componentId);
					components.remove(componentId);
				}
				onEntityChanged(this);
			}
		}

		public Component getComponent(int componentId) {
			return components.get(componentId);
		}
	}

	private Array<Entity> entities;
	private Array<EntityManager.Listener> listeners;

	private ObjectIntMap<Class<? extends Component>> componentIds;
	private int nextComponentId = 0;

	public EntityStore() {
		entities = new Array<Entity>(false, 16);
		listeners = new Array<EntityManager.Listener>(false, 16);
		componentIds = new ObjectIntMap<Class<? extends Component>>();
	}

	public void addListener(EntityManager.Listener listener) {
		listeners.add(listener);

		for (Entity entity : entities) {
			listener.entityAdded(entity);
		}
	}

	public void removeListener(EntityManager.Listener listener) {
		listeners.removeValue(listener, true);
	}

	// EntityManager ////////////////////////////////////////

	@Override public void addEntity(Entity entity) {
		entities.add(entity);
		onEntityAdded(entity);
	}

	@Override public void removeEntity(Entity entity) {
		entities.removeValue(entity, true);
		onEntityRemoved(entity);
	}

	@Override public int getComponentId(Class<? extends Component> componentType) {
		int componentId = componentIds.get(componentType, -1);

		if (componentId < 0) {
			componentId = nextComponentId++;
			componentIds.put(componentType, componentId);
		}

		return componentId;
	}

	// EntityManager.Listener ///////////////////////////////

	private void onEntityAdded(Entity entity) {
		for (Listener listener : listeners) {
			listener.entityAdded(entity);
		}
	}

	private void onEntityRemoved(Entity entity) {
		for (Listener listener : listeners) {
			listener.entityRemoved(entity);
		}
	}

	private void onEntityChanged(Entity entity) {
		for (Listener listener : listeners) {
			listener.entityChanged(entity);
		}
	}
}
