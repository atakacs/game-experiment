package nu.takacs.gametest.factory;

import com.jme3.app.LegacyApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;

public class GrenadeFactory {
    private final Material grenadeMaterial;

    public GrenadeFactory(final LegacyApplication application) {
        grenadeMaterial = new Material(application.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        grenadeMaterial.setColor("Color", ColorRGBA.DarkGray);
    }

    public Spatial createGrenade(final Vector3f localTranslation) {
        final Sphere sphere = new Sphere(20, 20, 0.5f);
        final Geometry grenade = new Geometry("Grenade", sphere);

        grenade.setMaterial(grenadeMaterial);

        grenade.setLocalTranslation(localTranslation);

        return grenade;
    }
}
