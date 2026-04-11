package myau.module.modules.chatting.util;

/**
 * Ease Out Quad Animation
 * For smooth chat animations
 */
public class EaseOutQuad {
    
    private float currentValue;
    private float targetValue;
    private float speed;
    private boolean finished = true;
    
    public EaseOutQuad(float speed) {
        this.speed = speed;
        this.currentValue = 0f;
        this.targetValue = 0f;
    }
    
    /**
     * Set target value
     */
    public void setTarget(float target) {
        this.targetValue = target;
        this.finished = false;
    }
    
    /**
     * Update animation
     * @param delta time delta
     * @return current value
     */
    public float update(float delta) {
        if (finished) return currentValue;
        
        // Ease out quad formula
        float difference = targetValue - currentValue;
        currentValue += difference * speed * delta;
        
        // Check if close enough to finish
        if (Math.abs(difference) < 0.001f) {
            currentValue = targetValue;
            finished = true;
        }
        
        return currentValue;
    }
    
    /**
     * Reset to a value
     */
    public void reset(float value) {
        this.currentValue = value;
        this.targetValue = value;
        this.finished = true;
    }
    
    /**
     * Get current value
     */
    public float getValue() {
        return currentValue;
    }
    
    /**
     * Check if animation finished
     */
    public boolean isFinished() {
        return finished;
    }
    
    /**
     * Set speed
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }
}
