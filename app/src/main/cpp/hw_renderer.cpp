#include "hw_renderer.h"

#include <GLES2/gl2ext.h>

#include <algorithm>
#include <cstring>

#include "log.h"

namespace rh {

namespace {

constexpr const char *kVertexShader = R"(
attribute vec2 aPos;
attribute vec2 aUv;
uniform vec2 uUvScale;
uniform float uFlipY;
varying vec2 vUv;
void main() {
    vec2 uv = aUv;
    if (uFlipY > 0.5) {
        uv.y = 1.0 - uv.y;
    }
    vUv = uv * uUvScale;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
)";

constexpr const char *kFragmentShader = R"(
precision mediump float;
uniform sampler2D uTex;
uniform int uFilter;
uniform int uColorProfile;
uniform vec2 uSrcSize;
varying vec2 vUv;
void main() {
    vec4 c = texture2D(uTex, vUv);
    if (uFilter == 4 && uSrcSize.x > 0.0 && uSrcSize.y > 0.0) {
        // Real-time GLSL EPX / Scale2x diagonal edge-smoothing upscaler
        vec2 texel = 1.0 / uSrcSize;
        vec2 pos = vUv * uSrcSize;
        vec2 sub = fract(pos);
        vec2 center = (floor(pos) + 0.5) * texel;
        vec3 E = texture2D(uTex, center).rgb;
        vec3 B = texture2D(uTex, center + vec2(0.0, -texel.y)).rgb;
        vec3 H = texture2D(uTex, center + vec2(0.0,  texel.y)).rgb;
        vec3 D = texture2D(uTex, center + vec2(-texel.x, 0.0)).rgb;
        vec3 F = texture2D(uTex, center + vec2( texel.x, 0.0)).rgb;
        vec3 outRgb = E;
        bool bhDiff = dot(abs(B - H), vec3(1.0)) > 0.06;
        bool dfDiff = dot(abs(D - F), vec3(1.0)) > 0.06;
        if (bhDiff && dfDiff) {
            if (sub.x < 0.5 && sub.y < 0.5 && dot(abs(D - B), vec3(1.0)) < 0.06) outRgb = D;
            else if (sub.x >= 0.5 && sub.y < 0.5 && dot(abs(B - F), vec3(1.0)) < 0.06) outRgb = F;
            else if (sub.x < 0.5 && sub.y >= 0.5 && dot(abs(D - H), vec3(1.0)) < 0.06) outRgb = D;
            else if (sub.x >= 0.5 && sub.y >= 0.5 && dot(abs(H - F), vec3(1.0)) < 0.06) outRgb = F;
        }
        c.rgb = outRgb;
    } else if (uFilter == 5 && uSrcSize.x > 0.0 && uSrcSize.y > 0.0) {
        // Sharp Bilinear: crisp pixel centers with subtle anti-shimmer edge transitions
        vec2 pos = vUv * uSrcSize;
        vec2 fl = floor(pos);
        vec2 fr = fract(pos);
        vec2 sharpFr = clamp((fr - 0.5) * 3.0 + 0.5, 0.0, 1.0);
        c = texture2D(uTex, (fl + sharpFr) / uSrcSize);
    }
    if (uColorProfile == 3 && uSrcSize.x > 0.0) {
        // 3-tap horizontal NTSC composite blend (smooths Genesis/PS1 dither waterfalls & fog)
        float dx = 1.0 / uSrcSize.x;
        vec3 l = texture2D(uTex, vec2(max(0.0, vUv.x - dx), vUv.y)).rgb;
        vec3 r = texture2D(uTex, vec2(min(1.0, vUv.x + dx), vUv.y)).rgb;
        c.rgb = 0.25 * l + 0.50 * c.rgb + 0.25 * r;
    } else if (uColorProfile == 1) {
        // Authentic GBA AGB-001 reflective TFT LCD color matrix
        vec3 g = pow(max(c.rgb, vec3(0.0)), vec3(1.18));
        c.rgb = vec3(
            0.85 * g.r + 0.12 * g.g + 0.03 * g.b,
            0.08 * g.r + 0.78 * g.g + 0.14 * g.b,
            0.06 * g.r + 0.22 * g.g + 0.72 * g.b
        );
    } else if (uColorProfile == 2) {
        // Authentic GBC reflective LCD color matrix
        vec3 g = pow(max(c.rgb, vec3(0.0)), vec3(1.18));
        c.rgb = vec3(
            0.82 * g.r + 0.14 * g.g + 0.04 * g.b,
            0.10 * g.r + 0.74 * g.g + 0.16 * g.b,
            0.08 * g.r + 0.18 * g.g + 0.74 * g.b
        );
    }
    c.a = 1.0;
    if (uFilter == 2) {
        // Subtle CRT scanlines
        float scan = fract(vUv.y * uSrcSize.y);
        if (scan > 0.66) c.rgb *= 0.68;
    } else if (uFilter == 3) {
        // Handheld LCD pixel grid
        float gx = fract(vUv.x * uSrcSize.x);
        float gy = fract(vUv.y * uSrcSize.y);
        if (gx > 0.80 || gy > 0.80) c.rgb *= 0.78;
    }
    gl_FragColor = c;
}
)";

GLuint compileShader(GLenum type, const char *src) {
    GLuint sh = glCreateShader(type);
    if (!sh) return 0;
    glShaderSource(sh, 1, &src, nullptr);
    glCompileShader(sh);
    GLint ok = 0;
    glGetShaderiv(sh, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char logBuf[512];
        glGetShaderInfoLog(sh, sizeof(logBuf), nullptr, logBuf);
        LOGE("HwRenderer shader compile failed: %s", logBuf);
        glDeleteShader(sh);
        return 0;
    }
    return sh;
}

}  // namespace

