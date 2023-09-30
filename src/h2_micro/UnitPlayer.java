package h2_micro;

import aic2023.user.UnitController;
import aic2023.user.UnitType;

public class UnitPlayer {
    public void run(UnitController uc) {
        if (uc.getType() == UnitType.BATTER) {
            new BatterPlayer(uc).run();
        } else if (uc.getType() == UnitType.CATCHER) {
            new CatcherPlayer(uc).run();
        } else if (uc.getType() == UnitType.PITCHER) {
            new PitcherPlayer(uc).run();
        } else if (uc.getType() == UnitType.HQ) {
            new HqPlayer(uc).run();
        } else {
            throw new IllegalArgumentException("unrecognized unit type " + uc.getType());
        }
    }
}
