import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Jugador Mega Man X con todas las mecánicas base replicadas fielmente:
 *  - Correr con ciclo de 10 fotogramas
 *  - Salto variable con coyote time y jump buffer
 *  - Dash terrestre con reducción de hitbox y polvo
 *  - Dash-Jump (conservación de inercia del dash en el aire)
 *  - Wall Slide (deslizamiento por pared con fricción y chispas)
 *  - Wall Kick (patada en pared y dash wall kick escalable)
 *  - X-Buster con carga de 2 niveles y disparo en cualquier estado
 *  - Invencibilidad y retroceso al recibir daño
 */
public class Player extends Entity implements Damageable {

    // Constantes de físicas estilo Mega Man X (a 60 ticks/segundo)
    public static final double WALK_SPEED = 2.8;
    public static final double DASH_SPEED = 6.0;
    public static final int DASH_DURATION = 26; // ~0.43 segundos

    public static final double JUMP_VELOCITY = -10.5;
    public static final double GRAVITY = 0.52;
    public static final double TERMINAL_VELOCITY = 8.5;
    public static final double WALL_SLIDE_SPEED = 1.8;
    public static final double WALL_KICK_VY = -10.2;
    public static final double WALL_KICK_VX = 4.6;
    public static final double DASH_WALL_KICK_VX = 6.2;

    // Dimensiones de hitbox
    public static final int STAND_W = 26;
    public static final int STAND_H = 42;
    public static final int DASH_W = 30;
    public static final int DASH_H = 26; // Reduce altura para pasar bajo techos bajos

    // Salud
    private int health = 16;
    private final int maxHealth = 16;
    private int invulnerableTimer = 0;
    private int flinchTimer = 0;

    // Estado y movimiento
    private PlayerState state = PlayerState.IDLE;
    private boolean facingRight = true;
    private boolean onGround = false;
    private boolean touchingLeftWall = false;
    private boolean touchingRightWall = false;

    // Dash
    private boolean isDashing = false;
    private int dashTimer = 0;
    private boolean isDashJump = false; // Conserva inercia en el aire

    // Wall slide / kick
    private boolean isWallSliding = false;
    private int wallKickLockTimer = 0; // Breve bloqueo de dirección tras patada en pared
    private int wallSlideDir = 0;      // -1: pared a la izquierda, 1: pared a la derecha

    // Jump Buffers
    private int coyoteTimer = 0;
    private int jumpBufferTimer = 0;

    // Disparo y Buster
    private final BusterWeapon buster = new BusterWeapon();
    private int shootAnimTimer = 0;

    // Animación
    private int runAnimIndex = 0;
    private int runAnimTimer = 0;
    private int idleBlinkTimer = 0;

    // Detección de doble toque para Dash
    private int lastTapLeft = 0;
    private int lastTapRight = 0;

    public Player(double x, double y) {
        super(x, y, STAND_W, STAND_H);
    }

    public PlayerState getState() { return state; }
    public boolean isFacingRight() { return facingRight; }
    public boolean isOnGround() { return onGround; }
    public boolean isDashing() { return isDashing; }
    public boolean isWallSliding() { return isWallSliding; }
    public BusterWeapon getBuster() { return buster; }

    @Override
    public int getHealth() { return health; }
    @Override
    public int getMaxHealth() { return maxHealth; }
    @Override
    public boolean isDead() { return health <= 0; }

