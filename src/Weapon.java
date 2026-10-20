import java.util.List;

/**
 * Interfaz para armas del jugador (patrón Strategy).
 */
public interface Weapon {
    Bullet fire(double x, double y, boolean facingRight);
}
