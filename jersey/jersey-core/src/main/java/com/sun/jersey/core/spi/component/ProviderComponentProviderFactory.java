/*
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://jersey.dev.java.net/CDDL+GPL.html
 * or jersey/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at jersey/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.jersey.core.spi.component;

import com.sun.jersey.spi.inject.InjectableProviderContext;
import com.sun.jersey.spi.service.ComponentContext;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Paul.Sandoz@Sun.Com
 */
public class ProviderComponentProviderFactory implements ComponentProviderFactory<ComponentProvider> {
    private static final Logger LOGGER = Logger.getLogger(ProviderComponentProviderFactory.class.getName());

    private static final class SingletonComponentProvider implements ComponentProvider {
        private final Object o;

        private final ComponentInjector ci;

        SingletonComponentProvider(ComponentInjector ci, Object o) {
            this.ci = ci;
            this.o = o;
        }
        
        public Object getInstance() {
            return o;
        }

        public void inject() {
            ci.inject(o);
        }
    }
    
    private final Map<Class, SingletonComponentProvider> cache =
            new HashMap<Class, SingletonComponentProvider>();

    private final InjectableProviderContext ipc;

    public ProviderComponentProviderFactory(InjectableProviderContext ipc) {
        this.ipc = ipc;
    }

    public ComponentProvider getComponentProvider(Class c) {
        SingletonComponentProvider cp = cache.get(c);
        if (cp != null) return cp;

        try {
            Object o = getInstance(c);

            ComponentInjector ci = new ComponentInjector(ipc, c);
            ci.inject(o);
            
            cp = new SingletonComponentProvider(ci, o);
            cache.put(c, cp);
            return cp;
        } catch (NoClassDefFoundError ex) {
            // Dependent class of provider not found
            // This assumes that ex.getLocalizedMessage() returns
            // the name of a dependent class that is not found
            LOGGER.log(Level.CONFIG,
                    "A dependent class, " + ex.getLocalizedMessage() +
                    ", of the component " + c + " is not found." +
                    " The component is ignored.");
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println(ex.getLocalizedMessage());
            LOGGER.log(Level.CONFIG,
                    "The provider class, " + c +
                    ", could not be instantiated");
            return null;
        }
    }

    public ComponentProvider getComponentProvider(ComponentContext cc, Class c) {
        return getComponentProvider(c);
    }

    public void injectOnAllComponents() {
        for (SingletonComponentProvider scp : cache.values()) {
            scp.inject();
        }
    }

    public void injectOnProviderInstances(Collection<?> providers) {
        for (Object o : providers) {
            injectOnProviderInstance(o);
        }
    }

    public void injectOnProviderInstance(Object provider) {
        Class c = provider.getClass();
        ComponentInjector ci = new ComponentInjector(ipc, c);
        ci.inject(provider);
    }

    protected Object getInstance(Class c)
            throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        ComponentConstructor cc = new ComponentConstructor(ipc, c);
        return cc.getInstance();
    }
}