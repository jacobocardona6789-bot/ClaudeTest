import java.awt.geom.Rectangle2D;
import java.util.Random;

/**
 * Cámara suave 2D con look-ahead según la dirección del jugador, límites del mundo y sacudida de pantalla.
 */
public class Camera {
    private double x, y;
    private final int viewW, viewH;
    private final int worldW, worldH;

    private double shakeIntensity = 0;
    private final Random rnd = new Random();

    public Camera(int viewW, int viewH, int worldW, int worldH) {
        this.viewW = viewW;
        this.viewH = viewH;
        this.worldW = worldW;
        this.worldH = worldH;
    }

    public void follow(double targetX, double targetY, boolean facingRight, double smoothing) {
        // Look-ahead: la cámara se adelanta 60px en la dirección que mira el jugador
        double lookAhead = facingRight ? 60.0 : -60.0;
        double goalX = targetX + lookAhead - viewW / 2.0;
        double goalY = targetY - viewH / 2.0;

        x += (goalX - x) * smoothing;
        y += (goalY - y) * smoothing;

        clamp();

        if (shakeIntensity > 0) {
            shakeIntensity *= 0.88;
            if (shakeIntensity < 0.2) shakeIntensity = 0;
        }
    }

    public void snapTo(double targetX, double targetY) {
        x = targetX - viewW / 2.0;
        y = targetY - viewH / 2.0;
        clamp();
    }

    public void shake(double intensity) {
        this.shakeIntensity = Math.max(this.shakeIntensity, intensity);
    }

    private void clamp() {
        x = Math.max(0, Math.min(x, worldW - viewW));
        y = Math.max(0, Math.min(y, worldH - viewH));
    }

    public int ix() {
        int offX = shakeIntensity > 0 ? (int) ((rnd.nextDouble() * 2.0 - 1.0) * shakeIntensity) : 0;
        return (int) Math.round(x) + offX;
    }

    public int iy() {
        int offY = shakeIntensity > 0 ? (int) ((rnd.nextDouble() * 2.0 - 1.0) * shakeIntensity) : 0;
        return (int) Math.round(y) + offY;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getViewW() { return viewW; }
    public int getViewH() { return viewH; }
}