HwRenderer::~HwRenderer() {
    destroyContext();
    std::lock_guard<std::mutex> lk(winMutex_);
    if (pendingWindow_) {
        ANativeWindow_release(pendingWindow_);
        pendingWindow_ = nullptr;
    }
}

bool HwRenderer::initContext(unsigned requestedMajor, unsigned requestedMinor, bool needDepth, bool needStencil) {
    if (context_ != EGL_NO_CONTEXT) return true;
    needDepth_ = needDepth;
    needStencil_ = needStencil;

    display_ = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display_ == EGL_NO_DISPLAY) {
        LOGE("eglGetDisplay failed");
        return false;
    }
    EGLint major = 0, minor = 0;
    if (!eglInitialize(display_, &major, &minor)) {
        LOGE("eglInitialize failed");
        display_ = EGL_NO_DISPLAY;
        return false;
    }

    auto chooseCfg = [&](EGLint renderable) -> bool {
        const EGLint attribs[] = {
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT | EGL_PBUFFER_BIT,
            EGL_RENDERABLE_TYPE, renderable,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, needDepth ? 24 : 0,
            EGL_STENCIL_SIZE, needStencil ? 8 : 0,
            EGL_NONE,
        };
        EGLint num = 0;
        if (eglChooseConfig(display_, attribs, &config_, 1, &num) && num > 0) return true;
        // Fallback without strict 24-bit depth on window surface (FBO has its own depth/stencil)
        const EGLint fallbackAttribs[] = {
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT | EGL_PBUFFER_BIT,
            EGL_RENDERABLE_TYPE, renderable,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_NONE,
        };
        return eglChooseConfig(display_, fallbackAttribs, &config_, 1, &num) && num > 0;
    };

    const bool wantGles3 = requestedMajor == 0 || requestedMajor >= 3;
    if (wantGles3 && chooseCfg(EGL_OPENGL_ES3_BIT)) {
        const EGLint ctx32[] = {EGL_CONTEXT_MAJOR_VERSION, 3, EGL_CONTEXT_MINOR_VERSION, 2, EGL_NONE};
        const EGLint ctx31[] = {EGL_CONTEXT_MAJOR_VERSION, 3, EGL_CONTEXT_MINOR_VERSION, 1, EGL_NONE};
        const EGLint ctx30[] = {EGL_CONTEXT_MAJOR_VERSION, 3, EGL_CONTEXT_MINOR_VERSION, 0, EGL_NONE};
        if (requestedMinor >= 2) context_ = eglCreateContext(display_, config_, EGL_NO_CONTEXT, ctx32);
        if (context_ == EGL_NO_CONTEXT && requestedMinor >= 1)
            context_ = eglCreateContext(display_, config_, EGL_NO_CONTEXT, ctx31);
        if (context_ == EGL_NO_CONTEXT) context_ = eglCreateContext(display_, config_, EGL_NO_CONTEXT, ctx30);
        if (context_ != EGL_NO_CONTEXT) isGles3_ = true;
    }
    if (context_ == EGL_NO_CONTEXT) {
        if (!chooseCfg(EGL_OPENGL_ES2_BIT)) {
            LOGE("eglChooseConfig failed for GLES3 and GLES2");
            destroyContext();
            return false;
        }
        const EGLint ctx20[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
        context_ = eglCreateContext(display_, config_, EGL_NO_CONTEXT, ctx20);
        isGles3_ = false;
    }
    if (context_ == EGL_NO_CONTEXT) {
        LOGE("eglCreateContext failed (err=0x%x)", eglGetError());
        destroyContext();
        return false;
    }

    const EGLint pbAttribs[] = {EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE};
    pbuffer_ = eglCreatePbufferSurface(display_, config_, pbAttribs);
    if (pbuffer_ == EGL_NO_SURFACE) {
        LOGE("eglCreatePbufferSurface failed");
        destroyContext();
        return false;
    }

    if (!eglMakeCurrent(display_, pbuffer_, pbuffer_, context_)) {
        LOGE("eglMakeCurrent (pbuffer) failed");
        destroyContext();
        return false;
    }

    initQuadProgram();
    ensureFbo(640, 480);
    applyPendingWindow();

    LOGI("HwRenderer initialized: %s | %s",
         reinterpret_cast<const char *>(glGetString(GL_VERSION)),
         reinterpret_cast<const char *>(glGetString(GL_RENDERER)));
    return true;
}

