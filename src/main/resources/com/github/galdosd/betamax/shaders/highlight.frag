#version 330 core

in vec2 fTexPos;

out vec4 outColor;

uniform sampler2D ourTexture; // FIXME using default texture unit should assign it

void main() {
    vec4 textureColor = texture(ourTexture, fTexPos);
    float lum = (textureColor.x + textureColor.y + textureColor.z) / 3.0;

    vec4 reddishColor = vec4(lum * 1.2, 0.0, 0.0, 0.7 * textureColor.a);

    outColor = reddishColor * 0.7 + textureColor * 0.3;
   //texture(ourTexture, fTexPos) * vec4( 2.0, 0.5, 0.5, 0.5);
}
