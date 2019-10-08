package com.emamaker.voxelengine.world;

import static com.emamaker.voxelengine.utils.Globals.MAXX;
import static com.emamaker.voxelengine.utils.Globals.MAXY;
import static com.emamaker.voxelengine.utils.Globals.MAXZ;
import static com.emamaker.voxelengine.utils.Globals.chunkSize;
import static com.emamaker.voxelengine.utils.Globals.debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.emamaker.voxelengine.block.CellId;
import com.emamaker.voxelengine.utils.Globals;
import com.emamaker.voxelengine.utils.math.MathHelper;

//import  com.emamaker.voxelengine.block.TextureManager;

public class Chunk {

	boolean toBeSet = true;
	boolean toUpdateMesh = false;
	boolean keepUpdating = true;
	boolean loaded = false;
	boolean toUnload = false;
	boolean phyLoaded = false;
	boolean meshing = false;
	public boolean generated = false;
	public boolean decorated = false;

	int partIndex = 0;

	// the chunk coords in the world
	public int x, y, z;

	// public Cell[] cells = new Cell[chunkSize * chunkSize * chunkSize];
	public byte[] cells = new byte[chunkSize * chunkSize * chunkSize];

//	public Mesh chunkMesh = new Mesh();
//	public Geometry chunkGeom;
	Vector3 pos = new Vector3();
	Random rand = new Random();

	public Chunk() {
		this(0, 0, 0);
	}

	public Chunk(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;

		pos = new Vector3(x * chunkSize, y * chunkSize, z * chunkSize);
		debug("Creating chunk starting at world coords" + pos.toString());

//		chunkGeom = new Geometry(this.toString() + pos.toString(), chunkMesh);
//		chunkGeom.setMaterial(Globals.mat);
//		chunkGeom.setLocalTranslation(pos);
//
//		Globals.terrainNode.addControl((AbstractControl) this);

		prepareCells();
		markForUpdate(true);
	}

	void prepareCells() {
		for (int i = 0; i < cells.length; i++) {
			cells[i] = CellId.ID_AIR;
		}
	}

	public void processCells() {
		if (toBeSet) {

			for (int i = 0; i < chunkSize; i++) {
				for (int j = 0; j < chunkSize; j++) {
					for (int k = 0; k < chunkSize; k++) {
						dirtToGrass(i, j, k);
						grassToDirt(i, j, k);
					}
				}
			}

			kindaBetterGreedy();
			markForUpdate(false);
			debug("Updated " + this.toString() + " at " + x + ", " + y + ", " + z);
		}
	}

//	public void updateMesh() {
//		if (toUpdateMesh) {
//			keepUpdating = false;
//
////			chunkMesh = new Mesh();
////			setMesh();
////			chunkGeom.setMesh(chunkMesh);
//
//			keepUpdating = true;
//			markMeshForUpdate(false);
//		}
//	}

//	public void load() {
//		// on first load, Global material is null because it hasn't been initialized
//		// yet, so it's set here
//		if (chunkGeom.getMaterial() == null) {
//			chunkGeom.setMaterial(Globals.mat);
//		}
//
//		if (!loaded) {
//			loaded = true;
//			meshing = false;
//			Globals.terrainNode.attachChild(chunkGeom);
//			chunkGeom.setCullHint(Spatial.CullHint.Never);
//		}
//	}
//
//	public void unload() {
//		if (loaded) {
//			loaded = false;
//			Globals.terrainNode.detachChild(chunkGeom);
//		}
//	}

//	public void loadPhysics() {
//		if (!phyLoaded && Globals.phyEnabled()) {
//			try {
//				this.chunkGeom.addControl(new RigidBodyControl(CollisionShapeFactory.createMeshShape(chunkGeom), 0f));
//				Globals.main.getStateManager().getState(BulletAppState.class).getPhysicsSpace()
//						.add(chunkGeom.getControl(RigidBodyControl.class));
//				phyLoaded = true;
//			} catch (Exception e) {
//			}
//		}
//	}
//
//	public void unloadPhysics() {
//		if (phyLoaded && Globals.phyEnabled()) {
//
//			chunkGeom.getControl(RigidBodyControl.class).setEnabled(false);
//			chunkGeom.removeControl(chunkGeom.getControl(RigidBodyControl.class));
//			// Globals.main.getStateManager().getState(BulletAppState.class).getPhysicsSpace().remove(chunkGeom);
//			phyLoaded = false;
//		}
//	}
//
//	public void refreshPhysics() {
//		unloadPhysics();
//		loadPhysics();
//	}

