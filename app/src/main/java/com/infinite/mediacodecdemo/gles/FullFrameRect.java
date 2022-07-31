package com.infinite.mediacodecdemo.gles;

public class FullFrameRect {
    private static final String TAG = "FullFrameRect";
    private final Drawable2d mRectDrawable = new Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE);
    private Texture2dProgram mProgram;

    /**
     * Prepares the object.
     *
     * @param program The program to use.  FullFrameRect takes ownership, and will release
     *                the program when no longer needed.
     */
    public FullFrameRect(Texture2dProgram program) {
        mProgram = program;
    }

    /**
     * Releases resources.
     * <p>
     * This must be called with the appropriate EGL context current (i.e. the one that was
     * current when the constructor was called).  If we're about to destroy the EGL context,
     * there's no value in having the caller make it current just to do this cleanup, so you
     * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
     */
    public void release(boolean doEglCleanup) {
        if (mProgram != null) {
            if (doEglCleanup) {
                mProgram.release();
            }
            mProgram = null;
        }
    }

    //Returns the program currently in use.
    public Texture2dProgram getProgram() {
        return mProgram;
    }

    /**
     * Changes the program.  The previous program will be released.
     * <p>
     * The appropriate EGL context must be current.
     */
    public void changeProgram(Texture2dProgram program) {
        mProgram.release();
        mProgram = program;
    }

    public int createTextureObject() {
        return mProgram.createTextureObject();
    }

    //Draws a viewport-filling rect, texturing it with the specified texture object.
    public void drawFrame(int textureId, float[] texMatrix) {
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        mProgram.draw(GlUtil.IDENTITY_MATRIX,//单元矩阵
                mRectDrawable.getVertexArray(),//顶点数组，FloatBuffer类型，存放顶点坐标数据
                0, //起始索引
                mRectDrawable.getVertexCount(),// 顶点个数 4 = 8/2; FULL_RECTANGLE_COORDS.length / mCoordsPerVertex;
                mRectDrawable.getCoordsPerVertex(), //每个顶点坐标的个数, Now only use x,y so is 2. (x,y) 顶点数组每组有几个数值,可能是2,3,4
                mRectDrawable.getVertexStride(), // 8, 顶点数组跨步 一个坐标点（x,y）占用多大内存
                texMatrix, // surface的旋转矩阵 transform 数组值
                mRectDrawable.getTexCoordArray(), // 纹理数组，FloatBuffer类型，存放纹理坐标数据
                textureId, // 纹理id
                mRectDrawable.getTexCoordStride() // 8, 纹理数组跨步  一个坐标点（s,t）占用多大内存
        );
    }


}
