import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Carga de sprites y generación de gráficos auténticos de Mega Man X (estilo SNES).
 */
public class Sprites {

    // Paleta oficial de Mega Man X (SNES)
    public static final int C_OUTLINE    = 0xFF181818;
    public static final int C_CYAN_LIGHT = 0xFF78D8F0;
    public static final int C_CYAN_MED   = 0xFF50A0F0;
    public static final int C_BLUE_LIGHT = 0xFF0080F8;
    public static final int C_BLUE_MED   = 0xFF0040F0;
    public static final int C_BLUE_DARK  = 0xFF1858B0;
    public static final int C_NAVY       = 0xFF203080;
    public static final int C_GEM_RED    = 0xFFF04010;
    public static final int C_SKIN_LIGHT = 0xFFF8B080;
    public static final int C_SKIN_MED   = 0xFFB86048;
    public static final int C_SKIN_DARK  = 0xFF804020;
    public static final int C_WHITE      = 0xFFF0F0F0;
    public static final int C_GRAY_LIGHT = 0xFFE0E0E0;
    public static final int C_GRAY_MED   = 0xFF989898;

    // Fotogramas del jugador
    public static BufferedImage[] runFrames;
    public static BufferedImage[] runShootFrames;
    public static BufferedImage idleFrame;
    public static BufferedImage idleBlinkFrame;
    public static BufferedImage idleShootFrame;
    public static BufferedImage jumpFrame;
    public static BufferedImage jumpShootFrame;
    public static BufferedImage fallFrame;
    public static BufferedImage fallShootFrame;
    public static BufferedImage dashFrame;
    public static BufferedImage dashShootFrame;
    public static BufferedImage wallSlideFrame;
    public static BufferedImage wallSlideShootFrame;

    // Proyectiles
    public static BufferedImage bulletLemon;
    public static BufferedImage bulletCharge1;
    public static BufferedImage bulletCharge2;
    public static BufferedImage bulletEnemy;

    // Enemigos
    public static BufferedImage mettaurHide;
    public static BufferedImage[] mettaurWalk;
    public static BufferedImage[] gunnerFrames;

    private static boolean initialized = false;

    public static synchronized void init() {
        if (initialized) return;
        loadRunFrames();
        generateExtraPlayerFrames();
        generateBullets();
        generateEnemies();
        initialized = true;
    }

    private static void loadRunFrames() {
        BufferedImage sheet = null;
        try {
            InputStream in = Sprites.class.getResourceAsStream("/XWalkDefault.png");
            if (in != null) {
                sheet = ImageIO.read(in);
            }
        } catch (Exception ignored) {}

        if (sheet == null) {
            File f = new File("src/XWalkDefault.png");
            if (!f.exists()) f = new File("XWalkDefault.png");
            if (f.exists()) {
                try {
                    sheet = ImageIO.read(f);
                } catch (IOException e) {
                    System.err.println("No se pudo cargar XWalkDefault.png: " + e.getMessage());
                }
            }
        }

        int[][] bounds = {
            {8, 27}, {39, 61}, {69, 100}, {102, 135}, {140, 165},
            {177, 198}, {209, 233}, {240, 269}, {272, 305}, {309, 337}
        };

        runFrames = new BufferedImage[10];
        runShootFrames = new BufferedImage[10];

        if (sheet != null) {
            int h = sheet.getHeight();
            for (int i = 0; i < 10; i++) {
                int x1 = bounds[i][0];
                int x2 = bounds[i][1];
                int w = x2 - x1 + 1;
                runFrames[i] = sheet.getSubimage(x1, 0, w, h);
                runShootFrames[i] = attachBuster(runFrames[i], 12);
            }
        } else {
            for (int i = 0; i < 10; i++) {
                runFrames[i] = createFallbackSprite(28, 35, Color.CYAN);
                runShootFrames[i] = attachBuster(runFrames[i], 12);
            }
        }
    }

    private static BufferedImage createFallbackSprite(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(4, 4, w - 8, h - 8);
        g.setColor(Color.WHITE);
        g.fillRect(w - 10, 8, 4, 4);
        g.dispose();
        return img;
    }

