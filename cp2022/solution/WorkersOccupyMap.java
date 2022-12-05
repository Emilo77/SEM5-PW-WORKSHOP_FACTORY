package cp2022.solution;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/* Mapa przechowująca potrzebne informacje o wątkach. */
public class WorkersOccupyMap {
    private final ConcurrentHashMap<Long, WorkerInfo> occupyMap;
    private final Semaphore mapBlocker;
    private static final String alertMessage
            = "panic: couldn't find information about worker!";


    protected WorkersOccupyMap() {
        this.occupyMap = new ConcurrentHashMap<>();
        this.mapBlocker = new Semaphore(1, true);
    }

    /* Wstawienie nowego wątku do mapy. */
    protected void insertNewWorker(Long pid) throws InterruptedException {
        mapBlocker.acquire();

        /* Utworzenie nowej klasy informacji o wątku. */
        WorkerInfo info = new WorkerInfo();
        /* Wstawienie informacji do mapy. */
        occupyMap.put(pid, info);

        mapBlocker.release();
    }

    /* Usunięcie wątku z kolejki. */
    protected void removeWorker(Long pid) throws InterruptedException {
        mapBlocker.acquire();

        /* Wyciągnięcie informacji o wątku. */
        WorkerInfo info = occupyMap.get(pid);

        if (info == null) {
            throw new RuntimeException(alertMessage);
        }
        /* Usunięcie informacji z mapy */
        occupyMap.remove(pid);

        mapBlocker.release();
    }

    /* Zwrócenie stanowiska, w którym przebywa wątek o danym identyfikatorze. */
    protected WorkshopFactory.WorkplaceWrapper getCurrentWorkplace(Long pid) throws InterruptedException {
        mapBlocker.acquire();

        /* Wyciągnięcie informacji na temat wątku. */
        WorkerInfo info = occupyMap.get(pid);
        if (info == null) {
            throw new RuntimeException(alertMessage);
        }

        mapBlocker.release();
        /* Zwrócenie stanowiska, w którym znajduje się wątek. */
        return info.getCurrentWorkplace();
    }

    /* Zwrócenie stanowiska,
    * na które chciałby przejść wątek o podanym identyfikatorze. */
    protected WorkshopFactory.WorkplaceWrapper getDesiredWorkplace(Long pid) throws InterruptedException {
        mapBlocker.acquire();

        /* Wyciągnięcie informacji na temat wątku. */
        WorkerInfo info = occupyMap.get(pid);
        if (info == null) {
            throw new RuntimeException(alertMessage);
        }

        mapBlocker.release();
        /* Zwrócenie stanowiska, na które wątek chciałby przejść. */
        return info.getDesiredWorkplace();
    }

    /* Zwrócenie akcji, jaką wątek chce wykonać. */
    protected Utils.Action getAction(Long pid) throws InterruptedException {
        mapBlocker.acquire();

        WorkerInfo info = occupyMap.get(pid);
        if (info == null) {
            throw new RuntimeException(alertMessage);
        }

        mapBlocker.release();
        /* Zwrócenie akcji, jaką wątek ma zamiar wykonać. */
        return info.getAction();
    }

    /* Aktualizacja informacji o wątku. */
    protected void updateWorkerInfo(Long pid,
                                    Utils.Action action,
                                    WorkshopFactory.WorkplaceWrapper workplace)
            throws InterruptedException {
        mapBlocker.acquire();

        WorkerInfo info = occupyMap.get(pid);
        if (info == null) {
            throw new RuntimeException(alertMessage);
        }

        info.update(action, workplace);

        mapBlocker.release();
    }

    /* Aktualizacja informacji o wątku. */
    protected void updateWorkerInfo(Long pid,
                                    Utils.Action action,
                                    WorkshopFactory.WorkplaceWrapper currentWorkplace,
                                    WorkshopFactory.WorkplaceWrapper desiredWorkplace)
            throws InterruptedException {
        mapBlocker.acquire();

        WorkerInfo info = occupyMap.get(pid);
        if (info == null) {
            throw new RuntimeException(alertMessage);
        }

        info.update(action, currentWorkplace, desiredWorkplace);

        mapBlocker.release();
    }

}