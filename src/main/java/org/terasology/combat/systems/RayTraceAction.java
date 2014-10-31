/*
 * Copyright 2012 Benjamin Glatzel <benjamin.glatzel@me.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.combat.systems;


import org.terasology.combat.components.RayTraceActionComponent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.common.ActivateEvent;
import org.terasology.physics.HitResult;
import org.terasology.physics.engine.PhysicsSystem;
import org.terasology.registry.In;

@RegisterSystem
public class RayTraceAction extends BaseComponentSystem {

    @In
    EntityManager entityManager;
    @In
    private PhysicsSystem physicsSystem;


    @ReceiveEvent(components = {RayTraceActionComponent.class})
    public void onActivate(ActivateEvent event, EntityRef entity) {
        System.out.println("Raytest");
        HitResult hitResult = physicsSystem.getPhysics().rayTrace(event.getInstigatorLocation(), event.getDirection(), 1000000, org.terasology.physics.StandardCollisionGroup.ALL);
        EntityRef projectile = entityManager.create("combat:raytest", hitResult.getHitPoint());
    }
}  
