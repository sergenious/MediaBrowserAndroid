precision mediump float;
uniform sampler2D texture0;
uniform float opacity;
varying vec2 currTexCoord0;

void main() {
	gl_FragColor = texture2D(texture0, currTexCoord0);
	gl_FragColor.a *= opacity;
}
