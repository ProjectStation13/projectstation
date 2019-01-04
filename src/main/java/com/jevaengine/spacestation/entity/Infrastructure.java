/*
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.jevaengine.spacestation.entity;

import io.github.jevaengine.rpg.entity.Door;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Nullable;
import io.github.jevaengine.util.Observers;
import io.github.jevaengine.world.Direction;
import io.github.jevaengine.world.World;
import io.github.jevaengine.world.entity.IEntity;
import io.github.jevaengine.world.entity.IEntityTaskModel;
import io.github.jevaengine.world.entity.NullEntityTaskModel;
import io.github.jevaengine.world.entity.WorldAssociationException;
import io.github.jevaengine.world.physics.IPhysicsBody;
import io.github.jevaengine.world.physics.NonparticipantPhysicsBody;
import io.github.jevaengine.world.physics.NullPhysicsBody;
import io.github.jevaengine.world.physics.PhysicsBodyDescription;
import io.github.jevaengine.world.physics.PhysicsBodyDescription.PhysicsBodyType;
import io.github.jevaengine.world.scene.model.IImmutableSceneModel;
import io.github.jevaengine.world.scene.model.ISceneModel;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class Infrastructure implements IEntity {
	private static final AtomicInteger m_unnamedCount = new AtomicInteger(0);

	private final String m_name;
	@Nullable
	private final PhysicsBodyDescription m_physicsBodyDescription;
	private final Observers m_observers = new Observers();
	private final EntityBridge m_bridge;
	private final boolean m_isStatic;
	private final boolean m_isTransparent;
	@Nullable
	private final ISceneModel m_model;
	private IPhysicsBody m_body = new NullPhysicsBody();
	@Nullable
	private World m_world;

	private final boolean m_isAirTight;

	private HashMap<String, Integer> m_flags = new HashMap<>();

	private final Set<String> m_infrastructureType = new HashSet<>();

	private final float m_heatConductivity;

	public Infrastructure(ISceneModel model, boolean isStatic, boolean isTraversable, String[] infrastructureTypes, boolean isAirTight, boolean isTransparent, float heatConductivity) {
		m_name = this.getClass().getName() + m_unnamedCount.getAndIncrement();
		m_isTransparent = isTransparent;
		m_isAirTight = isAirTight;
		m_infrastructureType.addAll(Arrays.asList(infrastructureTypes));
		m_isStatic = isStatic;
		m_heatConductivity = heatConductivity;
		m_model = model;

		if (!isTraversable) {
			m_physicsBodyDescription = new PhysicsBodyDescription(PhysicsBodyType.Static, model.getBodyShape(), 1.0F, true, false, 1.0F);
			m_physicsBodyDescription.collisionExceptions = new Class[] {
					Infrastructure.class,
					Door.class
			};
		} else
			m_physicsBodyDescription = null;


		m_bridge = new EntityBridge(this);
	}

	public float getHeatConductivity() {
		return m_heatConductivity;
	}

	public boolean hasInfrastructureType(String type) {
		return m_infrastructureType.contains(type);
	}

	public boolean isAirTight() {
		return m_isAirTight;
	}

	public boolean isTransparent() {
		return m_isTransparent;
	}

	@Override
	public void dispose() {
		if (m_world != null)
			m_world.removeEntity(this);

		m_observers.clear();
	}

	@Override
	public String getInstanceName() {
		return m_name;
	}

	@Override
	public final World getWorld() {
		return m_world;
	}

	@Override
	public final void associate(World world) {
		if (m_world != null)
			throw new WorldAssociationException("Already associated with world");

		m_world = world;

		constructPhysicsBody();
		m_observers.raise(IEntityWorldObserver.class).enterWorld();
	}

	@Override
	public final void disassociate() {
		if (m_world == null)
			throw new WorldAssociationException("Not associated with world");

		m_observers.raise(IEntityWorldObserver.class).leaveWorld();

		destroyPhysicsBody();

		m_world = null;
	}

	private void constructPhysicsBody() {
		Direction dir = m_body.getDirection();
		if (m_physicsBodyDescription == null)
			m_body = new NonparticipantPhysicsBody(this, m_model.getAABB());
		else {
			m_body = m_world.getPhysicsWorld().createBody(this, m_physicsBodyDescription);
			m_observers.raise(IEntityBodyObserver.class).bodyChanged(new NullPhysicsBody(), m_body);
		}

		m_body.setDirection(dir);
	}

	private void destroyPhysicsBody() {
		Direction dir = m_body.getDirection();

		m_body.destory();
		m_body = new NullPhysicsBody();
		m_body.setDirection(dir);
		m_observers.raise(IEntityBodyObserver.class).bodyChanged(new NullPhysicsBody(), m_body);
	}

	@Override
	public boolean isStatic() {
		return m_isStatic;
	}

	@Override
	public final IPhysicsBody getBody() {
		return m_body;
	}

	@Override
	public void update(int deltaTime) {
		m_model.update(deltaTime);
	}

	@Override
	@Nullable
	public IImmutableSceneModel getModel() {
		Direction dir = this.getBody().getDirection();
		if(dir != Direction.Zero)
			m_model.setDirection(this.getBody().getDirection());
		return m_model;
	}

	@Override
	public Map<String, Integer> getFlags() {
		return m_flags;
	}

	@Override
	public IObserverRegistry getObservers() {
		return m_observers;
	}

	@Override
	public EntityBridge getBridge() {
		return m_bridge;
	}

	@Override
	public IEntityTaskModel getTaskModel() {
		return new NullEntityTaskModel();
	}
}
