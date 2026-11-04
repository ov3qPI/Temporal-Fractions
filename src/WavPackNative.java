package cc0.temporalfractions.a0he7vjet48;
import java.io.IOException;
public final class WavPackNative {
public static final String SOURCE_ID="WavPackNative_v20261104T222055Z";
static {System.loadLibrary("temporalwavpack");}
public static native long nativeOpen(int fd) throws IOException;
public static native int[] nativeInfo(long handle);
public static native int nativeRead(long handle,float[] buffer,int frames) throws IOException;
public static native void nativeClose(long handle);
private WavPackNative(){}
}