    private static void generateExtraPlayerFrames() {
        // Idle basado en fotograma 0 de XWalk
        idleFrame = copyImage(runFrames[0]);
        idleBlinkFrame = copyImage(runFrames[0]);
        for (int y = 9; y <= 11; y++) {
            for (int x = 0; x < idleBlinkFrame.getWidth(); x++) {
                int c = idleBlinkFrame.getRGB(x, y);
                if ((c & 0x00FFFFFF) == (C_WHITE & 0x00FFFFFF)) {
                    idleBlinkFrame.setRGB(x, y, C_SKIN_LIGHT);
                }
            }
        }
        idleShootFrame = attachBuster(idleFrame, 12);

        // Jump y Fall
        jumpFrame = copyImage(runFrames[3]);
        jumpShootFrame = attachBuster(jumpFrame, 10);

        fallFrame = copyImage(runFrames[8]);
        fallShootFrame = attachBuster(fallFrame, 12);

        // Dash: postura baja e inclinada con propulsores en botas
        dashFrame = createDashFrame();
        dashShootFrame = attachBuster(dashFrame, 12);

        // Wall Slide: deslizarse contra pared
        wallSlideFrame = createWallSlideFrame();
        wallSlideShootFrame = attachBuster(wallSlideFrame, 14);
    }