void HwRenderer::destroyContext() {
    if (display_ != EGL_NO_DISPLAY) {
        if (context_ != EGL_NO_CONTEXT) {
            EGLSurface s = winSurface_ != EGL_NO_SURFACE ? winSurface_ : pbuffer_;
            if (s != EGL_NO_SURFACE) eglMakeCurrent(display_, s, s, context_);
            destroyFbo();
            if (program_) {
                glDeleteProgram(program_);
                program_ = 0;
            }
        }
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        destroyWindowSurface();
        if (pbuffer_ != EGL_NO_SURFACE) {
            eglDestroySurface(display_, pbuffer_);
            pbuffer_ = EGL_NO_SURFACE;
        }
        if (context_ != EGL_NO_CONTEXT) {
            eglDestroyContext(display_, context_);
            context_ = EGL_NO_CONTEXT;
        }
        eglTerminate(display_);
        display_ = EGL_NO_DISPLAY;
    }
    config_ = nullptr;
}

void HwRenderer::setWindow(ANativeWindow *window) {
    std::lock_guard<std::mutex> lk(winMutex_);
    if (pendingWindow_) ANativeWindow_release(pendingWindow_);
    pendingWindow_ = window;
    windowDirty_ = true;
}

void HwRenderer::applyPendingWindow() {
    ANativeWindow *next = nullptr;
    {
        std::lock_guard<std::mutex> lk(winMutex_);
        if (!windowDirty_) return;
        next = pendingWindow_;
        pendingWindow_ = nullptr;
        windowDirty_ = false;
    }
    if (next == activeWindow_) {
        if (next) ANativeWindow_release(next);
        return;
    }
    // Switch context to pbuffer while swapping window surfaces so GL state stays 100% intact.
    if (display_ != EGL_NO_DISPLAY && context_ != EGL_NO_CONTEXT && pbuffer_ != EGL_NO_SURFACE) {
        eglMakeCurrent(display_, pbuffer_, pbuffer_, context_);
    }
    destroyWindowSurface();
    activeWindow_ = next;
    if (activeWindow_ && display_ != EGL_NO_DISPLAY && config_) {
        winSurface_ = eglCreateWindowSurface(display_, config_, activeWindow_, nullptr);
        if (winSurface_ == EGL_NO_SURFACE) {
            LOGE("eglCreateWindowSurface failed (err=0x%x)", eglGetError());
        } else {
            eglMakeCurrent(display_, winSurface_, winSurface_, context_);
            eglSwapInterval(display_, 0);  // Frontend loop paces frame timing accurately
        }
    }
}

