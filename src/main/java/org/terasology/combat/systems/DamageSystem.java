package org.terasology.combat.systems;

import org.terasology.combat.components.ProjectileComponent;
import org.terasology.combat.events.HitEvent;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.In;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.health.DestroyEvent;
import org.terasology.logic.health.HealthComponent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.physics.engine.PhysicsSystem;
import org.terasology.world.block.BlockComponent;

import javax.vecmath.Vector3f;

/**
 * TODO Define hit Zones like,legs,feet,arms,head,organs,hands, fingers
 * TODO Handle hit event for non Projectiles collision volumes
 */
@RegisterSystem
public class DamageSystem extends BaseComponentSystem {


    DeadEntityFactory deadEntityFactory = new DeadEntityFactory();
    @In
    PhysicsSystem physicsSystem;

    @Override
    public void initialise() {
    }

    @Override
    public void shutdown() {
    }

    @ReceiveEvent(components = {ProjectileComponent.class, ItemComponent.class})
    public void onProjectileHit(HitEvent event, EntityRef entity) {
        ProjectileComponent projectileComponent = entity.getComponent(ProjectileComponent.class);
        ItemComponent item = entity.getComponent(ItemComponent.class);
        event.getOther().send(new DamageEvent(item.baseDamage, projectileComponent.owner));
        event.cancel();
        pushBack(event);
    }

    public void pushBack(HitEvent event) {
        EntityRef other = event.getOther();
        org.terasology.physics.character.CharacterMovementComponent characterMovement = other.getComponent(org.terasology.physics.character.CharacterMovementComponent.class);
        Vector3f direction = new Vector3f(event.getHitNormal());
        direction.scale(3);
        direction.negate();
        if (characterMovement != null) {
            characterMovement.getVelocity().add(direction);
            other.saveComponent(characterMovement);
        }
    }

    @ReceiveEvent(components = {HealthComponent.class})
    public void onDeath(DestroyEvent event, EntityRef entity) {
        if (!entity.hasComponent(BlockComponent.class)) {
            EntityRef deadEntity = deadEntityFactory.newInstance(entity);
            entity.destroy();
            //TODO temporary "exagarated death animation"
            if (physicsSystem != null) {
                while (physicsSystem.getRigidBodyForEntity(deadEntity) == null) {
                }
                ;
                physicsSystem.getRigidBodyForEntity(deadEntity).applyImpulse(new Vector3f(0, 20, 10), new Vector3f(0, 0.75f, 2.5f));
            }
        }
    }

}
