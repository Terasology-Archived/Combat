/*
 * Copyright 2012
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

import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.prefab.Prefab;
import org.terasology.entitySystem.prefab.PrefabManager;
import org.terasology.logic.common.lifespan.LifespanComponent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.rendering.logic.MeshComponent;

public class DeadEntityFactory {

    private final float corpseDecayDelay = 30f;
    private final String deadBodyPrefab = "combat:deadbody";

    public DeadEntityFactory() {
    }

    public EntityRef newInstance(float lifespan) {
        return newInstance(lifespan, EntityRef.NULL);
    }

    public EntityRef newInstance(float lifespan, EntityRef deadEntity) {
        Prefab prefab = CoreRegistry.get(PrefabManager.class).getPrefab(deadBodyPrefab);
        LocationComponent location = deadEntity.getComponent(LocationComponent.class);
        MeshComponent meshDeadEntity = deadEntity.getComponent(MeshComponent.class);
        InventoryComponent inventoryComponent = deadEntity.getComponent(InventoryComponent.class);

        if (prefab != null) {
            EntityRef bodyEntity = CoreRegistry.get(EntityManager.class).create(prefab, location.getWorldPosition());
            bodyEntity.addComponent(location);
            bodyEntity.addComponent(meshDeadEntity);
            bodyEntity.addComponent(inventoryComponent);
            LifespanComponent lifespanComp = bodyEntity.getComponent(LifespanComponent.class);
            lifespanComp.lifespan = lifespan;
            bodyEntity.saveComponent(lifespanComp);
            return bodyEntity;
        }
        return EntityRef.NULL;
    }

    public EntityRef newInstance(EntityRef deadEntity) {
        Prefab prefab = CoreRegistry.get(PrefabManager.class).getPrefab(deadBodyPrefab);
        LocationComponent location = deadEntity.getComponent(LocationComponent.class);
        MeshComponent meshDeadEntity = deadEntity.getComponent(MeshComponent.class);
        InventoryComponent inventoryComponent = deadEntity.getComponent(InventoryComponent.class);
        if (prefab != null) {
            EntityRef bodyEntity = CoreRegistry.get(EntityManager.class).create(prefab, location.getWorldPosition());
            bodyEntity.addComponent(location);
            bodyEntity.addComponent(meshDeadEntity);
            if (inventoryComponent != null) {
                bodyEntity.addComponent(inventoryComponent);
            }
            LifespanComponent lifespanComp = bodyEntity.getComponent(LifespanComponent.class);
            lifespanComp.lifespan = corpseDecayDelay;
            bodyEntity.saveComponent(lifespanComp);
            return bodyEntity;
        }
        return EntityRef.NULL;
    }
}
