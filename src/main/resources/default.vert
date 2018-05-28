#version 330 core

layout (location = 0) in vec2 position;
layout (location = 1) in vec2 vTexPos;

out vec2 fTexPos;


void main() {
    gl_Position = vec4( position, 0.0, 1.0 );
    fTexPos = vec2(vTexPos.x, vTexPos.y);
}