package nu.takacs.gametest.control;

import com.jme3.anim.AnimComposer;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class AiControl extends AbstractControl {
    //TODO: implement saveable
    private static final Logger LOG = LoggerFactory.getLogger(AiControl.class);

    private static Random random = new Random();
    private static final String AI_STATE_CREATED = "created";
    private static final String AI_STATE_WALKING = "walking";
    private static final String AI_STATE_IDLE = "idle";

    private final float WALKING_SPEED = 2.0f;

    private String state = "created";
    private long stateUpdated = -1;


//    public static enum AiState implements Savable {
//        WALKING, STANDING;
//    }

    @Override
    protected void controlUpdate(final float tpf) {
        switch (getState()) {
            case AI_STATE_CREATED:
                transition(AI_STATE_CREATED, AI_STATE_WALKING);
                break;

            case AI_STATE_WALKING:
                if (System.currentTimeMillis() - getStateUpdated() > 3000) {
                    transition(AI_STATE_WALKING, AI_STATE_IDLE);
                }
                break;

            case AI_STATE_IDLE:
                if (System.currentTimeMillis() - getStateUpdated() > 3000) {
                    transition(AI_STATE_IDLE, AI_STATE_WALKING);
                }
                break;
        }
    }

    private void transition(final String fromState, final String toState) {
        switch (toState) {
            case AI_STATE_WALKING:
                setState(AI_STATE_WALKING);
                //setAction("Dodge");
                setAnimationAction("Walk");
                setRandomWalkingDirection();
                break;

            case AI_STATE_IDLE:
                setState(AI_STATE_IDLE);
                //setAction("Dodge");
                setAnimationAction("stand"); //TODO: change
                setStandStill();
                break;
        }
    }

    private void setAnimationAction(final String action) {
        final var model = ((Node) spatial).getChild("npcModel");

        final var animComposer = model.getControl(AnimComposer.class);

        animComposer.setCurrentAction(action);
    }

    private void setRandomWalkingDirection() {
        final var characterControl = spatial.getControl(BetterCharacterControl.class);

        if (characterControl == null) {
            return;
        }

        final var moveX = random.nextFloat() * 2 - 1;
        final var moveZ = random.nextFloat() * 2 - 1;

        final var movement = new Vector3f(moveX, 0.0f, moveZ).normalizeLocal().mult(WALKING_SPEED);

        characterControl.setWalkDirection(movement);
        characterControl.setViewDirection(movement);
    }

    private void setStandStill() {
        final var characterControl = spatial.getControl(BetterCharacterControl.class);

        if (characterControl == null) {
            return;
        }

        characterControl.setWalkDirection(Vector3f.ZERO);
    }

    private String getState() {
        return state;
    }

    private long getStateUpdated() {
        if (stateUpdated == -1) {
            throw new RuntimeException("State not updated yet");
        }

        return stateUpdated;
    }

    private void setState(final String state) {

        this.state = state;
        this.stateUpdated = System.currentTimeMillis();
    }

    @Override
    protected void controlRender(final RenderManager rm, final ViewPort vp) {
    }
}
