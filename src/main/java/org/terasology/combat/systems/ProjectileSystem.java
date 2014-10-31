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


import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.terasology.combat.components.ProjectileComponent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.BeforeRemoveComponent;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.event.EventPriority;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Vector3fUtil;
import org.terasology.physics.HitResult;
import org.terasology.physics.components.RigidBodyComponent;
import org.terasology.physics.components.TriggerComponent;
import org.terasology.physics.engine.PhysicsSystem;
import org.terasology.physics.events.CollideEvent;
import org.terasology.registry.In;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * TODO handle possible lags/ interpolation ???
 * TODO angular velocity
 * TODO manually defined translations and rotations for the projectiles
 * TOD= Curve/ Function based translation/rotation of projectiles (Parametric equations/Brezier Curves and other curevs and so on)
 */
@RegisterSystem
public class ProjectileSystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    @In
    PhysicsSystem physicsSystem;
    @In
    WorldProvider worldProvider;
    @In
    EntityManager entityManager;

    private volatile float delta = 0;
    private Deque<EntityRef> remove = new LinkedList<EntityRef>();
    private Deque<EntityRef> add = new LinkedList<EntityRef>();
    private Deque<EntityRef> projectiles = new LinkedList<EntityRef>();
    private ConcurrentMap<EntityRef, CollideEvent> collisions = Maps.newConcurrentMap();

    Vector3f forward = new Vector3f(0, 0, 1);
    Vector3f gravity = new Vector3f(0, -9.81f, 0);


    @ReceiveEvent(components = {ProjectileComponent.class, LocationComponent.class}, priority = EventPriority.PRIORITY_NORMAL)
    public void removeProjectile(BeforeRemoveComponent event, EntityRef entity) {
        remove.add(entity);
    }

    @ReceiveEvent(components = {ProjectileComponent.class, LocationComponent.class}, priority = EventPriority.PRIORITY_NORMAL)
    public void addProjectile(OnActivatedComponent event, EntityRef entity) {
        add.add(entity);
    }

    /**
     * Mirrors the direction vector based on the hitnormal
     * TODO check if translation/rotation is possible without collision.
     */
    private void riccochette(Vector3f hitNormal, EntityRef projectile) {
        ProjectileComponent projectileComponent = projectile.getComponent(ProjectileComponent.class);
        Vector3f reflect = new Vector3f();
        hitNormal.normalize();
        Vector3fUtil.reflect(projectileComponent.initialVelocity, hitNormal, reflect);
        projectileComponent.initialVelocity.set(reflect);
    }

    /**
     * Mirrors the velocity of the projectile and scales it down
     * TODO calculate velocity of bounce based on the "hardness" of the colilding materials and the restitution of the colliding object
     */
    private void bounce(CollideEvent event, EntityRef projectile) {
        ProjectileComponent projectileComponent = projectile.getComponent(ProjectileComponent.class);
        riccochette(event.getHitNormal(), projectile);
        projectileComponent.initialVelocity.scale(0.1f);//TODO this shouldn't be static
    }

    /**
     * Negates the direction of the passed Vector.
     */
    private void reflect(Vector3f direction) {
        direction.negate();
    }

    /**
     * Pierces a hit Entity
     * TODO add logic to support piercing for Moving entities
     * TODO Velocity based Piercing
     * TODO Material based piercing
     */
    private void pierce(CollideEvent event, EntityRef projectile) {
        freeze(projectile);//stop projectile
        ProjectileComponent projectileComponent = projectile.getComponent(ProjectileComponent.class);
        projectileComponent.piercing = false;//Disable piercing after collision
        //Pierce into CollsionVolume
        LocationComponent location = projectile.getComponent(LocationComponent.class);
        Vector3f hitPoint = event.getHitPoint();
        location.setWorldPosition(hitPoint);
        //deactivate Trigger to prevent another Collide Event
        TriggerComponent trigger = projectile.getComponent(TriggerComponent.class);
        trigger.enabled = false;
        projectile.saveComponent(trigger);
        remove.add(projectile);//dont rotate or move the projectile anymore
    }


