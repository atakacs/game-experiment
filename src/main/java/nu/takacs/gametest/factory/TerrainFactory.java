package nu.takacs.gametest.factory;

import com.jme3.app.LegacyApplication;
import com.jme3.material.Material;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;

public class TerrainFactory {

    private final Material terrainMaterial;

    private final Texture grassTexture;
    private final Texture dirtTexture;
    private final Texture heightMapImage;
    private final AbstractHeightMap heightMap;

    public TerrainFactory(final LegacyApplication application) {
        var assetManager = application.getAssetManager();
        /** 1. Create terrain material and load four textures into it. */
        terrainMaterial = new Material(assetManager,
                "Common/MatDefs/Terrain/Terrain.j3md");

        /** 1.1) Add ALPHA map (for red-blue-green coded splat textures) */
        terrainMaterial.setTexture("Alpha", assetManager.loadTexture(
                "Textures/Terrain/splat/alphamap.png"));

        /** 1.2) Add GRASS texture into the red layer (Tex1). */
        grassTexture = assetManager.loadTexture(
                "Textures/Terrain/splat/grass.jpg");
        grassTexture.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("Tex1", grassTexture);
        terrainMaterial.setFloat("Tex1Scale", 64f);

        /** 1.3) Add DIRT texture into the green layer (Tex2) */
        dirtTexture = assetManager.loadTexture(
                "Textures/Terrain/splat/dirt.jpg");
        dirtTexture.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("Tex2", dirtTexture);
        terrainMaterial.setFloat("Tex2Scale", 32f);

        /** 1.4) Add ROAD texture into the blue layer (Tex3) */
        Texture rock = assetManager.loadTexture(
                "Textures/Terrain/splat/road.jpg");
        rock.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("Tex3", rock);
        terrainMaterial.setFloat("Tex3Scale", 128f);

        /** 2. Create the height map */
        heightMapImage = assetManager.loadTexture(
                "Textures/Terrain/splat/mountains512.png");
        heightMap = new ImageBasedHeightMap(heightMapImage.getImage());
        heightMap.load();
    }


    public TerrainQuad createTerrain() {

        /** 3. We have prepared material and heightmap.
         * Now we create the actual terrain:
         * 3.1) Create a TerrainQuad and name it "my terrain".
         * 3.2) A good value for terrain tiles is 64x64 -- so we supply 64+1=65.
         * 3.3) We prepared a heightmap of size 512x512 -- so we supply 512+1=513.
         * 3.4) As LOD step scale we supply Vector3f(1,1,1).
         * 3.5) We supply the prepared heightmap itself.
         */
        int patchSize = 65;
        var terrain = new TerrainQuad("my terrain", patchSize, 513, heightMap.getHeightMap());

        /** 4. We give the terrain its material, position & scale it, and attach it. */
        terrain.setMaterial(terrainMaterial);
        terrain.setLocalTranslation(0, -100, 0);
        terrain.setLocalScale(2f, 1f, 2f);

        return terrain;
    }
}
