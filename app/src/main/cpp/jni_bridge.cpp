#include <android/native_window_jni.h>
#include <jni.h>

#include <string>
#include <vector>

#include "frontend.h"

using rh::Frontend;

namespace {

std::string str(JNIEnv *env, jstring s) {
    if (!s) return {};
    const char *c = env->GetStringUTFChars(s, nullptr);
    std::string out(c ? c : "");
    if (c) env->ReleaseStringUTFChars(s, c);
    return out;
}

}  // namespace

#define JNI_FN(name) Java_com_retrohero_app_emulator_NativeBridge_##name

extern "C" {

JNIEXPORT jboolean JNICALL JNI_FN(loadCore)(JNIEnv *env, jobject, jstring core, jstring sys,
                                            jstring save) {
    return Frontend::get().loadCore(str(env, core), str(env, sys), str(env, save));
}

JNIEXPORT jboolean JNICALL JNI_FN(loadGame)(JNIEnv *env, jobject, jstring rom, jstring sram) {
    return Frontend::get().loadGame(str(env, rom), str(env, sram));
}

JNIEXPORT jboolean JNICALL JNI_FN(loadGameMemory)(JNIEnv *env, jobject, jstring virtualPath,
                                                  jbyteArray romBytes, jstring sram,
                                                  jstring scratchDir) {
    if (!romBytes) return JNI_FALSE;
    jsize len = env->GetArrayLength(romBytes);
    if (len <= 0) return JNI_FALSE;
    jbyte *ptr = env->GetByteArrayElements(romBytes, nullptr);
    if (!ptr) return JNI_FALSE;
    bool ok = Frontend::get().loadGameFromMemory(
        str(env, virtualPath), reinterpret_cast<const uint8_t *>(ptr), static_cast<size_t>(len),
        str(env, sram), str(env, scratchDir));
    env->ReleaseByteArrayElements(romBytes, ptr, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL JNI_FN(start)(JNIEnv *, jobject) { Frontend::get().start(); }

JNIEXPORT void JNICALL JNI_FN(setPaused)(JNIEnv *, jobject, jboolean paused) {
    Frontend::get().setPaused(paused);
}

JNIEXPORT void JNICALL JNI_FN(setFastForward)(JNIEnv *, jobject, jboolean on, jfloat speed) {
    Frontend::get().setFastForward(on, speed);
}

JNIEXPORT void JNICALL JNI_FN(setSlowMotion)(JNIEnv *, jobject, jboolean on) {
    Frontend::get().setSlowMotion(on);
}

JNIEXPORT void JNICALL JNI_FN(setRunAhead)(JNIEnv *, jobject, jint frames) {
    Frontend::get().setRunAhead(frames);
}

JNIEXPORT void JNICALL JNI_FN(onVsync)(JNIEnv *, jobject, jlong frameTimeNanos, jfloat displayHz) {
    Frontend::get().onVsync(static_cast<int64_t>(frameTimeNanos), static_cast<float>(displayHz));
}

JNIEXPORT void JNICALL JNI_FN(setVsyncConfig)(JNIEnv *, jobject, jboolean vsyncLock, jboolean bfi120Hz) {
    Frontend::get().setVsyncConfig(vsyncLock, bfi120Hz);
}

JNIEXPORT jfloat JNICALL JNI_FN(getCoreFps)(JNIEnv *, jobject) {
    return Frontend::get().coreFps();
}

JNIEXPORT jboolean JNICALL JNI_FN(isVsyncLocked)(JNIEnv *, jobject) {
    return Frontend::get().isVsyncLocked();
}

JNIEXPORT jboolean JNICALL JNI_FN(isBfiActive)(JNIEnv *, jobject) {
    return Frontend::get().isBfiActive();
}

JNIEXPORT jfloatArray JNICALL JNI_FN(getPerfStats)(JNIEnv *env, jobject) {
    float fps = 0.0f, ms = 0.0f;
    int ra = 0;
    Frontend::get().perfStats(fps, ms, ra);
    jfloatArray arr = env->NewFloatArray(3);
    jfloat vals[3] = {fps, ms, static_cast<jfloat>(ra)};
    env->SetFloatArrayRegion(arr, 0, 3, vals);
    return arr;
}

JNIEXPORT void JNICALL JNI_FN(reset)(JNIEnv *, jobject) { Frontend::get().reset(); }

JNIEXPORT jboolean JNICALL JNI_FN(saveState)(JNIEnv *env, jobject, jstring path) {
    return Frontend::get().saveState(str(env, path));
}

JNIEXPORT jboolean JNICALL JNI_FN(loadState)(JNIEnv *env, jobject, jstring path) {
    return Frontend::get().loadState(str(env, path));
}

JNIEXPORT jboolean JNICALL JNI_FN(saveSram)(JNIEnv *env, jobject, jstring path) {
    return Frontend::get().saveSram(str(env, path));
}

JNIEXPORT jbyteArray JNICALL JNI_FN(readMemory)(JNIEnv *env, jobject, jint memoryType, jint maxBytes) {
    std::vector<uint8_t> buf;
    size_t cap = maxBytes > 0 ? static_cast<size_t>(maxBytes) : 0;
    if (!Frontend::get().readMemory(static_cast<unsigned>(memoryType), buf, cap)) return nullptr;
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(buf.size()));
    if (!arr) return nullptr;
    env->SetByteArrayRegion(arr, 0, static_cast<jsize>(buf.size()),
                            reinterpret_cast<const jbyte *>(buf.data()));
    return arr;
}

JNIEXPORT void JNICALL JNI_FN(shutdown)(JNIEnv *env, jobject, jstring sram) {
    Frontend::get().shutdown(str(env, sram));
}

JNIEXPORT void JNICALL JNI_FN(setInitialDisc)(JNIEnv *, jobject, jint index) {
    if (index >= 0) Frontend::get().setInitialDisc(static_cast<unsigned>(index));
}

JNIEXPORT jint JNICALL JNI_FN(getDiscCount)(JNIEnv *, jobject) {
    return Frontend::get().discCount();
}

JNIEXPORT jint JNICALL JNI_FN(getCurrentDisc)(JNIEnv *, jobject) {
    return Frontend::get().currentDiscIndex();
}

JNIEXPORT jobjectArray JNICALL JNI_FN(getDiscLabels)(JNIEnv *env, jobject) {
    auto labels = Frontend::get().discLabels();
    jclass strClass = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray(static_cast<jsize>(labels.size()), strClass, nullptr);
    for (size_t i = 0; i < labels.size(); i++) {
        jstring js = env->NewStringUTF(labels[i].c_str());
        env->SetObjectArrayElement(arr, static_cast<jsize>(i), js);
        env->DeleteLocalRef(js);
    }
    return arr;
}

JNIEXPORT jboolean JNICALL JNI_FN(swapDisc)(JNIEnv *, jobject, jint index) {
    return index >= 0 && Frontend::get().swapDisc(static_cast<unsigned>(index));
}

JNIEXPORT void JNICALL JNI_FN(setSurface)(JNIEnv *env, jobject, jobject surface) {
    ANativeWindow *win = surface ? ANativeWindow_fromSurface(env, surface) : nullptr;
    Frontend::get().setWindow(win);
}

JNIEXPORT void JNICALL JNI_FN(setFilter)(JNIEnv *, jobject, jint filter) {
    Frontend::get().setFilter(filter);
}

JNIEXPORT void JNICALL JNI_FN(setColorProfile)(JNIEnv *, jobject, jint profile) {
    Frontend::get().setColorProfile(profile);
}

JNIEXPORT void JNICALL JNI_FN(setUpscaleFactor)(JNIEnv *, jobject, jint factor) {
    Frontend::get().setUpscaleFactor(factor);
}

JNIEXPORT void JNICALL JNI_FN(setAmbilightEnabled)(JNIEnv *, jobject, jboolean enabled) {
    Frontend::get().setAmbilightEnabled(enabled == JNI_TRUE);
}

JNIEXPORT void JNICALL JNI_FN(pollAmbilight)(JNIEnv *env, jobject, jintArray outArr) {
    if (!outArr || env->GetArrayLength(outArr) < 5) return;
    uint32_t raw[5] = {0};
    Frontend::get().pollAmbilight(raw);
    jint vals[5] = {
        static_cast<jint>(raw[0]),
        static_cast<jint>(raw[1]),
        static_cast<jint>(raw[2]),
        static_cast<jint>(raw[3]),
        static_cast<jint>(raw[4]),
    };
    env->SetIntArrayRegion(outArr, 0, 5, vals);
}

JNIEXPORT void JNICALL JNI_FN(setRewindEnabled)(JNIEnv *, jobject, jboolean on) {
    Frontend::get().setRewindEnabled(on);
}

JNIEXPORT void JNICALL JNI_FN(setRewinding)(JNIEnv *, jobject, jboolean on) {
    Frontend::get().setRewinding(on);
}

JNIEXPORT jboolean JNICALL JNI_FN(supportsCheats)(JNIEnv *, jobject) {
    return Frontend::get().supportsCheats();
}

JNIEXPORT jboolean JNICALL JNI_FN(setCheats)(JNIEnv *env, jobject, jobjectArray codes) {
    std::vector<std::string> out;
    jsize n = codes ? env->GetArrayLength(codes) : 0;
    for (jsize i = 0; i < n; i++) {
        auto js = static_cast<jstring>(env->GetObjectArrayElement(codes, i));
        out.push_back(str(env, js));
        env->DeleteLocalRef(js);
    }
    return Frontend::get().setCheats(out);
}

JNIEXPORT jintArray JNICALL JNI_FN(getFrameSize)(JNIEnv *env, jobject) {
    unsigned w = 0, h = 0;
    Frontend::get().frameSize(w, h);
    jintArray arr = env->NewIntArray(2);
    jint dims[2] = {static_cast<jint>(w), static_cast<jint>(h)};
    env->SetIntArrayRegion(arr, 0, 2, dims);
    return arr;
}

// Returns [width, height, ARGB pixels...] or null when no frame has been drawn yet.
JNIEXPORT jintArray JNICALL JNI_FN(grabFrame)(JNIEnv *env, jobject) {
    std::vector<uint32_t> px;
    unsigned w = 0, h = 0;
    if (!Frontend::get().grabArgb(px, w, h)) return nullptr;
    jintArray arr = env->NewIntArray(static_cast<jsize>(px.size() + 2));
    if (!arr) return nullptr;
    jint dims[2] = {static_cast<jint>(w), static_cast<jint>(h)};
    env->SetIntArrayRegion(arr, 0, 2, dims);
    env->SetIntArrayRegion(arr, 2, static_cast<jsize>(px.size()), reinterpret_cast<const jint *>(px.data()));
    return arr;
}

JNIEXPORT void JNICALL JNI_FN(setButton)(JNIEnv *, jobject, jint port, jint id, jboolean pressed) {
    Frontend::get().setButton(port, id, pressed);
}

JNIEXPORT jint JNICALL JNI_FN(getButtonsMask)(JNIEnv *, jobject, jint port) {
    if (port < 0) return 0;
    return static_cast<jint>(Frontend::get().getButtonsMask(static_cast<unsigned>(port)));
}

JNIEXPORT jboolean JNICALL JNI_FN(netplayStart)(JNIEnv *, jobject, jint localPort, jint inputDelayFrames) {
    return Frontend::get().netplayStart(localPort, inputDelayFrames) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL JNI_FN(netplayStop)(JNIEnv *, jobject) {
    Frontend::get().netplayStop();
}

JNIEXPORT void JNICALL JNI_FN(netplayPushRemoteInput)(JNIEnv *, jobject, jint frame, jint buttonsMask) {
    if (frame < 0) return;
    Frontend::get().netplayPushRemoteInput(static_cast<uint32_t>(frame), static_cast<uint32_t>(buttonsMask));
}

JNIEXPORT jintArray JNICALL JNI_FN(netplayPollTelemetry)(JNIEnv *env, jobject) {
    rh::RollbackTelemetry t = Frontend::get().netplayTelemetry();
    jintArray arr = env->NewIntArray(7);
    if (!arr) return nullptr;
    jint vals[7] = {
        t.active ? 1 : 0,
        static_cast<jint>(t.currentFrame),
        static_cast<jint>(t.lastLocalFrame),
        static_cast<jint>(t.lastLocalMask),
        static_cast<jint>(t.rollbackCount),
        static_cast<jint>(t.maxRollbackDepth),
        static_cast<jint>(t.stateChecksum),
    };
    env->SetIntArrayRegion(arr, 0, 7, vals);
    return arr;
}

JNIEXPORT void JNICALL JNI_FN(setAnalog)(JNIEnv *, jobject, jint port, jint index, jint axis,
                                         jfloat value) {
    float v = value < -1.f ? -1.f : (value > 1.f ? 1.f : value);
    Frontend::get().setAnalog(port, index, axis, static_cast<int16_t>(v * 32767.f));
}

JNIEXPORT void JNICALL JNI_FN(setPointer)(JNIEnv *, jobject, jfloat x, jfloat y, jboolean down) {
    auto clamp = [](float v) { return v < -1.f ? -1.f : (v > 1.f ? 1.f : v); };
    Frontend::get().setPointer(static_cast<int16_t>(clamp(x) * 32767.f),
                               static_cast<int16_t>(clamp(y) * 32767.f), down);
}

JNIEXPORT void JNICALL JNI_FN(setOption)(JNIEnv *env, jobject, jstring key, jstring value) {
    Frontend::get().setOptionOverride(str(env, key), str(env, value));
}

// Each option is encoded as "key\x1Fdescription\x1Fcurrent\x1Fv1\x1Ev2\x1E..."
JNIEXPORT jobjectArray JNICALL JNI_FN(getOptions)(JNIEnv *env, jobject) {
    auto opts = Frontend::get().options();
    jclass strClass = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray(static_cast<jsize>(opts.size()), strClass, nullptr);
    for (size_t i = 0; i < opts.size(); i++) {
        std::string enc = opts[i].key + '\x1F' + opts[i].description + '\x1F' + opts[i].current + '\x1F';
        for (size_t j = 0; j < opts[i].values.size(); j++) {
            if (j) enc += '\x1E';
            enc += opts[i].values[j];
        }
        jstring js = env->NewStringUTF(enc.c_str());
        env->SetObjectArrayElement(arr, static_cast<jsize>(i), js);
        env->DeleteLocalRef(js);
    }
    return arr;
}

JNIEXPORT jfloat JNICALL JNI_FN(getAspectRatio)(JNIEnv *, jobject) {
    return Frontend::get().aspectRatio();
}

JNIEXPORT jfloat JNICALL JNI_FN(consumeAudioHaptic)(JNIEnv *, jobject) {
    return Frontend::get().audio().consumeHapticEnergy();
}

JNIEXPORT jstring JNICALL JNI_FN(getLastError)(JNIEnv *env, jobject) {
    return env->NewStringUTF(Frontend::get().lastError().c_str());
}

}  // extern "C"
