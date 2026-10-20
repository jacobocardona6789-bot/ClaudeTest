import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Gestor de partículas y emisores de efectos visuales (dash, pared, carga, impactos).
 */
public class ParticleSystem {
    private final List<Particle> particles = new ArrayList<>();
    private final Random rnd = new Random();

    public void update() {
        for (Particle p : particles) {
            p.update();
        }
        particles.removeIf(Particle::isDead);
    }

    public void draw(Graphics2D g) {
        for (Particle p : particles) {
            p.draw(g);
        }
    }

    public void clear() {
        particles.clear();
    }

    // --- Emisores especializados ---

    public void spawnDashDust(double x, double y, boolean facingRight) {
        double vx = (facingRight ? -1.0 : 1.0) * (1.0 + rnd.nextDouble() * 1.5);
        double vy = -0.3 - rnd.nextDouble() * 0.8;
        Color c = rnd.nextBoolean() ? new Color(220, 220, 230) : new Color(170, 180, 200);
        particles.add(new Particle(x, y, vx, vy, 16 + rnd.nextInt(8), Particle.Type.DASH_DUST, c, 6 + rnd.nextInt(5)));
    }

    public void spawnWallSparks(double x, double y, boolean touchingLeftWall) {
        double vx = (touchingLeftWall ? 1.0 : -1.0) * (1.2 + rnd.nextDouble() * 2.2);
        double vy = -1.5 - rnd.nextDouble() * 2.5;
        Color c = rnd.nextBoolean() ? new Color(255, 240, 80) : new Color(255, 160, 40);
        particles.add(new Particle(x, y, vx, vy, 12 + rnd.nextInt(6), Particle.Type.WALL_SPARK, c, 2 + rnd.nextInt(2)));
    }

    public void spawnWallKickBurst(double x, double y) {
        for (int i = 0; i < 8; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            double spd = 2.0 + rnd.nextDouble() * 3.5;
            double vx = Math.cos(angle) * spd;
            double vy = Math.sin(angle) * spd;
            Color c = rnd.nextBoolean() ? Color.WHITE : new Color(80, 220, 255);
            particles.add(new Particle(x, y, vx, vy, 10 + rnd.nextInt(6), Particle.Type.WALL_KICK_BURST, c, 4 + rnd.nextInt(4)));
        }
    }

    public void spawnMuzzleFlash(double x, double y, Color color) {
        particles.add(new Particle(x, y, 0, 0, 4, Particle.Type.MUZZLE_FLASH, color, 14));
    }

    public void spawnChargeOrb(double centerX, double centerY, int chargeLevel) {
        // Partículas que orbitan y convergen hacia X
        double radius = 24.0 + rnd.nextDouble() * 16.0;
        double angle = rnd.nextDouble() * Math.PI * 2;
        double px = centerX + Math.cos(angle) * radius;
        double py = centerY + Math.sin(angle) * radius;
        double vx = (centerX - px) * 0.12;
        double vy = (centerY - py) * 0.12;

        Color c;
        if (chargeLevel == 1) {
            c = rnd.nextBoolean() ? new Color(80, 220, 255) : new Color(0, 140, 255);
        } else {
            c = rnd.nextBoolean() ? new Color(255, 100, 240) : new Color(100, 255, 255);
        }
        particles.add(new Particle(px, py, vx, vy, 10, Particle.Type.CHARGE_ORB, c, 5 + rnd.nextInt(3)));
    }

    public void spawnExplosion(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            double angle = rnd.nextDouble() * Math.PI * 2;
            double spd = 1.0 + rnd.nextDouble() * 4.0;
            double vx = Math.cos(angle) * spd;
            double vy = Math.sin(angle) * spd;
            Color c = rnd.nextBoolean() ? new Color(255, 120, 30) : new Color(255, 230, 80);
            particles.add(new Particle(x, y, vx, vy, 14 + rnd.nextInt(10), Particle.Type.EXPLOSION, c, 6 + rnd.nextInt(6)));
        }
    }

    public void spawnDamageText(double x, double y, String text, Color color) {
        particles.add(Particle.createText(x, y, text, color));
    }
}
