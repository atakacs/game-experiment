package nu.takacs.gametest.control;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

public class TimedDestructionControl extends AbstractControl {

    private final Instant blowUpAt;
    private final Consumer<Spatial> onBlowup;

    public TimedDestructionControl(final Consumer<Spatial> onBlowup, final Long ttlMillis) {
        this.onBlowup = onBlowup;

        blowUpAt = Instant.now().plus(ttlMillis, ChronoUnit.MILLIS);
    }

    @Override
    protected void controlUpdate(final float tpf) {
        if (!Instant.now().isBefore(blowUpAt)) {
            spatial.removeFromParent();
            onBlowup.accept(spatial);
        }
    }

    @Override
    protected void controlRender(final RenderManager rm, final ViewPort vp) {}
}