void HwRenderer::destroyWindowSurface() {
    if (winSurface_ != EGL_NO_SURFACE && display_ != EGL_NO_DISPLAY) {
        eglDestroySurface(display_, winSurface_);
        winSurface_ = EGL_NO_SURFACE;
    }
    if (activeWindow_) {
        ANativeWindow_release(activeWindow_);
        activeWindow_ = nullptr;
    }
}

void HwRenderer::destroyFbo() {
    if (depthRb_) {
        glDeleteRenderbuffers(1, &depthRb_);
        depthRb_ = 0;
    }
    if (colorTex_) {
        glDeleteTextures(1, &colorTex_);
        colorTex_ = 0;
    }
    if (fbo_) {
        glDeleteFramebuffers(1, &fbo_);
        fbo_ = 0;
    }
    fboW_ = fboH_ = 0;
}

bool HwRenderer::ensureFbo(unsigned width, unsigned height) {
    unsigned targetW = std::clamp(width, 256u, 2048u);
    unsigned targetH = std::clamp(height, 256u, 2048u);
    if (fbo_ && targetW <= fboW_ && targetH <= fboH_) return true;

    // Grow monotonically (up to 2048) to avoid mid-frame texture churn when cores switch resolutions
    targetW = std::max(targetW, fboW_);
    targetH = std::max(targetH, fboH_);
    destroyFbo();

    glGenFramebuffers(1, &fbo_);
    glBindFramebuffer(GL_FRAMEBUFFER, fbo_);

    glGenTextures(1, &colorTex_);
    glBindTexture(GL_TEXTURE_2D, colorTex_);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, static_cast<GLsizei>(targetW), static_cast<GLsizei>(targetH),
                 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex_, 0);

    if (needDepth_ || needStencil_) {
        glGenRenderbuffers(1, &depthRb_);
        glBindRenderbuffer(GL_RENDERBUFFER, depthRb_);
        if (isGles3_ || needStencil_) {
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8,
                                  static_cast<GLsizei>(targetW), static_cast<GLsizei>(targetH));
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRb_);
            if (needStencil_) {
                glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthRb_);
            }
        } else {
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16,
                                  static_cast<GLsizei>(targetW), static_cast<GLsizei>(targetH));
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRb_);
        }
    }

    GLenum status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status != GL_FRAMEBUFFER_COMPLETE) {
        LOGE("HwRenderer FBO incomplete: 0x%x (%ux%u)", status, targetW, targetH);
        return false;
    }
    fboW_ = targetW;
    fboH_ = targetH;
    return true;
}

bool HwRenderer::initQuadProgram() {
    GLuint vs = compileShader(GL_VERTEX_SHADER, kVertexShader);
    GLuint fs = compileShader(GL_FRAGMENT_SHADER, kFragmentShader);
    if (!vs || !fs) {
        if (vs) glDeleteShader(vs);
        if (fs) glDeleteShader(fs);
        return false;
    }
    program_ = glCreateProgram();
    glAttachShader(program_, vs);
    glAttachShader(program_, fs);
    glLinkProgram(program_);
    glDeleteShader(vs);
    glDeleteShader(fs);

    GLint ok = 0;
    glGetProgramiv(program_, GL_LINK_STATUS, &ok);
    if (!ok) {
        LOGE("HwRenderer program link failed");
        glDeleteProgram(program_);
        program_ = 0;
        return false;
    }
    locPos_ = glGetAttribLocation(program_, "aPos");
    locUv_ = glGetAttribLocation(program_, "aUv");
    locTex_ = glGetUniformLocation(program_, "uTex");
    locFilter_ = glGetUniformLocation(program_, "uFilter");
    locColorProfile_ = glGetUniformLocation(program_, "uColorProfile");
    locSrcSize_ = glGetUniformLocation(program_, "uSrcSize");
    locUvScale_ = glGetUniformLocation(program_, "uUvScale");
    locFlipY_ = glGetUniformLocation(program_, "uFlipY");
    return true;
}

