/**
 * Interfaz que define entidades capaces de recibir daño.
 */
public interface Damageable {
    void takeDamage(int amount);
    boolean isDead();
    default boolean isAlive() {
        return !isDead();
    }
    int getHealth();
    int getMaxHealth();
}
