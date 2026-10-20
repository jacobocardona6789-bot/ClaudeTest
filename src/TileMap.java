import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapa de tiles del nivel y motor de colisiones AABB con resolución por ejes separados.
 */
public class TileMap {
    public static final int TILE_SIZE = 32;
    public static final int COLS = 100; // 3200 px
    public static final int ROWS = 25;  // 800 px

    public static final byte AIR = 0;
    public static final byte SOLID = 1;
    public static final byte PLATFORM = 2; // Atravesable desde abajo
    public static final byte SPIKES = 3;   // Daño al tocar
    public static final byte WALL = 4;      // Pared metálica vertical
    public static final byte BREAKABLE = 5; // Bloque rompible con disparo cargado

    private final byte[][] tiles = new byte[ROWS][COLS];
    private final List<Sign> signs = new ArrayList<>();

    public record Sign(int x, int y, String line1, String line2) {}

    public TileMap() {
        buildLevel();
    }

    public int getWidth() { return COLS * TILE_SIZE; }
    public int getHeight() { return ROWS * TILE_SIZE; }

    public byte getTile(int col, int row) {
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return SOLID;
        return tiles[row][col];
    }

    public void setTile(int col, int row, byte type) {
        if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
            tiles[row][col] = type;
        }
    }

    public boolean isSolid(int col, int row) {
        byte t = getTile(col, row);
        return t == SOLID || t == WALL || t == BREAKABLE;
    }

    public boolean isSpikes(int col, int row) {
        return getTile(col, row) == SPIKES;
    }

    public boolean isPlatform(int col, int row) {
        return getTile(col, row) == PLATFORM;
    }

    public boolean damageTile(int col, int row, int damage, ParticleSystem ps) {
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return false;
        if (tiles[row][col] == BREAKABLE && damage >= 3) {
            tiles[row][col] = AIR;
            if (ps != null) {
                ps.spawnExplosion(col * TILE_SIZE + 16, row * TILE_SIZE + 16, 8);
                ps.spawnDamageText(col * TILE_SIZE + 16, row * TILE_SIZE + 8, "CRUSH!", Color.CYAN);
            }
            SoundSystem.play("explode");
            return true;
        }
        return false;
    }

    private void buildLevel() {
        // 1. Suelo base en la fila 22 (y = 704)
        for (int c = 0; c < COLS; c++) {
            for (int r = 22; r < ROWS; r++) {
                tiles[r][c] = SOLID;
            }
        }

        // Bordes izquierdo y derecho del mundo
        for (int r = 0; r < ROWS; r++) {
            tiles[r][0] = SOLID;
            tiles[r][COLS - 1] = SOLID;
        }

        // ==========================================
        // ZONA 1: PISTA DE DESPEGUE (cols 0 a 18)
        // ==========================================
        signs.add(new Sign(100, 21 * TILE_SIZE - 20, "MOVIMIENTO", "A/D o Flechas: Caminar   W/Espacio: Saltar"));
        signs.add(new Sign(280, 21 * TILE_SIZE - 20, "DASH", "C / Shift: Dash instantáneo"));

        // ==========================================
        // ZONA 2: TÚNEL DE TECHO BAJO (cols 20 a 28)
        // Altura libre = 32px (1 tile libre). Mega Man X de pie (44px) NO cabe.
        // Debe usar DASH (altura 26px) para deslizarse por debajo.
        // ==========================================
        signs.add(new Sign(20 * TILE_SIZE - 10, 21 * TILE_SIZE - 20, "TECHO BAJO", "¡Usa DASH (C) para deslizarte!"));
        for (int c = 21; c <= 27; c++) {
            tiles[20][c] = SOLID; // Techo en fila 20 (fila 21 libre = 32px)
            tiles[19][c] = SOLID;
        }
        // Paredes bloqueando paso de pie
        tiles[20][20] = SOLID;
        tiles[20][28] = SOLID;

        // ==========================================
        // ZONA 3: ABISMO ANCHO (cols 31 a 39)
        // Fila 22 es vacío con pinchos abajo en fila 24.
        // Salto normal no llega. Requiere DASH-JUMP.
        // ==========================================
        signs.add(new Sign(30 * TILE_SIZE - 20, 21 * TILE_SIZE - 20, "ABISMO ANCHO", "¡DASH-JUMP (Dash + Salto) para cruzar!"));
        for (int c = 32; c <= 38; c++) {
            tiles[22][c] = AIR;
            tiles[23][c] = AIR;
            tiles[24][c] = SPIKES; // Pinchos en el fondo
        }

        // ==========================================
        // ZONA 4: POZO VERTICAL / CHIMENEA (cols 42 a 50)
        // Gran pared vertical desde fila 7 hasta fila 22.
        // El jugador debe escalar usando WALL SLIDE y WALL KICK.
        // ==========================================
        signs.add(new Sign(41 * TILE_SIZE - 30, 21 * TILE_SIZE - 20, "POZO VERTICAL", "Sostén hacia la pared para WALL SLIDE"));
        signs.add(new Sign(41 * TILE_SIZE - 30, 21 * TILE_SIZE - 4, "WALL KICK", "Presiona SALTAR en la pared para impulsarte"));

        // Pared izquierda del pozo (col 43, filas 7 a 21)
        for (int r = 8; r <= 21; r++) {
            tiles[r][43] = WALL;
        }
        // Pared derecha del pozo (col 47, filas 8 a 18)
        for (int r = 8; r <= 18; r++) {
            tiles[r][47] = WALL;
        }
        // Piso intermedio arriba del pozo (fila 8, cols 48 a 70)
        for (int c = 48; c <= 75; c++) {
            tiles[8][c] = SOLID;
        }

        // ==========================================
        // ZONA 5: PLATAFORMAS FLOTANTES Y BLOQUES DESTRUIBLES (cols 52 a 75, nivel superior)
        // ==========================================
        signs.add(new Sign(50 * TILE_SIZE, 7 * TILE_SIZE - 20, "X-BUSTER", "X o Clic: Disparar. Mantenlo: ¡CARGA!"));
        signs.add(new Sign(62 * TILE_SIZE, 7 * TILE_SIZE - 20, "BLOQUES", "¡Disparo cargado destruye bloques!"));

        // Plataformas semi-sólidas
        for (int c = 53; c <= 56; c++) tiles[6][c] = PLATFORM;
        for (int c = 59; c <= 62; c++) tiles[5][c] = PLATFORM;

        // Muro de bloques destructibles
        tiles[6][64] = BREAKABLE;
        tiles[7][64] = BREAKABLE;
        tiles[6][65] = BREAKABLE;
        tiles[7][65] = BREAKABLE;

        // Caída hacia la arena final (col 76 baja a la fila 22)
        for (int r = 9; r <= 21; r++) {
            tiles[r][75] = WALL;
        }

        // ==========================================
        // ZONA 6: ARENA DE COMBATE FINAL (cols 78 a 98)
        // ==========================================
        signs.add(new Sign(77 * TILE_SIZE, 21 * TILE_SIZE - 20, "ARENA DE PRUEBAS", "Objetivo de combate con barra de salud"));
        for (int c = 82; c <= 86; c++) tiles[18][c] = PLATFORM;
        for (int c = 90; c <= 94; c++) tiles[15][c] = PLATFORM;
    }

    public List<Sign> getSigns() {
        return signs;
    }

    public void draw(Graphics2D g, int viewX, int viewY, int viewW, int viewH) {
        int startCol = Math.max(0, viewX / TILE_SIZE);
        int endCol = Math.min(COLS - 1, (viewX + viewW) / TILE_SIZE + 1);
        int startRow = Math.max(0, viewY / TILE_SIZE);
        int endRow = Math.min(ROWS - 1, (viewY + viewH) / TILE_SIZE + 1);

        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                byte type = tiles[r][c];
                if (type == AIR) continue;
                int tx = c * TILE_SIZE;
                int ty = r * TILE_SIZE;

                if (type == SOLID) {
                    drawSolidTile(g, tx, ty, c, r);
                } else if (type == WALL) {
                    drawWallTile(g, tx, ty, r);
                } else if (type == PLATFORM) {
                    drawPlatformTile(g, tx, ty);
                } else if (type == SPIKES) {
                    drawSpikesTile(g, tx, ty);
                } else if (type == BREAKABLE) {
                    drawBreakableTile(g, tx, ty);
                }
            }
        }

        // Dibujar letreros informativos
        for (Sign s : signs) {
            if (s.x + 300 >= viewX && s.x - 50 <= viewX + viewW) {
                drawSign(g, s);
            }
        }
    }

    private void drawSolidTile(Graphics2D g, int x, int y, int c, int r) {
        // Bloque de autopista/laboratorio estilo Mega Man X
        g.setColor(new Color(45, 50, 70));
        g.fillRect(x, y, TILE_SIZE, TILE_SIZE);

        // Borde superior biselado metálico
        g.setColor(new Color(85, 95, 125));
        g.fillRect(x, y, TILE_SIZE, 3);
        g.setColor(new Color(25, 28, 40));
        g.fillRect(x, y + TILE_SIZE - 2, TILE_SIZE, 2);

        // Remaches en las esquinas
        g.setColor(new Color(110, 125, 160));
        g.fillRect(x + 3, y + 3, 2, 2);
        g.fillRect(x + TILE_SIZE - 5, y + 3, 2, 2);

        // Línea divisoria de panel
        g.setColor(new Color(35, 38, 55));
        g.drawRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
    }

    private void drawWallTile(Graphics2D g, int x, int y, int r) {
        // Pared con estrías verticales para escalar
        g.setColor(new Color(50, 60, 80));
        g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        g.setColor(new Color(100, 120, 150));
        g.fillRect(x + 4, y, 4, TILE_SIZE);
        g.fillRect(x + TILE_SIZE - 8, y, 4, TILE_SIZE);
        // Franja de advertencia cada 2 bloques
        if (r % 2 == 0) {
            g.setColor(new Color(230, 180, 40));
            g.fillRect(x + 12, y + 6, 8, 4);
        }
    }

    private void drawPlatformTile(Graphics2D g, int x, int y) {
        // Viga metálica semi-sólida
        g.setColor(new Color(70, 85, 115));
        g.fillRect(x, y, TILE_SIZE, 10);
        g.setColor(new Color(120, 150, 195));
        g.fillRect(x, y, TILE_SIZE, 2);
        g.setColor(new Color(0, 200, 255));
        g.fillRect(x + 4, y + 4, TILE_SIZE - 8, 2); // Línea de neón cian
    }

    private void drawSpikesTile(Graphics2D g, int x, int y) {
        g.setColor(new Color(220, 40, 40));
        for (int i = 0; i < 4; i++) {
            int sx = x + i * 8;
            g.fillPolygon(new int[]{sx, sx + 4, sx + 8}, new int[]{y + TILE_SIZE, y + 8, y + TILE_SIZE}, 3);
        }
    }

    private void drawBreakableTile(Graphics2D g, int x, int y) {
        // Bloque agrietado con símbolo de energía
        g.setColor(new Color(90, 60, 40));
        g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        g.setColor(new Color(160, 110, 70));
        g.drawRect(x + 1, y + 1, TILE_SIZE - 2, TILE_SIZE - 2);
        // Grieta
        g.setColor(new Color(255, 200, 100));
        g.drawLine(x + 6, y + 6, x + 16, y + 16);
        g.drawLine(x + 16, y + 16, x + 24, y + 10);
        g.drawLine(x + 16, y + 16, x + 12, y + 26);
    }

    private void drawSign(Graphics2D g, Sign s) {
        g.setColor(new Color(20, 30, 50, 220));
        g.fillRoundRect(s.x, s.y, 250, 32, 8, 8);
        g.setColor(new Color(80, 180, 255));
        g.drawRoundRect(s.x, s.y, 250, 32, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(255, 220, 80));
        g.drawString("▼ " + s.line1, s.x + 8, s.y + 13);
        g.setFont(new Font("Monospaced", Font.PLAIN, 9));
        g.setColor(Color.WHITE);
        g.drawString(s.line2, s.x + 8, s.y + 26);
    }
}
