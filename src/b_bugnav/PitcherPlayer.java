package b_bugnav;

import aic2023.user.UnitController;
import b_bugnav.util.Communications;

public class PitcherPlayer {
    private final UnitController uc;
    private final Communications comms;

    PitcherPlayer(UnitController uc) {
        this.uc = uc;
        this.comms = new Communications(uc);
    }

    void run() {
        while (true) {
            comms.checkIn();

            uc.yield();
        }
    }
}
