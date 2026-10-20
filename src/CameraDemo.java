import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

/**
 * Mega Man X - Motor de mecánicas base en Java 2D:
 *  - Correr con ciclo de fotogramas auténticos de X
 *  - Salto variable (short-hop / full jump), Coyote time y Jump buffer
 *  - Dash con reducción de hitbox para pasar por techos bajos
 *  - Dash-Jump con conservación de inercia en el aire
 *  - Wall Slide con fricción y chispas
 *  - Wall Kick (patada y dash wall kick escalable)
 *  - X-Buster con 2 niveles de carga, aura de energía y disparo en cualquier postura
 *  - Enemigos clásicos (Mettaurs que deflectan y Gunners voladores) y dummy de pruebas
 *  - Cámara suave con look-ahead y sacudida de pantalla
 *  - HUD auténtico de Mega Man X (barra vertical de vida y medidor de carga)
 *  - Efectos de sonido retro procedurales integrados
 */
public class CameraDemo {

    // ---------- Constantes de Ventana y Mundo ----------
    public static final int W = 800, H = 450;
    public static final int WORLD_W = TileMap.COLS * TileMap.TILE_SIZE; // 3200 px
    public static final int WORLD_H = TileMap.ROWS * TileMap.TILE_SIZE; // 800 px

    // ---------- Gestor de Teclado con detección de pulsaciones únicas ----------
    public static class Input extends KeyAdapter {
        private final Set<Integer> down = new HashSet<>();
        private final Set<Integer> pressed = new HashSet<>();

        @Override
        public synchronized void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            if (!down.contains(code)) {
                pressed.add(code);
            }
            down.add(code);
        }

        @Override
        public synchronized void keyReleased(KeyEvent e) {
            down.remove(e.getKeyCode());
        }

        public synchronized boolean isDown(int... codes) {
            for (int c : codes) if (down.contains(c)) return true;
            return false;
        }

        public synchronized boolean wasPressed(int... codes) {
            for (int c : codes) if (pressed.contains(c)) return true;
            return false;
        }

