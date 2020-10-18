package nu.takacs.gametest.factory;

import com.jme3.app.LegacyApplication;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import nu.takacs.gametest.TimedDestructionControl;

public class ExplosionFactory {
    private final Material explosionMaterial;

    public ExplosionFactory(final LegacyApplication application) {
        explosionMaterial = new Material(application.getAssetManager(),
                "Common/MatDefs/Misc/Particle.j3md");
        explosionMaterial.setTexture("Texture", application.getAssetManager()
                .loadTexture("Effects/Explosion/flame.png"));
    }

    public Spatial createExplosion(final Vector3f location) {
        final ParticleEmitter fire =
                new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);

        fire.setMaterial(explosionMaterial);
        fire.setImagesX(2);
        fire.setImagesY(2); // 2x2 texture animation
        fire.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f));   // red
        fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
        fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fire.setStartSize(10.0f);
        fire.setEndSize(50.0f);
        fire.setGravity(0, 0, 0);
        fire.setLowLife(1f);
        fire.setHighLife(3f);
        fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0.0f, 5.0f, 0.0f));
        //fire.getParticleInfluencer().setVelocityVariation(1.0f);
        fire.setLocalTranslation(location);
        fire.addControl(new TimedDestructionControl(spatial -> {}, 500L));

        return fire;
    }
}
