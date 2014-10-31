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

public enum HitDetection {
    ON_CONTACT,  //Detect Hit once per Contact with another Entity
    ON_COLLISION,//Base Hit detection on Collision
    ON_PERIOD,   //Detect hit every given time period if colliding with an object
    ONCE,         //Hit Detection is triggered only once on the collision
    ONCE_PER_ENTITY, //Hit Detection is triggered only once with each collided entity	
    ALWAYS,         //Hit Detection is triggered only once with each collided entity
    TIMED_COLLSION_PER_ENTITY, //Hit Detection is triggered only once with each collided entity	
    PERIODIC,     //Hit Detection with every other entity is only triggered after a given
    PERIODIC_PER_ENTITY, //Hit Detection is triggered only once with each collided entity every given time period
    DISABLED,    //Hit Detection is deactivated
    WHILE_MOVING//Hit Detection is only triggered if the detecting entity was moving 

}
