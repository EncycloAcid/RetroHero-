#pragma once
#include <android/log.h>

#define RH_TAG "RetroHero"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, RH_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, RH_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, RH_TAG, __VA_ARGS__)
