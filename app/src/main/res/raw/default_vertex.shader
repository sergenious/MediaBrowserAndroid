uniform mat4 modelViewMatrix;
uniform mat4 projMatrix;
uniform mat4 texture0Matrix;
attribute vec3 position;
attribute vec2 texCoord0;
varying vec2 currTexCoord0;

void main() {
  	gl_Position = projMatrix * modelViewMatrix * vec4(position, 1.0);
  	currTexCoord0 = (texture0Matrix * vec4(texCoord0, 1.0, 1.0)).xy;
}