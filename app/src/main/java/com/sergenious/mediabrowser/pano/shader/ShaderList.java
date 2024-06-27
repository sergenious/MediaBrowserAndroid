package com.sergenious.mediabrowser.pano.shader;

import android.content.Context;
import android.opengl.GLES20;

import com.sergenious.mediabrowser.R;

import java.util.HashMap;
import java.util.Map;

/** Shader list used as a cache of all loaded shaders. */
public class ShaderList {
	private final Map<String, Shader> shaderList = new HashMap<>();
	private Shader currentShader = null;

	public ShaderList(Context ctx) {
		initAllShaders(ctx);
	}

	public void initAllShaders(Context ctx) {
		clear();
		shaderList.put("default", new Shader(ctx, "default", R.raw.default_vertex, R.raw.default_fragment));
	}

	public void useNoShader() {
		currentShader = null;
		Shader.useNoProgram();
	}

	public Shader getShader(String name) {
		return shaderList.get(name);
	}

	public Shader useShader(String name) {
		if ((name == null) && (currentShader == null)) {
			return null; // no shader already used
		}
		if ((currentShader != null) && (name != null) && name.equals(currentShader.name)) {
			return currentShader; // the shader already used
		}
		if (name != null) {
			currentShader = shaderList.get(name);
			if (currentShader != null) {
				currentShader.useProgram();
			}
		}
		else {
			currentShader = null;
		}
		return currentShader;
	}

	public void clear() {
		for (Map.Entry<String, Shader> v: shaderList.entrySet()) {
			v.getValue().deleteProgram();
		}
		GLES20.glUseProgram(0);
		shaderList.clear();
		currentShader = null;
	}
}
