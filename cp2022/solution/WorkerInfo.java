package cp2022.solution;

import java.util.concurrent.Semaphore;

/* Klasa przechowująca informacje o danym wątku. */
public class WorkerInfo {
    private Utils.Action desiredAction;
    private WorkshopFactory.WorkplaceWrapper currentWorkplace;
    private WorkshopFactory.WorkplaceWrapper desiredWorkplace;
    private final Semaphore workerInfoBlocker;

    public WorkerInfo() {
        this.desiredAction = Utils.Action.ENTER;
        this.currentWorkplace = null;
        this.desiredWorkplace = null;
        this.workerInfoBlocker = new Semaphore(1, true);
    }

    /* Zaktualizowanie informacji o wątku. */
    public void update(Utils.Action newAction,
                       WorkshopFactory.WorkplaceWrapper workplace)
            throws InterruptedException {
        workerInfoBlocker.acquire();

        this.desiredAction = newAction;

        if (newAction.equals(Utils.Action.ENTER)) {
            this.currentWorkplace = null;
            this.desiredWorkplace = workplace;

        } else if (newAction.equals(Utils.Action.USE)) {
            this.currentWorkplace = workplace;
            this.desiredWorkplace = null;
        }

        workerInfoBlocker.release();
    }

    /* Zaktualizowanie informacji o wątku. */
    public void update(Utils.Action newAction,
                       WorkshopFactory.WorkplaceWrapper newCurrentWorkplace,
                       WorkshopFactory.WorkplaceWrapper newDesiredWorkplace)
            throws InterruptedException {

        workerInfoBlocker.acquire();

        desiredAction = newAction;
        currentWorkplace = newCurrentWorkplace;
        desiredWorkplace = newDesiredWorkplace;

        workerInfoBlocker.release();
    }

    public WorkshopFactory.WorkplaceWrapper getCurrentWorkplace() {
        return this.currentWorkplace;
    }

    public WorkshopFactory.WorkplaceWrapper getDesiredWorkplace() {
        return this.desiredWorkplace;
    }

    public Utils.Action getAction() {
        return this.desiredAction;
    }
}