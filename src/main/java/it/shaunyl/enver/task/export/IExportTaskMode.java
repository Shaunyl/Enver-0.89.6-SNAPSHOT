/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.shaunyl.enver.task.export;

import it.shaunyl.enver.ITaskMode;
import lombok.Getter;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public abstract class IExportTaskMode extends ITaskMode {

    @Getter
    public String table, query, filename;
    
    @Getter
    public int start, end;
}
