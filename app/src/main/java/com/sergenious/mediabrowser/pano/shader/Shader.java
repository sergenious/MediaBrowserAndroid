package com.sergenious.mediabrowser.pano.shader;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.sergenious.mediabrowser.Constants;
import com.sergenious.mediabrowser.pano.matrix.Matrix;
import com.sergenious.mediabrowser.pano.matrix.Matrix.MatrixMode;
import com.sergenious.mediabrowser.pano.mesh.AbstractVertexMesh;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Shader {
	public int programId;
	public final String name;
	public int positionHandle, normalHandle;
	public final int[] texCoordHandle = new int[AbstractVertexMesh.MAX_TEX_UNITS];
	private long projMatrixRevision, modelViewMatrixRevision, normalMatrixRevision;
	private int projMatrixHandle, modelViewMatrixHandle, normalMatrixHandle;
	private final int[] textureMatrixHandle = new int[AbstractVertexMesh.MAX_TEX_UNITS];
	private final long[] textureMatrixRevision = new long[AbstractVertexMesh.MAX_TEX_UNITS];

	public Shader(Context ctx, String name, int vertexShaderResId, int fragmentShaderResId) {
		this.name = name;
		int vertexShaderId = loadShader(ctx, GLES20.GL_VERTEX_SHADER, vertexShaderResId);
		int fragmentShaderId = loadShader(ctx, GLES20.GL_FRAGMENT_SHADER, fragmentShaderResId);

		if ((vertexShaderId < 0) || (fragmentShaderId < 0)) {
			if (vertexShaderId >= 0) {
				GLES20.glDeleteShader(vertexShaderId);
			}
			if (fragmentShaderId >= 0) {
				GLES20.glDeleteShader(fragmentShaderId);
			}
			return; // both shaders must be defined
		}

		programId = GLES20.glCreateProgram();

		GLES20.glAttachShader(programId, vertexShaderId);
		GLES20.glAttachShader(programId, fragmentShaderId);
		GLES20.glLinkProgram(programId);

		String programLog = GLES20.glGetProgramInfoLog(programId);
		if ((programLog != null) && !programLog.trim().isEmpty()) {
			Log.e(Constants.appNameInternal, "Error linking OpenGL shader program \"" + name + "\": \n" + programLog);
        	deleteProgram();
            return;
        }

		GLES20.glUseProgram(programId);
        projMatrixHandle = GLES20.glGetUniformLocation(programId, "projMatrix");
        modelViewMatrixHandle = GLES20.glGetUniformLocation(programId, "modelViewMatrix");
        normalMatrixHandle = GLES20.glGetUniformLocation(programId, "normalMatrix");
        positionHandle = GLES20.glGetAttribLocation(programId, "position");
        normalHandle = GLES20.glGetAttribLocation(programId, "normal");

        for (int tex = 0; tex < AbstractVertexMesh.MAX_TEX_UNITS; tex++) {
        	texCoordHandle[tex] = GLES20.glGetAttribLocation(programId, "texCoord" + tex);
        	textureMatrixHandle[tex] = GLES20.glGetUniformLocation(programId, "texture" + tex + "Matrix");

        	int textureHandle = GLES20.glGetUniformLocation(programId, "texture" + tex);
        	if (textureHandle >= 0) {
        		GLES20.glUniform1i(textureHandle, tex);
			}
        }

        GLES20.glUseProgram(0);
	}

	public static void useNoProgram() {
		GLES20.glUseProgram(0);
	}

	public void useProgram() {
		GLES20.glUseProgram(programId);
	}

	public void deleteProgram() {
		if (programId > 0) {
			GLES20.glUseProgram(programId);
			int[] shaderIds = new int[2];
			int[] count = new int[1];
			GLES20.glGetAttachedShaders(programId, 2, count, 0, shaderIds, 0);
			
			for (int i = 0; i < count[0]; i++) {
				GLES20.glDetachShader(programId, shaderIds[i]);
				GLES20.glDeleteShader(shaderIds[i]);
			}
			GLES20.glUseProgram(0);
			GLES20.glDeleteProgram(programId);
			GLES20.glGetError(); // silently ignore the error, as the GL context might be re-created, so the programs are invalid
		}
		programId = 0;
	}

	private long setMatrix(int handle, long currRevision, MatrixMode mode) {
		if (handle >= 0) {
			long rev = Matrix.getRevision(mode);
			if (currRevision != rev) {
				GLES20.glUniformMatrix4fv(handle, 1, false, Matrix.get(mode), 0);
				return rev;
			}
		}
		return currRevision;
	}
	
	public void setMatrices() {
		projMatrixRevision = setMatrix(projMatrixHandle, projMatrixRevision, MatrixMode.PROJECTION);
		modelViewMatrixRevision = setMatrix(modelViewMatrixHandle, modelViewMatrixRevision, MatrixMode.MODELVIEW);
		normalMatrixRevision = setMatrix(normalMatrixHandle, normalMatrixRevision, MatrixMode.NORMAL);
		for (int tex = 0; tex < AbstractVertexMesh.MAX_TEX_UNITS; tex++) {
			textureMatrixRevision[tex] = setMatrix(textureMatrixHandle[tex],
				textureMatrixRevision[tex], MatrixMode.values()[MatrixMode.TEXTURE0.ordinal() + tex]);
		}
	}

	public void setUniformValue(String name, float value) {
		if (programId == 0) {
			return;
		}
		int handle = GLES20.glGetUniformLocation(programId, name);
		if (handle >= 0) {
			GLES20.glUniform1f(handle, value);
		}
	}

	private static int loadShader(Context ctx, int type, int resourceId) {
		try (InputStream is = ctx.getResources().openRawResource(resourceId)) {
			byte[] shaderData = new byte[is.available()];
		    if (is.read(shaderData, 0, shaderData.length) < shaderData.length) {
				return -1; // should not happen
			}
		    String shaderCode = new String(shaderData, StandardCharsets.UTF_8);
		    
			int shaderID = GLES20.glCreateShader(type);
		    GLES20.glShaderSource(shaderID, shaderCode);
		    GLES20.glCompileShader(shaderID);
		    
		    int[] compileStatus = new int[1];
		    GLES20.glGetShaderiv(shaderID, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
		    if (compileStatus[0] != GLES20.GL_TRUE) {
		    	Log.e(Constants.appNameInternal, "Error compiling OpenGL shader: \n" + GLES20.glGetShaderInfoLog(shaderID));
		    	GLES20.glDeleteShader(shaderID);
		    	return -1;
		    }
		    
		    return shaderID;
		}
		catch (IOException e) {
			Log.e(Constants.appNameInternal, "Error loading shader", e);
			return -1;
		}
	}
}
