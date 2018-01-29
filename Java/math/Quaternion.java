package edu.vtekaev.posturecontrol.math;

/**
 * Created by Slava on 28.01.2018.
 */

public class Quaternion {
    public float w;
    public float x;
    public float y;
    public float z;

    public Quaternion() {
        w = 1.0f;
        x = 0.0f;
        y = 0.0f;
        z = 0.0f;
    }

    public Quaternion(float nw, float nx, float ny, float nz) {
        w = nw;
        x = nx;
        y = ny;
        z = nz;
    }

    public Quaternion getProduct(Quaternion q) {
        if (q == null) {
            return null;
        }
        return new Quaternion(
                w * q.w - x * q.x - y * q.y - z * q.z,  // new w
                w * q.x + x * q.w + y * q.z - z * q.y,  // new x
                w * q.y - x * q.z + y * q.w + z * q.x,  // new y
                w * q.z + x * q.y - y * q.x + z * q.w); // new z
    }

    public void product(Quaternion q) {
        float _w = q.w * w - q.x * x - q.y * y - q.z * z;
        float _x = q.w * x + q.x * w + q.y * z - q.z * y;
        float _y = q.w * y - q.x * z + q.y * w + q.z * x;
        float _z = q.w * z + q.x * y - q.y * x + q.z * w;
        w = _w;
        x = _x;
        y = _y;
        z = _z;
    }

    public Quaternion getConjugate() {
        return new Quaternion(w, -x, -y, -z);
    }

    public void conjugate() {
        x = -x;
        y = -y;
        z = -z;
    }

    public float getMagnitude() {
        return (float) Math.sqrt(w * w + x * x + y * y + z * z);
    }

    public void normalize() {
        float m = getMagnitude();
        w /= m;
        x /= m;
        y /= m;
        z /= m;
    }

    public Quaternion getNormalized() {
        Quaternion r = new Quaternion(w, x, y, z);
        r.normalize();
        return r;
    }

    public void getEuler(float[] euler) {
        euler[0] = -(float) Math.atan2(2 * x * y - 2 * w * z, 2 * w * w + 2 * x * x - 1); // psi Y
        euler[1] = (float) Math.asin(2 * x * z + 2 * w * y); // theta X
        euler[2] = -(float) Math.atan2(2 * y * z - 2 * w * x, 2 * w * w + 2 * z * z - 1); // phi Z
    }

    public void resetQuat() {
        w = 1;
        x = 0;
        y = 0;
        z = 0;
    }

    public void copy(Quaternion q) {
        if (q == null) {
            return;
        }
        w = q.w;
        x = q.x;
        y = q.y;
        z = q.z;
    }
}
