package com.primo.optimizer;

import android.opengl.*;

/** Reads the driver-reported renderer using a tiny offscreen surface; no benchmark claim. */
final class GpuProbe {
    static String renderer() {
        android.opengl.EGLDisplay display=EGL14.EGL_NO_DISPLAY;
        android.opengl.EGLContext context=EGL14.EGL_NO_CONTEXT;
        android.opengl.EGLSurface surface=EGL14.EGL_NO_SURFACE;
        try {
            display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] version=new int[2];
            if (!EGL14.eglInitialize(display,version,0,version,1)) return "Unavailable";
            android.opengl.EGLConfig[] configs=new android.opengl.EGLConfig[1];int[] count=new int[1];
            int[] attributes={EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_PBUFFER_BIT,EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_NONE};
            if(!EGL14.eglChooseConfig(display,attributes,0,configs,0,1,count,0) || count[0]==0) return "Unavailable";
            context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE},0);
            surface=EGL14.eglCreatePbufferSurface(display,configs[0],new int[]{EGL14.EGL_WIDTH,1,EGL14.EGL_HEIGHT,1,EGL14.EGL_NONE},0);
            if(!EGL14.eglMakeCurrent(display,surface,surface,context)) return "Unavailable";
            String value=GLES20.glGetString(GLES20.GL_RENDERER);
            return value==null?"Unavailable":value;
        } catch(RuntimeException e) { return "Unavailable"; }
        finally {
            if(display!=EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
                if(surface!=EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display,surface);
                if(context!=EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display,context);
                EGL14.eglTerminate(display);EGL14.eglReleaseThread();
            }
        }
    }
}