    private static BufferedImage copyImage(BufferedImage src) {
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage attachBuster(BufferedImage base, int armY) {
        int extraW = 10;
        BufferedImage out = new BufferedImage(base.getWidth() + extraW, base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(base, 0, 0, null);

        int bx = base.getWidth() - 3;
        int by = armY;

        // Cañón buster
        g.setColor(new Color(C_OUTLINE, true));
        g.fillRect(bx, by - 1, 9, 8);
        g.setColor(new Color(C_BLUE_MED, true));
        g.fillRect(bx + 1, by, 7, 6);
        g.setColor(new Color(C_CYAN_LIGHT, true));
        g.fillRect(bx + 1, by + 1, 7, 2);
        g.setColor(new Color(C_BLUE_DARK, true));
        g.fillRect(bx + 1, by + 4, 7, 2);
        // Boquilla de energía amarilla
        g.setColor(new Color(255, 230, 60));
        g.fillRect(bx + 7, by + 1, 2, 4);

        g.dispose();
        return out;
    }

    private static BufferedImage createDashFrame() {
        int w = 34, h = 35;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Casco inclinado
        g.setColor(new Color(C_OUTLINE, true));
        g.fillRoundRect(12, 8, 16, 13, 6, 6);
        g.setColor(new Color(C_BLUE_LIGHT, true));
        g.fillRoundRect(13, 9, 14, 11, 5, 5);
        g.setColor(new Color(C_CYAN_LIGHT, true));
        g.fillRect(16, 9, 8, 3);
        // Rostro
        g.setColor(new Color(C_SKIN_LIGHT, true));
        g.fillRect(20, 12, 6, 6);
        g.setColor(new Color(C_WHITE, true));
        g.fillRect(22, 13, 2, 3);
        g.setColor(new Color(C_GEM_RED, true));
        g.fillRect(19, 9, 3, 2);

        // Pechera
        g.setColor(new Color(C_OUTLINE, true));
        g.fillRect(8, 17, 16, 9);
        g.setColor(new Color(C_WHITE, true));
        g.fillRect(12, 18, 10, 7);
        g.setColor(new Color(C_GRAY_LIGHT, true));
        g.fillRect(14, 20, 6, 4);

        // Piernas extendidas bajas
        g.setColor(new Color(C_OUTLINE, true));
        g.fillRoundRect(2, 23, 26, 12, 6, 6);
        g.setColor(new Color(C_BLUE_DARK, true));
        g.fillRoundRect(3, 24, 24, 10, 5, 5);
        g.setColor(new Color(C_CYAN_MED, true));
        g.fillRect(8, 25, 12, 4);

        // Propulsores en los talones
        g.setColor(new Color(255, 255, 210));
        g.fillOval(0, 24, 5, 8);
        g.setColor(new Color(110, 230, 255));
        g.fillOval(1, 26, 3, 4);

        g.dispose();
        return img;
    }

    private static BufferedImage createWallSlideFrame() {
        int w = 24, h = 35;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Brazo apoyado en la pared (a la izquierda x=0)
        g.setColor(new Color(C_OUTLINE, true));
        g.fillRect(0, 10, 8, 6);
        g.setColor(new Color(C_WHITE, true));
        g.fillRect(1, 11, 6, 4);

        // Casco
        g.setColor(new Color(C_OUTLINE, true));
        g.fillRoundRect(6, 4, 15, 15, 6, 6);
        g.setColor(new Color(C_BLUE_LIGHT, true));
        g.fillRoundRect(7, 5, 13, 13, 5, 5);
        g.setColor(new Color(C_CYAN_LIGHT, true));
        g.fillRect(9, 5, 8, 3);
        // Rostro
        g.setColor(new Color(C_SKIN_LIGHT, true));
        g.fillRect(12, 9, 6, 6);
        g.setColor(new Color(C_WHITE, true));
        g.fillRect(14, 10, 2, 3);
        g.setColor(new Color(C_GEM_RED, true));
        g.fillRect(11, 6, 3, 2);

        // Torso
        g.setColor(new Color(C_OUTLINE, true));
        g.fillRect(5, 16, 14, 9);
        g.setColor(new Color(C_WHITE, true));
        g.fillRect(6, 17, 12, 7);

        // Botas apoyadas en pared a la izquierda
        g.setColor(new Color(C_OUTLINE, true));
        g.fillRoundRect(0, 24, 18, 11, 4, 4);
        g.setColor(new Color(C_BLUE_MED, true));
        g.fillRoundRect(1, 25, 16, 9, 3, 3);
        g.setColor(new Color(C_CYAN_LIGHT, true));
        g.fillRect(4, 26, 10, 3);

        g.dispose();
        return img;
    }

    private static void generateBullets() {
        // Limón normal
        bulletLemon = new BufferedImage(10, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = bulletLemon.createGraphics();
        g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g1.setColor(new Color(40, 180, 40));
        g1.fillOval(0, 0, 10, 8);
        g1.setColor(new Color(250, 255, 80));
        g1.fillOval(1, 1, 8, 6);
        g1.setColor(Color.WHITE);
        g1.fillOval(2, 2, 4, 3);
        g1.dispose();

        // Disparo semicarregado (Charge 1)
        bulletCharge1 = new BufferedImage(18, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = bulletCharge1.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0, 100, 240, 180));
        g2.fillOval(0, 0, 18, 16);
        g2.setColor(new Color(60, 220, 255));
        g2.fillOval(2, 2, 14, 12);
        g2.setColor(new Color(200, 245, 255));
        g2.fillOval(4, 4, 10, 8);
        g2.setColor(Color.WHITE);
        g2.fillOval(6, 5, 5, 4);
        g2.dispose();

        // Disparo cargado completo (Charge 2)
        bulletCharge2 = new BufferedImage(38, 26, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g3 = bulletCharge2.createGraphics();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.setColor(new Color(240, 80, 220, 140));
        g3.fillRoundRect(0, 0, 38, 26, 12, 12);
        g3.setColor(new Color(0, 160, 255, 220));
        g3.fillRoundRect(3, 2, 32, 22, 10, 10);
        g3.setColor(new Color(120, 240, 255));
        g3.fillRoundRect(6, 4, 26, 18, 8, 8);
        g3.setColor(Color.WHITE);
        g3.fillRoundRect(10, 7, 18, 12, 6, 6);
        g3.setColor(new Color(255, 200, 255));
        g3.drawOval(4, 2, 14, 22);
        g3.drawOval(18, 2, 14, 22);
        g3.dispose();

        // Disparo enemigo
        bulletEnemy = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ge = bulletEnemy.createGraphics();
        ge.setColor(new Color(240, 50, 30));
        ge.fillOval(0, 0, 8, 8);
        ge.setColor(new Color(255, 200, 120));
        ge.fillOval(2, 2, 4, 4);
        ge.dispose();
    }

    private static void generateEnemies() {
        // Mettaur escondido
        mettaurHide = new BufferedImage(24, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mettaurHide.createGraphics();
        g.setColor(new Color(C_OUTLINE, true));
        g.fillRoundRect(1, 2, 22, 16, 8, 8);
        g.setColor(new Color(250, 200, 20));
        g.fillRoundRect(2, 3, 20, 14, 6, 6);
        g.setColor(new Color(255, 230, 80));
        g.fillRect(4, 4, 16, 4);
        g.setColor(Color.WHITE);
        g.fillRect(10, 6, 4, 8);
        g.fillRect(8, 8, 8, 4);
        g.setColor(new Color(140, 140, 140));
        g.fillRect(3, 16, 18, 2);
        g.dispose();

        // Mettaur caminando
        mettaurWalk = new BufferedImage[2];
        for (int i = 0; i < 2; i++) {
            mettaurWalk[i] = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gw = mettaurWalk[i].createGraphics();
            gw.setColor(new Color(C_OUTLINE, true));
            gw.fillRoundRect(1, 0, 22, 14, 6, 6);
            gw.setColor(new Color(250, 200, 20));
            gw.fillRoundRect(2, 1, 20, 12, 5, 5);
            gw.setColor(Color.WHITE);
            gw.fillRect(10, 3, 4, 6);
            gw.fillRect(8, 5, 8, 3);
            gw.setColor(new Color(20, 20, 20));
            gw.fillRect(4, 12, 16, 7);
            gw.setColor(Color.WHITE);
            gw.fillRect(7, 13, 3, 5);
            gw.fillRect(14, 13, 3, 5);
            gw.setColor(Color.BLACK);
            gw.fillRect(8, 14, 2, 3);
            gw.fillRect(15, 14, 2, 3);
            gw.setColor(new Color(240, 180, 10));
            if (i == 0) {
                gw.fillRect(3, 19, 7, 5);
                gw.fillRect(14, 19, 7, 5);
            } else {
                gw.fillRect(4, 18, 7, 5);
                gw.fillRect(13, 20, 7, 4);
            }
            gw.dispose();
        }

        // Gunner volador
        gunnerFrames = new BufferedImage[2];
        for (int i = 0; i < 2; i++) {
            gunnerFrames[i] = new BufferedImage(28, 22, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gf = gunnerFrames[i].createGraphics();
            gf.setColor(new Color(C_OUTLINE, true));
            gf.fillRoundRect(4, 2, 20, 16, 6, 6);
            gf.setColor(new Color(200, 40, 40));
            gf.fillRoundRect(5, 3, 18, 14, 5, 5);
            gf.setColor(new Color(120, 30, 30));
            gf.fillRect(5, 12, 18, 5);
            gf.setColor(i == 0 ? new Color(80, 240, 255) : new Color(255, 255, 100));
            gf.fillOval(10, 6, 8, 8);
            gf.setColor(Color.WHITE);
            gf.fillOval(12, 7, 3, 3);
            gf.setColor(new Color(100, 100, 100));
            gf.fillRect(8, 18, 4, 3);
            gf.fillRect(16, 18, 4, 3);
            gf.setColor(i == 0 ? new Color(255, 180, 40) : new Color(255, 230, 100));
            gf.fillPolygon(new int[]{8, 10, 12}, new int[]{21, 21 + (i == 0 ? 3 : 5), 21}, 3);
            gf.fillPolygon(new int[]{16, 18, 20}, new int[]{21, 21 + (i == 0 ? 5 : 3), 21}, 3);
            gf.dispose();
        }
    }

    public static void drawFlipped(Graphics2D g, BufferedImage img, int x, int y, boolean facingRight) {
        if (img == null) return;
        if (facingRight) {
            g.drawImage(img, x, y, null);
        } else {
            g.drawImage(img, x + img.getWidth(), y, -img.getWidth(), img.getHeight(), null);
        }
    }
}
