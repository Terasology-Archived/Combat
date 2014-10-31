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

import com.google.common.collect.Maps;
import org.terasology.combat.components.HitDetectionComponent;
import org.terasology.combat.events.HitEvent;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeDeactivateComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.physics.events.CollideEvent;
import org.terasology.registry.CoreRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RegisterSystem
public class HitDetectionSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    public HitDetectionContextImpl context;
    private volatile float delta = 0;

    @Override
    public void initialise() {
        context = new HitDetectionContextImpl();
        CoreRegistry.put(HitDetectionSystem.class, this);
    }

    /**
     * @param event
     * @param entity
     */
    @ReceiveEvent(components = HitDetectionComponent.class, priority = EventPriority.PRIORITY_NORMAL)
    public void onCollision(CollideEvent event, EntityRef entity) {
        EntityRef other = event.getOtherEntity();
        HitDetectionComponent hitDetection = entity.getComponent(HitDetectionComponent.class);
        if (hitDetection.hitBlocks || !other.hasComponent(org.terasology.world.block.BlockComponent.class)) {
            if (hitDetection.trigger != HitDetection.DISABLED) {
                HitEvent hitEvent = new HitEvent(entity, other, event.getHitPoint(), event.getHitNormal());
                if (hitDetection.trigger == HitDetection.ONCE) {//Collision Event doesnt need to be saved for later checks
                    hitDetection.trigger = HitDetection.DISABLED;
                    entity.saveComponent(hitDetection);
                    entity.send(hitEvent);
                } else if (context.addHit(entity, other)) {
                    entity.send(hitEvent);
                }
            }
        }
    }

    @ReceiveEvent(components = {HitDetectionComponent.class}, priority = EventPriority.PRIORITY_NORMAL)
    public void addHitDetection(OnActivatedComponent event, EntityRef entity) {
        context.getOncePerEntity().remove(entity);
        context.getPeriodic().remove(entity);
        context.getPeriodicPerEntity().remove(entity);
    }

    @ReceiveEvent(components = {HitDetectionComponent.class})
    public void removeHitDetection(BeforeDeactivateComponent event, EntityRef entity) {
        HitDetectionComponent hitDetection = entity.getComponent(HitDetectionComponent.class);
        switch (hitDetection.trigger) {
            case PERIODIC:
                context.getPeriodic().remove(entity);
                break;
            case PERIODIC_PER_ENTITY:
                context.getPeriodicPerEntity().remove(entity);
                break;
            case ONCE_PER_ENTITY:
                context.getOncePerEntity().remove(entity);
                break;
            default:
                break;
        }
    }

    private synchronized void processHits(float delta) {

        //Periodic
        HashMap<EntityRef, Float> removeMap = Maps.newHashMap();
        for (Map.Entry<EntityRef, Float> entry : context.getPeriodic().entrySet()) {
            Float test = entry.getValue();
            test = test - delta;
            entry.setValue(test);
            if (test <= 0) {
                removeMap.put(entry.getKey(), test);
                entry = null;
            }
        }
        context.setPeriodic(Maps.newHashMap(Maps.difference(context.getPeriodic(), removeMap).entriesOnlyOnLeft()));
        //PeriodicPer Entity
        Set<Map.Entry<EntityRef, HashMap<EntityRef, Float>>> entries = context.getPeriodicPerEntity().entrySet();
        for (Map.Entry<EntityRef, HashMap<EntityRef, Float>> entry : entries) {
            Set<Map.Entry<EntityRef, Float>> subEntries = entry.getValue().entrySet();
            if (entry != null && entry.getKey().exists()) {
                for (Map.Entry<EntityRef, Float> entryTemp : subEntries) {
                    if (entryTemp != null && (entryTemp.getKey().exists())) {
                        Float test = entryTemp.getValue();
                        test = test - delta;
                        entryTemp.setValue(test);
                        if (test <= 0) {
                            entryTemp = null;//Eintrag entfernern
                        }
                    }
                }
            } else {
                entry = null;//Eintrag entfernern
            }
        }
    }

    @Override
    public void update(float delta) {
        this.delta += delta;//handle lags
        float consume = this.delta;
        processHits(consume);
        this.delta -= consume;
    }
}
