#version 330 core

out vec4 color;

uniform sampler2D u_Texture;
uniform sampler2D u_MaskTexture;
uniform vec2 u_Direction;
uniform float u_Intensity;
uniform int u_Radius;
uniform float u_Softness;
uniform bool u_CutoutSource;

in vec2 v_TexCoord;
in vec2 v_OneTexel;

const int MAX_RADIUS = 16;

void main() {
    vec3 colorSum = vec3(0.0);
    float alphaSum = 0.0;
    float totalWeight = 0.0;
    float radius = max(float(u_Radius), 1.0);

    for (int i = -MAX_RADIUS; i <= MAX_RADIUS; ++i) {
        if (abs(i) > u_Radius) {
            continue;
        }

        float distance = float(i) / radius;
        float weight = exp(-distance * distance * u_Softness);
        vec4 sampleColor = texture(u_Texture, v_TexCoord + v_OneTexel * float(i) * u_Direction);
        colorSum += sampleColor.rgb * sampleColor.a * weight;
        alphaSum += sampleColor.a * weight;
        totalWeight += weight;
    }

    if (alphaSum <= 0.0001) {
        discard;
    }

    vec3 glowColor = colorSum / alphaSum;
    float glowAlpha = clamp(alphaSum / totalWeight * u_Intensity, 0.0, 1.0);
    if (u_CutoutSource) {
        float sourceAlpha = texture(u_MaskTexture, v_TexCoord).a;
        glowAlpha *= 1.0 - smoothstep(0.01, 0.35, sourceAlpha);
    }

    if (glowAlpha <= 0.001) {
        discard;
    }

    color = vec4(glowColor, glowAlpha);
}