    @Override
    public void takeDamage(int amount) {
        if (amount <= 0 || invulnerableTimer > 0 || isDead()) return;

        health = Math.max(0, health - amount);
        invulnerableTimer = 60; // 1 segundo de i-frames
        flinchTimer = 14;       // Retroceso breve

        // Interrumpir dash o wall slide
        isDashing = false;
        isDashJump = false;
        isWallSliding = false;

        // Empuje hacia atrás
        vy = -3.5;
        vx = facingRight ? -2.5 : 2.5;

        SoundSystem.play("hurt");
    }

    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
    }

    @Override
    public void update(TileMap map) {
        // La actualización completa del jugador requiere entrada y contexto del juego
        // Para actualización autónoma básica:
        y += vy;
        x += vx;
    }

    /**
     * Actualiza la lógica completa del jugador procesando la entrada, el mapa y las armas.
     */
    public void update(CameraDemo.Input in, TileMap map, List<Bullet> bullets, ParticleSystem ps, Camera camera) {
        if (isDead()) {
            state = PlayerState.DEAD;
            return;
        }

        // Temporizadores de daño y animación
        if (invulnerableTimer > 0) invulnerableTimer--;
        if (flinchTimer > 0) flinchTimer--;
        if (shootAnimTimer > 0) shootAnimTimer--;
        if (wallKickLockTimer > 0) wallKickLockTimer--;

        // Entradas del teclado
        boolean leftDown = in.isDown(KeyEvent.VK_A, KeyEvent.VK_LEFT);
        boolean rightDown = in.isDown(KeyEvent.VK_D, KeyEvent.VK_RIGHT);
        boolean jumpPressed = in.wasPressed(KeyEvent.VK_W, KeyEvent.VK_UP, KeyEvent.VK_SPACE, KeyEvent.VK_Z);
        boolean jumpHeld = in.isDown(KeyEvent.VK_W, KeyEvent.VK_UP, KeyEvent.VK_SPACE, KeyEvent.VK_Z);
        boolean dashPressed = in.wasPressed(KeyEvent.VK_C, KeyEvent.VK_SHIFT, KeyEvent.VK_J);
        boolean dashHeld = in.isDown(KeyEvent.VK_C, KeyEvent.VK_SHIFT, KeyEvent.VK_J);
        boolean shootPressed = in.wasPressed(KeyEvent.VK_X, KeyEvent.VK_K, KeyEvent.VK_F);
        boolean shootHeld = in.isDown(KeyEvent.VK_X, KeyEvent.VK_K, KeyEvent.VK_F);
        boolean downHeld = in.isDown(KeyEvent.VK_S, KeyEvent.VK_DOWN);

        // Actualizar Coyote Time y Jump Buffer
        if (onGround) {
            coyoteTimer = 6;
            isDashJump = false;
        } else {
            if (coyoteTimer > 0) coyoteTimer--;
        }

        if (jumpPressed) {
            jumpBufferTimer = 6;
        } else if (jumpBufferTimer > 0) {
            jumpBufferTimer--;
        }

        // ==========================================
        // 1. SISTEMA DE X-BUSTER (CARGA Y DISPARO)
        // ==========================================
        if (shootPressed) {
            buster.startCharging();
        }

        buster.update(centerX(), centerY(), ps);

        // Si soltó el botón de disparo, soltar el tiro correspondiente
        if (buster.isCharging() && !shootHeld) {
            int lemonsCount = 0;
            for (Bullet b : bullets) {
                if (b instanceof Bullet.LemonBullet && b.isFromPlayer()) lemonsCount++;
            }

            double muzzleX = facingRight ? x + width + 2 : x - 12;
            double muzzleY = isDashing ? y + 8 : y + 10;
            Bullet b = buster.fireWithCharge(muzzleX, muzzleY, facingRight, ps, lemonsCount);
            if (b != null) {
                bullets.add(b);
                shootAnimTimer = 15;
                if (b instanceof Bullet.Charge2Bullet) {
                    camera.shake(6.0); // Sacudida al disparar carga completa
                }
            }
            buster.stopCharging();
        }

        // ==========================================
        // 2. DASH TERRESTRE
        // ==========================================
        if (flinchTimer == 0) {
            // Iniciar dash por tecla dedicada
            if (dashPressed && onGround && !isDashing) {
                startDash(ps);
            }

            // Actualizar dash activo
            if (isDashing) {
                dashTimer--;
                // Velocidad constante del dash
                vx = facingRight ? DASH_SPEED : -DASH_SPEED;

                // Partículas de humo en los pies cada 3 ticks
                if (dashTimer % 3 == 0 && ps != null) {
                    ps.spawnDashDust(facingRight ? x : x + width, y + height - 2, facingRight);
                }

                boolean lowCeiling = !canStandUp(map);

                // Cancelar si se suelta la dirección contraria o el dash termina (siempre que el techo lo permita)
                if (!lowCeiling) {
                    if ((facingRight && leftDown) || (!facingRight && rightDown) || dashTimer <= 0) {
                        stopDash();
                    }
                    if (!onGround && !isDashJump) {
                        stopDash();
                    }
                }
            }
        }

        // ==========================================
        // 3. SALTO Y DASH-JUMP
        // ==========================================
        if (flinchTimer == 0) {
            boolean canNormalJump = (onGround || coyoteTimer > 0);

            if (jumpBufferTimer > 0 && canNormalJump) {
                jumpBufferTimer = 0;
                coyoteTimer = 0;
                vy = JUMP_VELOCITY;
                onGround = false;
                SoundSystem.play("jump");

                // Si estaba haciendo dash, ¡DASH-JUMP!
                if (isDashing) {
                    isDashJump = true;
                    stopDash(); // Restablece hitbox a tamaño normal
                }
            }

            // Salto variable: si se suelta el botón mientras sube, recortar la subida
            if (!jumpHeld && vy < -3.2) {
                vy = -3.2;
            }
        }

        // ==========================================
        // 4. MOVIMIENTO HORIZONTAL
        // ==========================================
        if (flinchTimer == 0) {
            if (wallKickLockTimer <= 0 && !isDashing) {
                if (isDashJump && !onGround) {
                    // Durante Dash-Jump en el aire: conserva inercia máxima del dash
                    boolean opposing = facingRight ? leftDown : rightDown;
                    if (opposing) {
                        vx *= 0.90; // Frena solo si el jugador pulsa la dirección opuesta
                        if (Math.abs(vx) < WALK_SPEED) isDashJump = false;
                    } else {
                        vx = facingRight ? DASH_SPEED : -DASH_SPEED;
                    }
                } else {
                    double targetVx = 0;
                    if (leftDown && !rightDown) {
                        targetVx = -WALK_SPEED;
                        facingRight = false;
                    } else if (rightDown && !leftDown) {
                        targetVx = WALK_SPEED;
                        facingRight = true;
                    }

                    if (onGround) {
                        vx = targetVx;
                    } else {
                        if (targetVx != 0) {
                            vx = targetVx;
                        } else {
                            vx *= 0.85;
                        }
                    }
                }
            }
        }

        // ==========================================
        // 5. GRAVEDAD Y FÍSICA VERTICAL
        // ==========================================
        vy += GRAVITY;
        if (vy > TERMINAL_VELOCITY) vy = TERMINAL_VELOCITY;

        // ==========================================
        // 6. RESOLUCIÓN DE COLISIONES CON EL MAPA
        // ==========================================
        touchingLeftWall = false;
        touchingRightWall = false;
        boolean wasOnGround = onGround;
        onGround = false;
        isWallSliding = false;

        // 6.1 Movimiento Horizontal
        x += vx;
        resolveHorizontalCollisions(map);

        // 6.2 Movimiento Vertical
        y += vy;
        resolveVerticalCollisions(map, downHeld && jumpPressed);

        // ==========================================
        // 7. WALL SLIDE Y WALL KICK
        // ==========================================
        if (flinchTimer == 0 && !onGround) {
            boolean pressingLeftAgainstWall = leftDown && touchingLeftWall;
            boolean pressingRightAgainstWall = rightDown && touchingRightWall;

            if ((pressingLeftAgainstWall || pressingRightAgainstWall) && vy >= 0) {
                isWallSliding = true;
                wallSlideDir = pressingLeftAgainstWall ? -1 : 1;
                facingRight = !pressingLeftAgainstWall; // Mira hacia afuera de la pared

                // Limitar velocidad de caída (fricción de pared)
                if (vy > WALL_SLIDE_SPEED) {
                    vy = WALL_SLIDE_SPEED;
                }

                // Chispas de fricción contra la pared
                if (ps != null && Math.random() < 0.45) {
                    double sparkX = (wallSlideDir == -1) ? x : x + width;
                    ps.spawnWallSparks(sparkX, y + height - 12, wallSlideDir == -1);
                }

                // --- WALL KICK ---
                if (jumpBufferTimer > 0) {
                    jumpBufferTimer = 0;
                    vy = WALL_KICK_VY;

                    // Dirección de rebote hacia afuera de la pared
                    boolean kickRight = (wallSlideDir == -1);
                    boolean isDashKick = dashHeld || isDashing;
                    double kickSpeed = isDashKick ? DASH_WALL_KICK_VX : WALL_KICK_VX;

                    vx = kickRight ? kickSpeed : -kickSpeed;
                    facingRight = kickRight;
                    wallKickLockTimer = 7; // Breve bloqueo para formar el arco triangular

                    if (isDashKick) {
                        isDashJump = true;
                    }

                    SoundSystem.play("wallkick");
                    if (ps != null) {
                        double burstX = (wallSlideDir == -1) ? x : x + width;
                        ps.spawnWallKickBurst(burstX, y + height / 2.0);
                    }
                }
            }
        }

        // Limitar límites del mundo
        if (x < 0) { x = 0; vx = 0; }
        if (x > map.getWidth() - width) { x = map.getWidth() - width; vx = 0; }
        if (y > map.getHeight() - height) {
            // Caída en el vacío / pinchos
            takeDamage(999);
        }

        // ==========================================
        // 8. DETERMINAR ESTADO PARA ANIMACIÓN
        // ==========================================
        updateState();
    }

    private boolean canStandUp(TileMap map) {
        int leftCol = (int) Math.floor(x / TileMap.TILE_SIZE);
        int rightCol = (int) Math.floor((x + STAND_W - 1) / TileMap.TILE_SIZE);
        int standTopRow = (int) Math.floor((y - (STAND_H - DASH_H)) / TileMap.TILE_SIZE);
        for (int c = leftCol; c <= rightCol; c++) {
            if (map.isSolid(c, standTopRow)) {
                return false;
            }
        }
        return true;
    }

    private void startDash(ParticleSystem ps) {
        if (isDashing) return;
        isDashing = true;
        dashTimer = DASH_DURATION;
        y += (STAND_H - DASH_H);
        width = DASH_W;
        height = DASH_H;
        SoundSystem.play("dash");
        if (ps != null) {
            ps.spawnDashDust(facingRight ? x : x + width, y + height, facingRight);
        }
    }

    private void stopDash() {
        if (!isDashing) return;
        isDashing = false;
        dashTimer = 0;
        y -= (STAND_H - DASH_H);
        width = STAND_W;
        height = STAND_H;
    }

    private void updateState() {
        if (flinchTimer > 0) {
            state = PlayerState.HURT;
            return;
        }

        if (isWallSliding) {
            state = PlayerState.WALL_SLIDE;
            return;
        }

        if (isDashing) {
            state = PlayerState.DASH;
            return;
        }

        if (!onGround) {
            state = (vy < 0) ? PlayerState.JUMP : PlayerState.FALL;
            return;
        }

        if (Math.abs(vx) > 0.4) {
            state = PlayerState.RUN;
            runAnimTimer++;
            if (runAnimTimer >= 4) { // 4 ticks por fotograma (15 fps de animación)
                runAnimTimer = 0;
                runAnimIndex = (runAnimIndex + 1) % 10;
            }
        } else {
            state = PlayerState.IDLE;
            runAnimIndex = 0;
            runAnimTimer = 0;
            idleBlinkTimer++;
            if (idleBlinkTimer > 180) idleBlinkTimer = 0;
        }
    }

    // --- Colisiones precisas con el TileMap ---

    private void resolveHorizontalCollisions(TileMap map) {
        int leftCol = (int) Math.floor(x / TileMap.TILE_SIZE);
        int rightCol = (int) Math.floor((x + width - 1) / TileMap.TILE_SIZE);
        int topRow = (int) Math.floor(y / TileMap.TILE_SIZE);
        int bottomRow = (int) Math.floor((y + height - 1) / TileMap.TILE_SIZE);

        if (vx < 0) { // Moviéndose a la izquierda
            for (int r = topRow; r <= bottomRow; r++) {
                if (map.isSolid(leftCol, r)) {
                    x = (leftCol + 1) * TileMap.TILE_SIZE;
                    vx = 0;
                    touchingLeftWall = true;
                    isDashJump = false;
                    break;
                }
            }
        } else if (vx > 0) { // Moviéndose a la derecha
            for (int r = topRow; r <= bottomRow; r++) {
                if (map.isSolid(rightCol, r)) {
                    x = rightCol * TileMap.TILE_SIZE - width;
                    vx = 0;
                    touchingRightWall = true;
                    isDashJump = false;
                    break;
                }
            }
        }
    }

    private void resolveVerticalCollisions(TileMap map, boolean dropDownPlatform) {
        int leftCol = (int) Math.floor(x / TileMap.TILE_SIZE);
        int rightCol = (int) Math.floor((x + width - 1) / TileMap.TILE_SIZE);
        int topRow = (int) Math.floor(y / TileMap.TILE_SIZE);
        int bottomRow = (int) Math.floor((y + height) / TileMap.TILE_SIZE);

        if (vy < 0) { // Subiendo (techo)
            for (int c = leftCol; c <= rightCol; c++) {
                if (map.isSolid(c, topRow)) {
                    y = (topRow + 1) * TileMap.TILE_SIZE;
                    vy = 0;
                    break;
                }
            }
        } else if (vy >= 0) { // Cayendo (suelo o plataforma)
            for (int c = leftCol; c <= rightCol; c++) {
                // Comprobar pinchos
                if (map.isSpikes(c, bottomRow)) {
                    takeDamage(16);
                }

                if (map.isSolid(c, bottomRow)) {
                    y = bottomRow * TileMap.TILE_SIZE - height;
                    vy = 0;
                    onGround = true;
                    break;
                } else if (map.isPlatform(c, bottomRow) && !dropDownPlatform) {
                    // Plataforma atravesable desde abajo: solo colisiona si venía cayendo
                    double prevBottom = y + height - vy;
                    if (prevBottom <= bottomRow * TileMap.TILE_SIZE + 4) {
                        y = bottomRow * TileMap.TILE_SIZE - height;
                        vy = 0;
                        onGround = true;
                        break;
                    }
                }
            }
        }
    }

    // ==========================================
    // 9. DIBUJADO DE MEGA MAN X
    // ==========================================
    @Override
    public void draw(Graphics2D g) {
        // Parpadeo durante frames de invulnerabilidad
        if (invulnerableTimer > 0 && (invulnerableTimer / 3) % 2 == 1) {
            return;
        }

        int dx = (int) Math.round(x);
        int dy = (int) Math.round(y);
        boolean isShooting = shootAnimTimer > 0;

        BufferedImage sprite = null;

        switch (state) {
            case IDLE -> {
                if (isShooting) {
                    sprite = Sprites.idleShootFrame;
                } else if (idleBlinkTimer > 170) {
                    sprite = Sprites.idleBlinkFrame;
                } else {
                    sprite = Sprites.idleFrame;
                }
            }
            case RUN -> {
                sprite = isShooting ? Sprites.runShootFrames[runAnimIndex] : Sprites.runFrames[runAnimIndex];
            }
            case JUMP -> {
                sprite = isShooting ? Sprites.jumpShootFrame : Sprites.jumpFrame;
            }
            case FALL -> {
                sprite = isShooting ? Sprites.fallShootFrame : Sprites.fallFrame;
            }
            case DASH -> {
                sprite = isShooting ? Sprites.dashShootFrame : Sprites.dashFrame;
            }
            case WALL_SLIDE -> {
                sprite = isShooting ? Sprites.wallSlideShootFrame : Sprites.wallSlideFrame;
            }
            case HURT -> {
                sprite = Sprites.fallFrame; // Postura de impacto
            }
            case DEAD -> {
                return; // Efecto de explosión
            }
        }

        if (sprite != null) {
            // Alinear sprite con el hitbox
            int drawX = dx - (sprite.getWidth() - width) / 2;
            int drawY = dy - (sprite.getHeight() - height);

            if (state == PlayerState.WALL_SLIDE) {
                // Durante wall slide, la pared está a la izquierda o derecha
                boolean wallOnLeft = (wallSlideDir == -1);
                Sprites.drawFlipped(g, sprite, drawX, drawY, wallOnLeft);
            } else {
                Sprites.drawFlipped(g, sprite, drawX, drawY, facingRight);
            }
        } else {
            // Fallback geométrico
            g.setColor(Color.CYAN);
            g.fillRect(dx, dy, width, height);
        }

        // Efecto de aura de carga completa (parpadeo de color sobre X)
        int chargeLvl = buster.getChargeLevel();
        if (chargeLvl == 2 && (buster.getChargeTimer() / 2) % 2 == 1) {
            Composite orig = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            g.setColor(new Color(255, 100, 240));
            g.fillRoundRect(dx - 2, dy - 2, width + 4, height + 4, 8, 8);
            g.setComposite(orig);
        }
    }
}
