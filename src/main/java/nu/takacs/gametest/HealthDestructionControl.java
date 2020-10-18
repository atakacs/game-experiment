package nu.takacs.gametest;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

import java.util.function.Consumer;

public class HealthDestructionControl extends AbstractControl {

    private final Consumer<Spatial> onBlowup;

    public HealthDestructionControl(final Consumer<Spatial> onBlowup) {
        this.onBlowup = onBlowup;

    }

    @Override
    protected void controlUpdate(final float tpf) {
        final Object o = spatial.getUserData("health");
        if(o instanceof Integer && ((Integer)o) < 1) {
            spatial.removeFromParent();
            onBlowup.accept(spatial);
        }
    }

    @Override
    protected void controlRender(final RenderManager rm, final ViewPort vp) {}
}
