#version 330 core

in vec2 fTexPos;

out vec4 outColor;

uniform sampler2D ourTexture; // FIXME using default texture unit should assign it

void main() {
    //outColor = vec4( 0.9, 0.8, 1.0, 0.5 );
    outColor = texture(ourTexture, fTexPos);
}
