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
package org.terasology.combat.components;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;

import javax.vecmath.Vector3f;

public final class ProjectileComponent implements Component {
    public EntityRef owner;
    //Contraints for Flightpath
    public float delta = 0;
    public double distancePerSecond = 0;
    public Vector3f initialVelocity = new Vector3f();
    public Vector3f accelaration = new Vector3f();
    public boolean effectedByGravity = false;
    public boolean bounce = false;
    public boolean riccochet = false;
    public boolean piercing = false;
    public boolean reflect = false;
    public boolean velocityBasedRotation = true;

}