	public void render(ModelBatch batch, Environment e) {
		if (chunkModel != null) {
			instance = new ModelInstance(chunkModel);
			if (instance != null) {
				batch.render(instance, e);
//				debug("Rending " + this);
			}
		}
	}

	public byte getHighestCellAt(int i, int j) {
		return getCell(i, getHighestYAt(i, j), j);
	}

	public int getHighestYAt(int i, int j) {
		for (int a = chunkSize; a >= 0; a--) {
			if (getCell(i, a, j) != Byte.MIN_VALUE && getCell(i, a, j) != CellId.ID_AIR) {
				return a;
			}
		}
		return Integer.MAX_VALUE;
	}

	public byte getCell(int i, int j, int k) {
		if (i >= 0 && j >= 0 && k >= 0 && i < chunkSize && j < chunkSize && k < chunkSize) {
			return cells[MathHelper.flatCell3Dto1D(i, j, k)];
		}
		return Byte.MIN_VALUE;
	}

	public byte getCell(int index) {
		if (index >= 0 && index < cells.length || index != Integer.MAX_VALUE) {
			return cells[index];
		}
		return Byte.MIN_VALUE;

	}

	public void setCell(int i, int j, int k, byte id) {
		if (i >= 0 && j >= 0 && k >= 0 && i < chunkSize && j < chunkSize && k < chunkSize) {
			cells[MathHelper.flatCell3Dto1D(i, j, k)] = id;
		}
		markForUpdate(true);
	}

	public void generate() {
		if (!generated) {
			Globals.getWorldGenerator().generate(this);
			generated = true;
		}
	}

	public void decorate() {
		if (!decorated && Globals.decoratorsEnabled()) {
			Globals.getWorldDecorator().decorate(this);
			decorated = true;
		}
	}

//	@Override
//	protected void controlUpdate(float tpf) {
//		if (keepUpdating) {
//			if ((Math.sqrt(Math.pow(x - pX, 2) + Math.pow(y - pY, 2) + Math.pow(z - pZ, 2)) > renderDistance)) {
//				this.unload();
//				this.unloadPhysics();
//
//				if (Math.sqrt(Math.pow(x - pX, 2) + Math.pow(y - pY, 2) + Math.pow(z - pZ, 2)) > renderDistance
//						* 2.5f) {
//					saveToFile();
//					Globals.terrainNode.removeControl(this);
//					// Globals.voxelWorld.worldManager.setChunk(x, y, z, null);
//				}
//			} else {
//				this.load();
//				if (Math.sqrt(Math.pow(x - pX, 2) + Math.pow(y - pY, 2) + Math.pow(z - pZ, 2)) <= 1) {
//					this.refreshPhysics();
//				} else {
//					this.unloadPhysics();
//				}
//			}
//		}
//	}

//	@Override
//	protected void controlRender(RenderManager rm, ViewPort vp) {
//	}

	public boolean isEmpty() {
		for (int i = 0; i < cells.length; i++) {
			if (cells[i] != CellId.ID_AIR) {
				return false;
			}
		}
		return true;
	}

	// Saves the chunk to text file, with format X Y Z ID, separated by spaces
	public void saveToFile() {
		File f = Paths.get(Globals.workingDir + x + "-" + y + "-" + z + ".chunk").toFile();
		int[] coords;

		if (!f.exists() && !isEmpty()) {
			try {
				PrintWriter writer = new PrintWriter(f);
				for (int i = 0; i < cells.length; i++) {
					coords = MathHelper.cell1Dto3D(i);
					writer.println(coords[0] + "," + coords[1] + "," + coords[2] + "," + cells[i]);
				}

				writer.close();
			} catch (FileNotFoundException e) {
			}
		}
	}

