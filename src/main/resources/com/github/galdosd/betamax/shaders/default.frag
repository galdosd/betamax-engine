#version 330 core

in vec2 fTexPos;

out vec4 outColor;

uniform sampler2D ourTexture; // FIXME using default texture unit should assign it

void main() {
    outColor = texture(ourTexture, fTexPos);
}
