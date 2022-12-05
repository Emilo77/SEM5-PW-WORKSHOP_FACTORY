package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Semaphore;


public class Utils {

    public enum Action {
        ENTER,
        SWITCHTO,
        USE,
    }

    public static WorkshopFactory.WorkplaceWrapper getWorkplace
            (Collection<WorkshopFactory.WorkplaceWrapper> workplaces, WorkplaceId wid) {
        for (WorkshopFactory.WorkplaceWrapper w : workplaces) {
            if (w.getId().equals(wid)) {
                return w;
            }
        }
        throw new RuntimeException("panic: couldn't find workplace with given id!");
    }
}
