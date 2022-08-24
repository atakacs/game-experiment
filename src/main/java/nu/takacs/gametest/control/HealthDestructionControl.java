package nu.takacs.gametest.control;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

import java.util.function.Consumer;

public class HealthDestructionControl extends AbstractControl {

    private final Consumer<Spatial> onBlowup;
    private int health = 100;

    public HealthDestructionControl(final Consumer<Spatial> onBlowup) {
        this.onBlowup = onBlowup;

    }

    @Override
    protected void controlUpdate(final float tpf) {
        if(health < 1) {
            spatial.removeFromParent();
            onBlowup.accept(spatial);
        }
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(final int health) {
        this.health = health;
    }

    @Override
    protected void controlRender(final RenderManager rm, final ViewPort vp) {}
}
