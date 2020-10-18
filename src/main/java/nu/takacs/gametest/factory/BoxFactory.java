package nu.takacs.gametest.factory;

import com.jme3.app.LegacyApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import nu.takacs.gametest.HealthColorControl;

public class BoxFactory {
    private final Material boxMaterial;
    private final LegacyApplication application;
    private final FireFactory fireFactory;

    public BoxFactory(final LegacyApplication application,
                      final FireFactory fireFactory) {
        this.application = application;

        this.boxMaterial = new Material(application.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        this.fireFactory = fireFactory;
        this.boxMaterial.setColor("Color", ColorRGBA.Green);
    }

    public Spatial createBox() {
        final var fire = fireFactory.createFire(Vector3f.ZERO);
        final var node = new Node("BoxNode");
        final var box = new Geometry("BoxGeometry", new Box(1.0f, 1.0f, 1.0f));

        box.setMaterial(boxMaterial);

        node.setUserData("object_type", "box");
        node.setUserData("health", 100);

        node.attachChild(box);
        node.attachChild(fire);

        final var healthColorControl = new HealthColorControl(application);

        box.addControl(healthColorControl);

        return node;
    }
}
