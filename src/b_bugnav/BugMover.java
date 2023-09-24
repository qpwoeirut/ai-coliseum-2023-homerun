package b_bugnav;

import aic2023.user.*;

public class BugMover {

    private final UnitController uc;
    private final int INF = 10000;
    private int startvar = 0;
    BugMover(UnitController uc) {
        this.uc = uc;
    }

    Direction move (Location fin)
    {
        Direction final_move = Direction.NORTH;
        Location loc = uc.getLocation();
        int minDist = INF;
        for (int i = 0; i < 8; i++)
        {
            Direction current_dir = Direction.values()[i];

            if (uc.canMove(current_dir) && loc.add(current_dir).distanceSquared(fin) < minDist)
            {
                final_move = current_dir;
                minDist = loc.add(current_dir).distanceSquared(fin);
            }
        }

        return final_move;
    }

    Direction move_to_object (MapObject object)
    {
        uc.println("startvar: " + startvar);
        Location loc = uc.getLocation();
        Location[] objects = uc.senseObjects(object, INF);

        if (objects.length != 0)
        {
            //report existence of object
        }

        float vision_range = uc.getType().getStat(UnitStat.VISION_RANGE);
        for (int i = startvar; i < 8; i++)
        {
            Direction current_dir = Direction.values()[i];
            Location end_goal = loc.add(3 * (current_dir.dx), 3 * (current_dir.dy));
            if (!uc.isOutOfMap(end_goal))
            {
                return move(end_goal);
            }
            startvar = (startvar + 1) % 8;
            uc.println("got here");
        }
        return Direction.NORTH;
    }
}
