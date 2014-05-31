/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.shaunyl.enver.task.compare;

import it.shaunyl.enver.ITaskMode;
import java.sql.CallableStatement;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public abstract class ICompareTaskMode extends ITaskMode {

    protected CallableStatement callable;

}