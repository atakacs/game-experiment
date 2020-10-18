package nu.takacs.gametest.factory;

import com.jme3.app.LegacyApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;

public class GrenadeFactory {
    private final Material grenadeMaterial;
    private final LegacyApplication application;

    public GrenadeFactory(final LegacyApplication application) {
        this.application = application;

        this.grenadeMaterial = new Material(application.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        this.grenadeMaterial.setColor("Color", ColorRGBA.DarkGray);
    }

    public Spatial createGrenade() {
        final var grenade = application.getAssetManager()
                .loadModel("assets/grenade.j3o");

        grenade.setMaterial(grenadeMaterial);
        grenade.scale(5.0f);
        grenade.setUserData("object_type", "grenade");

        return grenade;
    }
}
