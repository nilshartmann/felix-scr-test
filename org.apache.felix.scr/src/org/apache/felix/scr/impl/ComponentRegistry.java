/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.ScrService;
import org.apache.felix.scr.impl.config.ComponentHolder;
import org.apache.felix.scr.impl.config.UnconfiguredComponentHolder;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.apache.felix.scr.impl.manager.ComponentFactoryImpl;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentException;


/**
 * The <code>ComponentRegistry</code> class acts as the global registry for
 * components by name and by component ID. As such the component registry also
 * registers itself as the {@link ScrService} to support access to the
 * registered components.
 */
public class ComponentRegistry implements ScrService
{

    /**
     * The map of known components indexed by component name. The values are
     * either the component names (for name reservations) or implementations
     * of the {@link ComponentHolder} interface.
     * <p>
     * The {@link #checkComponentName(String)} will first add an entry to this
     * map being the name of the component to reserve the name. After setting up
     * the component, the {@link #registerComponent(String, ComponentHolder)}
     * method replaces the value of the named entry with the actual
     * {@link ComponentHolder}.
     *
     * @see #checkComponentName(String)
     * @see #registerComponentHolder(String, ComponentHolder)
     * @see #unregisterComponentHolder(String)
     */
    private final Map m_componentHoldersByName;

    /**
     * Map of components by component ID. This map indexed by the component
     * ID number (<code>java.lang.Long</code>) contains the actual
     * {@link AbstractComponentManager} instances existing in the system.
     *
     * @see #registerComponentId(AbstractComponentManager)
     * @see #unregisterComponentId(long)
     */
    private final Map m_componentsById;

    /**
     * Counter to setup the component IDs as issued by the
     * {@link #registerComponentId(AbstractComponentManager)} method. This
     * counter is only incremented.
     */
    private volatile long m_componentCounter;

    /**
     * The OSGi service registration for the ScrService provided by this
     * instance.
     */
    private ServiceRegistration m_registration;


    protected ComponentRegistry( BundleContext context )
    {
        m_componentHoldersByName = new HashMap();
        m_componentsById = new HashMap();
        m_componentCounter = -1;

        // register as ScrService
        Dictionary props = new Hashtable();
        props.put( Constants.SERVICE_DESCRIPTION, "Declarative Services Management Agent" );
        props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
        m_registration = context.registerService( new String[]
            { ScrService.class.getName(), }, this, props );
    }


    public void dispose()
    {
        if ( m_registration != null )
        {
            m_registration.unregister();
            m_registration = null;
        }
    }


    //---------- ScrService interface

    public Component[] getComponents()
    {
        Object[] holders = getComponentHolders();
        ArrayList list = new ArrayList();
        for ( int i = 0; i < holders.length; i++ )
        {
            if ( holders[i] instanceof ComponentHolder )
            {
                Component[] components = ( ( ComponentHolder ) holders[i] ).getComponents();
                for ( int j = 0; j < components.length; j++ )
                {
                    list.add( components[j] );
                }
            }
        }

        // nothing to return
        if ( list.isEmpty() )
        {
            return null;
        }

        return ( Component[] ) list.toArray( new Component[list.size()] );
    }


    public Component[] getComponents( Bundle bundle )
    {
        Object[] holders = getComponentHolders();
        ArrayList list = new ArrayList();
        for ( int i = 0; i < holders.length; i++ )
        {
            if ( holders[i] instanceof ComponentHolder )
            {
                ComponentHolder holder = ( ComponentHolder ) holders[i];
                BundleComponentActivator activator = holder.getActivator();
                if ( activator != null && activator.getBundleContext().getBundle() == bundle )
                {
                    Component[] components = holder.getComponents();
                    for ( int j = 0; j < components.length; j++ )
                    {
                        list.add( components[j] );
                    }
                }
            }
        }

        // nothing to return
        if ( list.isEmpty() )
        {
            return null;
        }

        return ( Component[] ) list.toArray( new Component[list.size()] );
    }


    public Component getComponent( long componentId )
    {
        synchronized ( m_componentsById )
        {
            return ( Component ) m_componentsById.get( new Long( componentId ) );
        }
    }


    public Component[] getComponents( String componentName )
    {
        final ComponentHolder holder = getComponentHolder( componentName );
        if ( holder != null )
        {
            return holder.getComponents();
        }

        return null;
    }


    //---------- ComponentManager registration by component Id

    /**
     * Assigns a unique ID to the component, internally registers the
     * component under that ID and returns the assigned component ID.
     *
     * @param componentManager The {@link AbstractComponentManager} for which
     *      to assign a component ID and which is to be internally registered
     *
     * @return the assigned component ID
     */
    final long registerComponentId( final AbstractComponentManager componentManager )
    {
        long componentId;
        synchronized ( m_componentsById )
        {
            m_componentCounter++;
            componentId = m_componentCounter;

            m_componentsById.put( new Long( componentId ), componentManager );
        }

        return componentId;
    }


    /**
     * Unregisters the component with the given component ID from the internal
     * registry. After unregistration, the component ID should be considered
     * invalid.
     *
     * @param componentId The ID of the component to be removed from the
     *      internal component registry.
     */
    final void unregisterComponentId( final long componentId )
    {
        synchronized ( m_componentsById )
        {
            m_componentsById.remove( new Long( componentId ) );
        }
    }


    //---------- ComponentHolder registration by component name

