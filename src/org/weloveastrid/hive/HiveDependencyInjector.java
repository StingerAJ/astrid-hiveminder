/**
 * See the file "LICENSE" for the full license governing this code.
 */
package org.weloveastrid.hive;

import org.weloveastrid.hive.data.HiveListService;
import org.weloveastrid.hive.data.HiveMetadataService;

import com.todoroo.andlib.service.AbstractDependencyInjector;
import com.todoroo.andlib.service.DependencyInjectionService;
import com.todoroo.andlib.service.ExceptionService.ErrorReporter;

/**
 * Hiveminder Dependency Injection for service classes
 *
 * @author Tim Su <tim@todoroo.com>
 *
 */
public class HiveDependencyInjector extends AbstractDependencyInjector {

    /**
     * variable to prevent multiple copies of this injector to be loaded
     */
    private static HiveDependencyInjector instance = null;

    /**
     * Initialize list of injectables. Special care must used when
     * instantiating classes that themselves depend on dependency injection
     * (i.e. {@link ErrorReporter}.
     */
    @Override
    @SuppressWarnings("nls")
    protected void addInjectables() {
        injectables.put("hiveMetadataService", HiveMetadataService.class);
        injectables.put("hiveListService", HiveListService.class);
    }

    /**
     * Install this dependency injector
     */
    public static void initialize() {
        if(instance != null)
            return;
        synchronized(HiveDependencyInjector.class) {
            if(instance == null)
                instance = new HiveDependencyInjector();
            DependencyInjectionService.getInstance().addInjector(instance);
        }
    }

    HiveDependencyInjector() {
        // prevent instantiation
        super();
    }

}
