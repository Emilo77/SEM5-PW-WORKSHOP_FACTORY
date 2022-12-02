package cp2022.solution;

import cp2022.base.Workplace;
import cp2022.base.WorkplaceId;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Semaphore;


public class Utils {

//    public static Collection<WorkshopFactory.WorkplaceWrapper> initializeWrappers(Collection<Workplace> workplaces) {
//        return workplaces.stream().map(workplace -> {
////            return WorkshopFactory.WorkplaceWrapper(workplace);
//            return WorkshopFactory.WorkplaceWrapper;
//        }).collect(Collectors.toCollection())
//        //todo stworzyć kolekcję wrapperów na podstawie kolekcji Workplaces
//    }

    public static HashMap<WorkplaceId, Semaphore> initializeMap(Collection<Workplace> workplaces) {
        HashMap<WorkplaceId, Semaphore> map = new HashMap<>();
        for (Workplace w : workplaces) {
            map.put(w.getId(), new Semaphore(1, true));
        }
        return map;
    }

    public static void occupyMap(HashMap<Long, WorkplaceId> map, Long pid, WorkplaceId wid) {
        map.put(pid, wid);
    }

    public static void freeMap(HashMap<Long, WorkplaceId> map, Long pid) {
        if (map.get(pid) == null) {
            throw new RuntimeException("panic: couldn't find workplace occupied by this pid!");
        }
        map.remove(pid);
    }

    public static WorkplaceId getOccupiedId(HashMap<Long, WorkplaceId> map, Long pid) {
        if (map.get(pid) == null) {
            throw new RuntimeException("panic: couldn't find workplace occupied by this pid!");
        }
        return map.get(pid);
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