	// Retrieves back from the text file (X Y Z ID separated by commas)
	public void loadFromFile(File f) {
		List<String> lines;

		if (f.exists()) {
			if (!(f.length() == 0)) {
				try {
					lines = Files.readAllLines(f.toPath());

					for (String s : lines) {
						setCell(Integer.valueOf(s.split(",")[0]), Integer.valueOf(s.split(",")[1]),
								Integer.valueOf(s.split(",")[2]), Byte.valueOf(s.split(",")[3]));
					}

					generated = true;
					decorated = true;
					markForUpdate(true);
					f.delete();
				} catch (IOException | NumberFormatException e) {
				}
			} else {
				generated = false;
				decorated = false;
				generate();
			}
		} else {
			generated = false;
			decorated = false;
			generate();
		}
	}

	public void markForUpdate(boolean b) {
		toBeSet = b;
	}

	public void markMeshForUpdate(boolean b) {
		toUpdateMesh = b;
	}

	public boolean isVisible() {
		boolean v = false;

		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				for (int k = -1; k <= 1; k++) {
					if (x + i >= 0 && x + i < MAXX && y + j >= 0 && y + j < MAXY && z + k >= 0 && z + k < MAXZ) {
						if (Globals.voxelWorld.worldManager.getChunk(x + i, y + j, z + k) != null) {
							v = true;
						}
					} else {
						v = true;
					}
				}
			}
		}
		return v;
	}

	static boolean[][] meshed = new boolean[chunkSize * chunkSize * chunkSize][6];
	ModelBuilder modelBuilder = new ModelBuilder();
	MeshPartBuilder meshBuilder;
	VertexInfo v0, v1, v2, v3;
	Model chunkModel;
	ModelInstance instance;

	/**
	 * MESH CONSTRUCTING STUFF
	 */
	// Kinda better greedy meshing algorithm than before. Now expanding in both axis
	// (X-Y, Z-Y, X-Z), not gonna try to connect in negative side, it's not needed
	public void kindaBetterGreedy() {

		partIndex = 0;

		clearAll();
		meshing = true;

		for (int i = 0; i < meshed.length; i++) {
			for (int j = 0; j < 6; j++) {
				meshed[i][j] = false;
			}
		}

		int startX, startY, startZ, offX, offY, offZ, index, cPos, c1Pos;
		short i0 = 0, i1 = 0, i2 = 0, i3 = 0;
		byte c, c1;
		VertexInfo v0, v1, v2, v3;
		boolean done = false;

		modelBuilder.begin();
		for (int a = 0; a < cells.length; a++) {
			for (int s = 0; s < 3; s++) {
				for (int i = 0; i < 2; i++) {

					int backfaces[] = { 0, 0, 0 };
					backfaces[s] = i;
					index = s * 2 + i;

					c = getCell(a);

					if (c != CellId.ID_AIR && c != Byte.MIN_VALUE && cellHasFreeSideChunk(a, index)
							&& !meshed[a][index]) {
						cPos = a;
						startX = MathHelper.cell1Dto3D(cPos)[0];
						startY = MathHelper.cell1Dto3D(cPos)[1];
						startZ = MathHelper.cell1Dto3D(cPos)[2];

						offX = 0;
						offY = 0;
						offZ = 0;

						if (startX + offX < chunkSize && startY + offY < chunkSize && startZ + offZ < chunkSize) {
							if (s == 0 || s == 2) {
								offZ++;
							} else {
								offX++;
							}
						}

						c1Pos = MathHelper.flatCell3Dto1D(startX + offX, startY + offY, startZ + offZ);
						c1 = getCell(c1Pos);
						while (c1 != CellId.ID_AIR && c1 != Byte.MIN_VALUE && c1 == c
								&& cellHasFreeSideChunk(c1Pos, index) && !meshed[c1Pos][index]) {

							meshed[c1Pos][index] = true;
							if (startX + offX < chunkSize && startY + offY < chunkSize && startZ + offZ < chunkSize) {

								if ((s == 0 || s == 2)) {
									offZ++;
								} else {
									offX++;
								}
							}
							c1Pos = MathHelper.flatCell3Dto1D(startX + offX, startY + offY, startZ + offZ);
							c1 = getCell(c1Pos);
						}

						done = false;

						switch (s) {
						case 0:
							while (!done) {
								offY++;
								for (int k = startZ; k < startZ + offZ; k++) {
									c1Pos = MathHelper.flatCell3Dto1D(startX + offX, startY + offY, k);
									c1 = getCell(c1Pos);

									if (c1 != c || meshed[c1Pos][index] || !cellHasFreeSideChunk(c1Pos, index)) {
										done = true;
										break;
									}
								}

								if (!done) {
									for (int k = startZ; k < startZ + offZ; k++) {
										c1Pos = MathHelper.flatCell3Dto1D(startX + offX, startY + offY, k);
										meshed[c1Pos][index] = true;
									}
								}
							}
							break;
						case 1:
							while (!done) {
								offY++;
								for (int k = startX; k < startX + offX; k++) {
									c1Pos = MathHelper.flatCell3Dto1D(k, startY + offY, startZ + offZ);
									c1 = getCell(c1Pos);

									if (c1 != c || !cellHasFreeSideChunk(c1Pos, index) || meshed[c1Pos][index]) {
										done = true;
										break;
									}
								}
								if (!done) {
									for (int k = startX; k < startX + offX; k++) {
										c1Pos = MathHelper.flatCell3Dto1D(k, startY + offY, startZ + offZ);
										c1 = getCell(c1Pos);
										meshed[c1Pos][index] = true;
									}
								}
							}
							break;
						case 2:
							while (!done) {
								offX++;
								for (int k = startZ; k < startZ + offZ; k++) {
									c1Pos = MathHelper.flatCell3Dto1D(startX + offX, startY + offY, k);
									c1 = getCell(c1Pos);

									if (c1 != c || !cellHasFreeSideChunk(c1Pos, index) || meshed[c1Pos][index]) {
										done = true;
										break;
									}
								}
								if (!done) {
									for (int k = startZ; k < startZ + offZ; k++) {
										c1Pos = MathHelper.flatCell3Dto1D(startX + offX, startY + offY, k);
										c1 = getCell(c1Pos);
										meshed[c1Pos][index] = true;
									}
								}
							}
							break;
						}
						meshed[cPos][index] = true;

						// sets the vertices
						switch (s) {
						case 0:

							meshBuilder = modelBuilder.part("part" + partIndex, GL20.GL_TRIANGLES, Usage.Position,
									new Material());
							meshBuilder.setColor(Color.WHITE);

							v0 = new VertexInfo().setPos(startX + backfaces[0], startY, startZ).setCol(null).setUV(0.5f,
									0.5f).setNor(1-backfaces[i]*2,0,0);
							v1 = new VertexInfo().setPos(startX + backfaces[0], startY + offY, startZ).setCol(null)
									.setUV(0.5f, 0.5f).setNor(1-backfaces[i]*2,0,0);
							v2 = new VertexInfo().setPos(startX + backfaces[0], startY + offY, startZ + offZ)
									.setCol(null).setUV(0.5f, 0.5f).setNor(1-backfaces[i]*2,0,0);
							v3 = new VertexInfo().setPos(startX + backfaces[0], startY, startZ + offZ).setCol(null)
									.setUV(0.5f, 0.5f).setNor(1-backfaces[i]*2,0,0);

							partIndex++;

							if (backfaces[s] == 0)
								meshBuilder.rect(v3, v2, v1, v0);
							else
								meshBuilder.rect(v0, v1, v2, v3);

//							i0 = addVertex(v0);
//							i1 = addVertex(v1);
//							i2 = addVertex(v2);
//							i3 = addVertex(v3);
//
//							addTextureVertex(i0, new Vector3(0, 0, TextureManager.textures.get(c)[index]));
//							addTextureVertex(i1, new Vector3(0, offY, TextureManager.textures.get(c)[index]));
//							addTextureVertex(i2, new Vector3(offZ, offY, TextureManager.textures.get(c)[index]));
//							addTextureVertex(i3, new Vector3(offZ, 0, TextureManager.textures.get(c)[index]));

							break;
						case 1:
							meshBuilder = modelBuilder.part("part" + partIndex, GL20.GL_TRIANGLES,
									Usage.Position | Usage.Normal, new Material());
							meshBuilder.setColor(Color.WHITE);

							v0 = new VertexInfo().setPos(startX, startY, startZ + backfaces[1]).setCol(null)
									.setUV(0.5f, 0.5f).setNor(0, 0, 1 - backfaces[i] * 2);
							v1 = new VertexInfo().setPos(startX, startY + offY, startZ + backfaces[1]).setCol(null)
									.setUV(0.5f, 0.5f).setNor(0, 0, 1 - backfaces[i] * 2);
							v2 = new VertexInfo().setPos(startX + offX, startY + offY, startZ + backfaces[1])
									.setCol(null).setUV(0.5f, 0.5f).setNor(0, 0, 1 - backfaces[i] * 2);
							v3 = new VertexInfo().setPos(startX + offX, startY, startZ + backfaces[1]).setCol(null)
									.setUV(0.5f, 0.5f).setNor(0, 0, 1 - backfaces[i] * 2);
							partIndex++;

//							if (backfaces[s] == 0)
//								meshBuilder.rect();
//							else

							if (backfaces[s] == 0)
								meshBuilder.rect(v1, v2, v3, v0);
							else
								meshBuilder.rect(v3, v2, v1, v0);
							// i0 = addVertex(v0);
//							i1 = addVertex(v1);
//							i2 = addVertex(v2);
//							i3 = addVertex(v3);

//							addTextureVertex(i0, new Vector3(0, 0, TextureManager.textures.get(c)[index]));
//							addTextureVertex(i1, new Vector3(0, offY, TextureManager.textures.get(c)[index]));
//							addTextureVertex(i2, new Vector3(offX, offY, TextureManager.textures.get(c)[index]));
//							addTextureVertex(i3, new Vector3(offX, 0, TextureManager.textures.get(c)[index]));

							break;
						case 2:
							meshBuilder = modelBuilder.part("part" + partIndex, GL20.GL_TRIANGLES,
									Usage.Position | Usage.Normal, new Material());
							meshBuilder.setColor(Color.WHITE);
							v0 = new VertexInfo().setPos(startX, startY + backfaces[2], startZ).setCol(null).setUV(0.5f,
									0.5f).setNor(0,1-backfaces[i]*2,0);
							v1 = new VertexInfo().setPos(startX + offX, startY + backfaces[2], startZ).setCol(null)
									.setUV(0.5f, 0.5f).setNor(0,1-backfaces[i]*2,0);
							v2 = new VertexInfo().setPos(startX + offX, startY + backfaces[2], startZ + offZ)
									.setCol(null).setUV(0.5f, 0.5f).setNor(0,1-backfaces[i]*2,0);
							v3 = new VertexInfo().setPos(startX, startY + backfaces[2], startZ + offZ).setCol(null)
									.setUV(0.5f, 0.5f).setNor(0,1-backfaces[i]*2,0);
//

							if (backfaces[s] == 0)
								meshBuilder.rect(v1, v2, v3, v0);
							else
								meshBuilder.rect(v3, v2, v1, v0);
							partIndex++;
//							i0 = addVertex(v0);
//							i1 = addVertex(v1);
//							i2 = addVertex(v2);
//							i3 = addVertex(v3);

//							addTextureVertex(i0, new Vector3(0, 0, TextureManager.textures.get(c)[index]));
//							addTextureVertex(i1, new Vector3(0, offX, TextureManager.textures.get(c)[index]));
//							addTextureVertex(i2, new Vector3(offZ, offX, TextureManager.textures.get(c)[index]));
//							addTextureVertex(i3, new Vector3(offZ, 0, TextureManager.textures.get(c)[index]));
							break;
						default:
							System.out.println("puzzette");
							break;
						}
						// now constructs the mesh

						indicesList.add(i0);
						indicesList.add(i3);
						indicesList.add(i2);
						indicesList.add(i2);
						indicesList.add(i1);
						indicesList.add(i0);

						meshing = false;
						markMeshForUpdate(true);
//                        setMesh();
					}
				}
			}
		}
		chunkModel = modelBuilder.end();

	}

	public ArrayList<Vector3> verticesList = new ArrayList<>();
	public ArrayList<Vector3> textureList = new ArrayList<>();
	public ArrayList<Short> indicesList = new ArrayList<>();

	Short[] short1;
	short[] indices;

	// usually called at the end of the update() method of chunk. creates the mesh
	// from the vertices, indices and texCoord set by Cell and adds it to a geometry
	// with a material with correct texture loaded
