package chatwave.screens;

import chatwave.ui.Overlay;

/** Điều hướng giữa các màn hình. */
public interface Nav {
    Overlay overlay();
    void showAuth();
    void showMain();
    void showAdmin();
    void toggleTheme();
}
