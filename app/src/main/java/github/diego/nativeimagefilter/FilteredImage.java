package github.diego.nativeimagefilter;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.IntRange;

import java.nio.ByteBuffer;

@SuppressWarnings("WeakerAccess")
public class FilteredImage {

    @SuppressWarnings("unused")
    public static final int NORMAL_BINARIZATION          = 60;
    @SuppressWarnings("unused")
    public static final int MEAN_BINARIZATION            = 61;
    @SuppressWarnings("unused")
    public static final int INVERT_COLORS                = 70;

    @SuppressWarnings("unused")
    public static final int NO_ERROR                     =  0;
    @SuppressWarnings("unused")
    public static final int ERROR_INVALID_FILTER         = -1;
    @SuppressWarnings("unused")
    public static final int ERROR_NOT_IMPLEMENTED        = -2;
    @SuppressWarnings("unused")
    public static final int ERROR_INVALID_IMAGE_IN_SIZE  = -3;
    @SuppressWarnings("unused")
    public static final int ERROR_INVALID_IMAGE_OUT_SIZE = -4;

    public static final double DEFAULT_SENSITIVITY              = 0.5;

    public static FilteredImage fromBitmap(Bitmap bitmap) {
        int sizeX = bitmap.getWidth();
        int sizeY = bitmap.getHeight();
        int byteCount = bitmap.getByteCount();

        // this is bad - just abort.
        // BTW - this should not happen in real life...I am too worried
        int b = sizeX*sizeY*4;
        if (b != byteCount) {
            return null;
        }
        FilteredImage img = new FilteredImage(sizeX, sizeY);
        bitmap.copyPixelsToBuffer(img.buffer);
        return img;
    }

    public FilteredImage(int sizeX, int sizeY) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.buffer = ByteBuffer.allocate(sizeX * sizeY * 4);
        this.thisNumber = count;
        count ++;
    }

    private static int count = 0;
    int thisNumber;
    private ByteBuffer buffer;
    private int sizeX;
    private int sizeY;

    public Bitmap toBitmap(){
        Bitmap bitmap = Bitmap.createBitmap(sizeX, sizeY, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    public void putPixel(
            int x,
            int y,
            @IntRange(from = 0, to = 255)int R,
            @IntRange(from = 0, to = 255)int G,
            @IntRange(from = 0, to = 255)int B,
            @IntRange(from = 0, to = 255)int A ) {
        int offset = y * sizeX * 4 + x *4;
        buffer.position(offset);
        buffer.put((byte) R);
        buffer.put((byte) G);
        buffer.put((byte) B);
        buffer.put((byte) A);
    }

    public void putPixel(int x, int y, int color) {
        int R = Color.red(color);
        int G = Color.green(color);
        int B = Color.blue(color);
        int A = Color.alpha(color);
        putPixel(x, y, R, G, B, A);
    }

    public int getPixel(int x, int y) {
        int offset = y * sizeX * 4 + x * 4;
        return buffer.getInt(offset);
    }

    @SuppressWarnings("unused")
    public FilteredImage applyFilter(int filerType)
            throws UnsupportedOperationException, IllegalArgumentException{
        return applyFilter(filerType, DEFAULT_SENSITIVITY);
    }

    public FilteredImage applyFilter(int filerType, double sensitivity)
            throws UnsupportedOperationException, IllegalArgumentException{

        FilteredImage outputImage = new FilteredImage(sizeX, sizeY);
        int result = applyFilter(buffer.array(), outputImage.buffer.array(), sizeX, sizeY, filerType, sensitivity);
        switch (result) {
            case NO_ERROR:
                return outputImage;
            case ERROR_NOT_IMPLEMENTED:
                throw new UnsupportedOperationException();
            case ERROR_INVALID_FILTER:
            case ERROR_INVALID_IMAGE_IN_SIZE:
            case ERROR_INVALID_IMAGE_OUT_SIZE:
                throw new IllegalArgumentException();
        }
        return outputImage;
    }

    // JNI interface
    private native int applyFilter(byte[] imageIn, byte[] imageOut, int sizeX, int sizeY, int filterType, double sensitivity);
}