        public synchronized void clearPressed() {
            pressed.clear();
        }
    }

    // ---------- Panel Principal del Juego ----------
    public static class GamePanel extends JPanel {
        private static final double NS_PER_TICK = 1_000_000_000.0 / 60.0;

        private final Input input = new Input();
        private final TileMap map = new TileMap();
        private final ParticleSystem particles = new ParticleSystem();
        private final Camera camera = new Camera(W, H, WORLD_W, WORLD_H);

        private Player player;
        private final List<Enemy> enemies = new ArrayList<>();
        private final List<Bullet> bullets = new ArrayList<>();

        private long lastTime = System.nanoTime();
        private double accumulator = 0;
        private boolean showHelp = true;
        private boolean paused = false;

        public GamePanel() {
            setPreferredSize(new Dimension(W, H));
            setBackground(Color.BLACK);
            setFocusable(true);
            addKeyListener(input);

            // Inicializar subsistemas de gráficos y audio
            Sprites.init();
            SoundSystem.init();

            resetLevel();

            // Bucle principal de juego a 60 ticks/segundo
            new javax.swing.Timer(2, e -> {
                long now = System.nanoTime();
                accumulator += (now - lastTime) / NS_PER_TICK;
                lastTime = now;
                if (accumulator > 5) accumulator = 5;

                boolean updated = false;
                while (accumulator >= 1) {
                    tick();
                    accumulator--;
                    updated = true;
                }
                if (updated) repaint();
            }).start();
        }

        public void resetLevel() {
            player = new Player(120, 20 * TileMap.TILE_SIZE);
            camera.snapTo(player.centerX(), player.centerY());
            particles.clear();
            bullets.clear();
            enemies.clear();

            // Población de enemigos por secciones del nivel
            // Sección 5: Mettaurs en las plataformas superiores
            enemies.add(new Enemy.Mettaur(54 * TileMap.TILE_SIZE, 5 * TileMap.TILE_SIZE));
            enemies.add(new Enemy.Mettaur(60 * TileMap.TILE_SIZE, 4 * TileMap.TILE_SIZE));
            enemies.add(new Enemy.Mettaur(68 * TileMap.TILE_SIZE, 7 * TileMap.TILE_SIZE));

            // Gunners patrullando el cielo sobre el abismo y plataformas
            enemies.add(new Enemy.Gunner(1900, 320));
            enemies.add(new Enemy.Gunner(2200, 260));

            // Sección 6: Objetivo Dummy de pruebas / Mini-Jefe en la arena final
            enemies.add(new Enemy.CombatDummy(88 * TileMap.TILE_SIZE, 20 * TileMap.TILE_SIZE - 20));
        }

        private void tick() {
            if (input.wasPressed(KeyEvent.VK_H)) {
                showHelp = !showHelp;
            }
            if (input.wasPressed(KeyEvent.VK_P)) {
                paused = !paused;
            }
            if (input.wasPressed(KeyEvent.VK_R)) {
                resetLevel();
            }

            if (paused) {
                input.clearPressed();
                return;
            }

            // 1. Actualizar jugador
            player.update(input, map, bullets, particles, camera);

            // 2. Actualizar enemigos
            for (Enemy enemy : enemies) {
                enemy.think(map, player, bullets, particles);
                enemy.update(map);

                // Colisión de contacto con el jugador
                if (enemy.isAlive() && player.intersects(enemy)) {
                    player.takeDamage(enemy.getContactDamage());
                }
            }

            // 3. Actualizar proyectiles
            for (Bullet b : bullets) {
                b.update(map);

                if (!b.isActive()) continue;

                if (b.isFromPlayer()) {
                    // Impacto de balas del jugador contra enemigos
                    for (Enemy enemy : enemies) {
                        if (enemy.isAlive() && b.intersects(enemy)) {
                            boolean damaged = true;
                            if (enemy instanceof Enemy.Mettaur mettaur) {
                                damaged = mettaur.tryHitWithBullet(b, particles);
                            } else {
                                enemy.takeDamage(b.getDamage());
                                particles.spawnDamageText(b.centerX(), b.centerY() - 10, "-" + b.getDamage(), Color.WHITE);
                                particles.spawnExplosion(b.centerX(), b.centerY(), 4);
                            }

                            if (damaged) {
                                b.onHitEnemy(particles);
                            }
                            break;
                        }
                    }
                } else {
                    // Impacto de balas enemigas contra el jugador
                    if (b.intersects(player)) {
                        player.takeDamage(b.getDamage());
                        b.setActive(false);
                    }
                }
            }

            // 4. Limpieza de entidades inactivas usando removeIf (Principio POO / Colecciones)
            bullets.removeIf(b -> !b.isActive());
            enemies.removeIf(Enemy::isDead);

            // 5. Actualizar partículas
            particles.update();

            // 6. Cámara sigue al jugador
            camera.follow(player.centerX(), player.centerY(), player.isFacingRight(), 0.12);

            // Limpiar pulsaciones de este tick
            input.clearPressed();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // ==========================================
            // 1. FONDO CON PARALLAX MULTICAPA
            // ==========================================
            drawParallaxBackground(g2);

            // ==========================================
            // 2. MUNDO (Transformado con cámara)
            // ==========================================
            AffineTransform originalTransform = g2.getTransform();
            g2.translate(-camera.ix(), -camera.iy());

            // 2.1 Mapa de tiles y letreros
            map.draw(g2, camera.ix(), camera.iy(), W, H);

            // 2.2 Enemigos
            for (Enemy enemy : enemies) {
                enemy.draw(g2);
            }

            // 2.3 Proyectiles
            for (Bullet b : bullets) {
                b.draw(g2);
            }

            // 2.4 Jugador (Mega Man X)
            player.draw(g2);

            // 2.5 Partículas y efectos
            particles.draw(g2);

            g2.setTransform(originalTransform);

            // ==========================================
            // 3. HUD DE MEGA MAN X (Coordenadas de pantalla)
            // ==========================================
            drawHUD(g2);
        }

        /**
         * Fondo futurista estilo autopista central de Mega Man X con parallax.
         */
        private void drawParallaxBackground(Graphics2D g2) {
            // Gradiente cielo nocturno
            g2.setPaint(new GradientPaint(0, 0, new Color(12, 16, 45), 0, H, new Color(40, 25, 65)));
            g2.fillRect(0, 0, W, H);

            // Capa 1: Edificios y rascacielos lejanos con luces de neón (velocidad 0.15)
            int camX = camera.ix();
            int p1Period = 240;
            int shift1 = (int) (camX * 0.15) % p1Period;
            g2.setColor(new Color(25, 20, 50));
            for (int i = -1; i <= W / p1Period + 2; i++) {
                int bx = i * p1Period - shift1;
                int bh = 140 + (Math.abs(i * 37) % 70);
                g2.fillRect(bx, H - bh - 60, 55, bh + 60);

                // Ventanas de neón
                g2.setColor(new Color(60, 80, 140));
                for (int wy = H - bh - 40; wy < H - 70; wy += 16) {
                    g2.fillRect(bx + 8, wy, 8, 8);
                    g2.fillRect(bx + 24, wy, 8, 8);
                    g2.fillRect(bx + 40, wy, 8, 8);
                }
                g2.setColor(new Color(25, 20, 50));
            }

            // Capa 2: Vigas y columnas de la autopista suspendida (velocidad 0.40)
            int p2Period = 360;
            int shift2 = (int) (camX * 0.40) % p2Period;
            g2.setColor(new Color(35, 40, 65));
            for (int i = -1; i <= W / p2Period + 2; i++) {
                int px = i * p2Period - shift2;
                g2.fillRect(px, H - 240, 24, 240);
                g2.fillRect(px - 40, H - 240, 104, 18);
            }
        }

        /**
         * HUD auténtico estilo Mega Man X (barra vertical de vida, medidor de buster, instrucciones).
         */
        private void drawHUD(Graphics2D g2) {
            // Marco de barra vertical de vida de Mega Man X (esquina superior izquierda)
            int barX = 24, barY = 24;
            int barW = 14, barH = 70;

            g2.setColor(new Color(15, 15, 20, 220));
            g2.fillRect(barX - 2, barY - 2, barW + 4, barH + 4);
            g2.setColor(new Color(180, 190, 210));
            g2.drawRect(barX - 2, barY - 2, barW + 4, barH + 4);

            // Franjas de energía (16 ticks)
            int maxHp = player.getMaxHealth();
            int hp = player.getHealth();
            int tickH = barH / maxHp;

            for (int i = 0; i < maxHp; i++) {
                int ty = barY + barH - (i + 1) * tickH;
                if (i < hp) {
                    // Colores de barra de salud de X (amarillo brillante con borde negro)
                    g2.setColor(new Color(245, 220, 60));
                    g2.fillRect(barX, ty, barW, tickH - 1);
                    g2.setColor(new Color(255, 255, 180));
                    g2.fillRect(barX + 2, ty, 3, tickH - 1);
                } else {
                    g2.setColor(new Color(40, 40, 50));
                    g2.fillRect(barX, ty, barW, tickH - 1);
                }
            }

            // Ícono de X debajo de la barra de vida
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            g2.setColor(new Color(0, 220, 255));
            g2.drawString("X", barX + 3, barY + barH + 16);

            // Medidor de carga del X-Buster
            BusterWeapon buster = player.getBuster();
            int chargeLvl = buster.getChargeLevel();
            if (buster.isCharging()) {
                int chargeX = barX + 24;
                int chargeY = barY + barH - 8;
                g2.setFont(new Font("Monospaced", Font.BOLD, 10));
                if (chargeLvl == 2) {
                    g2.setColor(new Color(255, 100, 240));
                    g2.drawString("MAX CHARGE!", chargeX, chargeY);
                } else if (chargeLvl == 1) {
                    g2.setColor(new Color(60, 220, 255));
                    g2.drawString("CHARGING...", chargeX, chargeY);
                }
            }

            // Panel de ayuda e información de controles
            if (showHelp) {
                drawControlsOverlay(g2);
            }

            // Mensaje de Game Over o Victoria
            if (player.isDead()) {
                g2.setColor(new Color(0, 0, 0, 180));
                g2.fillRect(0, 0, W, H);
                g2.setColor(new Color(240, 50, 50));
                g2.setFont(new Font("Monospaced", Font.BOLD, 28));
                g2.drawString("MISSION FAILED", W / 2 - 130, H / 2 - 20);
                g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
                g2.setColor(Color.WHITE);
                g2.drawString("Presiona R para reiniciar la misión", W / 2 - 150, H / 2 + 15);
            }
        }

        private void drawControlsOverlay(Graphics2D g2) {
            int panelW = 480, panelH = 75;
            int px = W - panelW - 15, py = 12;

            g2.setColor(new Color(10, 15, 30, 200));
            g2.fillRoundRect(px, py, panelW, panelH, 8, 8);
            g2.setColor(new Color(0, 180, 255));
            g2.drawRoundRect(px, py, panelW, panelH, 8, 8);

            g2.setFont(new Font("Monospaced", Font.BOLD, 11));
            g2.setColor(new Color(255, 220, 80));
            g2.drawString("MECÁNICAS BASE DE MEGA MAN X", px + 10, py + 16);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.setColor(Color.WHITE);
            g2.drawString("• Movimiento: [A / D] o Flechas | Salto variable: [W / Espacio]", px + 10, py + 30);
            g2.drawString("• Dash terrestre: [C / Shift] (reduce altura para pasar bajo techos)", px + 10, py + 42);
            g2.drawString("• Dash-Jump: Salta durante el dash para conservar inercia máxima", px + 10, py + 54);
            g2.drawString("• Wall Slide & Kick: Deslízate en pared y presiona Salto | Disparo/Carga: [X]", px + 10, py + 66);
        }
    }

    // ---------- Entrada Principal ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Mega Man X - Base Mechanics Engine");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            GamePanel panel = new GamePanel();
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
}