    /**
     * Checks whether the component name is "globally" unique or not. If it is
     * unique, it is reserved until the actual component is registered with
     * {@link #registerComponent(String, AbstractComponentManager)} or until
     * it is unreserved by calling {@link #unregisterComponent(String)}.
     * If a component with the same name has already been reserved or registered
     * a ComponentException is thrown with a descriptive message.
     *
     * @param name the component name to check and reserve
     * @throws ComponentException if the name is already in use by another
     *      component.
     */
    final void checkComponentName( String name )
    {
        // register the name if no registration for that name exists already
        final Object existingRegistration;
        synchronized ( m_componentHoldersByName )
        {
            existingRegistration = m_componentHoldersByName.get( name );
            if ( existingRegistration == null )
            {
                m_componentHoldersByName.put( name, name );
            }
        }

        // there was a registration already, throw an exception and use the
        // existing registration to provide more information if possible
        if ( existingRegistration != null )
        {
            String message = "The component name '" + name + "' has already been registered";

            if ( existingRegistration instanceof ComponentHolder )
            {
                ComponentHolder c = ( ComponentHolder ) existingRegistration;
                Bundle cBundle = c.getActivator().getBundleContext().getBundle();
                ComponentMetadata cMeta = c.getComponentMetadata();

                StringBuffer buf = new StringBuffer( message );
                buf.append( " by Bundle " ).append( cBundle.getBundleId() );
                if ( cBundle.getSymbolicName() != null )
                {
                    buf.append( " (" ).append( cBundle.getSymbolicName() ).append( ")" );
                }
                buf.append( " as Component of Class " ).append( cMeta.getImplementationClassName() );
                message = buf.toString();
            }

            throw new ComponentException( message );
        }
    }


    /**
     * Registers the given component under the given name. If the name has not
     * already been reserved calling {@link #checkComponentName(String)} this
     * method throws a {@link ComponentException}.
     *
     * @param name The name to register the component under
     * @param component The component to register
     *
     * @throws ComponentException if the name has not been reserved through
     *      {@link #checkComponentName(String)} yet.
     */
    final void registerComponentHolder( String name, ComponentHolder component )
    {
        synchronized ( m_componentHoldersByName )
        {
            // only register the component if there is a m_registration for it !
            if ( !name.equals( m_componentHoldersByName.get( name ) ) )
            {
                // this is not expected if all works ok
                throw new ComponentException( "The component name '" + name + "' has already been registered." );
            }

            m_componentHoldersByName.put( name, component );
        }
    }


    /**
     * Returns the component registered under the given name or <code>null</code>
     * if no component is registered yet.
     */
    public final ComponentHolder getComponentHolder( String name )
    {
        Object entry;
        synchronized ( m_componentHoldersByName )
        {
            entry = m_componentHoldersByName.get( name );
        }

        // only return the entry if non-null and not a reservation
        if ( entry instanceof ComponentHolder )
        {
            return ( ComponentHolder ) entry;
        }

        return null;
    }


    /**
     * Returns an array of all values currently stored in the component holders
     * map. The entries in the array are either String types for component
     * name reservations or {@link ComponentHolder} instances for actual
     * holders of components.
     */
    private Object[] getComponentHolders()
    {
        synchronized ( m_componentHoldersByName )
        {
            return m_componentHoldersByName.values().toArray();
        }
    }


    /**
     * Removes the component registered under that name. If no component is
     * yet registered but the name is reserved, it is unreserved.
     * <p>
     * After calling this method, the name can be reused by other components.
     */
    final void unregisterComponentHolder( String name )
    {
        synchronized ( m_componentHoldersByName )
        {
            m_componentHoldersByName.remove( name );
        }
    }


    //---------- base configuration support

    /**
     * Factory method to issue {@link ComponentHolder} instances to manage
     * components described by the given component <code>metadata</code>.
     */
    public ComponentHolder createComponentHolder( BundleComponentActivator activator, ComponentMetadata metadata )
    {
        if ( metadata.isFactory() )
        {
            // 112.2.4 SCR must register a Component Factory
            // service on behalf ot the component
            // as soon as the component factory is satisfied
            return new ComponentFactoryImpl( activator, metadata );
        }

        return new UnconfiguredComponentHolder( activator, metadata );
    }


    //---------- Helper method

    /**
     * Returns <code>true</code> if the <code>bundle</code> is to be considered
     * active from the perspective of declarative services.
     * <p>
     * As of R4.1 a bundle may have lazy activation policy which means a bundle
     * remains in the STARTING state until a class is loaded from that bundle
     * (unless that class is declared to not cause the bundle to start). And
     * thus for DS 1.1 this means components are to be loaded for lazily started
     * bundles being in the STARTING state (after the LAZY_ACTIVATION event) has
     * been sent.  Hence DS must consider a bundle active when it is really
     * active and when it is a lazily activated bundle in the STARTING state.
     *
     * @param bundle The bundle check
     * @return <code>true</code> if <code>bundle</code> is not <code>null</code>
     *          and the bundle is either active or has lazy activation policy
     *          and is in the starting state.
     *
     * @see <a href="https://issues.apache.org/jira/browse/FELIX-1666">FELIX-1666</a>
     */
    static boolean isBundleActive( final Bundle bundle )
    {
        if ( bundle != null )
        {
            if ( bundle.getState() == Bundle.ACTIVE )
            {
                return true;
            }

            if ( bundle.getState() == Bundle.STARTING )
            {
                // according to the spec the activationPolicy header is only
                // set to request a bundle to be lazily activated. So in this
                // simple check we just verify the header is set to assume
                // the bundle is considered a lazily activated bundle
                return bundle.getHeaders().get( Constants.BUNDLE_ACTIVATIONPOLICY ) != null;
            }
        }

        // fall back: bundle is not considered active
        return false;
    }
}