/**    //TODO velocity based piercing ?
 Vector3f velocity = new Vector3f(projectileComponent.initialVelocity);

 RigidBodyComponent rigidBodyComponent = projectile.getComponent(RigidBodyComponent.class);
 if(rigidBodyComponent != null){
 RigidBody rigidBody = physicsSystem.getRigidBodyForEntity(projectile);
 rigidBody.getLinearVelocity(velocity);
 }else if(projectile.hasComponent(CharacterMovementComponent.class)){
 CharacterMovementComponent movement = projectile.getComponent(CharacterMovementComponent.class);
 velocity = movement.getDrive();
 }
 double momentum = velocity.length();
 velocity.normalize();
 velocity.scale((float)momentum*0.05f);
 event.getHitPoint().add(velocity);

 //TODO material based piercing ?
 EntityRef other = event.getOtherEntity();
 if(other.hasComponent(BlockComponent.class)){
 BlockComponent blockComponent = other.getComponent(BlockComponent.class);
 Block block = worldProvider.getBlock(blockComponent.getPosition());
 byte hardness = block.getHardness();
 velocity/hardness;
 if(hardness > 10){

 }
 }
 }*/

    /**
     * saves the first collision Event for the Entity that will be handled in the next frame.
     */
    @ReceiveEvent(components = {ProjectileComponent.class, LocationComponent.class}, priority = EventPriority.PRIORITY_CRITICAL)
    public void onCollision(CollideEvent event, EntityRef projectile) {
        if (checkCollision(event) && !collisions.containsKey(projectile)) {
            collisions.put(projectile, event);
        }
    }

    /**
     * Rotates the projectile based on its current velocity
     * TODO check if rotation is possible without a collision. Needed ?
     * Velocity is currently fixed based on the definition within the ProjectileComponent of the Entity
     */
    private void rotateToVelocity(float delta, EntityRef projectile) {
        RigidBodyComponent rigidBodyComponent = projectile.getComponent(RigidBodyComponent.class);
        if (rigidBodyComponent == null || rigidBodyComponent.kinematic) {
            ProjectileComponent projectileComponent = projectile.getComponent(ProjectileComponent.class);
            LocationComponent locationComponent = projectile.getComponent(LocationComponent.class);
            Vector3f direction = new Vector3f(projectileComponent.initialVelocity);
            direction.scale(delta);
            Quat4f rotation = locationComponent.getWorldRotation();
            QuatUtil.lookAt(rotation, direction, forward);
//    		QuaternionUtil.quatRotate(rotation, direction, forward);
            locationComponent.setWorldRotation(rotation);
            projectile.saveComponent(locationComponent);
        } else {
            rotateToVelocity(physicsSystem.getRigidBodyForEntity(projectile));
        }
    }


    /**
     * Translates the Entity based on its velocity
     * Velocity is currently fixed based on the definition within the ProjectileComponent of the Entity
     * TODO Support different Collision Volumes for Trigger and Rigid Body ?
     * TODO Support accelaration and decelaration for projectiles
     * TODO Support Translation of Dynamic rigged objects
     */
    private void translateByVelocity(float delta, EntityRef projectile) {
        ProjectileComponent projectileComponent = projectile.getComponent(ProjectileComponent.class);
        LocationComponent locationComponent = projectile.getComponent(LocationComponent.class);
        RigidBodyComponent rigidBodyComponent = projectile.getComponent(RigidBodyComponent.class);
        //Gravity
        if (projectileComponent.effectedByGravity) {
            Vector3f gravityDelta = new Vector3f(gravity);
            gravityDelta.scale(delta);
            locationComponent.getWorldPosition().add(gravityDelta);
            projectileComponent.initialVelocity.add(gravityDelta);
        }
        Vector3f translation = new Vector3f(projectileComponent.initialVelocity);
        float momentum = translation.length();
        double translationInFrame = 0;
        if (momentum > 0) {
            translationInFrame = momentum * delta;
        }
        Vector3f position = new Vector3f();
        locationComponent.getWorldPosition(position);
        if (translation.length() > 0) {//TODO Minimum Velocity for Translation ?
            if (translationInFrame > 0) {
                float radius[] = new float[1];
                Vector3f center = new Vector3f();
                //Check collision with Trigger
                TriggerComponent trigger = projectile.getComponent(TriggerComponent.class);
                short filterTrigger = physicsSystem.getPhysics().combineGroups(trigger.detectGroups);
                double translationInFrameTrigger = 0;
                PairCachingGhostObject ghost = physicsSystem.getTrigggerForEntity(projectile);
                if (ghost != null) {
                    ghost.getCollisionShape().getBoundingSphere(center, radius);
                }
                //if(translationInFrame > radius[0]){//TODO check optimization
                HitResult hitResult = physicsSystem.getPhysics().rayTrace(position, translation, (float) translationInFrame + radius[0], filterTrigger);
                if (hitResult.isHit()) {
                    translationInFrameTrigger = Vector3fUtil.calcdist(position, hitResult.getHitPoint());
                    translationInFrameTrigger -= radius[0];
                }
                double translationInFrameCollision = translationInFrameTrigger;
                //Check Collision with RigidBody
                if (rigidBodyComponent != null && rigidBodyComponent.kinematic) {
                    short filterCollision = physicsSystem.getPhysics().combineGroups(rigidBodyComponent.collidesWith);
                    HitResult hitResultCollision = physicsSystem.getPhysics().rayTrace(position, translation, (float) translationInFrame + radius[0], filterCollision);
                    if (hitResultCollision.isHit()) {
                        //Adjust Translation of Projectile
                        translationInFrameCollision = Vector3fUtil.calcdist(position, hitResultCollision.getHitPoint());
                        translationInFrameCollision -= radius[0];
                        translationInFrame = translationInFrameCollision;
                    }
                }
                //Needed because CollisionObject could tunnel through collision Volume that triggers the CollideEvent
                if (hitResult.isHit() && translationInFrameTrigger <= translationInFrameCollision) {
                    projectile.send(new CollideEvent(hitResult.getEntity(), hitResult.getHitPoint(), hitResult.getHitNormal()));
                }
                //	}
                //Translate the Projectile
                translation.normalize();
                translation.scale((float) translationInFrame);
                if (rigidBodyComponent == null || rigidBodyComponent.kinematic) {
                    position.add(translation);
                    locationComponent.setWorldPosition(position);
                } else {//TODO translate dynamic objects
                    RigidBody rigidBody = physicsSystem.getRigidBodyForEntity(projectile);
                    if (rigidBody != null) {
                        rigidBody.activate();
                        rigidBody.applyCentralForce(translation);
                        Vector3f velocity = new Vector3f();
                        rigidBody.getLinearVelocity(velocity);
                        System.out.println("velocity Rigid Body:" + velocity.length());
                    }
                }
            }
        }
    }

    /**
     * helper method that checks if the result of the Raytracing actually hit an unpassable target
     */
    private boolean checkHit(HitResult result) {
        boolean checkResult = false;
        if (result != null && result.isHit()) {
            if (result.isWorldHit()) {
                Block block = worldProvider.getBlock(result.getBlockPosition());
                if (!block.isPenetrable()) {
                    checkResult = true;
                }
            } else {
                checkResult = true;
            }
        }
        return checkResult;
    }


    /**
     * helper method that checks if collisionEvent actually hit an unpassable target
     * TODO check for Blockcomponent of hit entityy first
     */
    private boolean checkCollision(CollideEvent event) {
        boolean checkResult = false;
        EntityRef other = event.getOtherEntity();
        if (other != null) {
            if (other.hasComponent(org.terasology.world.block.BlockComponent.class)) {
                LocationComponent location = other.getComponent(LocationComponent.class);
                if (location != null) {
                    Block block = worldProvider.getBlock(location.getWorldPosition());
                    if (block == null || !block.isPenetrable() || !block.isLiquid()) {
                        checkResult = true;
                    }
                }
            }

        }
        return checkResult;
    }

    /**
     * Rotates the Entity based on the current velocity of its Rigidbody
     * TODO check if translation is possible without a collision.
     */
    private void rotateToVelocity(RigidBody rigidBody) {
        if (rigidBody != null) {
            Vector3f velocity = new Vector3f();
            rigidBody.getLinearVelocity(velocity); // This should use the real velocity not the intertopolated becuas of possible lags in multiplayer
            //rigidBody.getInterpolationLinearVelocity(velocity);
            if (velocity.length() > 0.33f) {//Do not Rotate Objects that are barely moving
                //set Orientation to  the direction vector (velocity)
                Transform transform = new Transform();
                //rigidBody.getInterpolationWorldTransform(transform);
                rigidBody.getWorldTransform(transform);
                Quat4f rotation = new Quat4f();
                transform.getRotation(rotation);
                QuatUtil.lookAt(rotation, velocity, forward);
                transform.setRotation(rotation);
                rigidBody.setWorldTransform(transform);
            }
        }
    }

    @Override
    public void update(float delta) {
        this.delta += delta; // handle possible lags
        processCollisions();
        processProjectiles();
        processMovement();
    }

    /**
     * TODO this doesnt work until Partiicle effects have been fixed
     */
    private void spawnEffects(EntityRef projectile) {
        LocationComponent location = projectile.getComponent(LocationComponent.class);
        entityManager.create("combat:bloodsplash", location.getWorldPosition());
    }

    /**
     *
     */
    private synchronized void processProjectiles() {
        while (!add.isEmpty()) {
            projectiles.add(add.poll());
        }
        while (!remove.isEmpty()) {
            projectiles.remove(remove.poll());
        }
    }

    /**
     * Iterate through the collisions in the last frame and hanlde the collisions
     */
    private synchronized void processCollisions() {
        Set<EntityRef> remove = Sets.newHashSet();
        for (Entry<EntityRef, CollideEvent> curCollisions : collisions.entrySet()) {
            handleCollision(curCollisions.getValue(), curCollisions.getKey());
            curCollisions.getKey().saveComponent(curCollisions.getKey().getComponent(ProjectileComponent.class));
            remove.add(curCollisions.getKey());
        }
        for (EntityRef curProjectile : remove) {
            collisions.remove(curProjectile);
        }
    }

    /**
     * delegate method to handle collisions
     */
    //TODO replace if else by switch case with enumaration   
    public void handleCollision(CollideEvent event, EntityRef projectile) {
        if (projectile != null && projectile.exists()) {
            ProjectileComponent projectileComponent = projectile.getComponent(ProjectileComponent.class);
            if (projectileComponent != null) {
                if (projectileComponent.riccochet) {
                    riccochette(event.getHitNormal(), projectile);
                } else if (projectileComponent.piercing) {
                    pierce(event, projectile);
                } else if (projectileComponent.reflect) {
                    reflect(projectileComponent.initialVelocity);
                } else if (projectileComponent.effectedByGravity) {
                    bounce(event, projectile);
                }
            }
        }
    }

    /**
     * switches the kinematic state of a rigged Projectile if its current velocity (momentum)
     * drops below a certain value
     */
    public boolean handleKinematicState(float delta, EntityRef projectile) {
        boolean result = true;
        if (projectile != null && projectile.exists()) {
            ProjectileComponent projectileComponent = projectile.getComponent(ProjectileComponent.class);
            if (projectileComponent != null) {
                if (projectileComponent.effectedByGravity && !projectileComponent.piercing) {
                    float momentum = projectileComponent.initialVelocity.length();
                    if (momentum < 0.33f) {
                        switchKinematicState(projectile);
                        result = false;
                    }
                }
            }
        }
        return result;
    }

    /**
     * helper method to switch the kinmeatic state of a rigged body
     * TODO fix jitter for kinematic projectiles that are switched to dynamic state
     */
    private void switchKinematicState(EntityRef projectile) {
        if (projectile != null && projectile.exists()) {
            RigidBodyComponent rigidBodyComponent = projectile.getComponent(RigidBodyComponent.class);
            TriggerComponent trigger = projectile.getComponent(TriggerComponent.class);
            if (trigger != null) {
                trigger.enabled = false;
                projectile.saveComponent(trigger);
            }
            if (rigidBodyComponent != null && rigidBodyComponent.kinematic) {//kinematic -->dynamic
                remove.add(projectile);
                rigidBodyComponent.kinematic = false;
                projectile.saveComponent(rigidBodyComponent);
                physicsSystem.recreate(projectile);
                RigidBody rigidBody = physicsSystem.getRigidBodyForEntity(projectile);
                rigidBody.setActivationState(rigidBody.WANTS_DEACTIVATION);
            } else {//dynamic --> kinematic
                rigidBodyComponent.kinematic = true;
                projectile.saveComponent(rigidBodyComponent);
            }
        }
    }

    //TODO
    private void rotateTo() {

    }

    //TODO
    private void translateTo() {

    }

    //TODO
    private void moveAlongPath() {

    }

    /**
     * Stops the movement of the Projectile immediately
     */
    public void freeze(EntityRef entity) {
        RigidBodyComponent rigidBodyComponent = entity.getComponent(RigidBodyComponent.class);
        ProjectileComponent projectile = entity.getComponent(ProjectileComponent.class);
        //dynamic Collision Object
        if (rigidBodyComponent != null && !rigidBodyComponent.kinematic) {
            rigidBodyComponent.kinematic = true;
            rigidBodyComponent.velocity = 0.0f;
            entity.saveComponent(rigidBodyComponent);
        } else {//kinematic or unrigged
            projectile.initialVelocity = new Vector3f();
        }
    }

    private void processMovement() {
        float consume = delta;
        for (EntityRef projectile : projectiles) {
            if (handleKinematicState(consume, projectile)) {
                translateByVelocity(consume, projectile);
                rotateToVelocity(consume, projectile);
                projectile.saveComponent(projectile.getComponent(ProjectileComponent.class));
                projectile.saveComponent(projectile.getComponent(LocationComponent.class));
            }
        }
        delta -= consume;
    }

    @Deprecated
    public void moveOutOfCollision(CollideEvent event, EntityRef projectile) {
        RigidBodyComponent rigidBodyCompoenent = projectile.getComponent(RigidBodyComponent.class);
        LocationComponent locationComponent = projectile.getComponent(LocationComponent.class);
        if (rigidBodyCompoenent == null || rigidBodyCompoenent.kinematic) {
            double translation = 0;
            Vector3f hitPoint = event.getHitPoint();
            Vector3f hitNormal = event.getHitNormal();
            float radius[] = new float[1];
            Vector3f center = new Vector3f();
            if (physicsSystem != null) {
                PairCachingGhostObject ghost = physicsSystem.getTrigggerForEntity(projectile);
                if (ghost != null) {
                    ghost.getCollisionShape().getBoundingSphere(center, radius);
                    double centerToHitpoint = Vector3fUtil.calcdist(center, hitPoint);
                    double centerToHitnormal = Vector3fUtil.calcdist(center, hitNormal);
                    double centerToLocationToCenter = Vector3fUtil.calcdist(center, locationComponent.getWorldPosition());
                    System.out.println("centerToHitPoint:" + centerToHitpoint);
                    System.out.println("centerToHitnormal:" + centerToHitnormal);
                    System.out.println("LocationTOCenter:" + centerToLocationToCenter);
                    System.out.println("radius:" + radius[0]);
                    if (centerToHitpoint < radius[0]) {
                        translation = radius[0] - centerToHitpoint;
                    }
                }
            } else {
//   				LocationComponent otherLocation = event.getOtherEntity().getComponent(LocationComponent.class);
//   				double entityToOtherentity = Vector3fUtil.calcdist(location.getWorldPosition(),otherLocation.getWorldPosition());
//   				double distanceFRomEntityToOtherMinusHitpint =  entityToOtherentity - centerToHitpoint;
//   				
            }
            if (translation > 0) {
                ProjectileComponent projectileComponent = projectile.getComponent(ProjectileComponent.class);
                LocationComponent location = projectile.getComponent(LocationComponent.class);
                Vector3f direction = new Vector3f(projectileComponent.initialVelocity);
                direction.normalize();
                direction.scale((float) translation);
                direction.negate();
                location.getWorldPosition().add(direction);
                System.out.println("Move out direction:" + direction);
                projectile.saveComponent(location);
                System.out.println("distanceToHitPoint after Translation" + Vector3fUtil.calcdist(hitPoint, location.getWorldPosition()));
            }
        }
    }
}


