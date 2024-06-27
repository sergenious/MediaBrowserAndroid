package com.sergenious.mediabrowser.pano.mesh;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;

import com.sergenious.mediabrowser.Constants;
import com.sergenious.mediabrowser.pano.model.Vector3D;
import com.sergenious.mediabrowser.pano.shader.Shader;
import com.sergenious.mediabrowser.pano.shader.ShaderList;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/** Abstract mesh object. */
public abstract class AbstractVertexMesh {
	public static final int MAX_TEX_UNITS = 1;

	public final static int POSITION_BIT = 0x00; // dummy placeholder, since vertex coords must always be present
	public final static int NORMAL_BIT = 0x01;
	public final static int COLOR_BIT = 0x02;
	public final static int TEXCOORD0_BIT = 0x04;
	public final static int TEXCOORD1_BIT = 0x08;
	public final static int TEXCOORD2_BIT = 0x10;
	public final static int TEXCOORD3_BIT = 0x20;
	public final static int TANGENT_BIT = 0x40;
	public final static int BINORMAL_BIT = 0x80;

	protected int[] textureObjectIds;
	protected String shaderName = "default";
	protected int vertexObjectId;
	protected int indexObjectId;
	protected int numVertices, numIndices;
	protected boolean isDestroyed = false;
	private final int typeBits; // which values represented by this buffer, a combination of *_BIT

	public AbstractVertexMesh(int typeBits, FloatBuffer vertexArray, ShortBuffer indexArray, boolean isStaticDraw) {
		this.typeBits = typeBits;
		if ((vertexArray != null) && (indexArray != null)) {
			this.vertexObjectId = createBufferObject(GLES20.GL_ARRAY_BUFFER, 4, vertexArray, isStaticDraw);
			this.numVertices = vertexArray.capacity() / (getVertexStride() >> 2);
			this.indexObjectId = createBufferObject(GLES20.GL_ELEMENT_ARRAY_BUFFER, 2, indexArray, isStaticDraw);
			this.numIndices = indexArray.capacity();
		}
	}
	
