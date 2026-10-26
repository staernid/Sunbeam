package io.github.staernid.sunbeam.binding.input;

import io.github.staernid.sunbeam.GameMenu;

import java.util.List;

/**
 * Description
 * Date: 2024-01-16
 * Time: 15:26
 * User: Genng(genng1991@gmail.com)
 */
public interface GameInputDevice {

    /**
     * @return list of device specific game menu options, e.g. configure a controller's mouse mode
     */
    List<GameMenu.MenuOption> getGameMenuOptions();
}