//  private void switchKinematicState(EntityRef projectile ){
//		RigidBodyComponent rigidBody = projectile.getComponent(RigidBodyComponent.class);
//    	ProjectileComponent projectileComponent = projectile.getComponent(ProjectileComponent.class);
//	 	RigidBody rb = physicsSystem.getRigidBodyForEntity(projectile);
//	 	CollisionShape cs = rb.getCollisionShape();
//	 	cs.calculateLocalInertia(rigidBody.mass, new Vector3f());
//	 	
//
//		if(rigidBody.kinematic){
//			rb.
//			rb.setMassProps(mass, inertia);
//		}
//			rb.ccalculateLocalInertia(Obj->Behavior.ObjTraits->Mass, inertia);
//    	Obj->BodyRigid->setCollisionFlags(btCollisionObject::CF_STATIC_OBJECT);
//    	Obj->BodyRigid->setMassProps(Obj->Behavior.ObjTraits->Mass, inertia);
//    	Obj->BodyRigid->updateInertiaTensor();
//    	Obj->BodyRigid->setLinearVelocity(btVector3(0,0,0));
//    	Obj->BodyRigid->setAngularVelocity(btVector3(0,0,0));
//    	Obj->BodyRigid->setActivationState(WANTS_DEACTIVATION);

//TODO reduce motion treshold for rigidbody of projectiles
//    public boolean isMoving(float delta,EntityRef projectile){
//    	boolean isMoving = false;
//    	System.out.println("#####################################################################################");
//    	System.out.println("Check if Entity is Moving before Translation/Rotation:");
//    	ProjectileComponent projectileComponent = projectile.getComponent(ProjectileComponent.class);
//		RigidBodyComponent rigidBody = projectile.getComponent(RigidBodyComponent.class);
//	 	RigidBody rb = physicsSystem.getRigidBodyForEntity(projectile);
//    	System.out.println("Velocity:"+projectileComponent.initialVelocity);
//    	if(rb.hasContactResponse()){
//    		rigidBody.kinematic = false;
//    		projectile.saveComponent(rigidBody);
//    		if(projectileComponent.initialVelocity.length() < 0.33){
//    			physicsSystem.recreate(projectile);
//    		}
//    	}else{
//    		isMoving = true;
//    	}
////    	if(Math.abs(projectileComponent.initialVelocity.x) < 0.03 && Math.abs(projectileComponent.initialVelocity.z) < 0.03){
////    		projectileComponent.initialVelocity.x += projectileComponent.initialVelocity.y*delta;
////    		projectileComponent.initialVelocity.x += projectileComponent.initialVelocity.y*delta;
////    		if(rigidBody.kinematic){
////    			remove.add(projectile);
////    		 	rb.setLinearVelocity(new Vector3f());
////            	if(rb != null){
////            		rigidBody.kinematic = false;
////            		projectile.saveComponent(rigidBody);
////            		rb.setActivationState(rb.ACTIVE_TAG);
////                	rb.updateInertiaTensor();
////                	rb.setLinearVelocity(projectileComponent.initialVelocity);
////            		rb.applyGravity();
////                	System.out.println("Change rigid body:");
////            		rb.setActivationState(rb.WANTS_DEACTIVATION);
////            		rb.applyGravity();
////            		Vector3f test = new Vector3f(projectileComponent.initialVelocity);
////            		test.scale(0.016f);
////            		rb.setLinearVelocity(test);
////            		 if(rb.isActive()){
////            			 rb.setActivationState(rb.);
////            		 }
//            		
//            	}    		
//            	//rb.applyCentralImpulse(projectileComponent.initialVelocity);
//            	
////    		}else{
////    			Vector3f linearVelocity = new Vector3f();
////            	RigidBody rb = physicsSystem.getRigidBodyForEntity(projectile);
////            	rb.getLinearVelocity(linearVelocity);
////            	System.out.println("linear velocity"+linearVelocity);
////    		}
//        //	RigidBody rigidBody = physicsSystem.getRigidBodyForEntity(projectile);
////        	if(rigidBody != null){
////        		 if(rigidBody.isActive()){
////        			 rigidBody.setActivationState(rigidBody.WANTS_DEACTIVATION);
////        		 }
////        	}
////        	//TODO Handle Trigger
////        	PairCachingGhostObject ghost = physicsSystem.getTrigggerForEntity(projectile);
////        	if(ghost != null){
////        	     if(ghost.isActive()){
////       			     ghost.setActivationState(rigidBody.WANTS_DEACTIVATION);
////       		     }
////        	}
//    	
//    	System.out.println("isMoving:"+isMoving);
//    	System.out.println("#####################################################################################");
//		return isMoving;
//    }
//}


