#include <android/native_window_jni.h>
#include <jni.h>

#include <string>

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

JNIEXPORT void JNICALL JNI_FN(start)(JNIEnv *, jobject) { Frontend::get().start(); }

JNIEXPORT void JNICALL JNI_FN(setPaused)(JNIEnv *, jobject, jboolean paused) {
    Frontend::get().setPaused(paused);
}

JNIEXPORT void JNICALL JNI_FN(setFastForward)(JNIEnv *, jobject, jboolean on, jfloat speed) {
    Frontend::get().setFastForward(on, speed);
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

JNIEXPORT void JNICALL JNI_FN(shutdown)(JNIEnv *env, jobject, jstring sram) {
    Frontend::get().shutdown(str(env, sram));
}

JNIEXPORT void JNICALL JNI_FN(setSurface)(JNIEnv *env, jobject, jobject surface) {
    ANativeWindow *win = surface ? ANativeWindow_fromSurface(env, surface) : nullptr;
    Frontend::get().video().setWindow(win);
}

JNIEXPORT void JNICALL JNI_FN(setSharpScale)(JNIEnv *, jobject, jint scale) {
    Frontend::get().video().setSharpScale(scale);
}

JNIEXPORT void JNICALL JNI_FN(setButton)(JNIEnv *, jobject, jint port, jint id, jboolean pressed) {
    Frontend::get().setButton(port, id, pressed);
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

JNIEXPORT jstring JNICALL JNI_FN(getLastError)(JNIEnv *env, jobject) {
    return env->NewStringUTF(Frontend::get().lastError().c_str());
}

}  // extern "C"
