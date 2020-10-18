package nu.takacs.gametest;

import com.jme3.app.LegacyApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

public class HealthColorControl extends AbstractControl {

    private final Material material;

    public HealthColorControl(final LegacyApplication application) {
        this.material = new Material(application.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
    }


    @Override
    protected void controlUpdate(final float tpf) {
        var health = (Integer)spatial.getParent().getUserData("health");

        if(health == null) {
            return;
        }

        if(health < 0) {
            health = 0;
        }

        this.material.setColor("Color",
                new ColorRGBA((100 - health)/100.0f, health/100.0f, 0.0f, 1.0f));
        spatial.setMaterial(material);
    }

    @Override
    protected void controlRender(final RenderManager rm, final ViewPort vp) {}
}
