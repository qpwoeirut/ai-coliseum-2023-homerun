package h1_scouting;

import aic2023.user.*;
import h1_scouting.util.Communications;

public class SpiralMover {
    private final UnitController uc;
    double hidden_x, hidden_y;
    int hidden_theta = 0;
    int counter = 0;
    final int CIRCLE_NUMBER = 360;
    int SPIRAL_ADD = 90;
    int total_turns = 0;
    double radius = 10;
    boolean going_to_HQ = false;

    SpiralMover(UnitController uc) {
        this.uc = uc;
        hidden_x = uc.getLocation().x;
        hidden_y = uc.getLocation().y;
    }

    Direction spiralMove(Location spiral_center) {
//        uc.println(total_turns + " " + radius + " " + hidden_theta + " " + counter);
//        uc.println("HQ is at " + spiral_center.x + " " + spiral_center.y);
        double angle = (double) total_turns / radius;
        Location fin = spiral_center.add((int)(radius * Math.cos(angle)), (int)(radius * Math.sin(angle)));
//        uc.println(fin.x + " " + fin.y);
        Direction dir = uc.getLocation().directionTo(fin);
        radius += 0.025;
//        if (total_turns % radius == 0) {
//            hidden_theta += SPIRAL_ADD;
//            hidden_theta %= CIRCLE_NUMBER;
//            total_turns = 0;
//        }
//
//

//
//
//        Direction dir = Direction.values()[counter];
//
        if (!uc.canMove(dir)) {
            if (uc.canMove(dir.rotateLeft())) {
                dir = dir.rotateLeft();
            } else if (uc.canMove(dir.rotateRight()))
            {
                dir = dir.rotateRight();
            } else
            {
                dir = Direction.ZERO;
            }
        }

        total_turns++;
        return dir;
    }

}
