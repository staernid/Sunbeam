package io.github.staernid.sunbeam.ui;

import io.github.staernid.sunbeam.binding.input.GameInputDevice;

public interface GameGestures {
    void toggleKeyboard();

    default void showGameMenu(GameInputDevice device){};
}
