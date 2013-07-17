/**
 * eobjects.org MetaModel
 * Copyright (C) 2010 eobjects.org
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.eobjects.metamodel.create;

import org.eobjects.metamodel.schema.Column;

/**
 * Builder object for creating {@link Column}s. This class also extendsthe
 * {@link TableCreationBuilder} (allowing to step immediately out of the column
 * building and back to the table building immediately).
 * 
 * @author Kasper Sørensen
 */
public interface ColumnCreationBuilder extends ColumnBuilder<ColumnCreationBuilder>, TableCreationBuilder {

}