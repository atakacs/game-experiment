package nu.takacs.gametest.factory;

import com.jme3.app.LegacyApplication;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

public class FireFactory {
    private final Material material;

    public FireFactory(final LegacyApplication application) {
        material = new Material(application.getAssetManager(),
                "Common/MatDefs/Misc/Particle.j3md");
        material.setTexture("Texture", application.getAssetManager()
                .loadTexture("Effects/Explosion/flame.png"));
    }

    public ParticleEmitter createFire(final Vector3f location) {
        final ParticleEmitter fire =
                new ParticleEmitter("FireEmitter", ParticleMesh.Type.Triangle, 1000);

        fire.setMaterial(material);
        fire.setImagesX(2);
        fire.setImagesY(2); // 2x2 texture animation
        fire.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f));   // red
        fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow

        fire.setStartSize(2.0f);
        fire.setEndSize(0.5f);
        fire.setGravity(0, 0, 0);
        fire.setLowLife(1f);
        fire.setHighLife(2f);
        fire.setParticlesPerSec(30.0f);
        fire.setShape(new EmitterSphereShape(Vector3f.ZERO, 2.0f));

        fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
        fire.getParticleInfluencer().setVelocityVariation(0.0f);
        fire.setLocalTranslation(location);
        return fire;
    }
}
