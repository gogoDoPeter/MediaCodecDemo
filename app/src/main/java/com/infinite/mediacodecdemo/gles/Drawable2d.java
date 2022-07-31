package com.infinite.mediacodecdemo.gles;

import java.nio.FloatBuffer;

public class Drawable2d {
    private FloatBuffer mVertexArray;
    private FloatBuffer mTexCoordArray;
    private int mVertexCount;
    private int mCoordsPerVertex;
    private int mVertexStride;
    private int mTexCoordStride;
    private Prefab mPrefab;
    private static final int SIZEOF_FLOAT = 4;

    /**
     * Enum values for constructor.
     */
    public enum Prefab {
        TRIANGLE, RECTANGLE, FULL_RECTANGLE
    }

    /**
     * A "full" square, extending from -1 to +1 in both dimensions.  When the model/view/projection
     * matrix is identity, this will exactly cover the viewport.
     * <p>
     * The texture coordinates are Y-inverted relative to RECTANGLE.  (This seems to work out
     * right with external textures from SurfaceTexture.)
     */
    private static final float FULL_RECTANGLE_COORDS[] = {
            -1.0f, -1.0f,   // 0 bottom left  x,y
            1.0f, -1.0f,   // 1 bottom right
            -1.0f, 1.0f,   // 2 top left
            1.0f, 1.0f,   // 3 top right
    };
    private static final float FULL_RECTANGLE_TEX_COORDS[] = {
            0.0f, 0.0f,     // 0 bottom left s,t
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
    };
    private static final FloatBuffer FULL_RECTANGLE_BUF =
            GlUtil.createFloatBuffer(FULL_RECTANGLE_COORDS);
    private static final FloatBuffer FULL_RECTANGLE_TEX_BUF =
            GlUtil.createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);

    /**
     * Prepares a drawable from a "pre-fabricated" shape definition.
     * <p>
     * Does no EGL/GL operations, so this can be done at any time.
     */
    public Drawable2d(Prefab shape) {
        switch (shape) {
            case TRIANGLE:
                //TODO
                break;
            case RECTANGLE:
                //TODO
                break;
            case FULL_RECTANGLE:
                mVertexArray = FULL_RECTANGLE_BUF;
                mTexCoordArray = FULL_RECTANGLE_TEX_BUF;
                mCoordsPerVertex = 2;//now only use x,y form x,y,z,w
                mVertexStride = mCoordsPerVertex * SIZEOF_FLOAT; //表示跨距占多大内存，每个坐标有两个值，每个值float占4个字节，一次跨距为 2 * 4 =8 字节
                mVertexCount = FULL_RECTANGLE_COORDS.length / mCoordsPerVertex; //4,表示顶点个数
                break;
            default:
                throw new RuntimeException("Unknown shape " + shape);
        }
        mTexCoordStride = 2 * SIZEOF_FLOAT; //纹理坐标只有s和t，所以为2
        this.mPrefab = shape;
    }

    //Returns the array of vertices.
    public FloatBuffer getVertexArray() {
        return mVertexArray;
    }

    //Returns the array of texture coordinates.
    public FloatBuffer getTexCoordArray() {
        return mTexCoordArray;
    }

    //Returns the number of vertices stored in the vertex array.
    public int getVertexCount() { // VertexCount 表示顶点个数
        return mVertexCount;
    }

    //Returns the width, in bytes, of the data for each vertex.
    public int getVertexStride() {
        return mVertexStride;
    }

    //Returns the width, in bytes, of the data for each texture coordinate.
    public int getTexCoordStride() {
        return mTexCoordStride;
    }

    //Returns the number of position coordinates per vertex.  This will be 2 or 3 or 4.
    public int getCoordsPerVertex() { //now only use x,y form x,y,z,w, so is 2
        return mCoordsPerVertex;
    }

    @Override
    public String toString() {
        if (mPrefab != null) {
            return "[Drawable2d: " + mPrefab + "]";
        } else {
            return "[Drawable2d: ...]";
        }
    }
}
