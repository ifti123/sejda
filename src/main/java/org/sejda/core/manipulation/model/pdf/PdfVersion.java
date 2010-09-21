/*
 * Created on 30/mag/2010
 * Copyright (C) 2010 by Andrea Vacondio (andrea.vacondio@gmail.com).
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.sejda.core.manipulation.model.pdf;

/**
 * pdf versions
 * @author Andrea Vacondio
 * 
 */
public enum PdfVersion {

    VERSION_1_2(2), VERSION_1_3(3), VERSION_1_4(4), VERSION_1_5(5), VERSION_1_6(6), VERSION_1_7(7);
    
    private int version;

    private PdfVersion(int version) {
        this.version = version;
    }

    /**
     * @return an int representation of the version
     */
    public int getVersion() {
        return version;
    }

    /**
     * @return a String representation of the int version
     */
    public String getVersionAsString() {
        return String.valueOf(version);
    }

    /**
     * @return a char representation of the int version
     */
    public char getVersionAsCharacter() {
        return getVersionAsString().charAt(0);
    }
}
