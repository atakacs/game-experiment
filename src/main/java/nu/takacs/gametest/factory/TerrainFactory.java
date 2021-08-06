package nu.takacs.gametest.factory;

import com.jme3.app.LegacyApplication;
import com.jme3.material.Material;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;

public class TerrainFactory {

    private final Material terrainMaterial;
    private final AbstractHeightMap heightMap;

    public TerrainFactory(final LegacyApplication application) {
        var assetManager = application.getAssetManager();
        /** 1. Create terrain material and load four textures into it. */
        terrainMaterial = new Material(assetManager,
                "Common/MatDefs/Terrain/TerrainLighting.j3md");

        /** 1.1) Add ALPHA map (for red-blue-green coded splat textures) */
        terrainMaterial.setTexture("AlphaMap", assetManager.loadTexture(
                "Textures/Terrain/splat/alphamap.png"));

        /** 1.2) Add GRASS texture into the red layer (Tex1). */
        final Texture grassTexture = assetManager.loadTexture(
                "Textures/Terrain/splat/grass.jpg");
        grassTexture.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("DiffuseMap", grassTexture);
        terrainMaterial.setFloat("DiffuseMap_0_scale", 64f);

        /** 1.3) Add DIRT texture into the green layer (Tex2) */
        final Texture dirtTexture = assetManager.loadTexture(
                "Textures/Terrain/splat/dirt.jpg");
        dirtTexture.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("DiffuseMap_1", dirtTexture);
        terrainMaterial.setFloat("DiffuseMap_1_scale", 32f);

        /** 1.4) Add ROAD texture into the blue layer (Tex3) */
        Texture rock = assetManager.loadTexture(
                "Textures/Terrain/splat/road.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("DiffuseMap_2", rock);
        terrainMaterial.setFloat("DiffuseMap_2_scale", 128f);

        /** 2. Create the height map */
        final Texture heightMapImage = assetManager.loadTexture(
                "Textures/Terrain/splat/mountains512.png");
        heightMap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightMap.load();
    }


    public TerrainQuad createTerrain() {

        int patchSize = 65;
        var terrain = new TerrainQuad("my terrain", patchSize, 513, heightMap.getHeightMap());

        /** 4. We give the terrain its material, position & scale it, and attach it. */
        terrain.setMaterial(terrainMaterial);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(2f, 1f, 2f);

        return terrain;
    }
}