	public static int createTexture(Bitmap bitmap) {
		int[] texObjectId = new int[1];
		GLES20.glGenTextures(1, texObjectId, 0);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texObjectId[0]);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		return texObjectId[0];
	}
	
	public static void deleteTexture(int texObjectId) {
		int[] tempID = new int[] {texObjectId};
		GLES20.glDeleteTextures(1, tempID, 0);
	}
	
	public static int createBufferObject(int bufferTarget, int elementSize, Buffer dataBuffer, boolean isStaticDraw) {
		int[] objectId = new int[1];
		GLES20.glGenBuffers(1, objectId, 0);
		GLES20.glBindBuffer(bufferTarget, objectId[0]);
		dataBuffer.position(0);
		GLES20.glBufferData(bufferTarget, dataBuffer.capacity() * elementSize, dataBuffer,
			isStaticDraw ? GLES20.GL_STATIC_DRAW : GLES20.GL_DYNAMIC_DRAW);
		GLES20.glBindBuffer(bufferTarget, 0);
		
			int error = GLES20.glGetError();
			if (error != GLES20.GL_NO_ERROR) {
				StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
				String info = " at " + ((stackTrace.length > 3) ? stackTrace[3].getFileName() + " (" + stackTrace[3].getLineNumber() + ")" : ""); 
				Log.e(Constants.appNameInternal, "OpenGL error" + info + ":" + GLU.gluErrorString(error) + " (" + error + ")");
			}
		
		return objectId[0];
	}
	
	public static void deleteBufferObject(int objectId) {
		int[] tempID = new int[] {objectId};
		GLES20.glDeleteBuffers(1, tempID, 0);
	}

	public void destroy() {
		isDestroyed = true;
		if (textureObjectIds != null) {
			for (int texObjId: textureObjectIds) {
				deleteTexture(texObjId);
			}
		}
		textureObjectIds = null;
		if (vertexObjectId >= 0) {
			deleteBufferObject(vertexObjectId);
		}
		if (indexObjectId >= 0) {
			deleteBufferObject(indexObjectId);
		}
		vertexObjectId = indexObjectId = -1;
		numIndices = 0;
	}

	public boolean render(Vector3D ignoredCameraPos, ShaderList shaderList, double depthOffset, double opacity) {
		if (!beforeRender(shaderList, depthOffset, opacity)) {
			return false;
		}

		renderElements();
		afterRender(shaderList);
		return true;
	}

	protected void renderElements() {
		GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, numIndices, GLES20.GL_UNSIGNED_SHORT, 0);
	}

	protected boolean beforeRender(ShaderList shaderList, double depthOffset, double opacity) {
		if (isDestroyed || (vertexObjectId == 0) || (indexObjectId == 0)) {
			return false;
		}

		Shader shader = (shaderList != null) ? shaderList.useShader(shaderName) : null;
		if (shader == null) {
			return false; // must contain a shader
		}
		shader.setMatrices();
		shader.setUniformValue("opacity", (float) opacity);

		if (textureObjectIds != null) {
			for (int texIndex = MAX_TEX_UNITS - 1; texIndex >= 0; texIndex--) {
				int texObjId = (texIndex < textureObjectIds.length) ? textureObjectIds[texIndex] : 0;
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texObjId);
			}
		}

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexObjectId);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexObjectId);

		setVertexAttribArray(shader.positionHandle, POSITION_BIT, 3, GLES20.GL_FLOAT, false);
		setVertexAttribArray(shader.normalHandle, NORMAL_BIT, 3, GLES20.GL_FLOAT, false);
		setVertexAttribArray(shader.texCoordHandle[0], TEXCOORD0_BIT, 2, GLES20.GL_FLOAT, false);

		GLES20.glPolygonOffset(0, (float) depthOffset);
		return true;
	}

	protected void afterRender(ShaderList shaderList) {
		GLES20.glPolygonOffset(0, 0);

		Shader shader = (shaderList != null) ? shaderList.getShader(shaderName) : null;
		if (shader != null) {
			if (shader.positionHandle != -1) {
				GLES20.glDisableVertexAttribArray(shader.positionHandle);
			}
			if (shader.texCoordHandle[0] != -1) {
				GLES20.glDisableVertexAttribArray(shader.texCoordHandle[0]);
			}
			if (shader.normalHandle != -1) {
				GLES20.glDisableVertexAttribArray(shader.normalHandle);
			}
		}

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	protected void setVertexAttribArray(int attribHandle, int whatComponent,
		int numValuesPerElement, int valueType, boolean doNormalize) {

		if (attribHandle != -1) {
			if ((whatComponent == POSITION_BIT) || ((typeBits & whatComponent) > 0)) {
				GLES20.glEnableVertexAttribArray(attribHandle);
				GLES20.glVertexAttribPointer(attribHandle, numValuesPerElement, valueType,
					doNormalize, getVertexStride(), getVertexAttributeOffset(whatComponent));
			}
			else if (whatComponent == TEXCOORD0_BIT) { // if texcoord is not present, just use vertex position instead
				GLES20.glEnableVertexAttribArray(attribHandle);
				GLES20.glVertexAttribPointer(attribHandle, numValuesPerElement, valueType,
					doNormalize, getVertexStride(), getVertexAttributeOffset(POSITION_BIT));
			}
			else {
				GLES20.glDisableVertexAttribArray(attribHandle);
			}
		}
	}

	/**
	 * @return The offset in bytes of the attribute in the vertex array
	 */
	public int getVertexAttributeOffset(int what) { // what = *_BIT
		int offset = 0;
		if (what > POSITION_BIT) {
			offset += 3 * 4; // vertex
		}
		if ((what > NORMAL_BIT) && ((typeBits & NORMAL_BIT) > 0)) {
			offset += 3 * 4; // normal
		}
		if ((what > COLOR_BIT) && ((typeBits & COLOR_BIT) > 0)) {
			offset += 4; // color
		}
		if ((what > TEXCOORD0_BIT) && ((typeBits & TEXCOORD0_BIT) > 0)) {
			offset += 2 * 4; // texcoord0
		}
		if ((what > TEXCOORD1_BIT) && ((typeBits & TEXCOORD1_BIT) > 0)) {
			offset += 2 * 4; // texcoord1
		}
		if ((what > TEXCOORD2_BIT) && ((typeBits & TEXCOORD2_BIT) > 0)) {
			offset += 2 * 4; // texcoord2
		}
		if ((what > TEXCOORD3_BIT) && ((typeBits & TEXCOORD3_BIT) > 0)) {
			offset += 2 * 4; // texcoord3
		}
		if ((what > TANGENT_BIT) && ((typeBits & TANGENT_BIT) > 0)) {
			offset += 3 * 4; // tangent
		}
		if ((what > BINORMAL_BIT) && ((typeBits & BINORMAL_BIT) > 0)) {
			offset += 3 * 4; // binormal
		}
		return offset;
	}

	/**
	 * @return The size in bytes of one packet of vertex attributes (depends on the type bits)
	 */
	public int getVertexStride() {
		return getVertexAttributeOffset(0xFFFF); // offset of all the possible values
	}

}
