/*
 * Copyright 2014 MovingBlocks
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.terasology.combat.components.HitDetectionComponent;
import org.terasology.entitySystem.entity.EntityRef;

import java.util.HashMap;
import java.util.HashSet;


public class HitDetectionContextImpl implements HitDetectionContext {

    HashMap<EntityRef, HashSet<EntityRef>> oncePerEntity = Maps.newHashMap();
    HashMap<EntityRef, HashMap<EntityRef, Float>> periodicPerEntity = Maps.newHashMap();
    HashMap<EntityRef, Float> periodic = Maps.newHashMap();

    public HitDetectionContextImpl() {
    }

    public boolean checkHit(EntityRef entity, EntityRef otherEntity) {
        boolean result = false;
        HitDetectionComponent hitDetectionComponent = entity.getComponent(HitDetectionComponent.class);
        if (hitDetectionComponent != null) {
            switch (hitDetectionComponent.trigger) {
                case PERIODIC_PER_ENTITY:
                    HashMap<EntityRef, Float> timedEntity = periodicPerEntity.get(entity);
                    if (timedEntity != null && !timedEntity.containsKey(otherEntity)) {
                        result = true;
                    }
                    break;
                case PERIODIC:
                    result = periodic.containsKey(entity);
                    break;
                case ONCE_PER_ENTITY:
                    HashSet<EntityRef> once = oncePerEntity.get(entity);
                    if (once != null && !once.contains(otherEntity)) {
                        result = true;
                    }
//					 if(!oncePerEntity.containsKey(entity)){
//						result = oncePerEntity.get(entity).contains(otherEntity);
//					}
                    break;
                default:
                    break;
            }
        }
        return result;
    }


    public boolean addHit(EntityRef entity, EntityRef otherEntity) {
        boolean result = false;
        HitDetectionComponent hitDetectionComponent = entity.getComponent(HitDetectionComponent.class);
        if (hitDetectionComponent != null) {
            switch (hitDetectionComponent.trigger) {
                case PERIODIC_PER_ENTITY:
                    if (!periodicPerEntity.containsKey(entity)) {
                        HashMap<EntityRef, Float> entities = Maps.newHashMap();
                        periodicPerEntity.put(entity, entities);
                    }
                    HashMap<EntityRef, Float> timedEntity = periodicPerEntity.get(entity);
                    if (!timedEntity.containsKey(otherEntity)) {
                        timedEntity.put(otherEntity, (float) 1);//Should be dynamic  ??
                        result = true;
                    }
                    break;
                case PERIODIC:
                    if (!periodic.containsKey(entity)) {
                        periodic.put(entity, (float) 1);//Shoukd be dynamic
                        result = true;
                    }
                    break;
                case ONCE_PER_ENTITY:
                    if (!oncePerEntity.containsKey(entity)) {
                        HashSet<EntityRef> entities = Sets.newHashSet();
                        result = entities.add(otherEntity);
                        oncePerEntity.put(entity, entities);
                    } else {
                        result = oncePerEntity.get(entity).add(otherEntity);
                    }
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    public boolean removeHit(EntityRef entity, EntityRef otherEntity) {
        boolean result = false;
        HitDetectionComponent hitDetectionComponent = entity.getComponent(HitDetectionComponent.class);
        if (hitDetectionComponent != null) {
            switch (hitDetectionComponent.trigger) {
                case PERIODIC_PER_ENTITY:
                    HashMap<EntityRef, Float> timedEntity = periodicPerEntity.get(entity);
                    result = timedEntity != null && timedEntity.remove(otherEntity) != null;
                    break;
                case PERIODIC:
                    result = periodic.containsKey(entity) && periodic.remove(entity) != null;
                    break;
                case ONCE_PER_ENTITY:
                    result = oncePerEntity.containsKey(entity) && oncePerEntity.get(entity).remove(otherEntity);
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    public HashMap<EntityRef, HashSet<EntityRef>> getOncePerEntity() {
        return oncePerEntity;
    }

    public void setOncePerEntity(
            HashMap<EntityRef, HashSet<EntityRef>> oncePerEntity) {
        this.oncePerEntity = oncePerEntity;
    }

    public HashMap<EntityRef, HashMap<EntityRef, Float>> getPeriodicPerEntity() {
        return periodicPerEntity;
    }

    public void setPeriodicPerEntity(
            HashMap<EntityRef, HashMap<EntityRef, Float>> periodicPerEntity) {
        this.periodicPerEntity = periodicPerEntity;
    }

    public HashMap<EntityRef, Float> getPeriodic() {
        return periodic;
    }

    public void setPeriodic(HashMap<EntityRef, Float> periodic) {
        this.periodic = periodic;
    }
}