//	public void setMesh() {
//		try {
//			// checking if there are empty buffers is important: loading empty buffers
//			// causes a core dumped crash
//			if (!verticesList.isEmpty() && !textureList.isEmpty() && !indicesList.isEmpty()) {
//				short1 = indicesList.toArray(new Short[indicesList.size()]);
//				indices = new short[short1.length];
//				for (int i = 0; i < short1.length; i++) {
//					indices[i] = Short.valueOf(Integer.toString(short1[i]));
//				}
//
//				chunkMesh.setBuffer(VertexBuffer.Type.Position, 3,
//						BufferUtils.createFloatBuffer(verticesList.toArray(new Vector3[verticesList.size()])));
//				chunkMesh.setBuffer(VertexBuffer.Type.TexCoord, 3,
//						BufferUtils.createFloatBuffer(textureList.toArray(new Vector3[textureList.size()])));
//				chunkMesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createShortBuffer(indices));
//
//				chunkMesh.updateBound();
////            clearAll();        
//			}
//		} catch (Exception e) {
//			debug(e);
//		}
//	}

	public void clearAll() {
		indicesList.clear();
		verticesList.clear();
		textureList.clear();
	}

	public Vector3 getVectorFor(int x, int y, int z) {
		for (Vector3 v : verticesList) {
			if (v.x == x && v.y == y && v.z == z) {
				return v;
			}
		}
		return null;
	}

	public short addVertex(Vector3 v) {
		verticesList.add(v);
		return (short) (verticesList.size() - 1);
	}

