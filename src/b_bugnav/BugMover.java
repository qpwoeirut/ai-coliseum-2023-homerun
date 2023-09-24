package b_bugnav;

import aic2023.user.*;

public class BugMover {

    private final UnitController uc;
    private final int INF = 10000;
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
        Location loc = uc.getLocation();
        Location[] bases = uc.senseObjects(object, INF);
        if (bases.length == 0)
        {
            float vision_range = uc.getType().getStat(UnitStat.VISION_RANGE);
            for (int i = 0; i < 8; i++)
            {
                Direction current_dir = Direction.values()[i];
                Location end_goal = loc.add((int)vision_range * (current_dir.dx), (int)vision_range * (current_dir.dy));
                if (!uc.isOutOfMap(end_goal))
                {
                    return move(end_goal);
                }
            }
        }
        return Direction.NORTH;
    }
}
