import java.awt.*;

/**
 * Proyectil abstracto disparado por el jugador o los enemigos.
 */
public abstract class Bullet extends Entity {
    protected int damage;
    protected boolean fromPlayer;
    protected int pierceRemaining;

    public Bullet(double x, double y, int width, int height, double vx, double vy, int damage, boolean fromPlayer, int pierce) {
        super(x, y, width, height);
        this.vx = vx;
        this.vy = vy;
        this.damage = damage;
        this.fromPlayer = fromPlayer;
        this.pierceRemaining = pierce;
    }

    public int getDamage() { return damage; }
    public boolean isFromPlayer() { return fromPlayer; }

    public boolean onHitEnemy(ParticleSystem ps) {
        pierceRemaining--;
        if (pierceRemaining < 0) {
            active = false;
        }
        return active;
    }

    @Override
    public void update(TileMap map) {
        x += vx;
        y += vy;

        // Fuera de los límites del nivel
        if (x < -100 || x > map.getWidth() + 100 || y < -100 || y > map.getHeight() + 100) {
            active = false;
            return;
        }

        // Colisión con tiles sólidos
        int col = (int) (x + width / 2) / TileMap.TILE_SIZE;
        int row = (int) (y + height / 2) / TileMap.TILE_SIZE;

        if (map.isSolid(col, row)) {
            // Intentar romper el bloque si es rompible
            map.damageTile(col, row, damage, null);
            active = false;
        }
    }

    // --- Subclases concretas ---

    public static class LemonBullet extends Bullet {
        public LemonBullet(double x, double y, boolean right) {
            super(x, y, 10, 8, right ? 9.5 : -9.5, 0, 1, true, 0);
        }

        @Override
        public void draw(Graphics2D g) {
            if (Sprites.bulletLemon != null) {
                Sprites.drawFlipped(g, Sprites.bulletLemon, (int) x, (int) y, vx > 0);
            } else {
                g.setColor(Color.YELLOW);
                g.fillOval((int) x, (int) y, width, height);
            }
        }
    }

    public static class Charge1Bullet extends Bullet {
        private int animTick = 0;

        public Charge1Bullet(double x, double y, boolean right) {
            super(x, y, 18, 16, right ? 11.0 : -11.0, 0, 3, true, 1);
        }

        @Override
        public void update(TileMap map) {
            super.update(map);
            animTick++;
        }

        @Override
        public void draw(Graphics2D g) {
            if (Sprites.bulletCharge1 != null) {
                Sprites.drawFlipped(g, Sprites.bulletCharge1, (int) x, (int) y, vx > 0);
            } else {
                g.setColor(Color.CYAN);
                g.fillOval((int) x, (int) y, width, height);
            }
        }
    }

    public static class Charge2Bullet extends Bullet {
        private int animTick = 0;

        public Charge2Bullet(double x, double y, boolean right) {
            super(x, y, 36, 24, right ? 12.0 : -12.0, 0, 6, true, 999);
        }

        @Override
        public void update(TileMap map) {
            super.update(map);
            animTick++;
        }

        @Override
        public void draw(Graphics2D g) {
            if (Sprites.bulletCharge2 != null) {
                Sprites.drawFlipped(g, Sprites.bulletCharge2, (int) x, (int) y, vx > 0);
            } else {
                g.setColor(new Color(255, 100, 255));
                g.fillRoundRect((int) x, (int) y, width, height, 10, 10);
            }
        }
    }

    public static class EnemyBullet extends Bullet {
        public EnemyBullet(double x, double y, double vx, double vy) {
            super(x, y, 8, 8, vx, vy, 2, false, 0);
        }

        @Override
        public void draw(Graphics2D g) {
            if (Sprites.bulletEnemy != null) {
                g.drawImage(Sprites.bulletEnemy, (int) x, (int) y, null);
            } else {
                g.setColor(Color.RED);
                g.fillOval((int) x, (int) y, width, height);
            }
        }
    }
}