//	public void addTextureVertex(short index, Vector3 texVec) {
//		try {
//			textureList.add(index, texVec);
//		} catch (IndexOutOfBoundsException e) {
//			while (textureList.size() < index + 1) {
//				textureList.add(Vector3.NAN);
//			}
//			textureList.add(index, texVec);
//		}
//	}

	public boolean cellHasFreeSideWorld(int cellX, int cellY, int cellZ, int side) {
//        System.out.println("Checking at world coords " + cellX + ", " + cellY + ", " + cellZ + " with side " + side);
		switch (side) {
		case 0:
			return (Globals.voxelWorld.worldManager.getCell(cellX - 1, cellY, cellZ) == CellId.ID_AIR
					|| Globals.voxelWorld.worldManager.getCell(cellX - 1, cellY, cellZ) == Byte.MIN_VALUE);
		case 1:
			return (Globals.voxelWorld.worldManager.getCell(cellX + 1, cellY, cellZ) == CellId.ID_AIR
					|| Globals.voxelWorld.worldManager.getCell(cellX + 1, cellY, cellZ) == Byte.MIN_VALUE);
		case 2:
			return (Globals.voxelWorld.worldManager.getCell(cellX, cellY, cellZ - 1) == CellId.ID_AIR
					|| Globals.voxelWorld.worldManager.getCell(cellX, cellY, cellZ - 1) == Byte.MIN_VALUE);
		case 3:
			return (Globals.voxelWorld.worldManager.getCell(cellX, cellY, cellZ + 1) == CellId.ID_AIR
					|| Globals.voxelWorld.worldManager.getCell(cellX, cellY, cellZ + 1) == Byte.MIN_VALUE);
		case 4:
			return (Globals.voxelWorld.worldManager.getCell(cellX, cellY - 1, cellZ) == CellId.ID_AIR
					|| Globals.voxelWorld.worldManager.getCell(cellX, cellY - 1, cellZ) == Byte.MIN_VALUE);
		case 5:
			return (Globals.voxelWorld.worldManager.getCell(cellX, cellY + 1, cellZ) == CellId.ID_AIR
					|| Globals.voxelWorld.worldManager.getCell(cellX, cellY + 1, cellZ) == Byte.MIN_VALUE);
		default:
			System.out.println("Ouch!");
			return false;
		}
	}

	public boolean cellHasFreeSideChunkToWorld(int cPos, int side) {
		return cellHasFreeSideWorld(x * chunkSize + MathHelper.cell1Dto3D(cPos)[0],
				y * chunkSize + MathHelper.cell1Dto3D(cPos)[1], z * chunkSize + MathHelper.cell1Dto3D(cPos)[2], side);
	}

	public boolean cellHasFreeSideChunkToWorld(int cellChunkX, int cellChunkY, int cellChunkZ, int side) {
		return cellHasFreeSideWorld(x * chunkSize + cellChunkX, y * chunkSize + cellChunkY, z * chunkSize + cellChunkZ,
				side);
	}

	public boolean cellHasFreeSideChunk(int cPos, int side) {
		return cellHasFreeSideChunk(MathHelper.cell1Dto3D(cPos)[0], MathHelper.cell1Dto3D(cPos)[1],
				MathHelper.cell1Dto3D(cPos)[2], side);
	}

	public boolean cellHasFreeSideChunk(int cellX, int cellY, int cellZ, int side) {
		switch (side) {
		case 0:
			return (getCell(cellX - 1, cellY, cellZ) == CellId.ID_AIR
					|| getCell(cellX - 1, cellY, cellZ) == Byte.MIN_VALUE);
		case 1:
			return (getCell(cellX + 1, cellY, cellZ) == CellId.ID_AIR
					|| getCell(cellX + 1, cellY, cellZ) == Byte.MIN_VALUE);
		case 2:
			return (getCell(cellX, cellY, cellZ - 1) == CellId.ID_AIR
					|| getCell(cellX, cellY, cellZ - 1) == Byte.MIN_VALUE);
		case 3:
			return (getCell(cellX, cellY, cellZ + 1) == CellId.ID_AIR
					|| getCell(cellX, cellY, cellZ + 1) == Byte.MIN_VALUE);
		case 4:
			return (getCell(cellX, cellY - 1, cellZ) == CellId.ID_AIR
					|| getCell(cellX, cellY - 1, cellZ) == Byte.MIN_VALUE);
		case 5:
			return (getCell(cellX, cellY + 1, cellZ) == CellId.ID_AIR
					|| getCell(cellX, cellY + 1, cellZ) == Byte.MIN_VALUE);
		default:
			System.out.println("Ouch!");
			return false;
		}
	}

	public void dirtToGrass(int cellX, int cellY, int cellZ) {
		if (getCell(cellX, cellY, cellZ) == CellId.ID_DIRT && getCell(cellX, cellY + 1, cellZ) == CellId.ID_AIR) {
			setCell(cellX, cellY, cellZ, CellId.ID_GRASS);
		}
	}

	public void grassToDirt(int cellX, int cellY, int cellZ) {
		if (getCell(cellX, cellY, cellZ) == CellId.ID_GRASS && getCell(cellX, cellY + 1, cellZ) != CellId.ID_AIR) {
			setCell(cellX, cellY, cellZ, CellId.ID_DIRT);
		}
	}

	public String info() {
		return (this.toString() + " at " + x + ", " + y + ", " + z);
	}

}
