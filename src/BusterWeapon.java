import java.awt.*;
import java.util.List;

/**
 * X-Buster clásico de Mega Man X con soporte para carga de 2 niveles y efectos de aura.
 */
public class BusterWeapon implements Weapon {
    public static final int CHARGE_LVL1_THRESHOLD = 35; // ~0.6s
    public static final int CHARGE_LVL2_THRESHOLD = 80; // ~1.3s
    public static final int MAX_LEMONS_ON_SCREEN = 3;

    private int chargeTimer = 0;
    private boolean charging = false;
    private int soundLoopTimer = 0;

    public void startCharging() {
        charging = true;
    }

    public void stopCharging() {
        charging = false;
        chargeTimer = 0;
    }

    public void update(double playerCenterX, double playerCenterY, ParticleSystem ps) {
        if (!charging) {
            chargeTimer = 0;
            return;
        }

        chargeTimer++;

        int lvl = getChargeLevel();
        if (lvl > 0 && ps != null) {
            // Generar partículas de aura convergiendo hacia el jugador
            if (chargeTimer % 3 == 0) {
                ps.spawnChargeOrb(playerCenterX, playerCenterY, lvl);
            }

            // Sonido de zumbido de carga
            soundLoopTimer++;
            if (lvl == 1 && soundLoopTimer % 20 == 0) {
                SoundSystem.play("charge1");
            } else if (lvl == 2 && soundLoopTimer % 14 == 0) {
                SoundSystem.play("charge1");
            }
        }
    }

    public int getChargeLevel() {
        if (chargeTimer >= CHARGE_LVL2_THRESHOLD) return 2;
        if (chargeTimer >= CHARGE_LVL1_THRESHOLD) return 1;
        return 0;
    }

    public int getChargeTimer() {
        return chargeTimer;
    }

    public boolean isCharging() {
        return charging;
    }

    @Override
    public Bullet fire(double x, double y, boolean facingRight) {
        return fireWithCharge(x, y, facingRight, null, 0);
    }

    public Bullet fireWithCharge(double x, double y, boolean facingRight, ParticleSystem ps, int currentLemons) {
        int lvl = getChargeLevel();
        Bullet b = null;

        if (lvl == 2) {
            b = new Bullet.Charge2Bullet(x, y - 6, facingRight);
            SoundSystem.play("charge2");
            if (ps != null) {
                ps.spawnMuzzleFlash(x + (facingRight ? 10 : -10), y + 6, new Color(255, 120, 240));
            }
        } else if (lvl == 1) {
            b = new Bullet.Charge1Bullet(x, y - 4, facingRight);
            SoundSystem.play("charge1");
            if (ps != null) {
                ps.spawnMuzzleFlash(x + (facingRight ? 8 : -8), y + 4, new Color(80, 220, 255));
            }
        } else {
            // Limón normal: respetar límite de 3 en pantalla
            if (currentLemons < MAX_LEMONS_ON_SCREEN) {
                b = new Bullet.LemonBullet(x, y, facingRight);
                SoundSystem.play("lemon");
                if (ps != null) {
                    ps.spawnMuzzleFlash(x + (facingRight ? 6 : -6), y + 3, new Color(250, 255, 80));
                }
            }
        }

        // Reiniciar carga al disparar
        chargeTimer = 0;
        return b;
    }
}
