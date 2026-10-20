import java.awt.*;

/**
 * Partícula individual para efectos visuales (polvo de dash, chispas de pared, aura de carga).
 */
public class Particle {
    public enum Type {
        DASH_DUST,
        WALL_SPARK,
        WALL_KICK_BURST,
        MUZZLE_FLASH,
        CHARGE_ORB,
        EXPLOSION,
        TEXT
    }

    private double x, y;
    private double vx, vy;
    private int life, maxLife;
    private final Type type;
    private Color color;
    private double size;
    private String text;

    public Particle(double x, double y, double vx, double vy, int maxLife, Type type, Color color, double size) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = maxLife;
        this.maxLife = maxLife;
        this.type = type;
        this.color = color;
        this.size = size;
    }

    public static Particle createText(double x, double y, String text, Color color) {
        Particle p = new Particle(x, y, 0, -1.2, 30, Type.TEXT, color, 12);
        p.text = text;
        return p;
    }

    public void update() {
        x += vx;
        y += vy;
        life--;

        if (type == Type.DASH_DUST) {
            vx *= 0.92;
            vy -= 0.05; // sube despacio
            size = Math.max(1, size * 0.96);
        } else if (type == Type.WALL_SPARK) {
            vy += 0.25; // gravedad en chispas
        } else if (type == Type.EXPLOSION) {
            size *= 1.08;
        }
    }

    public boolean isDead() {
        return life <= 0;
    }

    public void draw(Graphics2D g) {
        if (life <= 0) return;
        float alpha = Math.max(0f, Math.min(1f, (float) life / maxLife));

        if (type == Type.TEXT) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)));
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.drawString(text, (int) x, (int) y);
            return;
        }

        Composite orig = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(color);

        int s = (int) Math.round(size);
        if (type == Type.DASH_DUST) {
            g.fillOval((int) x - s / 2, (int) y - s / 2, s, s);
            g.setColor(Color.WHITE);
            g.fillOval((int) x - s / 4, (int) y - s / 4, s / 2, s / 2);
        } else if (type == Type.WALL_SPARK) {
            g.fillRect((int) x, (int) y, s, s);
        } else if (type == Type.WALL_KICK_BURST) {
            g.drawOval((int) x - s / 2, (int) y - s / 2, s, s);
        } else if (type == Type.MUZZLE_FLASH) {
            g.fillOval((int) x - s / 2, (int) y - s / 2, s, s);
        } else if (type == Type.CHARGE_ORB) {
            g.fillOval((int) x - s / 2, (int) y - s / 2, s, s);
            g.setColor(Color.WHITE);
            g.fillOval((int) x - s / 4, (int) y - s / 4, s / 2, s / 2);
        } else if (type == Type.EXPLOSION) {
            g.fillOval((int) x - s / 2, (int) y - s / 2, s, s);
            g.setColor(new Color(255, 230, 80));
            g.fillOval((int) x - s / 3, (int) y - s / 3, s * 2 / 3, s * 2 / 3);
        }

        g.setComposite(orig);
    }
}
