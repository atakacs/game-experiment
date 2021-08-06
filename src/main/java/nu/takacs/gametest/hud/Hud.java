package nu.takacs.gametest.hud;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

public class Hud {
    private static final int NUM_LINES = 10;
    private static final float LINE_MARGIN = 10.0f;

    private final SimpleApplication application;
    private final BitmapText[] bitmapTextLines = new BitmapText[NUM_LINES];
    private final String[] consoleLines = new String[NUM_LINES];

    private int currentLine = 0;
    private final Node consoleNode = new Node("hud-node");

    public Hud(final SimpleApplication application, final BitmapFont font) {
        this.application = application;
        for (int i = 0; i < NUM_LINES; ++i) {
            consoleLines[i] = "Line " + i;

            final var line = new BitmapText(font, false);
            line.setSize(font.getCharSet().getRenderedSize());
            line.setColor(ColorRGBA.White);
            line.setText(consoleLines[i]);
            line.setLocalTranslation(0, (NUM_LINES - i) * (line.getLineHeight() + LINE_MARGIN), 0);

            bitmapTextLines[i] = line;
            consoleNode.attachChild(line);
        }

        final float textHeight = (bitmapTextLines[0].getHeight() + LINE_MARGIN) * NUM_LINES;

        final var material = new Material(application.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", new ColorRGBA(0, 0, 0, 0.3f));
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        material.setTransparent(true);

        final var background = new Geometry("background-quad", new Quad(400, textHeight));
        background.setMaterial(material);

        consoleNode.attachChild(background);

        application.getGuiNode().attachChild(consoleNode);
    }

    private void redraw() {
        for (int i = 0; i < NUM_LINES; ++i) {
            bitmapTextLines[i].setText(consoleLines[(currentLine  + i) % NUM_LINES]);
        }
    }

    public void consoleAppend(final String text) {
        consoleLines[currentLine] = text;

        currentLine = (currentLine + 1) % NUM_LINES;

        redraw();
    }

    public void toggleVisibility() {
        if(application.getGuiNode().hasChild(consoleNode)) {
            application.getGuiNode().detachChild(consoleNode);
        } else {
            application.getGuiNode().attachChild(consoleNode);
        }
    }
}
