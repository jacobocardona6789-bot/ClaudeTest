import java.awt.*;

/**
 * Clase base para todos los objetos del juego (jugador, enemigos, proyectiles).
 */
public abstract class Entity {
    protected double x, y;
    protected double vx, vy;
    protected int width, height;
    protected boolean active = true;

    public Entity(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }

    public boolean intersects(Entity other) {
        return getBounds().intersects(other.getBounds());
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getVx() { return vx; }
    public double getVy() { return vy; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public double centerX() { return x + width / 2.0; }
    public double centerY() { return y + height / 2.0; }

    public abstract void update(TileMap map);
    public abstract void draw(Graphics2D g);
}
