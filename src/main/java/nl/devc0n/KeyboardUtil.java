package nl.devc0n;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class KeyboardUtil implements NativeKeyListener {

    private boolean left = false, right = false, up = false, down = false;

    public KeyboardUtil() {
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_LEFT) {
            System.out.println("left");
            resetKeys();
            left = true;
        } else if (e.getKeyCode() == NativeKeyEvent.VC_RIGHT) {
            System.out.println("right");
            resetKeys();
            right = true;
        } else if (e.getKeyCode() == NativeKeyEvent.VC_UP) {
            System.out.println("up");
            resetKeys();
            up = true;
        } else if (e.getKeyCode() == NativeKeyEvent.VC_DOWN) {
            System.out.println("down");
            resetKeys();
            down = true;
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        resetKeys(); // Reset key states on release
    }

    public String listen() {
        if (left) return "left";
        if (right) return "right";
        if (up) return "up";
        if (down) return "down";
        return "noop";
    }

    private void resetKeys() {
        left = false;
        right = false;
        up = false;
        down = false;
    }

}
