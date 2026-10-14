import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.Set;

/**
 * Cámara que sigue al jugador en un mundo MÁS GRANDE que la ventana.
 *
 * Ideas clave:
 *  1) La cámara es solo una posición (x, y) del mundo que se ve en la esquina superior izquierda de la pantalla.
 *  2) Cada tick, la cámara se acerca a la posición del jugador (follow).
 *  3) La cámara se limita (clamp) para no mostrar nada fuera del mundo.
 *  4) Al dibujar: translate(-cam.x, -cam.y) -> dibujar mundo -> restaurar -> dibujar HUD.
 *
 * Controles: A / D o flechas para moverse, W / flecha arriba / espacio para saltar.
 */
public class CameraDemo {

    // ---------- Constantes ----------
    static final int W = 800, H = 450;            // tamaño de la ventana (lo que ve la cámara)
    static final int WORLD_W = 3200, WORLD_H = 450; // tamaño total del nivel
    static final int FLOOR = 380;
    static final int TILE = 64;

    // ---------- Cámara ----------
    static class Camera {
        private double x, y;                      // esquina superior izquierda de lo que se ve
        private final int viewW, viewH, worldW, worldH;

        Camera(int viewW, int viewH, int worldW, int worldH) {
            this.viewW = viewW;
            this.viewH = viewH;
            this.worldW = worldW;
            this.worldH = worldH;
        }

        /**
         * Acerca la cámara al objetivo.
         * smoothing = 1.0 -> pegada al jugador (rígida)
         * smoothing = 0.1 -> lo sigue con un poco de retraso (más suave)
         */
        void follow(double targetX, double targetY, double smoothing) {
            double goalX = targetX - viewW / 2.0;     // centrar al objetivo en la pantalla
            double goalY = targetY - viewH / 2.0;
            x += (goalX - x) * smoothing;
            y += (goalY - y) * smoothing;
            clamp();
        }

        /** Coloca la cámara de golpe (útil al empezar un nivel, sin "viaje" inicial). */
        void snapTo(double targetX, double targetY) {
            x = targetX - viewW / 2.0;
            y = targetY - viewH / 2.0;
            clamp();
        }

        /** Evita que se vea fuera del mundo. */
        private void clamp() {
            x = Math.max(0, Math.min(x, worldW - viewW));
            y = Math.max(0, Math.min(y, worldH - viewH));
        }

        // Posición entera: evita que sprites y píxeles "tiemblen" al mover la cámara con decimales
        int ix() { return (int) Math.round(x); }
        int iy() { return (int) Math.round(y); }
    }

    // ---------- Teclado ----------
    static class Input extends KeyAdapter {
        private final Set<Integer> down = new HashSet<>();

        @Override public void keyPressed(KeyEvent e)  { down.add(e.getKeyCode()); }
        @Override public void keyReleased(KeyEvent e) { down.remove(e.getKeyCode()); }

        boolean isDown(int... codes) {
            for (int c : codes) if (down.contains(c)) return true;
            return false;
        }
    }

    // ---------- Jugador (coordenadas del MUNDO, no de la pantalla) ----------
    static class Player {
        double x, y, vx, vy;
        final int w = 40, h = 60;
        boolean onGround = true;

        Player(double x) {
            this.x = x;
            this.y = FLOOR - h;
        }

        double centerX() { return x + w / 2.0; }
        double centerY() { return y + h / 2.0; }

        void update(Input in) {
            vx = 0;
            if (in.isDown(KeyEvent.VK_A, KeyEvent.VK_LEFT))  vx = -5;
            if (in.isDown(KeyEvent.VK_D, KeyEvent.VK_RIGHT)) vx = 5;
            if (in.isDown(KeyEvent.VK_W, KeyEvent.VK_UP, KeyEvent.VK_SPACE) && onGround) {
                vy = -12;
                onGround = false;
            }

            vy += 0.55;
            x += vx;
            y += vy;

            if (y + h >= FLOOR) {
                y = FLOOR - h;
                vy = 0;
                onGround = true;
            }
            x = Math.max(0, Math.min(WORLD_W - w, x));   // límites del MUNDO
        }

