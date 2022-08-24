package nu.takacs.gametest.control;

import com.jme3.app.LegacyApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthColorControl extends AbstractControl {
    private static final Logger LOG = LoggerFactory.getLogger(HealthColorControl.class);

    private final Material material;

    public HealthColorControl(final LegacyApplication application) {
        this.material = new Material(application.getAssetManager(),
                "Common/MatDefs/Light/Lighting.j3md");
    }

    @Override
    protected void controlUpdate(final float tpf) {
        var healthControl = spatial.getControl(HealthDestructionControl.class);

        if (healthControl == null) {
            healthControl = spatial.getParent().getControl(HealthDestructionControl.class);

            if (healthControl == null) {
                return;
            }

        }

        final var health = Math.max(healthControl.getHealth(), 0);

        final var color = new ColorRGBA((100 - health) / 100.0f, health / 100.0f, 0.0f, 1.0f);

        material.setBoolean("UseMaterialColors", true);
        material.setColor("Ambient", color);
        material.setColor("Diffuse", color);

        spatial.setMaterial(material);
    }

    @Override
    protected void controlRender(final RenderManager rm, final ViewPort vp) {
    }
}
