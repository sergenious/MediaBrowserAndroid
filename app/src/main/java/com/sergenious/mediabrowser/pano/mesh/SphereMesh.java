package com.sergenious.mediabrowser.pano.mesh;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.graphics.Bitmap;
import android.graphics.RectF;

/** Generic spherical mesh object. */
public class SphereMesh extends AbstractVertexMesh {
	public SphereMesh(double radius, RectF panoRect, int meshXSegments, int meshYSegments) {
		super(POSITION_BIT + TEXCOORD0_BIT,
			createVertexArray(true, radius, panoRect, meshXSegments, meshYSegments),
			createIndexArray(meshXSegments, meshYSegments), true);

		shaderName = "default";
	}

	public void loadImage(Bitmap bitmap) {
		destroyImage();
		textureObjectIds = new int[] {createTexture(bitmap)};
	}

	public void destroyImage() {
		if (textureObjectIds != null) {
			for (int texObjId: textureObjectIds) {
				if (texObjId >= 0) {
					deleteTexture(texObjId);
				}
			}
		}
		textureObjectIds = null;
	}

	protected static FloatBuffer createVertexArray(boolean hasTexture, double radius, 
		RectF panoRect, int meshXSegments, int meshYSegments) {
		
		int vertexSize = 3 + (hasTexture ? 2 : 0);
		FloatBuffer array = FloatBuffer.allocate((meshXSegments + 1) * (meshYSegments + 1) * vertexSize);

		int vertexOfs = 0;
		for (int y = 0; y <= meshYSegments; y++) {
			double yRatio = (double) y / meshYSegments;
			for (int x = 0; x <= meshXSegments; x++) {
				double xRatio = (double) x / meshXSegments;
				
				double horzAngle = ((1 - xRatio) * panoRect.left + xRatio * panoRect.right) * Math.PI / 180.0;
				double vertAngle = ((1 - yRatio) * panoRect.top + yRatio * panoRect.bottom) * Math.PI / 180.0;
				array.put(vertexOfs++, (float) (radius * Math.cos(horzAngle) * Math.cos(vertAngle)));
				array.put(vertexOfs++, (float) (radius * Math.sin(horzAngle) * Math.cos(vertAngle)));
				array.put(vertexOfs++, (float) (radius * Math.sin(vertAngle)));

				if (hasTexture) {
					array.put(vertexOfs++, (float) x / meshXSegments);
					array.put(vertexOfs++, (float) y / meshYSegments);
				}
			}
		}

		return array;
	}

	protected static ShortBuffer createIndexArray(int meshXSegments, int meshYSegments) {
		ShortBuffer array = ShortBuffer.allocate(((meshXSegments + 1) * 2 + 2) * meshYSegments);

		int indexOfs = 0;
		for (int y = 1; y <= meshYSegments; y++) {
			int vertexOfs = (y - 1) * (meshXSegments + 1);
			array.put(indexOfs++, (short) vertexOfs); // make a denormalized triangle to make a jump

			for (int x = 0; x <= meshXSegments; x++) {
				array.put(indexOfs++, (short) (vertexOfs + x));
				array.put(indexOfs++, (short) (vertexOfs + x + meshXSegments + 1));
			}

			array.put(indexOfs++, (short) (vertexOfs + meshXSegments * 2 + 1)); // make a denormalized triangle to make a jump
		}
		return array;
	}
}
