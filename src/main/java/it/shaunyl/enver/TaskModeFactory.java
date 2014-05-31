/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.shaunyl.enver;

import it.shaunyl.enver.exception.EnverException;
import it.shaunyl.enver.exception.UnexpectedEnverException;
import it.shaunyl.enver.exception.UnknownTaskModeException;
import it.shaunyl.enver.task.compare.ICompareTaskMode;
import java.util.*;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public final class TaskModeFactory {

    private static TaskModeFactory instance;

    private List<ITaskMode> modes;

    public static void reset() {
        instance = new TaskModeFactory();
    }

    public static TaskModeFactory getInstance() {
        if (instance == null) {
            instance = new TaskModeFactory();
        }
        return instance;
    }

    /**
     * Set the instance used by this singleton. Used primarily for testing.
     */
    public static void setInstance(TaskModeFactory instance) {
        TaskModeFactory.instance = instance;
    }

    private TaskModeFactory() {
        this.modes = new ArrayList<ITaskMode>();
        try {
            ServiceLoader<ICompareTaskMode> serviceLoader = ServiceLoader.load(ICompareTaskMode.class);
            Iterator<ICompareTaskMode> iterator = serviceLoader.iterator();

            while (iterator.hasNext()) {
                register((ITaskMode) iterator.next().getClass().getConstructor().newInstance());
            }

        } catch (Exception e) {
            throw new UnexpectedEnverException(e);
        }

    }

    public List<ITaskMode> getModes() {
        return Collections.unmodifiableList(modes);
    }

    public ITaskMode getMode(String modality) throws EnverException {
        for (ITaskMode mode : modes) {
            if (mode.identify(modality)) {
                return mode;
            }
        }

        throw new UnknownTaskModeException("Cannot find mode '" + modality + "' supported by task COMPARE.");
    }

    public void register(ITaskMode taskMode) {
        modes.add(taskMode);
    }

    public void unregister(ITaskMode taskMode) {
        modes.remove(taskMode);
    }

    public void unregisterAll() {
        modes.clear();
    }
}