void HwRenderer::beginFrame(unsigned maxWidth, unsigned maxHeight) {
    if (context_ == EGL_NO_CONTEXT) return;
    applyPendingWindow();
    EGLSurface surf = winSurface_ != EGL_NO_SURFACE ? winSurface_ : pbuffer_;
    if (eglGetCurrentContext() != context_ || eglGetCurrentSurface(EGL_DRAW) != surf) {
        eglMakeCurrent(display_, surf, surf, context_);
    }
    if (maxWidth > 0 && maxHeight > 0) {
        ensureFbo(maxWidth, maxHeight);
    }
    glBindFramebuffer(GL_FRAMEBUFFER, fbo_);
}

void HwRenderer::presentFrame(unsigned width, unsigned height, bool bottomLeftOrigin) {
    if (context_ == EGL_NO_CONTEXT || width == 0 || height == 0) return;

    {
        std::lock_guard<std::mutex> lk(snapMutex_);
        lastW_ = width;
        lastH_ = height;
    }

    if (grabRequested_.exchange(false)) {
        captureSnapshotLocked(width, height, bottomLeftOrigin);
    }

    if (winSurface_ == EGL_NO_SURFACE || !program_ || !colorTex_) return;

    // Save minimal GL state that cores expect to remain intact across videoCb()
    GLint prevProg = 0, prevFbo = 0, prevTex = 0, prevActiveTex = 0, prevArrayBuf = 0, prevVao = 0;
    GLint prevViewport[4] = {0, 0, 0, 0};
    GLboolean prevBlend = glIsEnabled(GL_BLEND);
    GLboolean prevDepth = glIsEnabled(GL_DEPTH_TEST);
    GLboolean prevScissor = glIsEnabled(GL_SCISSOR_TEST);
    GLboolean prevCull = glIsEnabled(GL_CULL_FACE);
    glGetIntegerv(GL_CURRENT_PROGRAM, &prevProg);
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, &prevFbo);
    glGetIntegerv(GL_ACTIVE_TEXTURE, &prevActiveTex);
    glGetIntegerv(GL_TEXTURE_BINDING_2D, &prevTex);
    glGetIntegerv(GL_ARRAY_BUFFER_BINDING, &prevArrayBuf);
    if (isGles3_) glGetIntegerv(GL_VERTEX_ARRAY_BINDING, &prevVao);
    glGetIntegerv(GL_VIEWPORT, prevViewport);

    EGLint surfW = 0, surfH = 0;
    eglQuerySurface(display_, winSurface_, EGL_WIDTH, &surfW);
    eglQuerySurface(display_, winSurface_, EGL_HEIGHT, &surfH);

    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    if (isGles3_) glBindVertexArray(0);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glDisable(GL_BLEND);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_CULL_FACE);

    glViewport(0, 0, surfW, surfH);
    glClearColor(0.f, 0.f, 0.f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);

    glUseProgram(program_);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, colorTex_);
    const int flt = filter_.load();
    const GLint glFilter = (flt == 0) ? GL_NEAREST : GL_LINEAR;
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, glFilter);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, glFilter);

    const float uScale = fboW_ > 0 ? std::min(1.0f, static_cast<float>(width) / static_cast<float>(fboW_)) : 1.0f;
    const float vScale = fboH_ > 0 ? std::min(1.0f, static_cast<float>(height) / static_cast<float>(fboH_)) : 1.0f;
    glUniform1i(locTex_, 0);
    glUniform1i(locFilter_, flt);
    glUniform1i(locColorProfile_, colorProfile_.load());
    glUniform2f(locSrcSize_, static_cast<float>(width), static_cast<float>(height));
    glUniform2f(locUvScale_, uScale, vScale);
    glUniform1f(locFlipY_, bottomLeftOrigin ? 0.0f : 1.0f);

    static const GLfloat kQuadPos[] = {
        -1.f, -1.f,
         1.f, -1.f,
        -1.f,  1.f,
         1.f,  1.f,
    };
    static const GLfloat kQuadUv[] = {
        0.f, 0.f,
        1.f, 0.f,
        0.f, 1.f,
        1.f, 1.f,
    };
    glEnableVertexAttribArray(static_cast<GLuint>(locPos_));
    glEnableVertexAttribArray(static_cast<GLuint>(locUv_));
    glVertexAttribPointer(static_cast<GLuint>(locPos_), 2, GL_FLOAT, GL_FALSE, 0, kQuadPos);
    glVertexAttribPointer(static_cast<GLuint>(locUv_), 2, GL_FLOAT, GL_FALSE, 0, kQuadUv);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glDisableVertexAttribArray(static_cast<GLuint>(locPos_));
    glDisableVertexAttribArray(static_cast<GLuint>(locUv_));

    eglSwapBuffers(display_, winSurface_);

    // Restore core GL state
    if (isGles3_) glBindVertexArray(static_cast<GLuint>(prevVao));
    glBindBuffer(GL_ARRAY_BUFFER, static_cast<GLuint>(prevArrayBuf));
    glActiveTexture(static_cast<GLenum>(prevActiveTex));
    glBindTexture(GL_TEXTURE_2D, static_cast<GLuint>(prevTex));
    glUseProgram(static_cast<GLuint>(prevProg));
    glBindFramebuffer(GL_FRAMEBUFFER, static_cast<GLuint>(prevFbo));
    glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
    if (prevBlend) glEnable(GL_BLEND);
    if (prevDepth) glEnable(GL_DEPTH_TEST);
    if (prevScissor) glEnable(GL_SCISSOR_TEST);
    if (prevCull) glEnable(GL_CULL_FACE);
}

