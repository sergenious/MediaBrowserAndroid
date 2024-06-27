package com.sergenious.mediabrowser.pano.matrix;

import java.util.HashMap;
import java.util.Map;

/** Matrix stack and manipulation, simulation of old fixed-pipeline OpenGL. */
public class Matrix {
	public enum MatrixMode {
		MODELVIEW,
		PROJECTION,
		NORMAL,
		TEXTURE0, // must be at least as many entries, as is the value of TextureBuffer.MAX_TEX_UNITS
		TEXTURE1,
		TEXTURE2,
		TEXTURE3,
		TEXTURE4,
		TEXTURE5,
		TEXTURE6,
		TEXTURE7,
	}

	private final float[/*4x4*/] current = createIdentityMatrix();
	private long changeRevision = 1; // each time a matrix is changed, its version is incremented, so the users can know when to update

	private static final Map<MatrixMode, Matrix> matrices = new HashMap<MatrixMode, Matrix>() {{
		for (MatrixMode mode: MatrixMode.values()) {
			put(mode, new Matrix());
		}
	}};

	private static Matrix currentMatrix = matrices.get(MatrixMode.MODELVIEW);

	private Matrix() {
	}

	public static void setMatrixMode(MatrixMode mode) {
		Matrix matrix = matrices.get(mode);
		if (matrix != null) {
			currentMatrix = matrix;
		}
	}

	public static void resetAll() {
		for (Matrix matrix: matrices.values()) {
			android.opengl.Matrix.setIdentityM(matrix.current, 0);
			matrix.changeRevision++;
		}
	}

	public static void loadIdentity() {
		Matrix matrix = currentMatrix;
		android.opengl.Matrix.setIdentityM(matrix.current, 0);
		matrix.changeRevision++;
	}

	public static void translate(double x, double y, double z) {
		Matrix matrix = currentMatrix;
		android.opengl.Matrix.translateM(matrix.current, 0, (float) x, (float) y, (float) z);
		matrix.changeRevision++;
	}

	public static void rotate(double angle, double x, double y, double z) {
		Matrix matrix = currentMatrix;
		android.opengl.Matrix.rotateM(matrix.current, 0, (float) angle, (float) x, (float) y, (float) z);
		matrix.changeRevision++;
	}

	public static void frustum(double left, double right, double bottom, double top, double near, double far) {
		Matrix matrix = currentMatrix;
		android.opengl.Matrix.frustumM(matrix.current, 0, (float) left, (float) right,
			(float) bottom, (float) top, (float) near, (float) far);
		matrix.changeRevision++;
	}

	public static void multiply(float[/*4x4*/] other) {
		Matrix matrix = currentMatrix;
		float[] temp = new float[4 * 4];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				for (int k = 0; k < 4; k++) {
					temp[i + 4 * j] += matrix.current[k * 4 + i] * other[j * 4 + k];
				}
			}
		}
		System.arraycopy(temp, 0, matrix.current, 0, 16);
	}

	public static long getRevision(MatrixMode mode) {
		Matrix matrix = matrices.get(mode);
		return (matrix != null) ? matrix.changeRevision : 0;
	}

	public static float[/*4x4*/] get(MatrixMode mode) {
		Matrix matrix = matrices.get(mode);
		return (matrix != null) ? matrix.current : null;
	}

	private static float[/*4x4*/] createIdentityMatrix() {
		float[] matrix = new float[16];
		android.opengl.Matrix.setIdentityM(matrix, 0);
		return matrix;
	}
}
