package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;

import java.util.Collection;
import java.util.stream.Collectors;


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

    public static Collection<WorkshopFactory.WorkplaceWrapper> initializeWrappers(
            Collection<Workplace> workplaces,
            WorkersOccupyMap workerMap) {
        return workplaces.stream().map(workplace -> new WorkshopFactory.WorkplaceWrapper(workplace, workerMap)
        ).collect(Collectors.toList());
    }
}
