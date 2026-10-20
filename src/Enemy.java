import java.awt.*;
import java.util.List;

/**
 * Clase base abstracta para enemigos, implementando Damageable y el patrón Template Method.
 */
public abstract class Enemy extends Entity implements Damageable {
    protected int health;
    protected int maxHealth;
    protected int contactDamage;
    protected boolean facingRight = false;
    protected int hurtTimer = 0;

    public Enemy(double x, double y, int width, int height, int maxHealth, int contactDamage) {
        super(x, y, width, height);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.contactDamage = contactDamage;
    }

    public int getContactDamage() { return contactDamage; }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0) return;
        health = Math.max(0, health - amount);
        hurtTimer = 8;
        if (isDead()) {
            active = false;
            SoundSystem.play("explode");
        } else {
            SoundSystem.play("hit");
        }
    }

    @Override
    public boolean isDead() { return health <= 0; }
    @Override
    public int getHealth() { return health; }
    @Override
    public int getMaxHealth() { return maxHealth; }

    @Override
    public void update(TileMap map) {
        // Método plantilla: think es implementado por cada enemigo
        if (hurtTimer > 0) hurtTimer--;
        x += vx;
        y += vy;
    }

    public abstract void think(TileMap map, Player player, List<Bullet> bullets, ParticleSystem ps);

    // --- Subclases de enemigos ---

    /**
     * Mettaur (Casco protector): patrulla el suelo, se esconde bajo el casco
     * deflectando limones normales, y al salir dispara ráfagas.
     */
    public static class Mettaur extends Enemy {
        private boolean hiding = false;
        private int stateTimer = 0;
        private int walkFrame = 0;
        private int walkTick = 0;

        public Mettaur(double x, double y) {
            super(x, y, 24, 20, 3, 2);
            this.vx = -0.8;
            this.facingRight = false;
        }

        public boolean isHiding() { return hiding; }

        @Override
        public void takeDamage(int amount) {
            super.takeDamage(amount);
        }

        public boolean tryHitWithBullet(Bullet b, ParticleSystem ps) {
            if (hiding && b.getDamage() < 3) {
                // Deflectar disparo débil
                if (ps != null) {
                    ps.spawnDamageText(x + 12, y - 6, "DEFLECT!", Color.YELLOW);
                    ps.spawnWallSparks(x + 12, y + 10, b.getVx() < 0);
                }
                SoundSystem.play("hit");
                return false; // No recibió daño
            }
            takeDamage(b.getDamage());
            if (ps != null) {
                ps.spawnDamageText(x + 12, y - 6, "-" + b.getDamage(), Color.WHITE);
                ps.spawnExplosion(x + 12, y + 10, 4);
            }
            return true;
        }

        @Override
        public void think(TileMap map, Player player, List<Bullet> bullets, ParticleSystem ps) {
            stateTimer++;

            // Gravedad
            int col = (int) (x + width / 2) / TileMap.TILE_SIZE;
            int rowUnder = (int) (y + height + 1) / TileMap.TILE_SIZE;
            if (!map.isSolid(col, rowUnder)) {
                vy += 0.4;
            } else {
                vy = 0;
                y = rowUnder * TileMap.TILE_SIZE - height;
            }

            if (!hiding) {
                walkTick++;
                if (walkTick % 8 == 0) walkFrame = (walkFrame + 1) % 2;

                // Rebotar contra paredes o bordes de precipicios
                int nextCol = (int) (x + (facingRight ? width + 2 : -2)) / TileMap.TILE_SIZE;
                int feetRow = (int) (y + height - 2) / TileMap.TILE_SIZE;
                int belowCol = (int) (x + (facingRight ? width + 4 : -4)) / TileMap.TILE_SIZE;

                if (map.isSolid(nextCol, feetRow) || !map.isSolid(belowCol, feetRow + 1)) {
                    facingRight = !facingRight;
                    vx = facingRight ? 0.8 : -0.8;
                }

                // Esconderse si el jugador se acerca mucho o periódicamente
                double dist = Math.abs(player.centerX() - centerX());
                if (dist < 110 || stateTimer > 180) {
                    hiding = true;
                    stateTimer = 0;
                    vx = 0;
                }
            } else {
                // Estado escondido: 90 ticks
                if (stateTimer >= 80) {
                    hiding = false;
                    stateTimer = 0;
                    facingRight = player.centerX() > centerX();
                    vx = facingRight ? 0.8 : -0.8;

                    // Al salir, disparar 3 proyectiles pequeños en abanico
                    if (bullets != null) {
                        double bx = centerX();
                        double by = y + 4;
                        double dir = facingRight ? 1.0 : -1.0;
                        bullets.add(new Bullet.EnemyBullet(bx, by, dir * 3.5, 0));
                        bullets.add(new Bullet.EnemyBullet(bx, by, dir * 3.0, -1.8));
                        bullets.add(new Bullet.EnemyBullet(bx, by, dir * 3.0, 1.8));
                        SoundSystem.play("lemon");
                    }
                }
            }
        }

        @Override
        public void draw(Graphics2D g) {
            if (hurtTimer % 2 == 1) return; // Parpadeo de daño

            int dx = (int) Math.round(x);
            int dy = (int) Math.round(y);

            if (hiding) {
                if (Sprites.mettaurHide != null) {
                    Sprites.drawFlipped(g, Sprites.mettaurHide, dx, dy, facingRight);
                } else {
                    g.setColor(Color.YELLOW);
                    g.fillRoundRect(dx, dy, width, height, 8, 8);
                }
            } else {
                if (Sprites.mettaurWalk != null) {
                    Sprites.drawFlipped(g, Sprites.mettaurWalk[walkFrame], dx, dy - 4, facingRight);
                } else {
                    g.setColor(Color.ORANGE);
                    g.fillRect(dx, dy, width, height);
                }
            }
        }
    }

    /**
     * Gunner volador: vuela en onda senoidal y dispara hacia Mega Man X.
     */
    public static class Gunner extends Enemy {
        private double startY;
        private double angle = 0;
        private int shootCooldown = 80;

        public Gunner(double x, double y) {
            super(x, y, 28, 22, 4, 2);
            this.startY = y;
            this.vx = -1.2;
        }

        @Override
        public void think(TileMap map, Player player, List<Bullet> bullets, ParticleSystem ps) {
            angle += 0.06;
            y = startY + Math.sin(angle) * 35;

            // Rebotar en los límites de patrulla
            if (x < 1700) {
                vx = 1.2;
                facingRight = true;
            } else if (x > 2400) {
                vx = -1.2;
                facingRight = false;
            }

            // Disparar hacia el jugador si está en rango
            shootCooldown--;
            if (shootCooldown <= 0) {
                shootCooldown = 90;
                double dx = player.centerX() - centerX();
                double dy = player.centerY() - centerY();
                double dist = Math.hypot(dx, dy);
                if (dist < 400 && bullets != null) {
                    double speed = 4.0;
                    bullets.add(new Bullet.EnemyBullet(centerX(), centerY(), (dx / dist) * speed, (dy / dist) * speed));
                    SoundSystem.play("lemon");
                }
            }
        }

        @Override
        public void draw(Graphics2D g) {
            if (hurtTimer % 2 == 1) return;
            int dx = (int) Math.round(x);
            int dy = (int) Math.round(y);
            int frame = ((int) (angle * 4)) % 2;
            if (frame < 0) frame = 0;

            if (Sprites.gunnerFrames != null) {
                Sprites.drawFlipped(g, Sprites.gunnerFrames[frame], dx, dy, facingRight);
            } else {
                g.setColor(Color.RED);
                g.fillOval(dx, dy, width, height);
            }
        }
    }

    /**
     * CombatDummy (Jefe de entrenamiento): objetivo de combate resistente para probar disparos cargados.
     */
    public static class CombatDummy extends Enemy {
        private int attackTimer = 0;

        public CombatDummy(double x, double y) {
            super(x, y, 48, 64, 32, 4);
        }

        @Override
        public void think(TileMap map, Player player, List<Bullet> bullets, ParticleSystem ps) {
            facingRight = player.centerX() > centerX();
            attackTimer++;

            // Cada cierto tiempo lanza una ráfaga para probar evasión con dash y saltos
            if (attackTimer >= 120 && bullets != null) {
                attackTimer = 0;
                double dir = facingRight ? 1.0 : -1.0;
                bullets.add(new Bullet.EnemyBullet(centerX(), centerY() - 10, dir * 4.5, 0));
                bullets.add(new Bullet.EnemyBullet(centerX(), centerY() + 10, dir * 4.5, 0));
                SoundSystem.play("lemon");
            }
        }

        @Override
        public void draw(Graphics2D g) {
            int dx = (int) Math.round(x);
            int dy = (int) Math.round(y);

            if (hurtTimer % 2 == 1) {
                g.setColor(Color.WHITE);
            } else {
                g.setColor(new Color(180, 50, 60));
            }

            // Cuerpo del robot gigante de entrenamiento
            g.fillRoundRect(dx, dy, width, height, 12, 12);
            g.setColor(new Color(30, 30, 40));
            g.drawRoundRect(dx, dy, width, height, 12, 12);

            // Núcleo brillante
            g.setColor(new Color(0, 240, 255));
            g.fillOval(dx + 16, dy + 20, 16, 16);

            // Barra de salud sobre el jefe
            g.setColor(Color.DARK_GRAY);
            g.fillRect(dx - 4, dy - 14, width + 8, 8);
            g.setColor(new Color(255, 60, 60));
            int barW = (int) ((double) health / maxHealth * (width + 6));
            g.fillRect(dx - 3, dy - 13, barW, 6);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            g.drawString("DUMMY: " + health + "/" + maxHealth, dx - 10, dy - 18);
        }
    }
}
