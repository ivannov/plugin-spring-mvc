/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.forge.scaffold.spring.metawidget.widgetbuilder;

import org.jboss.forge.env.Configuration;
import org.metawidget.util.simple.ObjectUtils;

/**
 * Configures a <tt>ForgePropertyStyle</tt>.
 *
 * @author Richard Kennard
 */

public class EntityWidgetBuilderConfig
{

   //
   // Private members
   //

   private Configuration config;

   //
   // Public methods
   //

   public EntityWidgetBuilderConfig setConfig(Configuration config)
   {
      this.config = config;
      return this;
   }

   @Override
   public boolean equals(Object that)
   {
      if (this == that)
      {
         return true;
      }

      if (!ObjectUtils.nullSafeClassEquals(this, that))
      {
         return false;
      }

      if (this.config != ((EntityWidgetBuilderConfig) that).config)
      {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode()
   {
      return ObjectUtils.nullSafeHashCode(this.config);
   }

   //
   // Protected methods
   //

   protected Configuration getConfig()
   {
      return this.config;
   }
}