void HwRenderer::presentBlack() {
    if (display_ == EGL_NO_DISPLAY || winSurface_ == EGL_NO_SURFACE || context_ == EGL_NO_CONTEXT) return;
    GLint prevFbo = 0;
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, &prevFbo);
    GLboolean prevScissor = glIsEnabled(GL_SCISSOR_TEST);
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glDisable(GL_SCISSOR_TEST);
    glClearColor(0.f, 0.f, 0.f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);
    eglSwapBuffers(display_, winSurface_);
    glBindFramebuffer(GL_FRAMEBUFFER, static_cast<GLuint>(prevFbo));
    if (prevScissor) glEnable(GL_SCISSOR_TEST);
}

void HwRenderer::captureSnapshotLocked(unsigned width, unsigned height, bool bottomLeftOrigin) {
    const unsigned cw = std::min(width, fboW_);
    const unsigned ch = std::min(height, fboH_);
    if (cw == 0 || ch == 0) return;

    GLint prevFbo = 0;
    glGetIntegerv(GL_FRAMEBUFFER_BINDING, &prevFbo);
    glBindFramebuffer(GL_FRAMEBUFFER, fbo_);
    std::vector<uint8_t> rgba(static_cast<size_t>(cw) * ch * 4);
    glPixelStorei(GL_PACK_ALIGNMENT, 1);
    glReadPixels(0, 0, static_cast<GLsizei>(cw), static_cast<GLsizei>(ch), GL_RGBA, GL_UNSIGNED_BYTE, rgba.data());
    glBindFramebuffer(GL_FRAMEBUFFER, static_cast<GLuint>(prevFbo));

    std::lock_guard<std::mutex> lk(snapMutex_);
    lastArgb_.resize(static_cast<size_t>(cw) * ch);
    for (unsigned y = 0; y < ch; y++) {
        unsigned srcY = bottomLeftOrigin ? (ch - 1 - y) : y;
        const uint8_t *row = rgba.data() + static_cast<size_t>(srcY) * cw * 4;
        uint32_t *dst = lastArgb_.data() + static_cast<size_t>(y) * cw;
        for (unsigned x = 0; x < cw; x++) {
            uint32_t r = row[x * 4 + 0];
            uint32_t g = row[x * 4 + 1];
            uint32_t b = row[x * 4 + 2];
            dst[x] = 0xFF000000u | (r << 16) | (g << 8) | b;
        }
    }
    lastW_ = cw;
    lastH_ = ch;
}

void HwRenderer::frameSize(unsigned &width, unsigned &height) {
    std::lock_guard<std::mutex> lk(snapMutex_);
    width = lastW_;
    height = lastH_;
}

bool HwRenderer::grabArgb(std::vector<uint32_t> &out, unsigned &width, unsigned &height) {
    grabRequested_.store(true);
    std::lock_guard<std::mutex> lk(snapMutex_);
    if (lastArgb_.empty() || lastW_ == 0 || lastH_ == 0) return false;
    width = lastW_;
    height = lastH_;
    out = lastArgb_;
    return true;
}

}  // namespace rh