/** Returns true if 'end' can be reached at the given 'speed', otherwise
 it returns false.
 bool CalculateTrajectory(const Vector3& start, const Vector3& end,
 ���������������������const float speed, const float gravity,
 ���������������������const bool bUseHighArc, Vector3& outTrajectory,
 ���������������������float& outAngle)
 {
 ����bool canHit = false;
 �
 ����// We use doubles instead of floats because we need a lot of
 ����// precision for some uses of the pow() function coming up.
 ����double term1 = 0.0f;
 ����double term2 = 0.0f;
 ����double root = 0.0f;
 �
 ����Vector3 diffVector = destination - origin;
 �
 ����// A horizontally-flattened difference vector.
 ����Vector3 horzDiff = Vector3(diffVector.x, 0.0f, diffVector.y);
 ���
 ����// We shrink our values by this factor to prevent too much
 ����// precision loss.
 ����const float factor = 100.0;
 �
 ����// Remember that Unitize returns length
 ����float x = horz.Unitize() / factor;
 ����float y = diffVector.y / factor;
 ����float v = speed / factor;
 ����float g = gravity / factor;
 �
 ����term1 = pow(v, 4) - (g * ((g * pow(x,2)) + (2 * y * pow(v,2))));
 �
 ����// If term1 is positive, then the 'end' point can be reached
 ����// at the given 'speed'.
 ����if ( term1 >= 0 )
 ����{
 ��������canHit = true;
 �
 ��������term2 = sqrt(term1);
 �
 ��������double divisor = (g * x);
 �
 ��������if ( divisor != 0.0f )
 ��������{
 ������������if ( bUseHighArc )
 ������������{
 ����������������root = ( pow(v,2) + term2 ) / divisor;
 ������������}
 ������������else
 ������������{
 ����������������root = ( pow(v,2) - term2 ) / divisor;
 ������������}
 �
 ������������root = atan(root);
 �
 ������������angleOut = static_cast<float>(root);
 �
 ������������Vector3 rightVector = horz.Cross(Vector3::UnitY);
 �
 ������������// Rotate the 'horz' vector around 'rightVector'
 ������������// by '-angleOut' degrees.
 ������������RotatePointAroundAxis(rightVector, -angleOut, horz);
 ��������}
 �
 ��������// Now apply the speed to the direction, giving a velocity
 ��������outTrajectory = horz * speed;
 ����}
 �
 ����return canHit;
 }
 // /**
 //     * <code>mult</code> multiplies this quaternion by a parameter vector. The
 //     * result is stored in the supplied vector
 //     *
 //     * @param v
 //     *            the vector to multiply this quaternion by.
 //     * @return v
 //     */
