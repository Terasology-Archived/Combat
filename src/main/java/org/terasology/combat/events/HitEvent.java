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
package org.terasology.combat.events;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.Event;

import javax.vecmath.Vector3f;

public class HitEvent implements Event {
    private EntityRef instigator;
    private EntityRef other;
    private Vector3f hitPosition;
    private Vector3f hitNormal;

    public HitEvent(EntityRef instigator, EntityRef other, Vector3f drive, Vector3f hitNormal) {
        this.instigator = instigator;
        this.other = other;
        this.hitPosition = drive;
        this.hitNormal = hitNormal;
    }

    public EntityRef getInstigator() {
        return instigator;
    }

    public Vector3f getHitPosition() {
        return hitPosition;
    }

    public Vector3f getHitNormal() {
        return hitNormal;
    }

    public EntityRef getOther() {
        return other;
    }

}
