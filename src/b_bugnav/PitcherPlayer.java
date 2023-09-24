package b_bugnav;

import aic2023.user.UnitController;

public class PitcherPlayer {
    private final UnitController uc;

    PitcherPlayer(UnitController uc) {
        this.uc = uc;
    }

    void run() {
        while (true) {
            uc.yield();
        }
    }
}