//    public Vector3f multLocal(Quat4f q, Vector3f v) {
//
//        float tempX, tempY;
//        tempX = q.w * q.w * v.x + 2 * q.y * q.w * v.z - 2 * q.z * q.w * v.y +q.x * q.x * v.x
//                + 2 * q.y * q.x * v.y + 2 * q.z * q.x * v.z - q.z * q.z * v.x - q.y * q.y * v.x;
//        tempY = 2 * q.x * q.y * v.x + q.y * q.y * v.y + 2 * q.z * q.y * v.z + 2 * q.w * q.z
//                * v.x - q.z * q.z * v.y + q.w * q.w * v.y - 2 * q.x * q.w * v.z - q.x * q.x
//                * v.y;
//        v.z = 2 * q.x * q.z * v.x + 2 * q.y * q.z * v.y + q.z * q.z * v.z - 2 * q.w * q.y * v.x
//                - q.y * q.y * v.z + 2 * q.w * q.x * v.y - q.x * q.x * v.z + q.w * q.w * v.z;
//        v.x = tempX;
//        v.y = tempY;
//        return v;
//    }
//*
//*
//*
//*/
/**
 */
// 
//protected Point getPointOnBezierCurve( Point[] points, double distance )
//{
//	if( points.length > 2 ) {
//		Point p;
//		Point newPoints[] = new Point[points.length-1];
//		for(int i = 0, j=points.length-1; i<j; i++) {
//			p = this.getDistancePoint( points[i], points[i+1], distance );
//			newPoints[i] = p;
//		}
//		
//		return this.getPointOnBezierCurve( newPoints, distance );
//	} else {
//		return this.getDistancePoint( points[0], points[1], distance );
//	}
//}
//
//protected Point getDistancePoint( Point p1, Point p2, double distance)
//{
//	double slopeRise = (p2.y - p1.y);
//	double slopeRun  = (p2.x - p1.x);
//	double hypotenuse = Math.sqrt( Math.pow(slopeRise, 2) + Math.pow(slopeRun, 2));
//
//	double angle = Math.asin(slopeRise/hypotenuse);
//	
//	Point point = new Point(
//		(int) Math.round( p1.x + slopeRun*distance ),
//		(int) Math.round( p1.y + slopeRise*distance)
//	);
//	
//	return point;
//}
//}

