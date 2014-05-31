/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.shaunyl.enver.io;

import it.shaunyl.enver.exception.EnverException;
import java.io.IOException;

/**
 *
 * @author Filippo Testino (filippo.testino@gmail.com)
 */
public abstract class IEnverPropertiesFileUpdater {
    
    public abstract void writeSubValueByKey(final String key, final String subprop, final String value) throws IOException, EnverException;

    public abstract void writeValueByKey(final String key, final String value) throws IOException, EnverException;
}
