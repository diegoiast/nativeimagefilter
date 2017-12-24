#include <jni.h>
#include <string>
#include <stddef.h>
#include <android/log.h>

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedMacroInspection"

#define LOGV(TAG,...) __android_log_print(ANDROID_LOG_VERBOSE, TAG,__VA_ARGS__)
#define LOGD(TAG,...) __android_log_print(ANDROID_LOG_DEBUG  , TAG,__VA_ARGS__)
#define LOGI(TAG,...) __android_log_print(ANDROID_LOG_INFO   , TAG,__VA_ARGS__)
#define LOGW(TAG,...) __android_log_print(ANDROID_LOG_WARN   , TAG,__VA_ARGS__)
#define LOGE(TAG,...) __android_log_print(ANDROID_LOG_ERROR  , TAG,__VA_ARGS__)
#define TAG "LIB"

#define DEBUG_LINE LOGD(TAG, "%s: %d - ping", __FUNCTION__, __LINE__)

#define UNUSED(x) (void(x))

#define FILTER_NORMAL_BINARIZATION   60
#define FILTER_MEAN_BINARIZATION     61
#define FILTER_INVERT_COLORS         70

#define FILTER_NO_ERROR                      0
#define FILTER_ERROR_INVALID_FILTER         -1
#define FILTER_ERROR_NOT_IMPLEMENTED        -2
#define FILTER_ERROR_INVALID_IMAGE_IN_SIZE  -3
#define FILTER_ERROR_INVALID_IMAGE_OUT_SIZE -4

int process_image( int8_t* imageIn, int8_t* imageOut, int size_x, int size_y, double sensitivity,  int filterType);
void process_pixel_binary_normalization(const int8_t *in_image, int8_t *out_image, int offset, double sensitivity);
void process_pixel_invert_color(const int8_t *in_image, int8_t *out_image, int offset, double sensitivity);
double get_picture_average(int8_t *image, int sizeX, int sizeY);

/*
 *  I understand that in theory - I could implement something like:
 *
 *  applyFilter( FilteredImage source, FilteredImage dest, int type, double sensitivity )
 *
 *  and then access the sizes and buffer using JNI... but this way is easier. The glue
 *  function is uglier.. but it's kept as an implementation detail - and we have less places to
 *  fall off the wagon. JNI is pain.
 */

extern "C"
JNIEXPORT jint JNICALL
Java_github_diego_nativeimagefilter_FilteredImage_applyFilter(
        JNIEnv *env,
        jobject self,
        jbyteArray imageIn,
        jbyteArray imageOut,
        jint size_x,
        jint size_y,
        jint filterType,
        jdouble sensitivity
) {

    jint imageSize = size_x * size_y * 4;
    jint lengthOfArray;
    lengthOfArray = env->GetArrayLength(imageIn);
    if (lengthOfArray < imageSize) {
        return FILTER_ERROR_INVALID_IMAGE_IN_SIZE;
    }
    lengthOfArray = env->GetArrayLength(imageOut);
    if (lengthOfArray < imageSize) {
        return FILTER_ERROR_INVALID_IMAGE_OUT_SIZE;
    }

    jbyte* in_bits = env->GetByteArrayElements(imageIn, NULL);
    jbyte* out_bits = env->GetByteArrayElements(imageOut, NULL);

    // yes - this looks ugly - but it's ok mom, I am an engineer
    jint returnValue = process_image(in_bits, out_bits, size_x, size_y, sensitivity, filterType);

    env->ReleaseByteArrayElements(imageIn, in_bits, JNI_COMMIT);
    env->ReleaseByteArrayElements(imageOut, out_bits, JNI_COMMIT);
    return returnValue;
    UNUSED(self);
}

////////////////////////////////////////////////////////////////////////////////////////////////////
// implementation details

int process_image(int8_t* imageIn, int8_t* imageOut, int size_x, int size_y, double sensitivity, int filterType) {
    int offset = 0;
    if (filterType == FILTER_MEAN_BINARIZATION) {
        DEBUG_LINE;
        sensitivity = get_picture_average(imageIn, size_x, size_y);
        LOGD(TAG, "Average is %f", sensitivity);
    }
    for (int y=0; y < size_y; y++) {
        for (int x=0; x < size_x; x++) {
            switch (filterType) {
                case FILTER_NORMAL_BINARIZATION:
                    process_pixel_binary_normalization(imageIn, imageOut, offset, sensitivity);
                    break;
                case FILTER_INVERT_COLORS:
                    process_pixel_invert_color(imageIn, imageOut, offset, sensitivity);
                    break;
                case FILTER_MEAN_BINARIZATION:
                    process_pixel_binary_normalization(imageIn, imageOut, offset, sensitivity);
                    break;
                default:
                    // this will fail after a single loop
                    return FILTER_ERROR_INVALID_FILTER;
            }
            offset += 4;
        }
    }
    return FILTER_NO_ERROR;
}

double get_picture_average(int8_t *image, int size_x, int size_y) {
    int r_av = 0;
    int g_av = 0;
    int b_av = 0;

    int r_last = 0;
    int g_last = 0;
    int b_last = 0;
    int n = 1;

    int offset = 0;
    for(int y=0; y<size_y; y++) {
        for(int x=0; x<size_x; x++) {
            char r = image[offset + 0];
            char g = image[offset + 1];
            char b = image[offset + 2];

            r_av = r_av + r/n - r_last/n;
            g_av = g_av + r/n - g_last/n;
            b_av = b_av + b/n - b_last/n;

            r_last = r;
            g_last = g;
            b_last = b;

            n ++;
            offset += 4;
        }
    }

    double gray = (r_av + g_av + b_av) / 3;
    gray =  gray / 256;
    return gray;
}

void process_pixel_binary_normalization(const int8_t *in_image, int8_t *out_image, int offset, double sensitivity) {
    int threshold = (int) (256 * sensitivity - 128);
    int gray =
            in_image[offset + 0] +
            in_image[offset + 1] +
            in_image[offset + 2];
    gray = gray / 3;
    if (gray <= threshold) {
        gray = 0xff;
    } else {
        gray = 0;
    }
    out_image[offset + 0] = (int8_t) gray;
    out_image[offset + 1] = (int8_t) gray;
    out_image[offset + 2] = (int8_t) gray;
    // keep alpha channel as is
    out_image[offset + 3] =  in_image[offset + 3];
}

void process_pixel_invert_color(const int8_t *in_image, int8_t *out_image, int offset, double sensitivity) {
    out_image[offset+0] = (int8_t) ((in_image[offset + 0] + 128 ) % 256);
    out_image[offset+1] = (int8_t) ((in_image[offset + 1] + 128 ) % 256);
    out_image[offset+2] = (int8_t) ((in_image[offset + 2] + 128 ) % 256);
    // keep alpha channel as is
    out_image[offset+3] = in_image[offset + 3];
    UNUSED(sensitivity);
}

#pragma clang diagnostic pop