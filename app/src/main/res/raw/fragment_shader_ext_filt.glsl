#extension GL_OES_EGL_image_external : require
#define KERNEL_SIZE 9
precision highp float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform float uKernel[KERNEL_SIZE];
uniform vec2 uTexOffset[KERNEL_SIZE];
uniform float uColorAdjust;

void main() {
    int i = 0;
    vec4 sum = vec4(0.0);
    if (vTextureCoord.x < vTextureCoord.y - 0.005) {
        for (i = 0; i < KERNEL_SIZE; i++) {
            vec4 texc = texture2D(sTexture, vTextureCoord + uTexOffset[i]);
            sum += texc * uKernel[i];
        }
    sum += uColorAdjust;
    } else if (vTextureCoord.x > vTextureCoord.y + 0.005) {
        sum = texture2D(sTexture, vTextureCoord);
    } else {
        sum.r = 1.0;
    }
    gl_FragColor = sum;
}