        void draw(Graphics2D g) {
            g.setColor(Color.CYAN);
            g.fillRect((int) x, (int) y, w, h);
            g.setColor(Color.WHITE);
            g.fillRect((int) x + 24, (int) y + 12, 8, 8);   // "ojo" para ver hacia dónde mira
        }
    }

    // ---------- Panel ----------
    static class GamePanel extends JPanel {
        private static final double NS_PER_TICK = 1_000_000_000.0 / 60.0;

        private final Input input = new Input();
        private final Player player = new Player(100);
        private final Camera camera = new Camera(W, H, WORLD_W, WORLD_H);

        private long lastTime = System.nanoTime();
        private double accumulator = 0;
        private int tilesDrawn = 0;

        GamePanel() {
            setPreferredSize(new Dimension(W, H));
            setFocusable(true);
            addKeyListener(input);

            camera.snapTo(player.centerX(), player.centerY());

            // Paso fijo de 60 ticks/s aunque el Timer de Swing dispare irregular
            new javax.swing.Timer(2, e -> {
                long now = System.nanoTime();
                accumulator += (now - lastTime) / NS_PER_TICK;
                lastTime = now;
                if (accumulator > 5) accumulator = 5;

                boolean updated = false;
                while (accumulator >= 1) {
                    player.update(input);
                    // La cámara se actualiza DESPUÉS del jugador, así nunca va un tick atrasada
                    camera.follow(player.centerX(), player.centerY(), 0.12);
                    accumulator--;
                    updated = true;
                }
                if (updated) repaint();
            }).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1) FONDO (coordenadas de pantalla): cielo + montañas con parallax
            g2.setPaint(new GradientPaint(0, 0, new Color(15, 20, 60), 0, H, new Color(60, 40, 90)));
            g2.fillRect(0, 0, W, H);
            drawParallaxMountains(g2);

            // 2) MUNDO (coordenadas del mundo): se desplaza todo con translate
            AffineTransform original = g2.getTransform();       // guardar
            g2.translate(-camera.ix(), -camera.iy());

            drawWorld(g2);
            player.draw(g2);

            g2.setTransform(original);                          // restaurar

            // 3) HUD (coordenadas de pantalla): no se mueve con la cámara
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.drawString(String.format("Jugador x: %.0f   Cámara x: %d   Tiles dibujados: %d",
                    player.x, camera.ix(), tilesDrawn), 20, 25);
            g2.drawString("A/D o flechas: mover   W/Arriba/Espacio: saltar", 20, 45);
        }

        /** Montañas que se mueven más lento que el mundo (sensación de profundidad). */
        private void drawParallaxMountains(Graphics2D g2) {
            final int period = 300;
            int shift = (int) (camera.x * 0.3) % period;        // 0.3 = se mueven al 30 % de la velocidad
            g2.setColor(new Color(30, 25, 70));
            for (int i = -1; i <= W / period + 1; i++) {
                int mx = i * period - shift;
                g2.fillPolygon(new int[]{mx, mx + period / 2, mx + period},
                        new int[]{FLOOR, FLOOR - 170, FLOOR}, 3);
            }
        }

        /** Dibuja SOLO los tiles que caen dentro de la pantalla (no los 50 del mundo). */
        private void drawWorld(Graphics2D g2) {
            int firstTile = Math.max(0, camera.ix() / TILE);
            int lastTile = Math.min(WORLD_W / TILE - 1, (camera.ix() + W) / TILE + 1);
            tilesDrawn = 0;

            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            for (int t = firstTile; t <= lastTile; t++) {
                int tx = t * TILE;
                g2.setColor(t % 2 == 0 ? new Color(90, 90, 100) : new Color(75, 75, 85));
                g2.fillRect(tx, FLOOR, TILE, H - FLOOR);
                tilesDrawn++;

                // marcas para ver que el mundo se mueve
                if (t % 5 == 0) {
                    g2.setColor(new Color(255, 200, 80));
                    g2.fillRect(tx, FLOOR - 90, 6, 90);
                    g2.drawString("x=" + tx, tx + 10, FLOOR - 75);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Camera demo");
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