package h1_scouting;

import aic2023.user.Direction;
import aic2023.user.Location;
import aic2023.user.UnitController;

public class SpiralMover {
    private final UnitController uc;
    int degrees = 0;
    int moves = 1;
    double radius = 3;

    SpiralMover(UnitController uc) {
        this.uc = uc;
    }

    Direction spiralMove(Location spiral_center, int tries) {
        Direction dir = Direction.ZERO;
        while (dir == Direction.ZERO) {
            Location fin = spiral_center.add((int)(Math.round(radius * Math.cos(Math.toRadians(degrees)))), (int)(Math.round(radius * Math.sin(Math.toRadians(degrees)))));
            dir = uc.getLocation().directionTo(fin);
            uc.println(fin);
            radius += 0.5 / moves;
            degrees += 1;
        }
        if (uc.canMove(dir)) {
            ++moves;
            return dir;
        } else if (uc.canMove(dir.rotateLeft())) {
            ++moves;
            return dir.rotateLeft();
        } else if (uc.canMove(dir.rotateRight())) {
            ++moves;
            return dir.rotateRight();
        } else if (tries == 20) {
            return Direction.ZERO;
        } else {
            degrees = 10 * tries * (tries % 2 == 0 ? 1 : -1) - degrees;
            return spiralMove(spiral_center, tries + 1);
        }
    }
}
